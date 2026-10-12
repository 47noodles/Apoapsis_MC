package dev.apoapsis.client.ship;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.ship.ShipEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * A thin canopy frame at the top and sides of the cockpit view, so the pilot still feels inside a ship now that the
 * hull near the eye is cut away (see {@link ShipRenderer#COCKPIT_CUTAWAY}). Dark steel struts with a brass hairline on
 * the inner edge and a soft shadow, plain filled quads only. The bottom and the centre stay clear; the layer sits below
 * the crosshair and the flight HUD. Drawn only while the local player rides a ship in first person with no screen open
 * and the HUD not hidden. Client only.
 */
@EventBusSubscriber(modid = Apoapsis.MODID, value = Dist.CLIENT)
public final class CanopyFrame {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "canopy_frame");
    /** Dark riveted steel. */
    private static final int STEEL = 0xFF141A21;
    /** The lighter steel of the gusset's lit face. */
    private static final int STEEL_LIT = 0xFF1D252E;
    /** Brass trim, the colour of the HUD accents, kept dim. */
    private static final int BRASS = 0xFF8C6D33;
    /** Shadow the strut casts inward, fading to nothing. */
    private static final int SHADOW = 0x66000000;
    private static final int CLEAR = 0x00000000;
    /** Top strut depth and side post width, as a share of the screen height and width. */
    private static final double TOP = 0.030;
    private static final double SIDE = 0.011;
    /** The corner gusset's leg length as a share of the screen height. */
    private static final double GUSSET = 0.070;
    /** Shadow depth as a share of the screen height. */
    private static final double SHADE = 0.035;

    private CanopyFrame() {}

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CROSSHAIR, LAYER, CanopyFrame::render);
    }

    /** Whether the canopy shows: piloting in first person, no screen, HUD shown. */
    public static boolean visible(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gui.screen() == null && !mc.gui.hud.isHidden()
                && mc.options.getCameraType().isFirstPerson() && mc.player.getVehicle() instanceof ShipEntity;
    }

    private static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
        if (!visible(Minecraft.getInstance())) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        int top = Math.max(3, (int) Math.round(h * TOP));
        int side = Math.max(2, (int) Math.round(w * SIDE));
        int leg = (int) Math.round(h * GUSSET);
        int shade = (int) Math.round(h * SHADE);

        // Soft shadows first, so the struts sit over them.
        g.fillGradient(side, top, w - side, top + shade, SHADOW, CLEAR);
        int sideShade = Math.max(2, shade / 2);
        for (int i = 0; i < sideShade; i++) {
            int a = (int) Math.round(0x44 * (1.0 - (double) i / sideShade));
            int argb = a << 24;
            g.fill(side + i, top, side + i + 1, h, argb);
            g.fill(w - side - i - 1, top, w - side - i, h, argb);
        }

        // Top strut and side posts, with the brass hairline on the inner edge.
        g.fill(0, 0, w, top, STEEL);
        g.fill(0, top, side, h, STEEL);
        g.fill(w - side, top, w, h, STEEL);
        g.fill(side, top, w - side, top + 1, BRASS);
        g.fill(side - 1, top, side, h, BRASS);
        g.fill(w - side, top, w - side + 1, h, BRASS);

        // Corner gussets: a stair-stepped triangle in each top corner, one fill per row.
        for (int row = 0; row < leg; row++) {
            int len = leg - row;
            int y = top + row;
            g.fill(side, y, side + len, y + 1, row % 7 == 0 ? STEEL_LIT : STEEL);
            g.fill(w - side - len, y, w - side, y + 1, row % 7 == 0 ? STEEL_LIT : STEEL);
            g.fill(side + len, y, side + len + 1, y + 1, BRASS);
            g.fill(w - side - len - 1, y, w - side - len, y + 1, BRASS);
        }
        // Rivets on the top strut and gussets, brass, 2 px.
        int rivet = Math.max(2, top / 4);
        int ry = (top - rivet) / 2;
        for (int i = 1; i <= 5; i++) {
            int x = w * i / 12;
            g.fill(x, ry, x + rivet, ry + rivet, BRASS);
            g.fill(w - x - rivet, ry, w - x, ry + rivet, BRASS);
        }
    }
}
