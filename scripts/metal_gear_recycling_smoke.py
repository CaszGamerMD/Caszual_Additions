"""Smoke-test the 26.2 vanilla metal-gear recycling overrides.

The recipe IDs deliberately match the six vanilla nugget recipes, avoiding
ambiguous competing smelting/blasting recipes for the same input gear.
"""
from __future__ import annotations
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RECIPES = ROOT / "src/main/resources/data/minecraft/recipe"
TOOLS = ("pickaxe", "shovel", "axe", "hoe", "sword", "spear")
ARMOR = ("helmet", "chestplate", "leggings", "boots", "horse_armor", "nautilus_armor")
EXPECTED = {
    "copper": ("copper_ingot", [f"minecraft:copper_{part}" for part in (*TOOLS, *ARMOR)]),
    "iron": ("iron_ingot", [f"minecraft:iron_{part}" for part in (*TOOLS, *ARMOR)] +
             [f"minecraft:chainmail_{part}" for part in ("helmet", "chestplate", "leggings", "boots")]),
    "gold": ("gold_ingot", [f"minecraft:golden_{part}" for part in (*TOOLS, *ARMOR)]),
}
seen = set()
for metal, (result, ingredients) in EXPECTED.items():
    for method, time in (("smelting", 200), ("blasting", 100)):
        path = RECIPES / f"{metal}_nugget_from_{method}.json"
        recipe = json.loads(path.read_text(encoding="utf-8"))
        assert recipe["type"] == f"minecraft:{method}", path
        assert recipe["ingredient"] == ingredients, path
        assert recipe["result"] == {"id": f"minecraft:{result}"}, path
        assert recipe["cookingtime"] == time and recipe["experience"] == 0.1, path
        if method == "smelting":
            duplicates = seen.intersection(ingredients)
            assert not duplicates, f"Multiple outputs for: {duplicates}"
            seen.update(ingredients)
print(f"Metal gear recycling verified: 6 vanilla overrides, {len(seen)} different gear items, two furnace types.")
