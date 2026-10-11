package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ClientState;
import dev.apoapsis.flight.BodyId;
import dev.apoapsis.net.AgeSyncPayload;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    /** Milestone Age reached by {@link #onFirstFlight}. */
    static final int FIRST_FLIGHT_AGE = 4;

    public static void register(IEventBus modBus) {
        AgeData.ATTACHMENTS.register(modBus);
        AgeEvents.register();
        AgeGates.register();
        AgeCommands.register();
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

    /** Milestone: the player flew a ship they built above 1,500 m (Age 3 to 4). */
    public static void onFirstFlight(ServerPlayer player) {
        // Uses the stored value: sandbox must not leak a raised Age into the save.
        setAge(player, Math.max(stored(player), FIRST_FLIGHT_AGE));
    }

    /** Milestone: the player landed a ship on a body. */
    public static void onLanded(ServerPlayer player, BodyId body) {
        if (body != BodyId.PALE || player.getData(AgeData.PALE_ANNOUNCED.get())) {
            return;
        }
        player.setData(AgeData.PALE_ANNOUNCED.get(), true);
        AgeAnnounce.custodian(player, Component.translatable("apoapsis.ages.line.pale"));
    }

    /** Display name, for example "Age 4 · Ascent". */
    public static Component ageName(int age) {
        int n = Math.max(0, Math.min(MAX_AGE, age));
        return Component.translatable("apoapsis.ages.format", String.valueOf(n),
                Component.translatable("apoapsis.ages.name." + n));
    }

    /** The Age stored on the player, before sandbox. */
    static int stored(Player player) {
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
            default -> 0;
        };
    }
}
