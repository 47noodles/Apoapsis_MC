# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Particle textures for Apoapsis: glow, plume, spark, streak and puff.

Writes RGBA PNGs to src/main/resources/assets/apoapsis/textures/particle/
(fx_glow 64x64, fx_plume 64x64, fx_spark 32x32, fx_puff 64x64).

Run from the repo root: python tools/art/gen_fx.py
Deterministic: fixed seeds, no timestamps, unchanged files are not rewritten.
Glow, plume and spark are white with the soft shape in both RGB and alpha, so
they tint by vertex colour and blend additively at runtime. The puff is flat
grey (rgb 235) with the shape in alpha only, for normal blending.
Pillow encodes the PNGs when it imports; otherwise a zlib+struct writer does the
same job. The two encoders give identical pixels but different bytes, so keep
Pillow installed to keep the committed files stable.
"""
import io
import math
import random
import struct
import zlib
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    Image = None

ROOT = Path(__file__).resolve().parents[2]
PARTICLE_DIR = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis" / "textures" / "particle"

PLUME_SEED = 7
PUFF_SEED_LOW = 11
PUFF_SEED_HIGH = 13


def clamp01(x):
    return 0.0 if x < 0.0 else 1.0 if x > 1.0 else x


def smoothstep(e0, e1, x):
    t = clamp01((x - e0) / (e1 - e0))
    return t * t * (3.0 - 2.0 * t)


def to_byte(v):
    return int(math.floor(clamp01(v) * 255.0 + 0.5))


def radius(x, y, size):
    """Distance from the centre in units of the half-width (r = 1 at the edge midpoint).

    Samples on the pixel grid, so the centre pixel sits at r = 0 and reaches full strength.
    """
    half = size / 2.0
    dx = (x - half) / half
    dy = (y - half) / half
    return math.hypot(dx, dy)


def make_lattice(seed, n):
    rng = random.Random(seed)
    return [[rng.random() for _ in range(n)] for _ in range(n)]


def value_noise(lattice, u, v):
    """Smooth periodic value noise in [0, 1]; u and v are in lattice units and wrap at n."""
    n = len(lattice)
    iu = math.floor(u)
    iv = math.floor(v)
    fu = smoothstep(0.0, 1.0, u - iu)
    fv = smoothstep(0.0, 1.0, v - iv)
    a = lattice[iv % n][iu % n]
    b = lattice[iv % n][(iu + 1) % n]
    c = lattice[(iv + 1) % n][iu % n]
    d = lattice[(iv + 1) % n][(iu + 1) % n]
    top = a + (b - a) * fu
    bottom = c + (d - c) * fu
    return top + (bottom - top) * fv


def shape_pixels(size, shape):
    """Grey pixels where rgb and alpha both equal the shape value v(r, x, y, size)."""
    pixels = []
    for y in range(size):
        for x in range(size):
            g = to_byte(shape(radius(x, y, size), x, y, size))
            pixels.append((g, g, g, g))
    return pixels


def glow_v(r, x, y, size):
    return math.exp(-((r / 0.38) ** 2)) * smoothstep(1.0, 0.75, r)


def plume_v(r, x, y, size):
    lattice = make_lattice(PLUME_SEED, 8)
    core = 0.65 * math.exp(-((r / 0.22) ** 2)) + 0.35 * math.exp(-((r / 0.55) ** 2))
    n = value_noise(lattice, (x + 0.5) / size * 8, (y + 0.5) / size * 8)
    return clamp01(core * smoothstep(1.0, 0.75, r) * (0.85 + 0.15 * n))


def spark_v(r, x, y, size):
    v = min(1.0, math.exp(-((r / 0.14) ** 2)) + 0.25 * math.exp(-((r / 0.45) ** 2)))
    return v * smoothstep(1.0, 0.8, r)


def streak_v(r, x, y, size):
    """A thin soft line along the vertical axis, fading to nothing at both ends."""
    half = size / 2.0
    dx = (x - half) / half
    dy = (y - half) / half
    line = math.exp(-((dx / 0.07) ** 2))
    along = math.exp(-((dy / 0.55) ** 2)) * smoothstep(1.0, 0.7, abs(dy))
    return clamp01(line * along)


def puff_pixels(size):
    low = make_lattice(PUFF_SEED_LOW, 6)
    high = make_lattice(PUFF_SEED_HIGH, 12)
    pixels = []
    for y in range(size):
        for x in range(size):
            r = radius(x, y, size)
            base = smoothstep(1.0, 0.35, r)
            n1 = value_noise(low, (x + 0.5) / size * 6, (y + 0.5) / size * 6)
            n2 = value_noise(high, (x + 0.5) / size * 12, (y + 0.5) / size * 12)
            noise = 0.7 * n1 + 0.3 * n2
            pixels.append((235, 235, 235, to_byte(base * (0.55 + 0.45 * noise))))
    return pixels


TEXTURES = [
    ("fx_glow.png", 64, lambda size: shape_pixels(size, glow_v)),
    ("fx_plume.png", 64, lambda size: shape_pixels(size, plume_v)),
    ("fx_spark.png", 32, lambda size: shape_pixels(size, spark_v)),
    ("fx_streak.png", 64, lambda size: shape_pixels(size, streak_v)),
    ("fx_puff.png", 64, puff_pixels),
]


def encode_png_pillow(width, height, pixels):
    img = Image.new("RGBA", (width, height))
    img.putdata(pixels)
    buf = io.BytesIO()
    img.save(buf, format="PNG")
    return buf.getvalue()


def encode_png_zlib(width, height, pixels):
    raw = bytearray()
    for y in range(height):
        raw.append(0)  # filter type None
        for x in range(width):
            raw.extend(pixels[y * width + x])

    def chunk(tag, data):
        body = tag + data
        return struct.pack(">I", len(data)) + body + struct.pack(">I", zlib.crc32(body) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)  # 8-bit RGBA
    return (
        b"\x89PNG\r\n\x1a\n"
        + chunk(b"IHDR", ihdr)
        + chunk(b"IDAT", zlib.compress(bytes(raw), 9))
        + chunk(b"IEND", b"")
    )


def write_if_changed(path, data):
    if path.exists() and path.read_bytes() == data:
        return False
    path.write_bytes(data)
    return True


def main():
    encode = encode_png_pillow if Image is not None else encode_png_zlib

    PARTICLE_DIR.mkdir(parents=True, exist_ok=True)
    for name, size, make in TEXTURES:
        pixels = make(size)
        data = encode(size, size, pixels)
        path = PARTICLE_DIR / name
        changed = write_if_changed(path, data)
        print(f"{path.relative_to(ROOT).as_posix()} ({'written' if changed else 'unchanged'})")


if __name__ == "__main__":
    main()
