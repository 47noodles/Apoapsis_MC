package dev.apoapsis.client.notify;

import dev.apoapsis.Apoapsis;
import dev.apoapsis.net.MessagePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.AdvancementToast;
import net.minecraft.client.gui.components.toasts.RecipeToast;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ToastAddEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * The mod's messages on screen, in place of chat and the action bar (which sit over the flight instruments): one calm
 * event line near the top centre ({@link EventLine}), a short banner for a new Age ({@link AgeBanner}) and a small chip
 * for a first ({@link FirstChip}). The server sends {@link MessagePayload}; {@link CleanFlightView} takes the vanilla
 * hotbar, crosshair and dismount hint off the screen while the local player pilots a ship. Client only.
 */
public final class Notify {
    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "notify");

    private static final Identifier GUIDE_LAYER = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "guide_panel");

    private Notify() {}

    /** Called from {@code ApoapsisClient} with the mod event bus. */
    public static void register(IEventBus modBus) {
        modBus.addListener(Notify::onRegisterGuiLayers);
        NeoForge.EVENT_BUS.addListener(Notify::onClientTickPost);
        NeoForge.EVENT_BUS.addListener(Notify::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(CleanFlightView::onRenderLayer);
        NeoForge.EVENT_BUS.addListener(Notify::onToast);
        NeoForge.EVENT_BUS.addListener(Notify::onGuideLayer);
    }

    /** A message from the server. Called on the client thread. */
    public static void accept(MessagePayload p) {
        switch (p.kind()) {
            case EVENT -> EventLine.push(p.title(), false);
            case WARNING -> EventLine.push(p.title(), true);
            case AGE -> AgeBanner.show(p.title(), p.line());
            case FIRST -> FirstChip.show(p.title(), p.line());
        }
    }

    /**
     * The event line while it shows, or null. The maps and containers read it through {@code OverlayMessages}, because
     * the layer here is not drawn under a screen.
     */
    public static @Nullable Component eventLine() {
        return EventLine.current();
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(LAYER, (g, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gui.screen() != null || mc.gui.hud.isHidden()) {
                return;
            }
            float partial = deltaTracker.getGameTimeDeltaPartialTick(false);
            EventLine.render(g, mc, partial);
            FirstChip.render(g, mc, partial);
            AgeBanner.render(g, mc, partial);
        });
    }

    private static void onClientTickPost(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.isPaused()) {
            return;
        }
        // The event line runs under a screen too: the map shows it in its bottom row. The banner and the chip wait
        // for the view to be clear, so a moment that arrives under the map is still there when the map closes.
        EventLine.tick();
        if (mc.gui.screen() == null) {
            AgeBanner.tick();
            FirstChip.tick();
        }
    }

    /**
     * The vanilla toasts for recipes ("New Recipe(s) Unlocked"), advancements ("Advancement Made") and the tutorial
     * ("Move with W, A, S and D") are not shown: they are grey frames over the top right, where the regime label sits.
     * The mod's own recipes are set not to notify (data/apoapsis/recipe) and its advancements have show_toast false, but
     * the starting kit unlocks vanilla recipes and a new world starts the tutorial. A first shows {@link FirstChip}
     * instead. System toasts (a bad server, a failed save) still show.
     */
    private static void onToast(ToastAddEvent event) {
        Toast toast = event.getToast();
        if (toast instanceof RecipeToast || toast instanceof AdvancementToast || toast instanceof TutorialToast) {
            event.setCanceled(true);
        }
    }

    /**
     * The guide panel waits under an Age banner: both are at the top of the view, the banner is wider than the room
     * left of the panel on a small window, and they usually arrive together (the Age 3 welcome and the first guide step).
     * The panel keeps counting down, so it is a few seconds shorter.
     */
    private static void onGuideLayer(RenderGuiLayerEvent.Pre event) {
        if (GUIDE_LAYER.equals(event.getName()) && AgeBanner.showing()) {
            event.setCanceled(true);
        }
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        EventLine.clear();
        AgeBanner.clear();
        FirstChip.clear();
    }
}
