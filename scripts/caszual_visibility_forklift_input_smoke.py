#!/usr/bin/env python3
"""Regression: Suite item discovery, Input-rack routing and retained forklift driver."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
java=root/"src/main/java/com/caszgamermd/caszualadditions"
resource=root/"src/main/resources/assets/caszual_additions"
creative=(java/"CaszualItemGroups.java").read_text()
jei=(java/"compat/jei/CaszualJeiPlugin.java").read_text()
racks=java/"racks"
pallet=(racks/"MovingPallet.java").read_text()
input_aisle=(racks/"WarehouseAislePriority.java").read_text()
reach=(racks/"WarehouseSortingReach.java").read_text()
lift=(racks/"WarehouseForklift.java").read_text()
golem=(racks/"StorageGolem.java").read_text()
assert 'ns.startsWith("caszual")' in creative
assert "&& seen.add(item))" in creative
assert 'namespace.startsWith("caszual")' in jei
assert 'registration.addAlias(stack, "Caszual Suite")' in jei
assert 'if(!WarehouseSortingReach.groundRack(level,rack))continue;' in input_aisle
assert 'if(plan==null||plan.height()!=0)continue;' in input_aisle
assert 'return firstAvailableInputRack(level,racks,null);' in input_aisle
assert 'isCollisionShapeFullBlock(level,floor)' in reach
assert 'skippedReturnRack=rack.getBlockPos().immutable();' in pallet
assert 'skipReturnRackUntil=tickCount+400;' in pallet
assert 'WarehouseSortingReach.groundRack(level,candidate)' in pallet
assert 'abortDelivery("ELEVATED RACK REQUIRES FORKLIFT' in pallet
assert 'status=driver==null?"PARKED":"PARKED - DRIVER ON DUTY";' in lift
assert 'public boolean dismissDriver(ServerLevel world)' in lift
assert 'player.getItemInHand(hand).is(RackContent.FORKLIFT_CONTROLLER)' in lift
assert 'GOLEM LEAVING SHELF / BOARDING FORKLIFT' in lift
assert '&& !forkliftDriver' not in golem or '&&!forkliftDriver)checkPendingRecall(level);' in golem
assert 'label=getVehicle() instanceof WarehouseForklift' in golem
model=json.loads((resource/"models/item/forklift_controller.json").read_text())
assert model["textures"]["layer0"]=="caszual_additions:item/forklift_controller"
texture=(resource/"textures/item/forklift_controller.png").read_bytes()
assert texture[:8]==b"\x89PNG\r\n\x1a\n"
assert int.from_bytes(texture[16:20],"big")==32 and int.from_bytes(texture[20:24],"big")==32
lang=json.loads((resource/"lang/en_us.json").read_text())
assert lang["item.caszual_additions.forklift_controller"]=="Forklift Clipboard"
print("PASS: complete Caszual discovery, grounded Input racks with rerouting, retained forklift operator, clipboard asset.")
