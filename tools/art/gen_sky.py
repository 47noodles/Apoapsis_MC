# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Deterministic sky for Apoapsis: a star catalogue and a Milky Way texture.

Writes src/main/resources/assets/apoapsis/textures/environment/stars.bin
(magic APSK, version 1, 9000 stars, galactic basis in inertial axes) and
milky_way.png (2048x1024 RGB, galactic frame, linear light, global peak = 255; columns are equal
steps of longitude, rows equal steps of sin(latitude), north at the top).
With --preview PATH it also writes a galactic-frame preview with the stars splatted on.

Run from the repo root: python tools/art/gen_sky.py [--preview PATH]
Everything is computed (value noise, Planck blackbody, CIE 1931 fit); nothing is downloaded or painted.
Deterministic: fixed seeds, no timestamps, unchanged files are not rewritten.
"""
import argparse
import io
import math
import struct
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ENV_DIR = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis" / "textures" / "environment"
STARS_PATH = ENV_DIR / "stars.bin"
MILKY_PATH = ENV_DIR / "milky_way.png"

W, H = 2048, 1024
STAR_COUNT = 9000
STAR_SEED = 20260
TEX_SEED = 7
DEG = math.pi / 180.0
STAR_DTYPE = np.dtype([("xyz", "<f4", (3,)), ("mag", "u1"), ("rgb", "u1", (3,))])


def _unit(v):
    v = np.asarray(v, dtype=np.float64)
    return v / np.linalg.norm(v)


GZ = _unit([-0.814, -0.296, 0.5])
_V = np.array([0.17, 0.98, 0.0])
GX = _unit(_V - np.dot(_V, GZ) * GZ)
GY = np.cross(GZ, GX)
BASIS = np.stack([GX, GY, GZ])  # rows: galactic x, y, z axes expressed in inertial axes


def galactic_vectors(l, b):
    """Unit vectors in galactic-frame components for longitude l and latitude b (radians)."""
    cb = np.cos(b)
    return np.stack([cb * np.cos(l), cb * np.sin(l), np.sin(b)], axis=-1)


# ---------------------------------------------------------------- noise

_MUL = (
    np.uint64(0x9E3779B97F4A7C15),
    np.uint64(0xC2B2AE3D27D4EB4F),
    np.uint64(0x165667B19E3779F9),
    np.uint64(0x27D4EB2F165667C5),
)


def _lattice(ix, iy, iz, seed):
    """Hash integer lattice points to uniform values in [0, 1)."""
    h = ix.astype(np.uint64) * _MUL[0]
    h ^= iy.astype(np.uint64) * _MUL[1]
    h ^= iz.astype(np.uint64) * _MUL[2]
    h ^= np.uint64((seed * 0x27D4EB2F165667C5) & 0xFFFFFFFFFFFFFFFF)
    h ^= h >> np.uint64(33)
    h *= np.uint64(0xFF51AFD7ED558CCD)
    h ^= h >> np.uint64(33)
    h *= np.uint64(0xC4CEB9FE1A85EC53)
    h ^= h >> np.uint64(33)
    return (h >> np.uint64(11)).astype(np.float64) * (1.0 / 9007199254740992.0)


def value_noise(p, seed):
    """3D value noise in [0, 1) for points p of shape (N, 3), smoothstep-weighted trilinear blend."""
    cell = np.floor(p)
    f = p - cell
    s = f * f * (3.0 - 2.0 * f)
    ci = cell.astype(np.int64)
    out = np.zeros(p.shape[0])
    for dz in (0, 1):
        wz = s[:, 2] if dz else 1.0 - s[:, 2]
        for dy in (0, 1):
            wy = s[:, 1] if dy else 1.0 - s[:, 1]
            for dx in (0, 1):
                wx = s[:, 0] if dx else 1.0 - s[:, 0]
                v = _lattice(ci[:, 0] + dx, ci[:, 1] + dy, ci[:, 2] + dz, seed)
                out += v * (wx * wy * wz)
    return out


def fbm(p, octaves, seed):
    """Fractal sum of value noise, lacunarity 2.0, gain 0.5, normalised to [0, 1]."""
    total = np.zeros(p.shape[0])
    norm = 0.0
    amp = 1.0
    freq = 1.0
    for k in range(octaves):
        total += amp * value_noise(p * freq, seed + 101 * k)
        norm += amp
        amp *= 0.5
        freq *= 2.0
    return total / norm


def smoothstep(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


# ---------------------------------------------------------------- Milky Way

def milky_way_texture():
    """Return (uint8 image H x W x 3 in galactic frame, max over |b| > 45 deg as 0..255)."""
    jj, ii = np.mgrid[0:H, 0:W]
    u = ((ii + 0.5) / W).ravel()
    v = ((jj + 0.5) / H).ravel()
    l = 2.0 * np.pi * u
    b = np.arcsin(1.0 - 2.0 * v)  # rows are equal steps of sin(b): cheap to sample by direction
    G = galactic_vectors(l, b)

    ang = np.arccos(np.clip(G[:, 0], -1.0, 1.0))  # distance from l=0, b=0
    band = np.exp(-0.5 * (b / (7 * DEG)) ** 2) + 0.45 * np.exp(-0.5 * (b / (20 * DEG)) ** 2)
    bulge = 1.2 * np.exp(-0.5 * (ang / (20 * DEG)) ** 2)
    glow = band * (0.55 + bulge)
    glow *= 0.65 + 0.7 * fbm(G * 10.0, 5, TEX_SEED + 1)

    offset = np.array([17.3, -4.1, 9.7])
    dust = smoothstep(0.52, 0.72, fbm(G * 3.0 + offset, 6, TEX_SEED + 2))
    dust *= np.exp(-0.5 * ((b + 2 * DEG) / (4.5 * DEG)) ** 2)
    glow *= 1.0 - 0.85 * dust
    ridge = 1.0 - np.abs(2.0 * fbm(G * 6.0, 4, TEX_SEED + 3) - 1.0)
    glow *= 1.0 - 0.35 * smoothstep(0.75, 0.95, ridge) * np.exp(-0.5 * (b / (8 * DEG)) ** 2)
    glow /= glow.max()  # band peak = 1; nebula strengths are relative to this

    w = np.clip(bulge / 1.2, 0.0, 1.0)[:, None]
    cool = np.array([0.70, 0.80, 1.00])
    warm = np.array([1.00, 0.80, 0.55])
    col = cool * (1.0 - w) + warm * w
    for c, seed in enumerate((TEX_SEED + 4, TEX_SEED + 5, TEX_SEED + 6)):
        n = np.clip((fbm(G * 1.5, 2, seed) - 0.5) * 3.0, -1.0, 1.0)
        col[:, c] *= 1.0 + 0.06 * n
    col = np.where((dust > 0.3)[:, None], col * np.array([1.0, 0.88, 0.78]), col)

    rgb = glow[:, None] * col

    nebulae = (
        (25.0, -4.0, 5.0, (1.0, 0.35, 0.40), 0.30, TEX_SEED + 7),
        (140.0, 3.0, 4.0, (0.30, 0.80, 0.90), 0.22, TEX_SEED + 8),
        (290.0, -9.0, 6.0, (0.80, 0.40, 0.90), 0.18, TEX_SEED + 9),
    )
    for l0, b0, radius, colour, strength, seed in nebulae:
        c0 = galactic_vectors(l0 * DEG, b0 * DEG)
        dist = np.arccos(np.clip(G @ c0, -1.0, 1.0))
        near = np.nonzero(dist < 4.0 * radius * DEG)[0]  # beyond 4 radii the gaussian is below 0.1%
        gauss = np.exp(-0.5 * (dist[near] / (radius * DEG)) ** 2)
        amp = strength * gauss * (0.5 + fbm(G[near] * 15.0, 4, seed))
        rgb[near] += amp[:, None] * np.array(colour)

    peak = rgb.max()
    img = np.clip(np.round(rgb / peak * 255.0), 0, 255).astype(np.uint8).reshape(H, W, 3)
    far = int(img.reshape(-1, 3)[np.abs(b) > 45 * DEG].max())
    return img, far


# ---------------------------------------------------------------- stars

def _g(x, mu, s1, s2):
    """Piecewise Gaussian lobe from Wyman, Sloan and Shirley (2013)."""
    s = np.where(x < mu, s1, s2)
    return np.exp(-0.5 * ((x - mu) / s) ** 2)


def blackbody_rgb8(T):
    """Linear sRGB (uint8) of blackbody temperatures T (kelvin), 45% mixed toward white."""
    nm = np.arange(380.0, 781.0, 5.0)
    lam = nm * 1e-9
    h, c, k = 6.62607015e-34, 2.99792458e8, 1.380649e-23
    planck = (2.0 * h * c * c / lam ** 5) / np.expm1((h * c) / (lam[None, :] * k * T[:, None]))
    xb = 1.056 * _g(nm, 599.8, 37.9, 31.0) + 0.362 * _g(nm, 442.0, 16.0, 26.7) - 0.065 * _g(nm, 501.1, 20.4, 26.2)
    yb = 0.821 * _g(nm, 568.8, 46.9, 40.5) + 0.286 * _g(nm, 530.9, 16.3, 31.1)
    zb = 1.217 * _g(nm, 437.0, 11.8, 36.0) + 0.681 * _g(nm, 459.0, 26.0, 13.8)
    xyz = planck @ np.stack([xb, yb, zb], axis=1)
    M = np.array([
        [3.2406, -1.5372, -0.4986],
        [-0.9689, 1.8758, 0.0415],
        [0.0557, -0.2040, 1.0570],
    ])
    rgb = np.clip(xyz @ M.T, 0.0, None)
    rgb /= rgb.max(axis=1, keepdims=True)
    rgb = 0.55 * rgb + 0.45
    return np.clip(np.round(rgb * 255.0), 0, 255).astype(np.uint8)


def build_stars():
    """Return (stars.bin bytes, inertial xyz float32 (N,3), magnitude (N,), rgb uint8 (N,3)), brightest first."""
    rng = np.random.default_rng(STAR_SEED)

    ls, bs = [], []
    total = 0
    while total < STAR_COUNT:
        n = 40000
        b = np.arcsin(rng.uniform(-1.0, 1.0, n))
        l = rng.uniform(0.0, 2.0 * np.pi, n)
        ang = np.arccos(np.clip(galactic_vectors(l, b)[:, 0], -1.0, 1.0))
        weight = (0.30 + np.exp(-0.5 * (b / (14 * DEG)) ** 2)
                  + 0.8 * np.exp(-0.5 * (ang / (30 * DEG)) ** 2))
        keep = rng.random(n) < weight / 2.1
        ls.append(l[keep])
        bs.append(b[keep])
        total += int(keep.sum())
    l = np.concatenate(ls)[:STAR_COUNT]
    b = np.concatenate(bs)[:STAR_COUNT]

    a = 0.45 * math.log(10.0)
    lo, hi = math.exp(a * -1.5), math.exp(a * 6.8)
    mag = np.log(lo + rng.random(STAR_COUNT) * (hi - lo)) / a

    r = rng.random(STAR_COUNT)
    t = rng.random(STAR_COUNT)
    T = np.where(r < 0.08, 9000.0 + 11000.0 * t,
                 np.where(r < 0.20, 3000.0 + 1200.0 * t, 4200.0 + 3300.0 * t))

    order = np.argsort(mag, kind="stable")
    mag = mag[order]
    l = l[order]
    b = b[order]
    T = T[order]

    inert = (galactic_vectors(l, b) @ BASIS).astype("<f4")
    rgb8 = blackbody_rgb8(T)
    magcode = np.clip(np.round((mag + 2.0) * 25.0), 0, 255).astype(np.uint8)

    rec = np.zeros(STAR_COUNT, dtype=STAR_DTYPE)
    rec["xyz"] = inert
    rec["mag"] = magcode
    rec["rgb"] = rgb8
    header = struct.pack("<4sII9f", b"APSK", 1, STAR_COUNT, *[float(x) for x in BASIS.ravel()])
    data = header + rec.tobytes()
    assert len(data) == 48 + 16 * STAR_COUNT
    return data, inert, mag, rgb8


# ---------------------------------------------------------------- output

def preview_image(tex, inert, mag, rgb8):
    """Galactic-frame texture at 0.30 with stars splatted as small gaussians."""
    gal = inert.astype(np.float64) @ BASIS.T
    l = np.mod(np.arctan2(gal[:, 1], gal[:, 0]), 2.0 * np.pi)
    b = np.arcsin(np.clip(gal[:, 2], -1.0, 1.0))
    px = (l / (2.0 * np.pi)) * W - 0.5
    py = (0.5 - 0.5 * np.sin(b)) * H - 0.5
    img = tex.astype(np.float64) / 255.0 * 0.30
    peak = np.minimum(1.0, 10.0 ** (-0.4 * 0.55 * (mag - 1.0)))
    colour = rgb8.astype(np.float64) / 255.0
    cx = np.rint(px).astype(np.int64)
    cy = np.rint(py).astype(np.int64)
    for oy in range(-3, 4):
        for ox in range(-3, 4):
            ix = cx + ox
            iy = cy + oy
            ok = (iy >= 0) & (iy < H)
            dx = ix - px
            dy = iy - py
            wgt = peak * np.exp(-0.5 * (dx * dx + dy * dy) / 0.81)
            ix = np.mod(ix, W)
            np.add.at(img, (iy[ok], ix[ok]), wgt[ok, None] * colour[ok])
    return np.clip(np.round(img * 255.0), 0, 255).astype(np.uint8)


def encode_png(img):
    buf = io.BytesIO()
    Image.fromarray(img).save(buf, format="PNG", optimize=False)
    return buf.getvalue()


def write_if_changed(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists() and path.read_bytes() == data:
        return "unchanged"
    path.write_bytes(data)
    return "written"


def main():
    parser = argparse.ArgumentParser(description="Generate the Apoapsis sky: star catalogue and Milky Way texture.")
    parser.add_argument("--preview", metavar="PATH", help="also write a galactic-frame preview PNG to PATH")
    args = parser.parse_args()

    stars_bytes, inert, mag, rgb8 = build_stars()
    tex, far = milky_way_texture()
    png = encode_png(tex)

    print("stars.bin: %s (%d bytes, %d stars)" % (write_if_changed(STARS_PATH, stars_bytes), len(stars_bytes), len(mag)))
    print("milky_way.png: %s (%d bytes, %dx%d)" % (write_if_changed(MILKY_PATH, png), len(png), W, H))
    print("texture max=%d mean=%.3f max over |b|>45deg=%d/255" % (int(tex.max()), float(tex.mean()), far))
    print("stars: m<1.5 = %d, m<3.0 = %d" % (int((mag < 1.5).sum()), int((mag < 3.0).sum())))

    if args.preview:
        prev = encode_png(preview_image(tex, inert, mag, rgb8))
        print("preview: %s (%d bytes)" % (write_if_changed(Path(args.preview), prev), len(prev)))


if __name__ == "__main__":
    main()
