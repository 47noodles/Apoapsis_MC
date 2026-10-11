# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Measure frame-to-frame motion and its jitter in a folder of PNG frames.

Usage, from tools/dev:  python -E -P frame_jitter.py FOLDER PREFIX

For each consecutive pair of frames it reports:
  - the mean absolute grey-level difference (how much the picture changed), and
  - the global image shift (dx, dy) in pixels from phase correlation, which for a chase camera is the screen-space
    motion of the scene (the ship and the ground together).
The jitter is the deviation of the cumulative shift path from a smooth fit (a 9-frame moving average): its RMS in
pixels per axis, plus the largest single-frame deviation. A smooth climb gives small values; a shaking camera does not.

Note: no ship-specific marker is tracked; the phase-correlation shift is the whole-frame motion.
"""

import glob
import os
import sys

import cv2
import numpy as np

SMOOTH_FRAMES = 9


def main() -> None:
    if len(sys.argv) != 3:
        sys.exit("usage: frame_jitter.py FOLDER PREFIX")
    folder, prefix = sys.argv[1], sys.argv[2]
    files = sorted(glob.glob(os.path.join(folder, prefix + "*.png")))
    if len(files) < SMOOTH_FRAMES + 1:
        sys.exit(f"need at least {SMOOTH_FRAMES + 1} frames named {prefix}*.png in {folder}")
    grays = [cv2.cvtColor(cv2.imread(f), cv2.COLOR_BGR2GRAY).astype(np.float32) for f in files]
    window = cv2.createHanningWindow((grays[0].shape[1], grays[0].shape[0]), cv2.CV_32F)

    diffs = []
    shifts = []
    for a, b in zip(grays, grays[1:]):
        diffs.append(float(np.mean(np.abs(a - b))))
        (dx, dy), _ = cv2.phaseCorrelate(a, b, window)
        shifts.append((dx, dy))
    shifts = np.array(shifts)
    path = np.cumsum(shifts, axis=0)

    kernel = np.ones(SMOOTH_FRAMES) / SMOOTH_FRAMES
    pad = SMOOTH_FRAMES // 2
    smooth = np.column_stack([
        np.convolve(np.pad(path[:, i], pad, mode="edge"), kernel, mode="valid") for i in range(2)
    ])
    residual = path - smooth
    rms = np.sqrt((residual ** 2).mean(axis=0))
    worst = np.abs(residual).max(axis=0)

    print(f"frames {len(files)}, pairs {len(diffs)}")
    print(f"mean abs grey difference per pair: mean {np.mean(diffs):.2f}, max {np.max(diffs):.2f}")
    print(f"frame shift (px): mean dx {shifts[:, 0].mean():.2f}, mean dy {shifts[:, 1].mean():.2f}, "
          f"max |shift| {np.abs(shifts).max():.2f}")
    print(f"jitter, deviation of the path from a {SMOOTH_FRAMES}-frame smooth fit (px): "
          f"RMS x {rms[0]:.2f}, RMS y {rms[1]:.2f}; largest x {worst[0]:.2f}, largest y {worst[1]:.2f}")


if __name__ == "__main__":
    main()
