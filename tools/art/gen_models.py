# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Blockstate, block/item model and item definition JSON for the Apoapsis placeholder art.

Run from the repo root: python tools/art/gen_models.py
Formats follow the vanilla 26.3 files: blockstates use "variants", item definitions
under assets/<ns>/items/ use {"model": {"type": "minecraft:model", ...}}.
Deterministic: unchanged files are not rewritten. Textures come from gen_textures.py.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src" / "main" / "resources" / "assets" / "apoapsis"
NS = "apoapsis"

# Facing rotations, copied from the vanilla furnace and barrel blockstates.
FURNACE_LIKE = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}}
BARREL_LIKE = {
    "up": {},
    "down": {"x": 180},
    "north": {"x": 90},
    "south": {"x": 90, "y": 180},
    "west": {"x": 90, "y": 270},
    "east": {"x": 90, "y": 90},
}


def tex(name):
    return f"{NS}:block/{name}"


def single(model):
    return {"variants": {"": {"model": model}}}


def faced(property_name, table, model):
    variants = {}
    for facing in sorted(table):
        entry = {"model": model}
        entry.update(table[facing])
        variants[f"{property_name}={facing}"] = entry
    return {"variants": variants}


# Block id -> blockstate.
BLOCKSTATES = {
    "hull_plate": single(f"{NS}:block/hull_plate"),
    "gyroscope": single(f"{NS}:block/gyroscope"),
    "regolith": single(f"{NS}:block/regolith"),
    "fuel_tank": single(f"{NS}:block/fuel_tank"),
    "cockpit": faced("facing", FURNACE_LIKE, f"{NS}:block/cockpit"),
    "spark_thruster": faced("facing", BARREL_LIKE, f"{NS}:block/spark_thruster"),
}

MODELS = {
    "block/hull_plate": {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": tex("hull_plate")},
    },
    "block/regolith": {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": tex("regolith")},
    },
    "block/gyroscope": {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": tex("gyroscope_top"), "side": tex("gyroscope_side")},
    },
    "block/fuel_tank": {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": tex("fuel_tank_end"), "side": tex("fuel_tank_side")},
    },
    # Unrotated: canopy faces north. South shows the back, the rest the side.
    "block/cockpit": {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": tex("cockpit_side"),
            "down": tex("cockpit_side"),
            "up": tex("cockpit_top"),
            "north": tex("cockpit_front"),
            "south": tex("cockpit_back"),
            "east": tex("cockpit_side"),
            "west": tex("cockpit_side"),
        },
    },
    # Unrotated: exhaust points up. Rotated by the blockstate to follow the facing.
    "block/spark_thruster": {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": tex("spark_thruster_side"),
            "down": tex("spark_thruster_powerhead"),
            "up": tex("spark_thruster_exhaust"),
            "north": tex("spark_thruster_side"),
            "south": tex("spark_thruster_side"),
            "east": tex("spark_thruster_side"),
            "west": tex("spark_thruster_side"),
        },
    },
    "item/hydrolox_canister": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/hydrolox_canister"},
    },
}

# Items that use their block model.
BLOCK_ITEMS = ["hull_plate", "cockpit", "gyroscope", "fuel_tank", "spark_thruster", "regolith"]
ITEM_ONLY = ["hydrolox_canister"]


def item_definition(model):
    return {"model": {"type": "minecraft:model", "model": model}}


def write_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    data = (json.dumps(obj, indent=2) + "\n").encode("utf-8")
    if path.exists() and path.read_bytes() == data:
        status = "unchanged"
    else:
        path.write_bytes(data)
        status = "written"
    print(f"{status:9} {path.relative_to(ROOT).as_posix()}")


def main():
    for block_id, obj in BLOCKSTATES.items():
        write_json(ASSETS / "blockstates" / f"{block_id}.json", obj)
    for model_path, obj in MODELS.items():
        write_json(ASSETS / "models" / f"{model_path}.json", obj)
    for block_id in BLOCK_ITEMS:
        write_json(ASSETS / "items" / f"{block_id}.json",
                   item_definition(f"{NS}:block/{block_id}"))
    for item_id in ITEM_ONLY:
        write_json(ASSETS / "items" / f"{item_id}.json",
                   item_definition(f"{NS}:item/{item_id}"))


if __name__ == "__main__":
    main()
