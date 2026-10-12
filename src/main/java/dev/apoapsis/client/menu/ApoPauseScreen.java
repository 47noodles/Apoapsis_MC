package dev.apoapsis.client.menu;

import dev.apoapsis.client.ui.UiBackdrop;
import dev.apoapsis.client.ui.UiButton;
import dev.apoapsis.client.ui.UiButton.Variant;
import dev.apoapsis.client.ui.UiKeycap;
import dev.apoapsis.client.ui.UiPalette;
import dev.apoapsis.client.ui.UiPanel;
import dev.apoapsis.client.ui.UiScale;
import dev.apoapsis.client.ui.UiText;
import dev.apoapsis.client.ui.UiText.Type;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.toasts.NowPlayingToast;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.WorldOptionsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.friends.FriendsOverlayScreen;
import net.minecraft.client.gui.screens.options.OnlineOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.social.SocialInteractionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.dialog.Dialog;
import net.minecraft.server.dialog.Dialogs;
import net.minecraft.tags.DialogTags;
import net.minecraft.util.CommonLinks;
import net.neoforged.neoforge.client.gui.modlist.ModListScreen;

/**
 * The pause menu in the kit's style: one steel plate on the left over the dimmed, blurred world, the centre left
 * clear. It extends {@link PauseScreen} so code that looks for the pause screen (and mods that add to it) still find
 * it. Same functions as vanilla: back to game, advancements, statistics, options, world options, the server's custom
 * additions, mods, save and quit, and small links for feedback, bug reports, friends and player reporting. Widgets other
 * mods add in a screen-init hook are kept reachable in a column beside the plate. Client only.
 */
public final class ApoPauseScreen extends PauseScreen {
    private static final int ROW_W = 190;
    private static final int PAD = 12;

    private final List<UiButton> own = new ArrayList<>();
    private int plateX0;
    private int plateY0;
    private int plateX1;
    private int plateY1;
    private int headingY;
    private int extraX;
    private int extraY;

    public ApoPauseScreen(boolean showPauseMenu) {
        super(showPauseMenu);
    }

    @Override
    protected void init() {
        if (!showsPauseMenu()) {
            super.init();
            return;
        }
        own.clear();
        UiScale s = UiScale.current();
        int vh = s.height();
        int margin = Math.max(24, Math.min(56, Math.round(s.width() * 0.09f)));

        List<Entry> rows = new ArrayList<>();
        rows.add(new Entry("pause.back", Variant.PRIMARY, b -> {
            minecraft.gui.setScreen(null);
            minecraft.mouseHandler.grabMouse();
        }, true));
        rows.add(new Entry("pause.advancements", Variant.MENU,
                open(() -> new AdvancementsScreen(minecraft.player.connection.getAdvancements(), this)), true));
        rows.add(new Entry("pause.statistics", Variant.MENU,
                open(() -> new StatsScreen(this, minecraft.player.getStats())), true));
        if (minecraft.level != null) {
            rows.add(new Entry("pause.options", Variant.MENU,
                    open(() -> new OptionsScreen(this, minecraft.options)), true));
            rows.add(new Entry("pause.world", Variant.MENU,
                    open(() -> new WorldOptionsScreen(this, minecraft.level)), true));
        }
        customAddition().ifPresent(dialog -> rows.add(new Entry(
                dialog.value().common().computeExternalTitle(), Variant.MENU,
                b -> minecraft.player.connection.showDialog(dialog, this), true)));
        rows.add(new Entry("mods", Variant.MENU, open(() -> ModListScreen.create(this)), true));
        String leave = minecraft.isLocalServer() ? "pause.save_quit" : "pause.disconnect";
        rows.add(new Entry(leave, Variant.DANGER, b -> {
            b.active = false;
            minecraft.getReportingContext().draftReportHandled(minecraft, this,
                    () -> minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE), true);
        }, true));

        // Heights: shrink rows if the screen is short, so the plate always fits.
        int rowH = 24;
        int gaps = 10 + 8 + 8;
        int fixed = PAD * 2 + 22 + gaps;
        while (rowH > 18 && fixed + rows.size() * (rowH + 2) > vh - 70) {
            rowH--;
        }
        int primaryH = rowH + 4;
        int plateH = fixed + primaryH + (rows.size() - 2) * (rowH + 2) + rowH + 2;
        plateX0 = margin;
        plateX1 = margin + ROW_W + PAD * 2;
        plateY0 = Math.max(24, (vh - plateH) / 2 - 12);
        plateY1 = plateY0 + plateH;
        headingY = plateY0 + PAD + 3;
        int x = plateX0 + PAD;
        int y = plateY0 + PAD + 22 + 10;
        for (int i = 0; i < rows.size(); i++) {
            Entry e = rows.get(i);
            boolean last = i == rows.size() - 1;
            if (last) {
                y += 10 - 2;
            }
            int h = i == 0 ? primaryH : rowH;
            UiButton b = new UiButton(e.label(), e.press(), e.variant());
            b.place(s, x, y, ROW_W, h);
            b.active = e.active();
            own.add(b);
            addRenderableWidget(b);
            y += h + 2;
            if (i == 0) {
                y += 8;
            }
        }

        footerLinks(s, margin);
        extraX = plateX1 + 16;
        extraY = plateY0;
    }

    private record Entry(Component label, Variant variant, Button.OnPress press, boolean active) {
        Entry(String key, Variant variant, Button.OnPress press, boolean active) {
            this(Component.translatable("apoapsis.menu." + key), variant, press, active);
        }
    }

    private Button.OnPress open(Supplier<Screen> screen) {
        return b -> minecraft.gui.setScreen(screen.get());
    }

    private void footerLinks(UiScale s, int margin) {
        int y = plateY1 + 9;
        int x = plateX0 + PAD;
        URI feedback = SharedConstants.getCurrentVersion().stable() ? CommonLinks.RELEASE_FEEDBACK
                : CommonLinks.SNAPSHOT_FEEDBACK;
        x = link(s, "pause.feedback", x, y, ConfirmLinkScreen.confirmLink(this, feedback), true);
        boolean bugs = !SharedConstants.getCurrentVersion().dataVersion().isSideSeries();
        x = link(s, "pause.bugs", x, y, ConfirmLinkScreen.confirmLink(this, CommonLinks.SNAPSHOT_BUGS_FEEDBACK), bugs);
        boolean friends = !minecraft.isDemo() && !minecraft.isOfflineDeveloperMode();
        x = link(s, "friends", x, y, b -> OnlineOptionsScreen.confirmFriendsListEnabled(minecraft,
                () -> minecraft.gui.setScreen(new FriendsOverlayScreen(this)), this), friends);
        boolean anyone = true;
        if (minecraft.getSingleplayerServer() != null && minecraft.player != null) {
            anyone = minecraft.player.connection.getSeenPlayers().keySet().stream()
                    .anyMatch(id -> !id.equals(minecraft.player.getUUID()));
        }
        link(s, "pause.report", x, y, b -> minecraft.gui.setScreen(new SocialInteractionsScreen(this)), anyone);
    }

    private int link(UiScale s, String key, int x, int y, Button.OnPress press, boolean active) {
        Component label = Component.translatable("apoapsis.menu." + key);
        int w = Math.round(UiText.width(Type.LABEL, label.getString())) + 2;
        UiButton b = new UiButton(label, press, Variant.LINK);
        b.place(s, x, y, w, 12);
        b.active = active;
        own.add(b);
        addRenderableWidget(b);
        return x + w + 12;
    }

    /** The server's pause-screen additions (custom dialog or server links), if any. */
    private Optional<Holder<Dialog>> customAddition() {
        Registry<Dialog> registry = minecraft.player.connection.registryAccess().lookupOrThrow(Registries.DIALOG);
        Optional<? extends HolderSet<Dialog>> additions = registry.get(DialogTags.PAUSE_SCREEN_ADDITIONS);
        if (additions.isPresent() && additions.get().size() > 0) {
            if (additions.get().size() == 1) {
                return Optional.of(additions.get().get(0));
            }
            return registry.get(Dialogs.CUSTOM_OPTIONS).map(h -> (Holder<Dialog>) h);
        }
        if (!minecraft.player.connection.serverLinks().isEmpty()) {
            return registry.get(Dialogs.SERVER_LINKS).map(h -> (Holder<Dialog>) h);
        }
        return Optional.empty();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (!showsPauseMenu()) {
            return;
        }
        if (minecraft.gui.screen() == this) {
            extractBlurredBackground(g);
        }
        UiBackdrop.veil(g, 0.72f, 0.18f);
        UiScale s = UiScale.current();
        s.begin(g, 0, 0);
        UiPanel.plate(g, plateX0, plateY0, plateX1, plateY1);
        UiText.draw(g, Type.LABEL, Component.translatable("apoapsis.menu.pause.title").getString(), plateX0 + PAD + 2,
                headingY, UiPalette.BRASS);
        UiPanel.rule(g, plateX0 + PAD, plateX1 - PAD, plateY0 + PAD + 22, UiPalette.STEEL_700);
        UiKeycap.hint(g, plateX0, s.height() - 30, "Esc", Component.translatable("apoapsis.menu.hint.resume").getString());
        s.end(g);
        minecraft.gui.hud.extractDeferredSubtitles();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (!showsPauseMenu()) {
            super.extractRenderState(g, mouseX, mouseY, a);
            return;
        }
        // Widgets that other mods added are not ours: stack them in a column beside the plate.
        int ey = extraY;
        for (var child : children()) {
            if (child instanceof AbstractWidget w && !(w instanceof UiButton)
                    && !own.contains(w)) {
                w.setPosition(extraX, ey);
                ey += w.getHeight() + 4;
            }
        }
        for (Renderable r : renderables) {
            r.extractRenderState(g, mouseX, mouseY, a);
        }
        if (rendersNowPlayingToast()) {
            NowPlayingToast.extractToast(g, font);
        }
    }
}
