#!/usr/bin/env python3
"""Fail warehouse CI if IGC recipes, color models, or Golem console wiring disappear.
These are static checks, not gameplay simulation.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/"src/main/resources/assets/caszual_additions"
DATA=ROOT/"src/main/resources/data/caszual_additions/recipe"
COLORS=("white","orange","magenta","light_blue","yellow","lime",
        "pink","gray","light_gray","cyan","purple","blue","brown",
        "green","red","black")
for color in COLORS:
    name=color+"_industrial_grade_concrete"
    recipe=json.loads((DATA/(name+".json")).read_text())
    assert recipe["type"]=="minecraft:crafting_shaped"
    assert recipe["pattern"]==["CIC","III","CIC"]
    assert recipe["key"]["C"]=="minecraft:"+color+"_concrete"
    assert recipe["key"]["I"]=="minecraft:iron_ingot"
    assert recipe["result"]=={"id":"caszual_additions:"+name,"count":4}
    model=json.loads((ASSETS/"models/block"/(name+".json")).read_text())
    assert model["textures"]["concrete"]=="minecraft:block/"+color+"_concrete"
    assert len(model["elements"])>=2
    state=json.loads((ASSETS/"blockstates"/(name+".json")).read_text())
    assert state["variants"][""]["model"]=="caszual_additions:block/"+name
    item=json.loads((ASSETS/"items"/(name+".json")).read_text())
    assert item["model"]["model"]=="caszual_additions:block/"+name
    loot=json.loads((ROOT/"src/main/resources/data/caszual_additions/loot_table/blocks"/
                     (name+".json")).read_text())
    assert loot["pools"][0]["entries"][0]["name"]=="caszual_additions:"+name

harvest=json.loads((ROOT/"src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json").read_text())
stone=json.loads((ROOT/"src/main/resources/data/minecraft/tags/block/needs_stone_tool.json").read_text())
for color in COLORS:
    name="caszual_additions:"+color+"_industrial_grade_concrete"
    assert name in harvest["values"] and name in stone["values"]
def source(name):
    return (ROOT/"src"/name).read_text()
network=source("main/java/com/caszgamermd/caszualadditions/racks/WarehouseNetwork.java")
assert "MAX_CELLS=16384" in network and "level.hasChunkAt" in network
for part in ("IndustrialGradeConcreteBlock","RackBlock","RackConnectorBlock",
             "PtpBlock","StorageGolemShelfBlock","ShulkerTankBlock"):
    assert part in network
assert "RackNetwork.network(level,rack)" in network
racks=source("main/java/com/caszgamermd/caszualadditions/racks/RackNetwork.java")
assert "WarehouseNetwork.fromComputer(level,computer)" in racks
computer=source("main/java/com/caszgamermd/caszualadditions/racks/RackComputerNet.java")
assert "GolemWork.TYPE" in computer and 'root.add("workers",workerRows)' in computer
screen=source("client/java/com/caszgamermd/caszualadditions/racks/RackComputerScreen.java")
for text in ("STORAGE GOLEMS","RUN QUEUE","SCANNER HELP","RECALL ALL","FOLLOWING"):
    assert text in (screen+source("main/java/com/caszgamermd/caszualadditions/racks/StorageGolem.java"))
print("PASS: 16 IGC recipes and models, connected network, Golem task page")
