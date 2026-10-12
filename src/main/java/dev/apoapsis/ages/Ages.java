package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.AgeSyncPayload;
import dev.apoapsis.net.PlayerMessages;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * SevTech-style Ages: progression gates on items. The Age is stored per player; the sandbox flag makes everyone MAX_AGE.
 * Gates live in {@link AgeGates} (interaction and tooltips) and {@code dev.apoapsis.mixin.CraftingMenuMixin} (crafting).
 */
public final class Ages {
    private Ages() {}

    public static final int MAX_AGE = 6;

    /** Age a new survival player starts at, from {@link #start}. */
    static final int START_AGE = 3;

    /** Milestone Age reached by {@link #onFirstFlight}. */
    static final int FIRST_FLIGHT_AGE = 4;

    /** The ship part recipes (data/apoapsis/recipe) a player is awarded at {@link #start}: the Age 3 parts. */
    private static final List<String> START_RECIPES = List.of("cockpit", "fin", "fuel_tank", "gyroscope",
            "hull_plate", "hydrolox_canister", "nose_cone", "spark_thruster", "window");

    public static void register(IEventBus modBus) {
        AgeData.ATTACHMENTS.register(modBus);
        AgeEvents.register();
        AgeGates.register();
        Guide.register();
        AgeCommands.register();
        GuideCommands.register();
        AgeItems.register(modBus);
    }

    /** The player's Age, 0..6. Works on both sides (client reads the synced value). */
    public static int getAge(Player player) {
        if (player.level().isClientSide()) {
            return ClientState.age;
        }
        if (sandbox(player.level())) {
            return MAX_AGE;
        }
        return stored(player);
    }

    /** Sets the Age, syncs it to the client, and announces it if it went up. */
    public static void setAge(ServerPlayer player, int age) {
        setAge(player, age, true);
    }

    /**
     * Sets the Age and syncs it to the client. The Custodian announces a rise only when {@code announce} is true. The
     * test scenarios pass false: they grant an Age without a flight having happened.
     */
    public static void setAge(ServerPlayer player, int age, boolean announce) {
        int next = Math.max(0, Math.min(MAX_AGE, age));
        int before = getAge(player);
        player.setData(AgeData.AGE.get(), next);
        sync(player);
        int after = getAge(player);
        if (announce && after > before) {
            AgeAnnounce.ageReached(player, after);
        }
    }

    /** The Age an item needs, 0 if it is not gated. */
    public static int requiredAge(ItemStack stack) {
        return stack.isEmpty() ? 0 : requiredAgeOf(stack.getItem());
    }

    public static boolean canUse(Player player, ItemStack stack) {
        return getAge(player) >= requiredAge(stack);
    }

    /**
     * Once per player: Age 3 (never lower than they have), the ship part recipes, and one Custodian line. Existing
     * players who have not started get it at their next login. Sandbox still reports MAX_AGE on top of this.
     * It also backfills PALE_LANDING for saves that already announced Pale, on every login. A new player then gets the
     * first guide step; a player from a save that began before the guide is marked as past the steps their progress
     * covers (see {@link Guide}), quietly.
     */
    public static void start(ServerPlayer player) {
        backfillPaleLanding(player);
        if (player.getData(AgeData.STARTED.get())) {
            Guide.catchUp(player);
            return;
        }
        player.setData(AgeData.STARTED.get(), true);
        setAge(player, Math.max(stored(player), START_AGE), false);
        List<ResourceKey<Recipe<?>>> recipes = new ArrayList<>();
        for (String name : START_RECIPES) {
            recipes.add(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(Apoapsis.MODID, name)));
        }
        player.awardRecipesByKey(recipes);
        PlayerMessages.age(player,
                Component.translatable("apoapsis.ages.format", String.valueOf(START_AGE),
                        Component.translatable("apoapsis.ages.name." + START_AGE)),
                Component.translatable("apoapsis.ages.line.start"));
        Guide.reach(player, Guide.Step.PARTS);
    }

    /**
     * Old saves showed the Pale line (PALE_ANNOUNCED) without recording PALE_LANDING. Record it now, quietly: the
     * line flag is false, so the Custodian says nothing.
     */
    static void backfillPaleLanding(ServerPlayer player) {
        if (player.getData(AgeData.PALE_ANNOUNCED.get()) && !Firsts.has(player, Firsts.First.PALE_LANDING)) {
            Firsts.record(player, Firsts.First.PALE_LANDING, false, false);
        }
    }

    /** Milestone: the player flew a ship above 1,500 m and landed it on Verdance (Age 3 to 4). */
    public static void onFirstFlight(ServerPlayer player) {
        // Uses the stored value: sandbox must not leak a raised Age into the save.
        setAge(player, Math.max(stored(player), FIRST_FLIGHT_AGE));
        // The Age 4 line already speaks for First Flight: record it (and its advancement) without a second line.
        Firsts.record(player, Firsts.First.FIRST_FLIGHT, false);
    }

    /**
     * Milestone: the player landed a ship on a body. Pale records PALE_LANDING; a Verdance landing after that records
     * CREWED_RETURN. A pale sample carried home then grants Age 5. PALE_ANNOUNCED keeps the old Pale line silent on
     * saves that already showed it.
     */
    public static void onLanded(ServerPlayer player, BodyId body) {
        if (body == BodyId.PALE) {
            boolean announced = player.getData(AgeData.PALE_ANNOUNCED.get());
            player.setData(AgeData.PALE_ANNOUNCED.get(), true);
            Firsts.record(player, Firsts.First.PALE_LANDING, !announced);
            return;
        }
        boolean paleLanding = Firsts.has(player, Firsts.First.PALE_LANDING);
        boolean sampleDue = Firsts.sampleAgeDue(paleLanding, body == BodyId.VERDANCE, Firsts.carriesSample(player));
        if (body == BodyId.VERDANCE && paleLanding) {
            // On the landing that brings a sample home, the sample's line speaks for the return as well.
            Firsts.record(player, Firsts.First.CREWED_RETURN, !sampleDue);
        }
        if (sampleDue) {
            Firsts.record(player, Firsts.First.PALE_SAMPLE);
            setAge(player, Math.max(stored(player), Firsts.SAMPLE_AGE));
        }
    }

    /** Display name, for example "Age 4 · Ascent". */
    public static Component ageName(int age) {
        int n = Math.max(0, Math.min(MAX_AGE, age));
        return Component.translatable("apoapsis.ages.format", String.valueOf(n),
                Component.translatable("apoapsis.ages.name." + n));
    }

    /**
     * The Age stored on the player, before sandbox. Public so the test scenarios store from the real Age, not the
     * sandbox one.
     */
    public static int stored(Player player) {
        return Math.max(0, Math.min(MAX_AGE, player.getData(AgeData.AGE.get())));
    }

    /** Whether this level's world has the sandbox flag on. */
    static boolean sandbox(Level level) {
        MinecraftServer server = level.getServer();
        return server != null && SandboxData.get(server).isEnabled();
    }

    /** Sends the player's effective Age to their client. */
    static void sync(ServerPlayer player) {
        // Fake players (GameTests, other mods' machines) have no client that knows our channel.
        if (player.connection != null && player.connection.hasChannel(AgeSyncPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new AgeSyncPayload(getAge(player)));
        }
    }

    /** Turns the sandbox flag on or off for the whole server and re-syncs every player. */
    static void setSandbox(MinecraftServer server, boolean on) {
        SandboxData.get(server).setEnabled(on);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sync(player);
        }
    }

    /** Age per registry id. Names are matched by string so this package has no compile dependency on the grid. */
    static int requiredAgeOf(Item item) {
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !Apoapsis.MODID.equals(id.getNamespace())) {
            return 0;
        }
        return switch (id.getPath()) {
            case "hull_plate", "cockpit", "gyroscope", "fuel_tank", "spark_thruster", "hydrolox_canister",
                    "nose_cone", "fin", "window" -> 3;
            case "fuel_refinery", "parachute", "landing_leg" -> 4;
            default -> 0;
        };
    }
}
