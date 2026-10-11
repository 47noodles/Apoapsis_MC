# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Frame-to-frame jitter of the camera relative to the ship, from the takeoff CSV written by TestDriverRecord.

Usage, from tools/dev:  python -E -P camera_ship_jitter.py CSV

For a seated pilot the camera moves with the ship, so camera minus ship render position should stay constant from frame
to frame. For each capture mode (first, third) this reports the change of that offset between consecutive frames: its
RMS and maximum in blocks, per axis and as a length. It also reports the ship's own frame-to-frame step and the player
minus ship offset for comparison, and the frame rate from the nanotime column.
"""

import csv
import sys

import numpy as np


def column(rows, name):
    return np.array([float(r[name]) for r in rows])


def stats(label, values):
    """Prints the RMS and maximum of a per-frame change, as a length, per axis and overall."""
    if len(values) == 0:
        print(f"  {label}: no data")
        return
    length = np.linalg.norm(values, axis=1)
    rms_axes = np.sqrt((values ** 2).mean(axis=0))
    print(f"  {label}: RMS length {np.sqrt((length ** 2).mean()):.5f} blocks, max length {length.max():.5f} blocks; "
          f"RMS per axis x {rms_axes[0]:.5f} y {rms_axes[1]:.5f} z {rms_axes[2]:.5f}")


def main() -> None:
    if len(sys.argv) != 2:
        sys.exit("usage: camera_ship_jitter.py CSV")
    with open(sys.argv[1], newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    for mode in ("first", "third"):
        part = [r for r in rows if r["mode"] == mode and r["ship_x"] not in ("NaN", "")]
        if len(part) < 3:
            print(f"{mode}: fewer than 3 frames with a ship")
            continue
        nanos = column(part, "nanotime")
        fps = (len(part) - 1) / ((nanos[-1] - nanos[0]) / 1e9)
        cam = np.column_stack([column(part, "cam_x"), column(part, "cam_y"), column(part, "cam_z")])
        ship = np.column_stack([column(part, "ship_x"), column(part, "ship_y"), column(part, "ship_z")])
        player = np.column_stack([column(part, "player_x"), column(part, "player_y"), column(part, "player_z")])
        print(f"{mode} person: {len(part)} frames, {fps:.1f} fps over the capture")
        stats("camera minus ship, frame-to-frame change", np.diff(cam - ship, axis=0))
        stats("player minus ship, frame-to-frame change", np.diff(player - ship, axis=0))
        stats("ship position, frame-to-frame step", np.diff(ship, axis=0))


if __name__ == "__main__":
    main()
