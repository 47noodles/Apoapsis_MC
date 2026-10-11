package dev.apoapsis.ages;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.GuideStepPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Custodian's survival guide: ten short steps, each shown once, at the moment it becomes relevant, on the client's
 * guide panel (a small panel in the top right, clear of the flight instruments; chat for a client without it). A step
 * opens on a first ({@link Firsts}), on the login start, on crafting a cockpit, on first boarding a ship, or on coming
 * back into Verdance's sphere of influence. The shown steps are stored per player in {@link AgeData#GUIDE}; the text
 * lives in {@code lang/en_us/guide.json}. {@code /apoapsis guide} shows the current step again.
 */
public final class Guide {
    private Guide() {}

    /** The steps, in the order of the loop. {@link #id()} is the stored entry and the lang key part. */
    public enum Step {
        PARTS(null), BUILD(null), LAUNCH(null),
        ASCENT(Firsts.First.FIRST_FLIGHT), ORBIT(Firsts.First.SUBORBITAL), MAP(Firsts.First.ORBIT),
        PALE(Firsts.First.PALE_FLYBY), SAMPLE(Firsts.First.PALE_LANDING),
        HOME(null), DONE(Firsts.First.PALE_SAMPLE);

        /** The first whose recording opens this step, or null for a step another event opens. */
        private final Firsts.First opener;

        Step(Firsts.First opener) {
            this.opener = opener;
        }

        public String id() {
            return name().toLowerCase(Locale.ROOT);
        }

        /** 1-based position, as shown in the header. */
        public int number() {
            return ordinal() + 1;
        }

        /** The lang key of this step's title; its body lines are {@code apoapsis.guide.<id>.1} to {@code .LINES}. */
        public String titleKey() {
            return "apoapsis.guide." + id() + ".title";
        }

        /** The lang key of body line {@code line} (1-based). */
        public String lineKey(int line) {
            return "apoapsis.guide." + id() + "." + line;
        }

        /** The keys the step's text names, as arguments for its lines, in order. They show the player's own bindings. */
        public Object[] args() {
            return switch (this) {
                case LAUNCH -> keys("key.apoapsis.throttle_up", "key.apoapsis.throttle_full",
                        "key.apoapsis.throttle_cut", "key.forward", "key.back", "key.left", "key.right",
                        "key.apoapsis.roll_left", "key.apoapsis.roll_right", "key.apoapsis.sas");
                case ORBIT -> keys("key.apoapsis.sas", "key.apoapsis.throttle_cut");
                case MAP -> keys("key.apoapsis.system_map", "key.apoapsis.warp_up", "key.apoapsis.warp_down");
                case PALE -> keys("key.apoapsis.sas");
                case SAMPLE -> keys("key.apoapsis.disassemble", "key.apoapsis.system_map");
                case HOME -> keys("key.apoapsis.sas", "key.apoapsis.deploy_parachutes");
                default -> new Object[0];
            };
        }

        /** The step with this stored id, or null. */
        static Step byId(String id) {
            for (Step step : values()) {
                if (step.id().equals(id)) {
                    return step;
                }
            }
            return null;
        }
    }

    /** Body lines per step. */
    public static final int LINES = 2;

    /** The number of steps. */
    public static final int TOTAL = Step.values().length;

    private static final Identifier COCKPIT = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "cockpit");

    static void register() {
        NeoForge.EVENT_BUS.addListener(Guide::onCrafted);
    }

    /** BUILD: the player crafted a cockpit, so how to put a ship together is the next thing they need. */
    static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        ItemStack crafted = event.getCrafting();
        if (event.getEntity() instanceof ServerPlayer player
                && COCKPIT.equals(BuiltInRegistries.ITEM.getKey(crafted.getItem()))) {
            reach(player, Step.BUILD);
        }
    }

    /**
     * Shows the step if the player has not seen it yet and records it.
     *
     * @return whether the step was shown now
     */
    public static boolean reach(ServerPlayer player, Step step) {
        if (has(player, step)) {
            return false;
        }
        mark(player, step);
        show(player, step);
        return true;
    }

    /**
     * Shows the step: on the client's guide panel, where it stays clear of the flight instruments, or, for a client that
     * does not know the panel's channel (a fake player, another mod's machine), as a header and its body lines in chat.
     * Always, even if the player has seen it.
     */
    public static void show(ServerPlayer player, Step step) {
        if (player.connection != null && player.connection.hasChannel(GuideStepPayload.TYPE)) {
            PacketDistributor.sendToPlayer(player, new GuideStepPayload(step.ordinal()));
            return;
        }
        AgeAnnounce.custodian(player, header(step).withStyle(ChatFormatting.GRAY));
        for (int line = 1; line <= LINES; line++) {
            player.sendSystemMessage(Component.literal("  ").append(line(step, line)));
        }
    }

    /** The step's header: its number, the total and its title. */
    public static MutableComponent header(Step step) {
        return Component.translatable("apoapsis.guide.header", String.valueOf(step.number()), String.valueOf(TOTAL),
                Component.translatable(step.titleKey()));
    }

    /** Body line {@code line} (1-based) of the step, with the player's own key bindings named. */
    public static MutableComponent line(Step step, int line) {
        return Component.translatable(step.lineKey(line), step.args());
    }

    /** Whether the player has been shown the step. */
    public static boolean has(Player player, Step step) {
        return player.getData(AgeData.GUIDE.get()).contains(step.id());
    }

    /** The latest step the player has reached in the loop's order; {@link Step#PARTS} before any. */
    public static Step current(Player player) {
        Step latest = Step.PARTS;
        for (String id : player.getData(AgeData.GUIDE.get())) {
            Step step = Step.byId(id);
            if (step != null && step.ordinal() > latest.ordinal()) {
                latest = step;
            }
        }
        return latest;
    }

    /** Called when a first is recorded: opens the step that follows it, if there is one. */
    static void onFirst(ServerPlayer player, Firsts.First first) {
        for (Step step : Step.values()) {
            if (step.opener == first) {
                reach(player, step);
            }
        }
    }

    /** LAUNCH: someone took the pilot's seat of a ship. Does nothing for anything but a player. */
    public static void onBoard(Entity rider) {
        if (rider instanceof ServerPlayer player) {
            reach(player, Step.LAUNCH);
        }
    }

    /** HOME: the ship came back into Verdance's sphere of influence after Pale. */
    public static void homeward(Entity pilot) {
        if (pilot instanceof ServerPlayer player && Firsts.has(player, Firsts.First.PALE_FLYBY)) {
            reach(player, Step.HOME);
        }
    }

    /**
     * For a save that began before the guide: the player has already started, so mark the steps their progress covers as
     * shown, quietly. Does nothing once any step is recorded, so a new player still gets every step in turn.
     */
    static void catchUp(ServerPlayer player) {
        if (!player.getData(AgeData.GUIDE.get()).isEmpty()) {
            return;
        }
        for (Step step : Step.values()) {
            Firsts.First proof = step == Step.HOME ? Firsts.First.CREWED_RETURN : step.opener;
            if (proof == null || Firsts.has(player, proof)) {
                mark(player, step);
            }
        }
    }

    private static void mark(ServerPlayer player, Step step) {
        List<String> done = new ArrayList<>(player.getData(AgeData.GUIDE.get()));
        done.add(step.id());
        player.setData(AgeData.GUIDE.get(), List.copyOf(done));
    }

    private static Object[] keys(String... names) {
        Object[] out = new Object[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = Component.keybind(names[i]).withStyle(ChatFormatting.YELLOW);
        }
        return out;
    }
}
