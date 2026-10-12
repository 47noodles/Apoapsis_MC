# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Encode the Apoapsis music tracks to Ogg Vorbis.

Usage:
    python tools/audio/encode_music.py \\
        --galactic GalacticTemple.ogg \\
        --observing ObservingTheStar.ogg \\
        --starfield starfield_romance1.wav \\
        --firstlight first_light_particles_0.wav \\
        --out src/main/resources/assets/apoapsis/sounds/music \\
        --quality 0.45

Each input is scanned once (duration, format, whole-file peak and RMS in dBFS)
and one table row is printed for it. Leading and trailing digital near-silence
(every channel |sample| < 1e-4, about -80 dBFS) is trimmed, keeping 0.05 s of
lead-in and 0.05 s of tail. Nothing else is processed: no gain, no effects.

WAV inputs are encoded to Ogg Vorbis at --quality. Ogg inputs are copied
byte-for-byte when nothing is trimmed, otherwise decoded and re-encoded at
--quality. Every output is then re-opened, decoded in full and reported.

Output names: galactic_temple.ogg, observing_the_star.ogg,
starfield_romance.ogg, first_light_particles.ogg.

Source credits (all CC0, opengameart.org):
    "Galactic Temple" by yd
    "Another space background track" by yd
    "Starfield Romance" by Yoiyami
    "First Light Particles" by Yoiyami

Requires: soundfile (bundles libsndfile with Vorbis) and numpy.
"""

import argparse
import math
import os
import shutil

import numpy as np
import soundfile as sf

BLOCK_FRAMES = 1 << 16
SILENCE_THRESHOLD = 1e-4  # full-scale units, about -80 dBFS
EDGE_PAD_SECONDS = 0.05
VERIFY_TOLERANCE_SECONDS = 0.1

OUTPUT_NAMES = {
    "galactic": "galactic_temple.ogg",
    "observing": "observing_the_star.ogg",
    "starfield": "starfield_romance.ogg",
    "firstlight": "first_light_particles.ogg",
}

IN_ROW = "{:<26} {:>10.3f} {:>8} {:>3} {:>9} {:>10} {:>10}"
IN_HEADER = "{:<26} {:>10} {:>8} {:>3} {:>9} {:>10} {:>10}".format(
    "file", "dur_s", "sr_hz", "ch", "subtype", "peak_dBFS", "rms_dBFS")
OUT_ROW = "{:<26} {:>10.3f} {:>8} {:>3} {:>9} {:>10} {:>10} {:>9.3f}"
OUT_HEADER = "{:<26} {:>10} {:>8} {:>3} {:>9} {:>10} {:>10} {:>9}".format(
    "file", "dur_s", "sr_hz", "ch", "subtype", "peak_dBFS", "rms_dBFS", "size_MB")


def to_dbfs(value):
    if value <= 0.0:
        return float("-inf")
    return 20.0 * math.log10(value)


def fmt_db(value):
    return "{:.2f}".format(to_dbfs(value))


def stream_stats(f):
    """Read an open soundfile to the end in blocks; return whole-file stats."""
    f.seek(0)
    peak = 0.0
    sumsq = 0.0
    count = 0
    first = None
    last = None
    pos = 0
    for block in f.blocks(blocksize=BLOCK_FRAMES, dtype="float64", always_2d=True):
        mag = np.abs(block)
        peak = max(peak, float(mag.max()))
        flat = block.ravel()
        sumsq += float(np.dot(flat, flat))
        count += flat.size
        active = np.flatnonzero(mag.max(axis=1) >= SILENCE_THRESHOLD)
        if active.size:
            if first is None:
                first = pos + int(active[0])
            last = pos + int(active[-1])
        pos += block.shape[0]
    rms = math.sqrt(sumsq / count) if count else 0.0
    return {"frames": pos, "peak": peak, "rms": rms, "first": first, "last": last}


def scan(path):
    """Open one input with soundfile and collect its format and whole-file stats."""
    with sf.SoundFile(path, "r") as f:
        if f.format == "OGG" and f.subtype != "VORBIS":
            raise SystemExit("{}: Ogg stream is {}, not VORBIS".format(path, f.subtype))
        info = {
            "name": os.path.basename(path),
            "path": path,
            "samplerate": f.samplerate,
            "channels": f.channels,
            "subtype": f.subtype,
            "format": f.format,
        }
        stats = stream_stats(f)
    if stats["frames"] == 0:
        raise SystemExit("{}: no audio frames".format(path))
    info.update(stats)
    return info


def keep_range(info):
    """Frame range to keep: strip digital near-silence, keep 0.05 s pad each side."""
    if info["first"] is None:
        raise SystemExit("{}: whole file is digital silence".format(info["name"]))
    pad = int(round(EDGE_PAD_SECONDS * info["samplerate"]))
    start = max(0, info["first"] - pad)
    end = min(info["frames"], info["last"] + 1 + pad)
    return start, end


def encode_range(info, start, end, dst, quality):
    """Write frames [start, end) of the input to an Ogg Vorbis file, in blocks."""
    with sf.SoundFile(info["path"], "r") as src, sf.SoundFile(
        dst,
        "w",
        samplerate=info["samplerate"],
        channels=info["channels"],
        format="OGG",
        subtype="VORBIS",
        compression_level=quality,
    ) as out:
        if start:
            src.seek(start)
        remaining = end - start
        while remaining > 0:
            block = src.read(min(BLOCK_FRAMES, remaining), dtype="float64", always_2d=True)
            if block.shape[0] == 0:
                raise SystemExit("{}: unexpected end of input".format(info["name"]))
            out.write(block)
            remaining -= block.shape[0]


OGG_SERIAL = 0x41504F41  # fixed stream serial so that output bytes are reproducible

_CRC_TABLE = []
for _i in range(256):
    _r = _i << 24
    for _ in range(8):
        _r = ((_r << 1) ^ 0x04C11DB7) & 0xFFFFFFFF if _r & 0x80000000 else (_r << 1) & 0xFFFFFFFF
    _CRC_TABLE.append(_r)


def ogg_crc(buf):
    """CRC-32 as used by Ogg pages (poly 0x04C11DB7, no reflection, init 0)."""
    crc = 0
    table = _CRC_TABLE
    for b in buf:
        crc = ((crc << 8) & 0xFFFFFFFF) ^ table[((crc >> 24) & 0xFF) ^ b]
    return crc


def normalize_ogg(path):
    """Replace the random Ogg stream serial with OGG_SERIAL and fix page CRCs.

    libsndfile picks a random serial, so two runs on the same input would
    otherwise differ in bytes. The audio packets are not touched.
    """
    with open(path, "rb") as fh:
        data = bytearray(fh.read())
    pages = []
    serials = set()
    pos = 0
    while pos < len(data):
        if data[pos:pos + 4] != b"OggS":
            raise SystemExit("{}: malformed Ogg page at byte {}".format(path, pos))
        nseg = data[pos + 26]
        end = pos + 27 + nseg + sum(data[pos + 27:pos + 27 + nseg])
        if end > len(data):
            raise SystemExit("{}: truncated Ogg page at byte {}".format(path, pos))
        serials.add(bytes(data[pos + 14:pos + 18]))
        pages.append((pos, end))
        pos = end
    if len(serials) != 1:
        raise SystemExit("{}: expected one Ogg stream, found {}".format(path, len(serials)))
    serial = OGG_SERIAL.to_bytes(4, "little")
    for start, end in pages:
        data[start + 14:start + 18] = serial
        data[start + 22:start + 26] = b"\0\0\0\0"
        data[start + 22:start + 26] = ogg_crc(data[start:end]).to_bytes(4, "little")
    with open(path, "wb") as fh:
        fh.write(data)


def verify(path, expected_frames):
    """Re-open an output, decode it fully and check the length."""
    with sf.SoundFile(path, "r") as f:
        if f.format != "OGG" or f.subtype != "VORBIS":
            raise SystemExit("{}: not an Ogg Vorbis stream".format(path))
        header_frames = f.frames
        samplerate = f.samplerate
        stats = stream_stats(f)
    if stats["frames"] == 0 or stats["frames"] != header_frames:
        raise SystemExit("{}: decoded {} frames, header says {}".format(
            path, stats["frames"], header_frames))
    tolerance = VERIFY_TOLERANCE_SECONDS * samplerate
    if abs(stats["frames"] - expected_frames) > tolerance:
        raise SystemExit("{}: decoded {} frames, expected {}".format(
            path, stats["frames"], expected_frames))
    return stats


def main(argv=None):
    parser = argparse.ArgumentParser(description="Encode Apoapsis music to Ogg Vorbis.")
    parser.add_argument("--galactic", required=True, help="GalacticTemple .ogg")
    parser.add_argument("--observing", required=True, help="ObservingTheStar .ogg")
    parser.add_argument("--starfield", required=True, help="starfield_romance WAV")
    parser.add_argument("--firstlight", required=True, help="first_light_particles WAV")
    parser.add_argument("--out", required=True, help="output directory")
    parser.add_argument("--quality", type=float, default=0.45,
                        help="Vorbis quality 0.0-1.0 (default 0.45)")
    args = parser.parse_args(argv)

    if not 0.0 <= args.quality <= 1.0:
        parser.error("--quality must be between 0.0 and 1.0")
    inputs = [
        ("galactic", args.galactic),
        ("observing", args.observing),
        ("starfield", args.starfield),
        ("firstlight", args.firstlight),
    ]
    for _, path in inputs:
        if not os.path.isfile(path):
            parser.error("input not found: {}".format(path))

    print("INPUTS")
    print(IN_HEADER)
    infos = {}
    for key, path in inputs:
        info = scan(path)
        infos[key] = info
        print(IN_ROW.format(
            info["name"],
            info["frames"] / info["samplerate"],
            info["samplerate"],
            info["channels"],
            info["subtype"],
            fmt_db(info["peak"]),
            fmt_db(info["rms"]),
        ))
        if info["channels"] == 1:
            print("  WARNING: {} is mono".format(info["name"]))

    os.makedirs(args.out, exist_ok=True)
    print()
    print("OUTPUTS (quality {}, dir {})".format(args.quality, args.out))
    print(OUT_HEADER)
    for key, path in inputs:
        info = infos[key]
        samplerate = info["samplerate"]
        start, end = keep_range(info)
        # An Ogg input is only re-encoded when the trim is audible (over 20 ms); a copy loses nothing.
        slack = int(0.02 * samplerate)
        trimmed = start > slack or info["frames"] - end > slack
        if not trimmed and info["format"] != "OGG":
            trimmed = (start, end) != (0, info["frames"])
        dst = os.path.join(args.out, OUTPUT_NAMES[key])
        is_ogg = info["format"] == "OGG"
        if is_ogg and not trimmed:
            shutil.copyfile(path, dst)
            action = "copied byte-for-byte"
        else:
            encode_range(info, start, end, dst, args.quality)
            normalize_ogg(dst)
            action = "re-encoded" if is_ogg else "encoded"
        stats = verify(dst, end - start)
        size_mb = os.path.getsize(dst) / 1e6
        print(OUT_ROW.format(
            OUTPUT_NAMES[key],
            stats["frames"] / samplerate,
            samplerate,
            info["channels"],
            "VORBIS",
            fmt_db(stats["peak"]),
            fmt_db(stats["rms"]),
            size_mb,
        ))
        print("  {}; trimmed {:.3f} s lead, {:.3f} s tail; decode OK".format(
            action,
            start / samplerate,
            (info["frames"] - end) / samplerate,
        ))
        if info["channels"] == 1:
            print("  WARNING: {} is mono".format(OUTPUT_NAMES[key]))


if __name__ == "__main__":
    main()
