package dev.apoapsis.client.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps the map's labels apart. Each frame starts empty: the title block and hint are reserved first, then a label is
 * drawn only when its box meets no box already claimed. A label that would collide is dropped. Client only.
 */
final class Labels {
    private final List<int[]> boxes = new ArrayList<>();

    /** Claims the box {@code x0..x1} by {@code y0..y1} (exclusive) when it meets nothing claimed. */
    boolean claim(int x0, int y0, int x1, int y1) {
        for (int[] b : boxes) {
            if (x0 < b[2] && b[0] < x1 && y0 < b[3] && b[1] < y1) {
                return false;
            }
        }
        boxes.add(new int[] {x0, y0, x1, y1});
        return true;
    }

    /** Claims a box unconditionally, for fixed blocks such as the title. */
    void reserve(int x0, int y0, int x1, int y1) {
        boxes.add(new int[] {x0, y0, x1, y1});
    }
}
