#!/usr/bin/env python3
"""Static invariants for portable pallet wrapping and four-Golem off-site lift.
A real in-game client is still required to verify 3D model and navigation.
"""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
MAIN=ROOT/"src/main/java/com/caszgamermd/caszualadditions"
CLIENT=ROOT/"src/client/java/com/caszgamermd/caszualadditions"
def j(path): return json.loads((ROOT/path).read_text())
def m(path): return (MAIN/path).read_text()
def c(path): return (CLIENT/path).read_text()
recipe=j("src/main/resources/data/caszual_additions/recipe/portable_pallet_wrapper.json")
assert recipe["result"]["id"]=="caszual_additions:portable_pallet_wrapper"
assert recipe["pattern"]==["GIG","ICI","GIG"]
assert recipe["key"]["G"]=="minecraft:glass_pane"
assert recipe["key"]["I"]=="minecraft:iron_ingot"
assert recipe["key"]["C"]=="minecraft:copper_ingot"
assert j("src/main/resources/assets/caszual_additions/items/portable_pallet_wrapper.json")["model"]
wrapper=m("racks/PortablePalletWrapperItem.java")
for block in ("PalletBlock","RackBlock","PtpBlock"):
    assert block in wrapper
assert "setGlassWrapped" in wrapper and "setSealed" in wrapper
scanner=m("racks/StorageScannerItem.java")
assert "StoragePalletCrew.pickupGround" in scanner
crew=m("racks/StoragePalletCrew.java")
assert "beginGroundPickup" in crew and "wrappedController" in crew
transfer=m("racks/StorageGroundPalletTransfer.java")
assert "for(int i=0;i<216;i++)" in transfer
lift=transfer[transfer.index("    public static boolean lift(ServerLevel"):]
assert lift.index("leader.loadPallet") < lift.index("pallet.clearContent()")
assert lift.index("pallet.clearContent()") < lift.index("level.removeBlock(root,false)")
assert "PalletCargoCollisionBlock.clear" in transfer
golem=m("racks/StorageGolem.java")
for token in ("groundPickup","DISPLAY_PALLET","DISPLAY_CARGO","syncPalletVisual",
              "WarehouseTeamProgress.complete(mask)","StorageGroundPalletTransfer.lift",
              "carryingPallet=ItemStack.EMPTY","out.putBoolean(\"ground_pickup\""):
    assert token in golem,token
state=c("racks/StorageGolemRenderState.java")
renderer=c("racks/StorageGolemRenderer.java")
assert "showSharedPallet" in state and "visibleCargo" in state
assert 'label.startsWith("PALLET 1/4")' in renderer
assert "for(int i=0;i<state.visibleCargo;i++)" in renderer
assert "Blocks.GLASS" in renderer and "state.palletDeck" in renderer
model=c("racks/StorageGolemModel.java")
assert "if(state.carryingPallet)" in model
print("PASS: portable wrapper, ground-pallet single-owner transfer, four-carrier pallet visualization")
