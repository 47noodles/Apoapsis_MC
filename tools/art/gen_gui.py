# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""GUI art for Apoapsis: the title emblem and two map textures.

Writes three RGBA PNGs into src/main/resources/assets/apoapsis/textures/gui/:
emblem.png (256x128), verdance_map.png (1024x512,
equirectangular Earth-like colour map, no lighting baked in) and pale_map.png
(256x128, equirectangular grey airless moon).

Run from the repo root: python -X utf8 tools/art/gen_gui.py
Deterministic: fixed seeds, no timestamps, unchanged files are not rewritten.
Needs only Pillow and numpy.
"""
import io
import math
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
GUI_DIR = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis" / "textures" / "gui"
LANCZOS = Image.Resampling.LANCZOS
SS = 4  # supersampling factor for the drawing


def hex_rgb(code):
    return tuple(int(code[i:i + 2], 16) for i in (1, 3, 5))


IVORY = hex_rgb("#e8e2d4")
BRASS = hex_rgb("#c9a25a")
GREY_DOT = hex_rgb("#56616d")

# ---- emblem: tilted brass ring with two dots -------------------------------
def ring_point(cx, cy, a, b, rot_deg, t_deg):
    t = math.radians(t_deg)
    p = math.radians(rot_deg)
    x = cx + a * math.cos(t) * math.cos(p) - b * math.sin(t) * math.sin(p)
    y = cy + a * math.cos(t) * math.sin(p) + b * math.sin(t) * math.cos(p)
    return x, y


def draw_emblem():
    w, h = 256, 128
    img = Image.new("RGBA", (w * SS, h * SS), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    cx, cy, rot = 128, 64, -15

    def ring(a, b):
        pts = [ring_point(cx, cy, a, b, rot, 360.0 * i / 720) for i in range(720)]
        return [(x * SS, y * SS) for x, y in pts]

    draw.polygon(ring(120, 44), fill=BRASS + (255,))
    draw.polygon(ring(115, 39), fill=(0, 0, 0, 0))
    for t_deg, r, col in ((-45, 9, IVORY), (180, 6, GREY_DOT)):
        x, y = ring_point(cx, cy, 117.5, 41.5, rot, t_deg)
        draw.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=col + (255,))
    return img.resize((w, h), LANCZOS)


# ---- equirectangular maps ---------------------------------------------------
def sphere_grid(w, h):
    """Unit-sphere point and latitude (degrees) for every pixel centre, row-major."""
    lon = (np.arange(w) + 0.5) / w * 2.0 * np.pi - np.pi
    lat = np.pi / 2.0 - (np.arange(h) + 0.5) / h * np.pi
    lon_g, lat_g = np.meshgrid(lon, lat)
    cl = np.cos(lat_g)
    pts = np.stack([cl * np.cos(lon_g), cl * np.sin(lon_g), np.sin(lat_g)], axis=-1)
    return pts.reshape(-1, 3), np.degrees(lat_g).reshape(-1)


def make_field(seed, octaves, base_freq, gain=0.5):
    """Fractal value noise on the unit sphere: a function of the 3D point, so the map wraps seamlessly.

    Each octave doubles the lattice frequency and scales the amplitude by gain; the lattice values come from a
    seeded hash, interpolated trilinearly with a smoothstep. The result is roughly zero-mean.
    """
    rng = np.random.default_rng(seed)
    size = 64
    tables = [rng.uniform(-1.0, 1.0, (size, size, size)) for _ in range(octaves)]
    offsets = rng.uniform(0.0, 50.0, (octaves, 3))

    def field(points):
        total = np.zeros(len(points))
        amp = 1.0
        freq = base_freq
        for o in range(octaves):
            p = points * freq + offsets[o]
            i0 = np.floor(p).astype(int)
            f = p - i0
            f = f * f * (3.0 - 2.0 * f)
            acc = np.zeros(len(points))
            for dx in (0, 1):
                for dy in (0, 1):
                    for dz in (0, 1):
                        w = (f[:, 0] if dx else 1 - f[:, 0]) * (f[:, 1] if dy else 1 - f[:, 1])                             * (f[:, 2] if dz else 1 - f[:, 2])
                        acc += w * tables[o][(i0[:, 0] + dx) % size, (i0[:, 1] + dy) % size, (i0[:, 2] + dz) % size]
            total += amp * acc
            amp *= gain
            freq *= 2.0
        return total

    return field


def sstep(e0, e1, x):
    t = np.clip((np.asarray(x, dtype=float) - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def mix(c0, c1, t):
    c0 = np.asarray(c0, dtype=float)
    c1 = np.asarray(c1, dtype=float)
    t = np.asarray(t, dtype=float)[..., None]
    return c0 + (c1 - c0) * t


def to_rgba(rgb, w, h):
    arr = np.clip(np.rint(rgb), 0, 255).astype(np.uint8).reshape(h, w, 3)
    return Image.fromarray(arr).convert("RGBA")


SHALLOW = hex_rgb("#3f76e4")
DEEP = hex_rgb("#0e2a6b")
SAND = hex_rgb("#dbcfa3")
GRASS_LIGHT = hex_rgb("#8db360")
GRASS_DARK = hex_rgb("#6e9a44")
DARK_GREEN = hex_rgb("#4e9e2e")
STONE = hex_rgb("#7e7e7e")
SNOW = hex_rgb("#f9fefe")
ICE = hex_rgb("#c9e4f5")
HIGHLANDS = hex_rgb("#979793")
MARIA = hex_rgb("#515156")


def draw_verdance_map():
    w, h = 1024, 512
    pts, lat = sphere_grid(w, h)
    absl = np.abs(lat)
    height = make_field(11, 7, 1.7, 0.52)(pts)
    sea = np.percentile(height, 58)  # about 42 percent land
    elev = height - sea
    scale = height.std()
    land = elev >= 0.0

    tone = make_field(12, 4, 2.5)(pts)  # independent field for colour variation
    tone = tone / tone.std()
    grass = mix(GRASS_DARK, GRASS_LIGHT, np.clip(0.5 + 0.25 * tone, 0.0, 1.0))
    warm = sstep(0.25, 0.6, tone) * (1.0 - sstep(30.0, 40.0, absl))
    vegetation = mix(grass, DARK_GREEN, warm)
    land_rgb = mix(vegetation, STONE, sstep(1.25 * scale, 1.75 * scale, elev))
    cap = absl + 6.0 * tone
    snow = np.maximum(sstep(2.0 * scale, 2.5 * scale, elev), sstep(77.0, 80.0, cap))
    land_rgb = mix(land_rgb, SNOW, snow)
    land_rgb = mix(SAND, land_rgb, sstep(0.05 * scale, 0.10 * scale, elev))

    depth = np.maximum(-elev, 0.0)
    ocean_rgb = mix(SHALLOW, DEEP, np.clip(depth / (1.5 * scale), 0.0, 1.0))
    ocean_rgb = mix(ocean_rgb, ICE, sstep(78.0, 81.0, absl + 5.0 * tone))

    rgb = np.where(land[:, None], land_rgb, ocean_rgb)

    cloud = make_field(23, 6, 2.6, 0.55)(pts)
    cloud = (cloud - cloud.mean()) / cloud.std()
    thr = np.percentile(cloud, 91)  # about 9 percent coverage
    alpha = (0.65 * sstep(thr, thr + 0.5, cloud))[:, None]
    rgb = rgb * (1.0 - alpha) + 255.0 * alpha
    return to_rgba(rgb, w, h)


def draw_pale_map():
    w, h = 256, 128
    pts, _ = sphere_grid(w, h)
    field = make_field(5, 5, 1.3, 0.5)(pts)
    m01 = (field - field.min()) / (field.max() - field.min())
    edge = np.percentile(m01, 72)  # about 28 percent maria
    maria = np.clip((m01 - (edge - 0.03)) / 0.06, 0.0, 1.0)
    base = mix(HIGHLANDS, MARIA, maria)

    rng = np.random.default_rng([5, 1])  # grain and craters; separate stream from the field
    light = 1.0 + 0.06 * rng.uniform(-1.0, 1.0, len(pts))
    zs = rng.uniform(-1.0, 1.0, 60)
    phis = rng.uniform(0.0, 2.0 * np.pi, 60)
    radii = rng.uniform(1.5, 6.0, 60)  # angular radius in degrees
    for z, phi, rad in zip(zs, phis, radii):
        s = math.sqrt(1.0 - z * z)
        centre = np.array([s * math.cos(phi), s * math.sin(phi), z])
        ang = np.degrees(np.arccos(np.clip(pts @ centre, -1.0, 1.0)))
        r = ang / rad
        light[r < 0.9] *= 0.88
        light[(r >= 0.9) & (r < 1.0)] *= 1.14
    return to_rgba(base * light[:, None], w, h)


def save_png(img, name):
    data = io.BytesIO()
    img.save(data, format="PNG")
    payload = data.getvalue()
    path = GUI_DIR / name
    if path.exists() and path.read_bytes() == payload:
        state = "unchanged"
    else:
        path.write_bytes(payload)
        state = "written"
    print(f"{name} {img.width}x{img.height} {len(payload)} bytes {state}")


def main():
    GUI_DIR.mkdir(parents=True, exist_ok=True)
    save_png(draw_emblem(), "emblem.png")
    save_png(draw_verdance_map(), "verdance_map.png")
    save_png(draw_pale_map(), "pale_map.png")


if __name__ == "__main__":
    main()
