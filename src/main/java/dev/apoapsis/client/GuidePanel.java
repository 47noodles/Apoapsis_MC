package dev.apoapsis.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Guide;
import dev.apoapsis.client.hud.HudKeycap;
import dev.apoapsis.client.hud.HudLayout;
import dev.apoapsis.client.ship.PilotKeys;
import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiTextSmooth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The Custodian's guide step as a small calm block at the top right, under the regime label: the step's number and
 * title, one short line, and keycaps for the keys the step names (the player's own bindings). It is built on the UI
 * kit in UI units, so it reads the same at every GUI scale. It shows for {@link #SHOW_TICKS} ticks, fading over the
 * last {@link #FADE_TICKS}; the long text stays in the guide book and chat. The server sends the step
 * ({@code GuideStepPayload}); the clock runs only while no screen is open, so a step that arrives under the map is
 * still there when the map closes, and a step that arrives under an Age banner waits for it through {@code Notify}.
 * Client only.
 */
public final class GuidePanel {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "guide_panel");
    /** Client ticks a step stays: 25 seconds. */
    static final int SHOW_TICKS = 25 * 20;
    /** The last ticks of a step, over which it fades out. */
    static final int FADE_TICKS = 2 * 20;
    /** Inner padding left and right of the text, UI units. */
    private static final int PAD_X = 8;
    /** Inner padding above and below the text, UI units. */
    private static final int PAD_Y = 7;
    /** Width of the brass rule down the left edge, UI units. */
    private static final int RULE_W = 2;
    /** Row pitch of the header and the body lines, UI units. */
    private static final int PITCH = 12;
    /** Gap between the last body line and the keycap row, UI units. */
    private static final int KEY_ROW_GAP = 5;
    /** Gap between a keycap and its caption, UI units. */
    private static final int CAP_GAP = 5;
    /** Gap between the two keycaps of a pair, UI units. */
    private static final int PAIR_GAP = 2;
    /** Gap between keycap groups, UI units. */
    private static final int GROUP_GAP = 10;
    /** The narrowest block, UI units. */
    private static final int MIN_W = 150;
    /** The widest the short line wraps, UI units, and the largest share of the screen width it takes. */
    private static final int MAX_TEXT_W = 190;
    private static final float MAX_SHARE = 0.32f;
    /** The backdrop's alpha: the steel reads through a little. */
    private static final int BACKDROP_ALPHA = 0xB0;
    private static final String CAP = "apoapsis.guide.cap.";

    /** One keycap group: the keys it shows (a pair when there are two), then the lang key of its caption. */
    private record Group(String captionKey, KeyMapping... keys) {}

    /** The keys each step names, in the order its text names them. Steps with no keys have no entry. */
    private static final Map<Guide.Step, List<Group>> KEYS = new EnumMap<>(Guide.Step.class);

    static {
        KEYS.put(Guide.Step.LAUNCH, List.of(
                new Group(CAP + "throttle", PilotKeys.THROTTLE_UP, PilotKeys.THROTTLE_DOWN),
                new Group(CAP + "full", PilotKeys.THROTTLE_FULL),
                new Group(CAP + "cut", PilotKeys.THROTTLE_CUT),
                new Group(CAP + "sas", PilotKeys.SAS)));
        KEYS.put(Guide.Step.ORBIT, List.of(
                new Group(CAP + "sas", PilotKeys.SAS),
                new Group(CAP + "cut", PilotKeys.THROTTLE_CUT)));
        KEYS.put(Guide.Step.MAP, List.of(
                new Group(CAP + "map", PilotKeys.SYSTEM_MAP),
                new Group(CAP + "warp", PilotKeys.WARP_DOWN, PilotKeys.WARP_UP)));
        KEYS.put(Guide.Step.PALE, List.of(
                new Group(CAP + "sas", PilotKeys.SAS)));
        KEYS.put(Guide.Step.SAMPLE, List.of(
                new Group(CAP + "disassemble", PilotKeys.DISASSEMBLE),
                new Group(CAP + "map", PilotKeys.SYSTEM_MAP)));
        KEYS.put(Guide.Step.HOME, List.of(
                new Group(CAP + "sas", PilotKeys.SAS),
                new Group(CAP + "chutes", PilotKeys.DEPLOY_PARACHUTES)));
    }

    private static Guide.Step step;
    private static int ticksLeft;

    private GuidePanel() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(GuidePanel::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(GuidePanel::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(GuidePanel::onLoggingOut);
    }

    /** Shows a step, replacing any on show. An unknown number is ignored. */
    public static void show(int stepNumber) {
        Guide.Step[] steps = Guide.Step.values();
        if (stepNumber >= 0 && stepNumber < steps.length) {
            step = steps[stepNumber];
            ticksLeft = SHOW_TICKS;
        }
    }

    /** Whether a step is on show. For tests. */
    public static boolean showing() {
        return step != null && ticksLeft > 0;
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER, GuidePanel::render);
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (ticksLeft > 0 && mc.level != null && mc.gui.screen() == null && !mc.isPaused()) {
            ticksLeft--;
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        step = null;
        ticksLeft = 0;
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (!showing() || mc.gui.screen() != null || mc.gui.hud.isHidden()) {
            return;
        }
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        double fade = Math.min(1.0, Math.max(0.0, (ticksLeft - partialTick) / FADE_TICKS));
        if (fade <= 0.02) {
            return;
        }
        float f = (float) fade;

        HudLayout lay = HudLayout.current();
        UiScale s = lay.scale();
        String header = Component.translatable("apoapsis.guide.step", step.number(), Guide.TOTAL,
                Component.translatable(step.titleKey()).getString()).getString();
        List<String> lines = wrap(Component.translatable("apoapsis.guide." + step.id() + ".short").getString(),
                Math.min(MAX_TEXT_W, lay.width() * MAX_SHARE));
        List<Group> groups = KEYS.getOrDefault(step, List.of());

        float content = UiTextSmooth.width(UiText.Type.LABEL, header);
        for (String line : lines) {
            content = Math.max(content, UiTextSmooth.width(UiText.Type.BODY, line));
        }
        content = Math.max(content, groupsWidth(groups));
        int right = lay.rightAt(HudLayout.BELOW_REGIME);
        int left = Math.round(right - Math.max(MIN_W, content + 2 * PAD_X + RULE_W));
        int top = HudLayout.BELOW_REGIME;
        int headerY = top + PAD_Y;
        int lastLineY = headerY + PITCH * lines.size();
        int textBottom = lastLineY + UiText.Type.BODY.capHeight();
        int keyY = textBottom + KEY_ROW_GAP;
        int bottom = groups.isEmpty() ? textBottom + PAD_Y : keyY + UiKeycap.HEIGHT + PAD_Y;

        s.begin(g, 0, 0);
        g.fill(left, top, right, bottom, UiPalette.fade(UiPalette.alpha(UiPalette.STEEL_950, BACKDROP_ALPHA), f));
        g.fill(left, top, left + RULE_W, bottom, UiPalette.fade(UiPalette.BRASS, f));
        int x = left + RULE_W + PAD_X;
        UiTextSmooth.draw(g, UiText.Type.LABEL, header, x, headerY, UiPalette.fade(UiPalette.BRASS, f));
        for (int i = 0; i < lines.size(); i++) {
            UiTextSmooth.draw(g, UiText.Type.BODY, lines.get(i), x, headerY + PITCH * (i + 1),
                    UiPalette.fade(UiPalette.INK, f));
        }
        if (!groups.isEmpty()) {
            drawGroups(g, groups, x, keyY, f);
        }
        s.end(g);
    }

    /** Word-wraps {@code text} to {@code maxW} UI units in the body type. */
    private static List<String> wrap(String text, float maxW) {
        List<String> lines = new ArrayList<>();
        String current = "";
        for (String word : text.split(" ")) {
            String next = current.isEmpty() ? word : current + " " + word;
            if (!current.isEmpty() && UiTextSmooth.width(UiText.Type.BODY, next) > maxW) {
                lines.add(current);
                current = word;
            } else {
                current = next;
            }
        }
        if (!current.isEmpty()) {
            lines.add(current);
        }
        return lines;
    }

    /** The width of a row of keycap groups, UI units. */
    private static float groupsWidth(List<Group> groups) {
        float w = 0;
        for (Group group : groups) {
            w += groupWidth(group);
        }
        return groups.isEmpty() ? 0 : w + GROUP_GAP * (groups.size() - 1);
    }

    private static float groupWidth(Group group) {
        float keys = 0;
        for (KeyMapping key : group.keys()) {
            keys += UiKeycap.width(UiKeycap.label(key));
        }
        keys += PAIR_GAP * (group.keys().length - 1);
        return keys + CAP_GAP + UiTextSmooth.width(UiText.Type.LABEL, Component.translatable(group.captionKey()).getString());
    }

    /** Draws the keycap groups left to right from {@code x}, their keycaps' tops at {@code y}. */
    private static void drawGroups(GuiGraphicsExtractor g, List<Group> groups, float x, int y, float f) {
        float cx = x;
        for (int i = 0; i < groups.size(); i++) {
            Group group = groups.get(i);
            for (int k = 0; k < group.keys().length; k++) {
                cx += HudKeycap.cap(g, Math.round(cx), y, UiKeycap.label(group.keys()[k]), f);
                if (k == 0 && group.keys().length > 1) {
                    cx += PAIR_GAP;
                }
            }
            cx += CAP_GAP;
            String caption = Component.translatable(group.captionKey()).getString();
            UiTextSmooth.draw(g, UiText.Type.LABEL, caption, cx, y + 3, UiPalette.fade(UiPalette.INK_MUTED, f));
            cx += UiTextSmooth.width(UiText.Type.LABEL, caption);
            if (i < groups.size() - 1) {
                cx += GROUP_GAP;
            }
        }
    }
}
