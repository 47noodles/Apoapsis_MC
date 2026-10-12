package dev.apoapsis.client.menu;

import com.mojang.realmsclient.RealmsMainScreen;
import dev.apoapsis.Apoapsis;
import dev.apoapsis.client.ui.UiButton;
import dev.apoapsis.client.ui.UiButton.Variant;
import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiPanel;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiText.Type;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.gui.modlist.ModListScreen;

/**
 * Apoapsis's title screen, replacing vanilla's (see {@link MenuClient}): the wordmark top left, a short vertical menu
 * under it, Verdance and Pale turning on the right. It extends {@link TitleScreen} so code that looks for the title
 * screen still finds it; everything else is its own. Every vanilla destination stays reachable: the five menu rows,
 * and small links for Realms, Language, Accessibility, Friends and Credits. Client only.
 */
public final class ApoTitleScreen extends TitleScreen {
    private static final Identifier EMBLEM = Identifier.fromNamespaceAndPath(Apoapsis.MODID, "textures/gui/emblem.png");
    private static final int ROW_W = 156;
    private static final int ROW_H = 26;
    private static final int ROW_GAP = 2;
    private static final int PAD = 12;

    private int margin;
    private int plateX0;
    private int plateY0;
    private int plateX1;
    private int plateY1;

    public ApoTitleScreen() {
        super(false);
    }

    @Override
    protected void init() {
        UiScale s = UiScale.current();
        margin = Math.max(24, Math.min(56, Math.round(s.width() * 0.09f)));
        int vh = s.height();
        int rows = 5;
        int plateH = PAD * 2 + rows * ROW_H + (rows - 1) * ROW_GAP;
        plateX0 = margin;
        plateX1 = margin + ROW_W + PAD * 2;
        plateY0 = Math.max(112, vh / 2 - plateH / 2 - 14);
        plateY1 = plateY0 + plateH;
        int x = plateX0 + PAD;
        int y = plateY0 + PAD;

        add(s, "singleplayer", Variant.MENU, x, y, ROW_W, ROW_H,
                b -> minecraft.gui.setScreen(new SelectWorldScreen(this)));
        y += ROW_H + ROW_GAP;
        UiButton multi = add(s, "multiplayer", Variant.MENU, x, y, ROW_W, ROW_H, b -> minecraft.gui.setScreen(
                minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)));
        multi.active = minecraft.allowsMultiplayer();
        y += ROW_H + ROW_GAP;
        add(s, "options", Variant.MENU, x, y, ROW_W, ROW_H,
                b -> minecraft.gui.setScreen(new OptionsScreen(this, minecraft.options)));
        y += ROW_H + ROW_GAP;
        add(s, "mods", Variant.MENU, x, y, ROW_W, ROW_H, b -> minecraft.gui.setScreen(ModListScreen.create(this)));
        y += ROW_H + ROW_GAP;
        add(s, "quit", Variant.MENU, x, y, ROW_W, ROW_H, b -> minecraft.stop());

        // The rest of vanilla's title screen, as small links under the menu.
        int ly = plateY1 + 9;
        int lx = plateX0 + PAD;
        lx = link(s, "realms", lx, ly, b -> minecraft.gui.setScreen(new RealmsMainScreen(this)), multi.active);
        lx = link(s, "language", lx, ly, b -> minecraft.gui.setScreen(
                new LanguageSelectScreen(this, minecraft.options, minecraft.getLanguageManager())), true);
        lx = link(s, "accessibility", lx, ly,
                b -> minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, minecraft.options)), true);
        boolean friends = !minecraft.isDemo() && !minecraft.isOfflineDeveloperMode();
        lx = link(s, "friends", lx, ly, b -> OnlineOptionsScreen.confirmFriendsListEnabled(minecraft,
                () -> minecraft.gui.setScreen(new FriendsOverlayScreen(this)), this), friends);
        link(s, "credits", lx, ly, b -> minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)), true);

    }

    private UiButton add(UiScale s, String key, Variant v, int x, int y, int w, int h, UiButton.OnPress press) {
        UiButton b = new UiButton(Component.translatable("apoapsis.menu." + key), press, v);
        b.place(s, x, y, w, h);
        return addRenderableWidget(b);
    }

    /** Adds a footer link at ({@code x}, {@code y}); returns where the next one starts. */
    private int link(UiScale s, String key, int x, int y, UiButton.OnPress press, boolean active) {
        Component label = Component.translatable("apoapsis.menu." + key);
        int w = Math.round(UiText.width(Type.LABEL, label.getString())) + 2;
        UiButton b = new UiButton(label, press, Variant.LINK);
        b.place(s, x, y, w, 12);
        b.active = active;
        addRenderableWidget(b);
        return x + w + 12;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        MenuScene.draw(g);
        UiScale s = UiScale.current();
        s.begin(g, 0, 0);
        // The wordmark block: emblem, name, tagline.
        int wx = margin + 64;
        int cap = Type.DISPLAY.capHeight();
        int capTop = 54;
        g.blit(RenderPipelines.GUI_TEXTURED, EMBLEM, margin - 2, capTop + cap / 2 - 15, 0, 0, 60, 30, 256, 128, 256, 128);
        UiText.draw(g, Type.DISPLAY, "Apoapsis", wx, capTop, UiPalette.INK);
        int tagY = capTop + cap + 12;
        UiPanel.rule(g, wx, wx + 16, tagY + 3, UiPalette.BRASS);
        UiText.draw(g, Type.LABEL, Component.translatable("apoapsis.menu.tagline").getString(), wx + 24, tagY,
                UiPalette.BRASS);
        // The menu plate.
        UiPanel.plate(g, plateX0, plateY0, plateX1, plateY1);
        // Key hints, bottom left.
        int hy = s.height() - 30;
        int hx = margin;
        hx += UiKeycap.draw(g, hx, hy, "↑") + 3;
        hx += UiKeycap.hint(g, hx, hy, "↓", Component.translatable("apoapsis.menu.hint.navigate").getString()) + 16;
        UiKeycap.hint(g, hx, hy, "Enter", Component.translatable("apoapsis.menu.hint.select").getString());
        s.end(g);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        for (Renderable r : renderables) {
            r.extractRenderState(g, mouseX, mouseY, a);
        }
    }
}
