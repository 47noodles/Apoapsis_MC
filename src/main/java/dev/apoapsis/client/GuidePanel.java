package dev.apoapsis.client;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ages.Guide;
import dev.apoapsis.client.map.Palette;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

/**
 * The Custodian's guide step as a small panel in the top right, for {@link #SHOW_TICKS} ticks, fading over the last
 * {@link #FADE_TICKS}. A step used to go to chat, where its six to nine rows sat over the flight instruments at the
 * bottom of the view; the top right is clear of them, of the view's centre and of the keycap hints at the left. The
 * server sends the step ({@code GuideStepPayload}); the text is the client's own lang entries. The clock runs only
 * while no screen is open, so a step that arrives under the map is still there when the map closes. Client only.
 */
public final class GuidePanel {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "guide_panel");
    /** Client ticks a step stays: 25 seconds, enough to read nine rows. */
    static final int SHOW_TICKS = 25 * 20;
    /** The last ticks of a step, over which it fades out. */
    static final int FADE_TICKS = 2 * 20;
    private static final int PAD = 10;
    /**
     * The panel's top, GUI px: below the regime label at the top right (PAD to PAD + 9) and the 32 px slot that vanilla
     * toasts (advancements, recipes) take there.
     */
    private static final int TOP = 38;
    /** The panel's text is a little smaller than chat's, so nine rows stay a small corner of the view. */
    private static final float TEXT_SCALE = 0.8f;
    /** The widest the text wraps, GUI px, and the largest share of the GUI width it takes. */
    private static final int MAX_WIDTH = 200;
    private static final double MAX_SHARE = 0.4;
    private static final int INSET = 5;
    private static final int ROW = 9;
    private static final int GAP = 3;
    private static final int RULE_W = 2;
    private static final int FILL = Palette.alpha(Palette.STEEL_950, 0xB8);

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
        Font font = mc.font;
        // The layout is in font units under a pose scaled by TEXT_SCALE, so the wrap width is the GUI width over it.
        int wrap = (int) (Math.max(80, Math.min(MAX_WIDTH, g.guiWidth() * MAX_SHARE)) / TEXT_SCALE);
        List<FormattedCharSequence> head = font.split(Guide.header(step), wrap);
        List<FormattedCharSequence> first = font.split(Guide.line(step, 1), wrap);
        List<FormattedCharSequence> second = font.split(Guide.line(step, 2), wrap);

        int textW = 0;
        for (List<FormattedCharSequence> rows : List.of(head, first, second)) {
            for (FormattedCharSequence row : rows) {
                textW = Math.max(textW, font.width(row));
            }
        }
        int rows = head.size() + first.size() + second.size();
        int boxW = RULE_W + INSET + textW + INSET;
        int boxH = INSET + rows * ROW + 2 * GAP + INSET;
        int left = g.guiWidth() - PAD - (int) Math.ceil(boxW * TEXT_SCALE);

        g.pose().pushMatrix();
        g.pose().translate(left, TOP);
        g.pose().scale(TEXT_SCALE, TEXT_SCALE);
        g.fill(0, 0, boxW, boxH, faded(FILL, fade));
        g.fill(0, 0, RULE_W, boxH, faded(Palette.BRASS, fade));
        int x = RULE_W + INSET;
        int y = INSET;
        y = draw(g, font, head, x, y, faded(Palette.BRASS, fade)) + GAP;
        y = draw(g, font, first, x, y, faded(Palette.INK, fade)) + GAP;
        draw(g, font, second, x, y, faded(Palette.INK, fade));
        g.pose().popMatrix();
    }

    /** Draws the rows from {@code y} down and returns the y under the last one. */
    private static int draw(GuiGraphicsExtractor g, Font font, List<FormattedCharSequence> rows, int x, int y,
                            int argb) {
        for (FormattedCharSequence row : rows) {
            g.text(font, row, x, y, argb, true);
            y += ROW;
        }
        return y;
    }

    /** The colour with its alpha scaled by {@code fade} (0..1). */
    private static int faded(int argb, double fade) {
        int alpha = (int) Math.round(((argb >>> 24) & 0xFF) * fade);
        return Palette.alpha(argb, Math.max(alpha, 4));
    }
}
