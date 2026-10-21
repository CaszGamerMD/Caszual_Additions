#!/usr/bin/env python3
"""Crafting vending machine safety contract (source and resource regressions).

Gameplay-level tests still need a server, but these checks guard the
transaction boundaries, UI controls and machine persistence.
"""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
base=ROOT/"src/main/java/com/caszgamermd/caszualadditions/autovending"
r=ROOT/"src/main/resources"
client=ROOT/"src/client/java/com/caszgamermd/caszualadditions/autovending"
block=(base/"CraftVendingBlock.java").read_text()
entity=(base/"CraftVendingBlockEntity.java").read_text()
menu=(base/"CraftVendingMenu.java").read_text()
screen=(client/"CraftVendingScreen.java").read_text()
reg=(base/"CraftVendingContent.java").read_text()
assert "player.isShiftKeyDown()" in block
assert "server.openMenu(machine)" in block
assert "machine.vend(level,player)" in block
assert "useItemOn(" in block and "useWithoutItem(" in block
assert "Containers.dropContents(level,pos,machine)" in block
assert "RECIPE_SLOTS=9,STOCK_SLOTS=27,TOTAL=36" in entity
assert "CraftingInput.of(3,3,samples)" in entity
assert "getRecipeFor(RecipeType.CRAFTING,input,world)" in entity
assert "getRemainingItems(input)" in entity
assert "NonNullList.withSize(STOCK_SLOTS,ItemStack.EMPTY)" in entity
assert "stock.shrink(1)" in entity
assert "Insufficient ingredients" in entity
assert entity.index("if(!found)return") < entity.index("contents.set(RECIPE_SLOTS+i,planned.get(i))")
assert "lastVendTick==world.getGameTime()" in entity
assert "customer.getInventory().placeItemBackInInventory(stack.copy())" in entity
assert "ContainerHelper.saveAllItems(out,contents)" in entity
assert "ContainerHelper.loadAllItems(in,contents)" in entity
assert "new CraftVendingMenu(id,inventory,this)" in entity
assert "MAX_BATCHES=16" in entity
assert "addStandardInventorySlots(player,28,208)" in menu
assert "moveItemStackTo(moving,TEMPLATE,PREVIEW,false)" in menu
assert "mayPickup(Player player){return false;}" in menu
assert "mayPlace(ItemStack stack){return false;}" in menu
assert "handleInventoryButtonClick(menu.containerId,id)" in screen
assert "RACK" not in reg
for file in ["assets/caszual_additions/blockstates/crafting_vending_machine.json",
             "assets/caszual_additions/models/block/crafting_vending_machine.json",
             "assets/caszual_additions/models/item/crafting_vending_machine.json",
             "assets/caszual_additions/items/crafting_vending_machine.json",
             "data/caszual_additions/recipe/crafting_vending_machine.json",
             "data/caszual_additions/loot_table/blocks/crafting_vending_machine.json"]:
    assert (r/file).exists(),file
    json.loads((r/file).read_text())
for file in ["src/main/java/com/caszgamermd/caszualadditions/CaszualAdditions.java",
            "src/main/java/com/caszgamermd/caszualadditions/CaszualItemGroups.java",
            "src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java"]:
    assert "CraftVendingContent" in (ROOT/file).read_text()
print("PASS: craft-on-demand vending has 9 recipe samples, 27 stocked slots, atomic 1-16 batches, shift-right settings and save persistence.")
