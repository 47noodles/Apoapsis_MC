package dev.apoapsis.client.map;

import java.util.Locale;
import net.minecraft.client.KeyMapping;

/**
 * Names for the keys the maps show on keycaps: whatever the player has bound, with punctuation spelled out so the two
 * warp keys never both read as a dot. Client only.
 */
public final class MapKeys {
    private MapKeys() {}

    /** The label of the key bound to {@code mapping}. */
    public static String label(KeyMapping mapping) {
        return name(mapping.getTranslatedKeyMessage().getString());
    }

    /** A key's name for a keycap: single punctuation marks become words, anything long is shortened. */
    public static String name(String raw) {
        String s = raw.strip();
        return switch (s) {
            case "," -> "COMMA";
            case "." -> "PERIOD";
            case "/" -> "SLASH";
            case "\\" -> "BACKSLASH";
            case ";" -> "SEMICOLON";
            case "'" -> "QUOTE";
            case "-" -> "MINUS";
            case "=" -> "EQUALS";
            case "[" -> "LBRACKET";
            case "]" -> "RBRACKET";
            case "`" -> "TILDE";
            case "" -> "NONE";
            default -> s.toUpperCase(Locale.ROOT)
                    .replace("LEFT ", "L ")
                    .replace("RIGHT ", "R ")
                    .replace("CONTROL", "CTRL");
        };
    }
}
