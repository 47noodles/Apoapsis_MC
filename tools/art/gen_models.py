# SPDX-FileCopyrightText: 2026 The Apoapsis contributors
# SPDX-License-Identifier: MIT
"""Blockstate, block/item model and item definition JSON for the Apoapsis placeholder art.

Run from the repo root: python tools/art/gen_models.py
Formats follow the vanilla 26.3 files: blockstates use "variants", item definitions
under assets/<ns>/items/ use {"model": {"type": "minecraft:model", ...}}.
Deterministic: unchanged files are not rewritten. Textures come from gen_textures.py.
The shaped models (spark_thruster bell, nose_cone, fin) are written by hand and not generated here.
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
    "fuel_refinery": {"variants": {"lit=false": {"model": f"{NS}:block/fuel_refinery"},
                                   "lit=true": {"model": f"{NS}:block/fuel_refinery_lit"}}},
    "cockpit": faced("facing", FURNACE_LIKE, f"{NS}:block/cockpit"),
    "spark_thruster": faced("facing", BARREL_LIKE, f"{NS}:block/spark_thruster"),
    "nose_cone": faced("facing", BARREL_LIKE, f"{NS}:block/nose_cone"),
    "fin": faced("facing", FURNACE_LIKE, f"{NS}:block/fin"),
    "window": single(f"{NS}:block/window"),
}

MODELS = {
    "block/hull_plate": {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": tex("hull_plate")},
    },
    # A full cube with tintindex 0 on every face, so Pale can tint its regolith (client/planet/PlanetBlockTints).
    # Parent block/block for the display transforms, as vanilla's tinted grass_block does.
    "block/regolith": {
        "parent": "minecraft:block/block",
        "textures": {"all": tex("regolith"), "particle": tex("regolith")},
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {face: {"texture": "#all", "cullface": face, "tintindex": 0}
                      for face in ("down", "up", "north", "south", "west", "east")},
        }],
    },
    "block/gyroscope": {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": tex("gyroscope_top"), "side": tex("gyroscope_side")},
    },
    "block/fuel_tank": {
        "parent": "minecraft:block/cube_column",
        "textures": {"end": tex("fuel_tank_end"), "side": tex("fuel_tank_side")},
    },
    # The grate faces north; no facing for the MVP. The lit model swaps in the glowing grate while it refines.
    **{f"block/fuel_refinery{suffix}": {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": tex("fuel_refinery_side"),
            "down": tex("fuel_refinery_side"),
            "up": tex("fuel_refinery_top"),
            "north": tex("fuel_refinery_front" + suffix),
            "south": tex("fuel_refinery_side"),
            "east": tex("fuel_refinery_side"),
            "west": tex("fuel_refinery_side"),
        },
    } for suffix in ("", "_lit")},
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
    # Hull block with portholes on its four sides.
    "block/window": {
        "parent": "minecraft:block/cube",
        "textures": {
            "particle": tex("window_side"),
            "down": tex("hull_plate"),
            "up": tex("hull_plate"),
            "north": tex("window_side"),
            "south": tex("window_side"),
            "east": tex("window_side"),
            "west": tex("window_side"),
        },
    },
    "item/hydrolox_canister": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/hydrolox_canister"},
    },
    "item/pale_sample": {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"{NS}:item/pale_sample"},
    },
}

# Items that use their block model.
BLOCK_ITEMS = ["hull_plate", "cockpit", "gyroscope", "fuel_tank", "fuel_refinery", "spark_thruster", "nose_cone", "fin", "window",
               "regolith"]
ITEM_ONLY = ["hydrolox_canister", "pale_sample"]


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
