# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Placeholder pixel art for Apoapsis, style "Brassworks NASA-punk".

Writes 16x16 RGBA PNGs to src/main/resources/assets/apoapsis/textures/{block,item}/
and, only when --sheet PATH is given, a labelled contact sheet (each texture x8, nearest-neighbour, on steel-950).

Run from the repo root: python tools/art/gen_textures.py [--sheet PATH] [--no-pillow]
Deterministic: fixed seeds, no timestamps, unchanged files are not rewritten.
Pillow encodes the PNGs when it imports; otherwise a zlib+struct writer does the
same job (identical pixels). The contact sheet needs Pillow for its labels.
"""
import argparse
import io
import math
import random
import struct
import zlib
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    Image = ImageDraw = ImageFont = None

SIZE = 16
ROOT = Path(__file__).resolve().parents[2]
TEXTURE_DIR = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis" / "textures"


def hexrgb(text):
    """Parse #rrggbb into an opaque RGBA tuple."""
    text = text.lstrip("#")
    return (int(text[0:2], 16), int(text[2:4], 16), int(text[4:6], 16), 255)


def mix(a, b, t):
    """Blend opaque colour a towards b by t (0..1)."""
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


# Palette: the style's hexes exactly, then shading steps derived from them.
STEEL_950 = hexrgb("#0d1013")
STEEL_900 = hexrgb("#14181d")
STEEL_800 = hexrgb("#1d232a")
STEEL_700 = hexrgb("#2a323b")
STEEL_500 = hexrgb("#56616d")
INK = hexrgb("#e8e2d4")
BRASS = hexrgb("#c9a25a")
BRASS_DEEP = hexrgb("#8a6a34")
COPPER = hexrgb("#c46f3e")
SIGNAL = hexrgb("#3ad1c1")
SIGNAL_GLASS = hexrgb("#0b1f1e")
SIGNAL_EDGE = hexrgb("#1f4f4a")
HYDROGEN = hexrgb("#5aa9f5")

STEEL_600 = mix(STEEL_700, STEEL_500, 0.5)
STEEL_400 = mix(STEEL_500, INK, 0.3)
BRASS_LIGHT = mix(BRASS, INK, 0.35)
BRASS_DARK = mix(BRASS_DEEP, STEEL_950, 0.35)
COPPER_LIGHT = mix(COPPER, INK, 0.3)
COPPER_DARK = mix(COPPER, STEEL_950, 0.45)
SIGNAL_MID = mix(SIGNAL_EDGE, SIGNAL, 0.5)
SIGNAL_GLINT = mix(SIGNAL_EDGE, SIGNAL, 0.4)
SIGNAL_LIGHT = mix(SIGNAL, INK, 0.45)
HYDROGEN_LIGHT = mix(HYDROGEN, INK, 0.4)
HYDROGEN_DARK = mix(HYDROGEN, STEEL_950, 0.35)
REG_BASE = hexrgb("#9a9a96")
REG_LIGHT = mix(REG_BASE, INK, 0.35)
REG_DARK = mix(REG_BASE, STEEL_950, 0.3)
REG_DEEP = mix(REG_BASE, STEEL_950, 0.5)
CANVAS_RED = hexrgb("#b5382f")
CANVAS_RED_DARK = mix(CANVAS_RED, STEEL_950, 0.4)


class Canvas:
    """A SIZE x SIZE grid of RGBA tuples, indexed px[y][x]."""

    def __init__(self, fill=(0, 0, 0, 0)):
        self.px = [[fill] * SIZE for _ in range(SIZE)]

    def put(self, x, y, color):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.px[y][x] = color

    def fill(self, x0, y0, x1, y1, color):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, color)

    def hline(self, x0, x1, y, color):
        for x in range(x0, x1 + 1):
            self.put(x, y, color)

    def vline(self, x, y0, y1, color):
        for y in range(y0, y1 + 1):
            self.put(x, y, color)

    def bevel(self, hi, lo):
        """One-pixel light top and left edges, dark bottom and right edges."""
        self.hline(0, SIZE - 1, 0, hi)
        self.vline(0, 0, SIZE - 1, hi)
        self.hline(0, SIZE - 1, SIZE - 1, lo)
        self.vline(SIZE - 1, 0, SIZE - 1, lo)

    def outline(self, x0, y0, x1, y1, hi, lo):
        """Rectangle outline: top and left in hi, bottom and right in lo."""
        self.hline(x0, x1, y0, hi)
        self.hline(x0, x1, y1, lo)
        self.vline(x0, y0, y1, hi)
        self.vline(x1, y0, y1, lo)

    def rivet(self, x, y, light, base, dark):
        """2x2 rivet with its highlight on the top-left pixel."""
        self.put(x, y, light)
        self.put(x + 1, y, base)
        self.put(x, y + 1, base)
        self.put(x + 1, y + 1, dark)

    def disc(self, cx, cy, paint):
        """Paint pixels by distance from (cx, cy); paint returns None to skip a pixel.

        Coordinates are continuous: the tile centre is (8, 8), and pixel (x, y)
        is sampled at (x + 0.5, y + 0.5)."""
        for y in range(SIZE):
            for x in range(SIZE):
                dx = x + 0.5 - cx
                dy = y + 0.5 - cy
                color = paint(math.hypot(dx, dy), dx, dy)
                if color is not None:
                    self.put(x, y, color)


def hull_plate():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.rivet(x, y, BRASS_LIGHT, BRASS, BRASS_DARK)
    return c


def cockpit_front():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.fill(2, 2, 13, 9, SIGNAL_EDGE)              # canopy glass
    c.hline(2, 13, 2, SIGNAL_MID)                 # lit top edge
    c.hline(2, 13, 9, SIGNAL_GLASS)               # shaded bottom edge
    for t in range(4):                            # faint reflection, left pane
        c.put(3 + t, 6 - t, SIGNAL_GLINT)
        c.put(4 + t, 6 - t, SIGNAL_GLINT)
    c.fill(7, 2, 8, 9, STEEL_700)                 # centre mullion
    c.hline(2, 13, 10, BRASS_DEEP)                # brass trim
    c.fill(2, 11, 13, 13, STEEL_900)              # instrument strip
    for x in (4, 6, 9, 11):
        c.put(x, 12, SIGNAL)
    return c


def cockpit_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(2, 13, 3, BRASS_DEEP)
    c.hline(2, 13, 12, BRASS_DEEP)
    c.outline(4, 5, 11, 10, BRASS_DEEP, STEEL_950)
    c.fill(5, 6, 10, 9, SIGNAL_EDGE)
    c.hline(6, 9, 7, SIGNAL)                      # live readout bar
    return c


def cockpit_top():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.outline(3, 3, 12, 12, BRASS, BRASS_DEEP)    # brass hatch frame
    c.fill(4, 4, 11, 11, SIGNAL_EDGE)
    c.hline(4, 11, 4, SIGNAL_MID)
    c.fill(7, 7, 8, 8, SIGNAL)                    # central lamp
    return c


def cockpit_back():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for y in (4, 6, 8, 10):                       # vent slots
        c.hline(4, 11, y, STEEL_950)
    c.hline(2, 13, 12, COPPER)                    # copper pipe
    c.hline(2, 13, 13, COPPER_DARK)
    c.rivet(2, 2, STEEL_400, STEEL_700, STEEL_950)
    c.rivet(12, 2, STEEL_400, STEEL_700, STEEL_950)
    return c


def gyroscope_top():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)

    def paint(d, dx, dy):
        if d < 1.6:
            return SIGNAL_LIGHT
        if d < 3.0:
            return SIGNAL
        if d < 4.0:
            return SIGNAL_EDGE
        if d < 4.8:
            return STEEL_950
        if d < 6.6:
            if dx + dy < -3.0:
                return BRASS_LIGHT
            if dx + dy > 3.0:
                return BRASS_DARK
            return BRASS
        return None

    c.disc(8.0, 8.0, paint)
    return c


def gyroscope_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(1, 14, 6, BRASS_LIGHT)                # brass band
    c.fill(1, 7, 14, 8, BRASS)
    c.hline(1, 14, 9, BRASS_DEEP)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def fuel_tank_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(1, 14, 4, HYDROGEN_LIGHT)             # hydrogen band
    c.fill(1, 5, 14, 6, HYDROGEN)
    c.hline(1, 14, 7, HYDROGEN_DARK)

    def gauge(d, dx, dy):
        if d < 2.0:
            return STEEL_950
        if d < 2.9:
            return BRASS_DEEP
        return None

    c.disc(11.5, 11.5, gauge)
    c.put(11, 10, INK)                            # needle
    return c


def fuel_tank_end():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)

    def face(d, dx, dy):
        if d < 1.1:
            return HYDROGEN
        if d < 2.2:
            return STEEL_500
        if d < 5.6:
            return STEEL_700
        if d < 6.4:
            return STEEL_950
        if d < 7.0:
            return STEEL_500 if dx + dy < 0 else STEEL_700
        return None

    c.disc(8.0, 8.0, face)
    for x, y in ((4, 4), (10, 4), (4, 10), (10, 10)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def spark_thruster_exhaust():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)

    def bell(d, dx, dy):
        if d < 2.0:
            return STEEL_950
        if d < 3.2:
            return COPPER_DARK
        if d < 4.2:
            return mix(COPPER_DARK, STEEL_950, 0.5)
        if d < 6.0:
            if dx + dy < -3.0:
                return COPPER_LIGHT
            if dx + dy > 3.0:
                return COPPER_DARK
            return COPPER
        if d < 6.8:
            return STEEL_700
        return None

    c.disc(8.0, 8.0, bell)
    return c


def spark_thruster_powerhead():
    c = Canvas(BRASS)
    c.bevel(BRASS_LIGHT, BRASS_DARK)

    def hub(d, dx, dy):
        if d < 1.2:
            return BRASS_LIGHT
        if d < 2.6:
            return STEEL_700
        if d < 4.0:
            return BRASS_DEEP
        if d < 4.8:
            return BRASS_DARK
        return None

    c.disc(8.0, 8.0, hub)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.rivet(x, y, STEEL_500, STEEL_700, STEEL_950)
    return c


def spark_thruster_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for x in (3, 7, 11):                          # copper cooling lines, lengthwise
        c.vline(x, 2, 13, COPPER_LIGHT)
        c.vline(x + 1, 2, 13, COPPER_DARK)
        c.hline(x, x + 1, 1, BRASS)               # brass collars
        c.hline(x, x + 1, 14, BRASS_DEEP)
    return c


def regolith():
    """Pale dust. Blotches use wrap-around distance, so the tile repeats without a seam."""
    rng = random.Random(0xA90)
    blobs = [
        (rng.uniform(0, SIZE), rng.uniform(0, SIZE), rng.uniform(2.0, 4.5), rng.choice((-1.0, 1.0)))
        for _ in range(7)
    ]
    c = Canvas(REG_BASE)
    for y in range(SIZE):
        for x in range(SIZE):
            shift = 0.0
            for cx, cy, r, sign in blobs:
                dx = abs(x + 0.5 - cx)
                dx = min(dx, SIZE - dx)
                dy = abs(y + 0.5 - cy)
                dy = min(dy, SIZE - dy)
                d = math.hypot(dx, dy)
                if d < r:
                    shift += sign * (1 - d / r)
            k = round(shift * 6)
            c.put(x, y, tuple(max(0, min(255, ch + k)) for ch in REG_BASE[:3]) + (255,))
    for _ in range(20):
        x, y = rng.randrange(SIZE), rng.randrange(SIZE)
        c.put(x, y, REG_DEEP if rng.random() < 0.6 else REG_DARK)
    for _ in range(8):
        c.put(rng.randrange(SIZE), rng.randrange(SIZE), REG_LIGHT)
    return c


def hydrolox_canister():
    """Item sprite: transparent background, 1px steel outline."""
    c = Canvas()
    c.hline(5, 10, 1, BRASS)                      # valve handle
    c.put(5, 1, BRASS_LIGHT)
    c.put(10, 1, BRASS_DEEP)
    c.vline(7, 2, 3, BRASS_LIGHT)                 # valve stem
    c.vline(8, 2, 3, BRASS_DEEP)
    c.fill(4, 5, 11, 14, STEEL_800)               # body
    c.vline(4, 5, 14, STEEL_600)                  # lit left edge
    c.vline(11, 5, 14, STEEL_900)                 # shaded right edge
    c.hline(5, 10, 5, STEEL_700)                  # dome
    c.hline(4, 11, 8, HYDROGEN_LIGHT)             # hydrogen band
    c.hline(4, 11, 9, HYDROGEN)
    c.hline(4, 11, 10, HYDROGEN_DARK)
    c.hline(4, 11, 4, STEEL_950)                  # outline
    c.hline(4, 11, 15, STEEL_950)
    c.vline(3, 5, 14, STEEL_950)
    c.vline(12, 5, 14, STEEL_950)
    c.hline(6, 9, 4, BRASS_DEEP)                  # valve collar
    return c


def pale_sample():
    """Item sprite: a sealed vial of Pale regolith, steel cap and brass label, transparent background."""
    glass = mix(STEEL_500, HYDROGEN, 0.2)
    c = Canvas()
    c.fill(5, 1, 10, 3, STEEL_600)                # cap
    c.hline(5, 10, 1, STEEL_400)
    c.hline(5, 10, 3, STEEL_950)
    c.fill(5, 4, 10, 13, glass)                   # glass
    c.fill(5, 9, 10, 13, REG_BASE)                # regolith fill
    c.hline(5, 10, 9, REG_LIGHT)                  # fill surface
    c.put(7, 11, REG_DARK)
    c.put(9, 12, REG_DEEP)
    c.put(6, 13, REG_LIGHT)
    c.hline(5, 10, 6, BRASS)                      # label band
    c.vline(4, 4, 14, STEEL_950)                  # outline
    c.vline(11, 4, 14, STEEL_950)
    c.hline(4, 11, 14, STEEL_950)
    return c


def hazard(x, y, period):
    """Diagonal hazard stripe: brass or steel-950 by the pixel's diagonal band."""
    return BRASS if ((x + y) // period) % 2 == 0 else STEEL_950


def nose_cone_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(1, 14, 6, COPPER)                     # copper cooling band
    c.hline(1, 14, 7, COPPER_DARK)
    for y in (11, 12, 13):                        # hazard striping near the base
        for x in range(1, 15):
            c.put(x, y, hazard(x, y, 2))
    for x, y in ((2, 2), (12, 2)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def nose_cone_tip():
    c = Canvas(STEEL_700)
    c.bevel(STEEL_600, STEEL_950)

    def cap(d, dx, dy):
        if d < 1.6:
            return BRASS_LIGHT
        if d < 3.4:
            return BRASS
        if d < 5.2:
            return BRASS_DEEP
        if d < 6.6:
            return STEEL_950
        return None

    c.disc(8.0, 8.0, cap)
    return c


def fin():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for y in range(10, 15):                       # hazard edge along the lower fin
        for x in range(1, 15):
            c.put(x, y, hazard(x, y, 3))
    c.vline(8, 2, 9, STEEL_600)                   # spine
    for x, y in ((3, 3), (12, 3)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def parachute_side():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for x in range(2, 14, 3):                     # red canopy stripes folded in the pack
        c.vline(x, 2, 13, CANVAS_RED)
        c.vline(x + 1, 2, 13, CANVAS_RED_DARK)
    c.hline(1, 14, 8, INK)                        # cord band
    return c


def parachute_pack():
    c = Canvas(STEEL_700)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(1, 14, 4, BRASS_DEEP)                 # strap bands
    c.hline(1, 14, 11, BRASS_DEEP)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def parachute_top():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)

    def canopy(d, dx, dy):
        if d < 2.0:
            return INK
        if d < 5.0:
            return CANVAS_RED
        if d < 7.0:
            return CANVAS_RED_DARK
        return None

    c.disc(8.0, 8.0, canopy)
    return c


def landing_leg_mount():
    c = Canvas(STEEL_700)
    c.bevel(STEEL_600, STEEL_950)
    c.outline(4, 4, 11, 11, BRASS, BRASS_DEEP)    # hinge plate
    c.fill(6, 6, 9, 9, BRASS_DEEP)
    for x, y in ((2, 2), (12, 2), (2, 12), (12, 12)):
        c.rivet(x, y, BRASS_LIGHT, BRASS, BRASS_DARK)
    return c


def landing_leg_strut():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.fill(5, 1, 10, 14, STEEL_600)               # telescoping strut body
    c.vline(5, 1, 14, STEEL_400)
    c.vline(10, 1, 14, STEEL_950)
    for y in (6, 7, 8):                           # hazard band mid-strut
        for x in range(5, 11):
            c.put(x, y, hazard(x, y, 2))
    return c


def landing_leg_foot():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    for y in (11, 12, 13):                        # hazard striping on the pad
        for x in range(1, 15):
            c.put(x, y, hazard(x, y, 2))
    c.hline(1, 14, 3, COPPER)                     # copper contact band
    for x, y in ((2, 6), (12, 6)):
        c.rivet(x, y, STEEL_400, STEEL_500, STEEL_950)
    return c


def window_side():
    """Hull side with a porthole: brass rim, translucent glass centre (alpha in the glass pixels)."""
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)

    def porthole(d, dx, dy):
        if d < 4.0:
            glass = SIGNAL_MID if dx + dy < -2.0 else SIGNAL_EDGE
            return glass[:3] + (150,)
        if d < 5.2:
            return BRASS_DEEP
        if d < 6.2:
            return STEEL_950
        return None

    c.disc(8.0, 8.0, porthole)
    return c


def fuel_refinery_side():
    """Machine side: copper and brass bands, an inset access panel, four bolts."""
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.hline(1, 14, 3, COPPER)
    c.hline(1, 14, 4, BRASS_DEEP)
    c.outline(5, 8, 10, 12, STEEL_500, STEEL_950)  # access panel
    c.fill(6, 9, 9, 11, STEEL_700)
    for x, y in ((2, 9), (12, 9), (2, 13), (12, 13)):
        c.put(x, y, STEEL_400)
    return c


def fuel_refinery_top():
    """Machine top: a vent grate under a brass lip."""
    c = Canvas(STEEL_700)
    c.bevel(STEEL_500, STEEL_950)
    c.hline(4, 11, 4, BRASS_DEEP)
    for y in range(5, 12):
        c.hline(4, 11, y, STEEL_600 if y % 2 else STEEL_950)
    return c


def _fuel_refinery_frame():
    c = Canvas(STEEL_800)
    c.bevel(STEEL_600, STEEL_950)
    c.outline(3, 3, 12, 12, BRASS, BRASS_DEEP)
    return c


def fuel_refinery_front():
    """Front, idle: a brass-framed grate with dark bars."""
    c = _fuel_refinery_frame()
    c.fill(4, 4, 11, 11, STEEL_500)
    for x in (5, 7, 9):
        c.vline(x, 4, 11, STEEL_900)
    return c


def fuel_refinery_front_lit():
    """Front, refining: the grate glows signal teal with hot white cores."""
    c = _fuel_refinery_frame()
    c.fill(4, 4, 11, 11, SIGNAL_EDGE)
    for x in (5, 7, 9):
        c.vline(x, 5, 11, SIGNAL)
    for x in (7, 9):
        c.vline(x, 6, 9, INK)
    return c


TEXTURES = [
    ("block/hull_plate", hull_plate),
    ("block/nose_cone_side", nose_cone_side),
    ("block/nose_cone_tip", nose_cone_tip),
    ("block/fin", fin),
    ("block/window_side", window_side),
    ("block/cockpit_front", cockpit_front),
    ("block/cockpit_side", cockpit_side),
    ("block/cockpit_top", cockpit_top),
    ("block/cockpit_back", cockpit_back),
    ("block/gyroscope_top", gyroscope_top),
    ("block/gyroscope_side", gyroscope_side),
    ("block/fuel_tank_side", fuel_tank_side),
    ("block/fuel_tank_end", fuel_tank_end),
    ("block/fuel_refinery_side", fuel_refinery_side),
    ("block/fuel_refinery_top", fuel_refinery_top),
    ("block/fuel_refinery_front", fuel_refinery_front),
    ("block/fuel_refinery_front_lit", fuel_refinery_front_lit),
    ("block/spark_thruster_exhaust", spark_thruster_exhaust),
    ("block/spark_thruster_powerhead", spark_thruster_powerhead),
    ("block/spark_thruster_side", spark_thruster_side),
    ("block/regolith", regolith),
    ("block/parachute_side", parachute_side),
    ("block/parachute_pack", parachute_pack),
    ("block/parachute_top", parachute_top),
    ("block/landing_leg_mount", landing_leg_mount),
    ("block/landing_leg_strut", landing_leg_strut),
    ("block/landing_leg_foot", landing_leg_foot),
    ("item/hydrolox_canister", hydrolox_canister),
    ("item/pale_sample", pale_sample),
]


def png_zlib(px):
    """Encode a SIZE x SIZE RGBA grid as PNG with zlib and struct only."""
    raw = bytearray()
    for row in px:
        raw.append(0)
        for r, g, b, a in row:
            raw += bytes((r, g, b, a))

    def chunk(tag, data):
        return (struct.pack(">I", len(data)) + tag + data
                + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF))

    ihdr = struct.pack(">IIBBBBB", SIZE, SIZE, 8, 6, 0, 0, 0)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr)
            + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))


def to_image(px):
    img = Image.new("RGBA", (SIZE, SIZE))
    img.putdata([p for row in px for p in row])
    return img


def encode_png(px, use_pillow):
    if use_pillow:
        buf = io.BytesIO()
        to_image(px).save(buf, "PNG")
        return buf.getvalue()
    return png_zlib(px)


def write_if_changed(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and path.read_bytes() == data:
        return "unchanged"
    path.write_bytes(data)
    return "written"


def load_font(size):
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        return ImageFont.load_default()


def contact_sheet(path, textures):
    """Every texture x8 (nearest-neighbour), labelled, on a steel-950 background."""
    if Image is None:
        print("contact sheet skipped: Pillow is not installed")
        return
    cols, cell, pad, label_h, title_h = 5, SIZE * 8, 24, 18, 30
    rows = math.ceil(len(textures) / cols)
    width = pad + cols * (cell + pad)
    height = title_h + rows * (cell + label_h + pad)
    sheet = Image.new("RGBA", (width, height), STEEL_950)
    draw = ImageDraw.Draw(sheet)
    font = load_font(11)
    draw.text((pad, 8), "Apoapsis placeholder art, Brassworks NASA-punk (x8, nearest)",
              fill=INK, font=font)
    nearest = getattr(Image, "Resampling", Image).NEAREST
    for i, (name, make) in enumerate(textures):
        x = pad + (i % cols) * (cell + pad)
        y = title_h + (i // cols) * (cell + label_h + pad)
        scaled = to_image(make().px).resize((cell, cell), nearest)
        draw.rectangle((x - 1, y - 1, x + cell, y + cell), outline=STEEL_700)
        sheet.paste(scaled, (x, y), scaled)  # alpha as mask: transparent pixels show the background
        draw.text((x, y + cell + 4), name.split("/", 1)[1], fill=INK, font=font)
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path, "PNG")


def main():
    parser = argparse.ArgumentParser(description="Generate Apoapsis placeholder textures.")
    parser.add_argument("--sheet", type=Path, default=None,
                        help="write a labelled contact sheet to PATH (none is written unless this is given)")
    parser.add_argument("--no-pillow", action="store_true",
                        help="encode PNGs with zlib+struct even when Pillow is installed")
    args = parser.parse_args()
    use_pillow = Image is not None and not args.no_pillow
    for name, make in TEXTURES:
        path = TEXTURE_DIR / (name + ".png")
        status = write_if_changed(path, encode_png(make().px, use_pillow))
        print(f"{status:9} {path.relative_to(ROOT).as_posix()}")
    if args.sheet is not None:
        contact_sheet(args.sheet, TEXTURES)
        if Image is not None:
            print(f"contact sheet: {args.sheet}")


if __name__ == "__main__":
    main()
