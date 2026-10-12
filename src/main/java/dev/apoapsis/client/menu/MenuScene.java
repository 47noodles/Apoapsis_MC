package dev.apoapsis.client.menu;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ui.UiBackdrop;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiScale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/**
 * The shared backdrop of the title and loading screens: stars, Verdance's limb filling the lower right, and Pale
 * above it, both turning slowly. The planets keep their phase between screens, so going from the title to a loading
 * screen does not restart the turn. Client only.
 */
final class MenuScene {
    private static final Identifier VERDANCE_MAP =
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "textures/gui/verdance_map.png");
    private static final Identifier PALE_MAP =
            Identifier.fromNamespaceAndPath(Apoapsis.MODID, "textures/gui/pale_map.png");
    /** Turns per second. */
    private static final double VERDANCE_RATE = 1.0 / 180;
    private static final double PALE_RATE = 1.0 / 90;
    private static final long START = System.nanoTime();

    private static PlanetPip.Mesh verdance;
    private static PlanetPip.Mesh pale;
    private static int meshW;
    private static int meshH;
    private static float meshScale;

    private MenuScene() {}

    /** Draws the stars and the planets into the current stratum. */
    static void draw(GuiGraphicsExtractor g) {
        UiBackdrop.draw(g);
        int w = g.guiWidth();
        int h = g.guiHeight();
        UiScale s = UiScale.current();
        if (verdance == null || meshW != w || meshH != h || meshScale != s.scale()) {
            float vw = s.width();
            float vh = s.height();
            // Verdance: a large limb whose top sits about 60 percent down, clear of the menu column on the left.
            float r = vh * 0.9f;
            verdance = new PlanetPip.Mesh(s.gui(vw * 0.84f), s.gui(vh * 1.5f), r * s.scale(), 48,
                    -0.62, 0.48, 0.62, UiPalette.alpha(0xFF7EC8FF, 215));
            pale = new PlanetPip.Mesh(s.gui(vw * 0.83f), s.gui(vh * 0.25f), 26 * s.scale(), 20,
                    -0.70, 0.38, 0.60, 0);
            meshW = w;
            meshH = h;
            meshScale = s.scale();
        }
        double t = (System.nanoTime() - START) / 1e9;
        g.submitPictureInPictureRenderState(
                PlanetPip.State.of(pale, PALE_MAP, (float) ((t * PALE_RATE + 0.3) % 1.0), w, h));
        g.submitPictureInPictureRenderState(
                PlanetPip.State.of(verdance, VERDANCE_MAP, (float) ((t * VERDANCE_RATE) % 1.0), w, h));
    }
}
