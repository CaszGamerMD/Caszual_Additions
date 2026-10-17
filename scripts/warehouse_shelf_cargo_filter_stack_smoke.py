#!/usr/bin/env python3
"""Regression guard for front shelf doors, load props and pallet preference/UI."""
import json
from pathlib import Path
base=Path(__file__).resolve().parents[1]
j=base/"src/main/java/com/caszgamermd/caszualadditions"
client=base/"src/client/java/com/caszgamermd/caszualadditions"
def get(root,path):return (root/path).read_text()
door=get(j,"racks/StorageGolemShelfDoor.java")
dispatch=get(j,"racks/StorageGolemDispatch.java")
golem=get(j,"racks/StorageGolem.java")
rig=get(j,"racks/StoragePalletCrew.java")
home=get(j,"racks/StorageGolemReturnPath.java")
render=get(client,"racks/StorageGolemRenderer.java")
screen=get(client,"pallet/PalletScreen.java")
filterNet=get(j,"pallet/PalletFilterNet.java")
stack=get(j,"racks/StackOfPalletsItem.java")
assert "StorageGolemShelfBlock.FACING" in door
assert "root.relative(facing,depth)" in door
assert "StorageGolemReturnPath.walkable(level,candidate)" in door
assert "StorageGolemShelfDoor.emerge(worker,shelfDoor" in dispatch
assert "StorageGolemShelfDoor.emerge(golem,shelfDoor" in rig
assert "canDockFrontNearby(feet,shelfPos,facing" in golem
assert "StorageGolemReturnPath.stepsHome(from,shelfPlane," in golem
assert "canDockFrontNearby(" in home
assert "DISPLAY_HELD_I" in golem and "displayedLoad(i)" in render
assert "PalletContent.CARDBOARD_RENDER_PROXY" in render
assert "getMoveControl" in golem
assert "int priority=2;" in dispatch
assert "priority=0;break;" in dispatch
assert "priority=1;" in dispatch
assert "rack.allows(sample)" in dispatch
assert "FILTER_SLOT=PAGE_SIZE" in get(j,"pallet/PalletMenu.java")
assert "new Slot(moduleSlot,0,356,53)" in get(j,"pallet/PalletMenu.java")
assert "boolean filterOpen;" in screen
assert "editGhost(row*4+col)" in screen
assert "if(rules.length>16)" in filterNet
assert "ItemContainerContents.fromItems(safe)" in stack
assert "public static final int CAPACITY=12;" in stack
assert "PalletCargoCollisionBlock.clear" in stack
assert "if(!pallet.isEmpty()" in stack
for name in ("pallet_filter","stack_of_pallets"):
    model=base/f"src/main/resources/assets/caszual_additions/models/item/{name}.json"
    data=json.loads(model.read_text())
    assert len(data["elements"])>=5
print("PASS: real front-door Golems, cargo visuals, 16 sample clipboard, sorting tiers and 12 independent pallets")
