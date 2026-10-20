#!/usr/bin/env python3
"""Rack Computer mining should require a pickaxe and return one block item."""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
resources=root/"src/main/resources"
content=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/RackContent.java").read_text()
block=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/RackComputerBlock.java").read_text()
tag=json.loads((resources/"data/minecraft/tags/block/mineable/pickaxe.json").read_text())
loot=json.loads((resources/"data/caszual_additions/loot_table/blocks/rack_computer.json").read_text())

assert 'RACK_COMPUTER=Registry.register' in content
assert 'new RackComputerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)' in content
computer=content[content.index('RACK_COMPUTER=Registry.register'):content.index('COMPUTER_ITEM=blockItem("rack_computer"')]
assert '.requiresCorrectToolForDrops()' in computer
assert '.strength(3.5f)' in computer
assert 'COMPUTER_ITEM=blockItem("rack_computer",RACK_COMPUTER)' in content
assert tag.get("replace") is False
assert tag["values"].count("caszual_additions:rack_computer")==1
assert loot["type"]=="minecraft:block"
assert len(loot["pools"])==1
pool=loot["pools"][0]
assert pool["rolls"]==1
assert pool["entries"]==[{"type":"minecraft:item","name":"caszual_additions:rack_computer"}]
assert {"condition":"minecraft:survives_explosion"} in pool["conditions"]
# Keep rack computer placement, rotation, GUI and network functionality.
assert "getStateForPlacement(BlockPlaceContext context)" in block
assert 'new RackComputerNet.Open(pos)' in block
assert 'BlockStateProperties.HORIZONTAL_FACING' in block
print("PASS: rack computer pickaxe-only drops, single-item loot, rotation and UI preserved")
