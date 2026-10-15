#!/usr/bin/env python3
"""Protect against 26.2 crashing on duplicate creative-tab item stacks."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
src=(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualItemGroups.java").read_text()
tag=json.loads((root/"src/main/resources/data/caszual_additions/tags/item/caszual_content.json").read_text())
for item in ("wool_helmet","wool_boots","warden_trap"):
    assert f"caszual_additions:{item}" in tag["values"]
    assert "AntiWardenContent." + {"wool_helmet":"WOOL_HELMET","wool_boots":"WOOL_BOOTS","warden_trap":"WARDEN_TRAP"}[item] in src
# The explicit item list and tag iteration must use the SAME guard.
assert "java.util.Set<net.minecraft.world.item.Item> seen" in src
assert "if (seen.add(item))output.accept(new ItemStack(item));" in src
assert "&& seen.add(item)) output.accept(stack);" in src
assert "add.accept(AntiWardenContent.WOOL_HELMET);" in src
assert "add.accept(AntiWardenContent.WOOL_BOOTS);" in src
assert "add.accept(AntiWardenContent.WARDEN_TRAP);" in src
# No unguarded stack submissions are allowed inside this tab callback.
assert "output.accept(new ItemStack(" in src
assert src.count("output.accept(new ItemStack(")==1
assert src.count("output.accept(stack)")==1
print("PASS: all Caszual tab paths use one duplicate guard, including Anti-Warden items")
