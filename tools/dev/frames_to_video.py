# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Stitch a folder of PNG frames into an MP4 (mp4v codec, 20 fps).

Usage, from tools/dev:  python -E -P frames_to_video.py FOLDER PREFIX OUT.mp4 [FPS]

Only files named PREFIX*.png in FOLDER are used, in name order, so zero-padded frame numbers keep their order.
Requires OpenCV (cv2). -E -P keeps the script from importing a stray module from its own or the current folder.
"""

import glob
import os
import sys

import cv2

FPS = 20.0


def main() -> None:
    if len(sys.argv) not in (4, 5):
        sys.exit("usage: frames_to_video.py FOLDER PREFIX OUT.mp4 [FPS]")
    folder, prefix, out = sys.argv[1], sys.argv[2], sys.argv[3]
    fps = float(sys.argv[4]) if len(sys.argv) == 5 else FPS
    files = sorted(glob.glob(os.path.join(folder, prefix + "*.png")))
    if not files:
        sys.exit(f"no frames named {prefix}*.png in {folder}")
    first = cv2.imread(files[0])
    if first is None:
        sys.exit(f"cannot read {files[0]}")
    height, width = first.shape[:2]
    writer = cv2.VideoWriter(out, cv2.VideoWriter_fourcc(*"mp4v"), fps, (width, height))
    if not writer.isOpened():
        sys.exit(f"cannot open {out} for writing")
    written = 0
    try:
        for path in files:
            frame = cv2.imread(path)
            if frame is None:
                continue
            if frame.shape[:2] != (height, width):
                frame = cv2.resize(frame, (width, height))
            writer.write(frame)
            written += 1
    finally:
        writer.release()
    print(f"wrote {written} frames at {fps:g} fps ({width}x{height}) to {out}")


if __name__ == "__main__":
    main()
