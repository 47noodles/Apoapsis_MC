# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Check the Apoapsis placeholder art: every id has its JSON and every reference resolves.

Run from the repo root: python tools/art/check_assets.py   (exit status 0 when clean)
Reads only; needs no Pillow (PNG sizes come from the IHDR chunk).
"""
import json
import struct
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis"
NS = "apoapsis"

# Registry ids from docs/dev/prototype-contract.md, with the blockstate variants each needs.
BLOCK_VARIANTS = {
    "hull_plate": {""},
    "cockpit": {f"facing={d}" for d in ("north", "east", "south", "west")},
    "gyroscope": {""},
    "fuel_tank": {""},
    "spark_thruster": {f"facing={d}" for d in ("up", "down", "north", "south", "east", "west")},
    "nose_cone": {f"facing={d}" for d in ("up", "down", "north", "south", "east", "west")},
    "fin": {f"facing={d}" for d in ("north", "east", "south", "west")},
    "window": {""},
    "regolith": {""},
}
ITEM_ONLY = ["hydrolox_canister"]
TEXTURES = {
    "block/hull_plate", "block/cockpit_front", "block/cockpit_side", "block/cockpit_top",
    "block/cockpit_back", "block/gyroscope_top", "block/gyroscope_side", "block/fuel_tank_side",
    "block/fuel_tank_end", "block/spark_thruster_exhaust", "block/spark_thruster_powerhead",
    "block/spark_thruster_side", "block/nose_cone_side", "block/nose_cone_tip", "block/fin",
    "block/window_side", "block/regolith", "item/hydrolox_canister",
}
PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"


def rel(path):
    return path.relative_to(ROOT).as_posix()


def load_json(path, errors):
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        errors.append(f"{rel(path)}: does not parse: {exc}")
        return None


def png_info(path):
    """Return (width, height, bit depth, colour type) from the IHDR chunk, or None."""
    with path.open("rb") as handle:
        head = handle.read(26)
    if len(head) < 26 or head[:8] != PNG_SIGNATURE or head[12:16] != b"IHDR":
        return None
    return struct.unpack(">IIBB", head[16:26])


def split_ref(ref):
    """'apoapsis:block/x' -> ('apoapsis', 'block/x'); vanilla refs return None."""
    namespace, _, path = ref.partition(":")
    if namespace != NS or not path:
        return None
    return path


def main():
    errors = []
    referenced_textures = set()
    model_files = {}

    for path in sorted(ASSETS.rglob("*.json")):
        obj = load_json(path, errors)
        if obj is None:
            continue
        if path.is_relative_to(ASSETS / "models"):
            model_files[path.relative_to(ASSETS / "models").with_suffix("").as_posix()] = obj

    def check_model_ref(ref, where):
        path = split_ref(ref)
        if path is None:
            return
        if not path.startswith("block/") and not path.startswith("item/"):
            errors.append(f"{where}: model {ref} is outside block/ and item/")
        elif path not in model_files:
            errors.append(f"{where}: model {ref} has no file under models/")

    def check_texture_ref(ref, where):
        path = split_ref(ref)
        if path is None:
            return
        referenced_textures.add(path)
        if path not in TEXTURES:
            errors.append(f"{where}: texture {ref} is not in the expected set")
        png = ASSETS / "textures" / f"{path}.png"
        if not png.is_file():
            errors.append(f"{where}: texture {ref} has no file {rel(png)}")
            return
        info = png_info(png)
        if info != (16, 16, 8, 6):
            errors.append(f"{rel(png)}: expected 16x16 8-bit RGBA, got {info}")

    # Blockstates: one per block, with the right variant keys.
    for block_id, variants in BLOCK_VARIANTS.items():
        path = ASSETS / "blockstates" / f"{block_id}.json"
        obj = load_json(path, errors) if path.is_file() else None
        if obj is None:
            errors.append(f"{rel(path)}: missing blockstate for block {block_id}")
            continue
        found = set(obj.get("variants", {}))
        if found != variants:
            errors.append(f"{rel(path)}: variants {sorted(found)} != {sorted(variants)}")
        for key, entry in obj.get("variants", {}).items():
            for item in (entry if isinstance(entry, list) else [entry]):
                check_model_ref(item.get("model", ""), rel(path))

    # Block and item-only models and item definitions.
    for block_id in BLOCK_VARIANTS:
        if f"block/{block_id}" not in model_files:
            errors.append(f"models/block/{block_id}.json is missing")
    for item_id in ITEM_ONLY:
        if f"item/{item_id}" not in model_files:
            errors.append(f"models/item/{item_id}.json is missing")
        if (ASSETS / "blockstates" / f"{item_id}.json").exists():
            errors.append(f"blockstates/{item_id}.json should not exist (item only)")

    for item_id in list(BLOCK_VARIANTS) + ITEM_ONLY:
        path = ASSETS / "items" / f"{item_id}.json"
        obj = load_json(path, errors) if path.is_file() else None
        if obj is None:
            errors.append(f"{rel(path)}: missing item definition")
            continue
        model = obj.get("model", {})
        if model.get("type") != "minecraft:model":
            errors.append(f"{rel(path)}: model.type must be minecraft:model")
        check_model_ref(model.get("model", ""), rel(path))

    # Model parents and textures.
    for name, obj in model_files.items():
        where = f"models/{name}.json"
        parent = obj.get("parent")
        if parent:
            parent_path = split_ref(parent)
            if parent_path is not None and parent_path not in model_files:
                errors.append(f"{where}: parent {parent} has no file")
        for value in obj.get("textures", {}).values():
            if value.startswith("#"):
                continue
            if split_ref(value) is not None:
                check_texture_ref(value, where)

    # Every texture on disk is used by a model, and every expected texture exists.
    on_disk = set()
    for png in sorted((ASSETS / "textures").rglob("*.png")):
        on_disk.add(png.relative_to(ASSETS / "textures").with_suffix("").as_posix())
    for missing in sorted(TEXTURES - on_disk):
        errors.append(f"textures/{missing}.png is missing")
    for orphan in sorted(on_disk - referenced_textures):
        errors.append(f"textures/{orphan}.png is not referenced by any model")

    if errors:
        print("FAILED:")
        for line in errors:
            print("  " + line)
        return 1
    print(f"OK: {len(BLOCK_VARIANTS)} blocks and {len(ITEM_ONLY)} item-only id complete; "
          f"{len(model_files)} models; {len(on_disk)} textures, all 16x16 RGBA and referenced.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
