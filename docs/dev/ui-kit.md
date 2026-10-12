# UI kit

The shared look for menus, HUD and map overlays: dark steel, thin brass trim, teal for focus, copper for the one risky action. The code is in `src/main/java/dev/apoapsis/client/ui/`; the screens built on it are in `client/menu/`. The direction is the "Brassworks NASA-punk" concept set, made calmer: one layer per plate, no boxes inside boxes, space instead of rules where it can.

## UI units

Lay screens out in UI units, not GUI pixels, so a screen looks the same at every GUI scale. `UiScale.current()` picks a unit of about 360 units of screen height (2 device pixels per unit at 720p, 3 at 1080p, 4 at 1440p). A 1080p screen is about 640 x 360 units.

```java
UiScale s = UiScale.current();
s.begin(g, 0, 0);          // draw in UI units from here
// ... fills, UiText, UiPanel, UiKeycap ...
s.end(g);
```

Widgets and the mouse still work in GUI pixels. `UiButton.place(scale, x, y, w, h)` takes UI units and converts.

## Type

`UiText` is the only place text is drawn. A `Type` names a font definition in `assets/apoapsis/font/` (`label`, `body`, `title`, `heading`, `display`); to change a typeface or a size, change that JSON and the size in `UiText.Type`, not the screens.

| Type | Font | Size | Use |
| --- | --- | --- | --- |
| `LABEL` | Barlow Condensed | 8 | captions, hints, keycaps (tracked capitals) |
| `BODY` | Barlow Condensed | 11 | running text |
| `TITLE` | Barlow Condensed | 16 | menu items and buttons (tracked capitals) |
| `HEADING` | Chakra Petch | 13 | screen headings (tracked capitals) |
| `DISPLAY` | Chakra Petch | 30 | the wordmark only |

```java
UiText.draw(g, Type.BODY, "Text", x, capTopY, UiPalette.INK);
UiText.drawCentered(g, Type.TITLE, "Options", centreX, capTopY, color);
float w = UiText.width(Type.LABEL, "Navigate");
```

`y` is the top of the capital letters, so rows of different types line up. The fonts are SIL OFL 1.1; each licence text sits next to its font file. Font sizes are in UI units and each JSON uses oversample 3, which keeps glyphs crisp at 2 to 4 device pixels per unit.

## Palette

`UiPalette` holds the constants: `STEEL_950..500` (ground), `INK`, `INK_MUTED`, `INK_DIM` (text), `BRASS`, `COPPER`, `SIGNAL` (teal), `HYDROGEN`, `GO`, `ALERT`, plus `alpha`, `fade` and `mix` helpers.

## Panel

```java
UiPanel.plate(g, x0, y0, x1, y1);          // steel, one-unit edge, brass top line, corner rivets
UiPanel.rule(g, x0, x1, y, UiPalette.STEEL_700);
UiPanel.brackets(g, x0, y0, x1, y1, 4, UiPalette.SIGNAL);   // focus mark
```

## Button

```java
UiButton b = new UiButton(Component.translatable("..."), btn -> doIt(), UiButton.Variant.MENU);
b.place(scale, x, y, w, h);
addRenderableWidget(b);
```

It is a vanilla `Button`, so Tab, arrows, Enter, narration and the click sound work. Variants: `MENU` (row, brass bar on hover), `PLATE` (bordered), `PRIMARY` (solid brass, one per screen), `DANGER` (copper hover), `LINK` (small footer text). States: hover (eased), keyboard focus (teal brackets, shown only after keyboard input), pressed, disabled (`b.active = false`, dim text).

## Keycap

```java
UiKeycap.hint(g, x, y, myKeyMapping, "Open map");   // shows the key the player has bound
UiKeycap.hint(g, x, y, "Enter", "Select");          // only for keys that cannot be rebound
```

Both return the width drawn, for laying hints in a row.

## Backdrop

`UiBackdrop.draw(g)` fills the screen with steel and a fixed star field (one device pixel per star at any scale). `UiBackdrop.veil(g, leftAlpha, rightAlpha)` darkens a rendered scene, strongest at the left, for menus over the world.

## Checking a screen

`APOAPSIS_MENU_SHOTS=title ./gradlew runClient` steps the menu screens through 1920x1080 and 1280x720 windows at GUI scale auto and 2 and saves `imm-menu-*.png` into `run/screenshots`; `APOAPSIS_MENU_SHOTS=world` does the pause menu once a world is loaded (use with a script that opens one, such as `runTestFlight`).
