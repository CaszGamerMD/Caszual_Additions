#!/usr/bin/env python3
"""Forklift logistics and two-block Golem reach: blocking source regressions."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
java=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
def read(file):return (java/file).read_text()
reach=read("WarehouseSortingReach.java")
plan=read("RackPickupPlan.java")
fork=read("WarehouseForklift.java")
io=read("ForkliftPalletIO.java")
item=read("WarehouseForkliftItem.java")
golem=read("StorageGolem.java")
dispatch=read("StorageGolemDispatch.java")
carrier=read("MovingPallet.java")
racks=read("RackBlockEntity.java")
assert "MAX_DROP = 2" in plan and "FORKLIFT_MAX_HEIGHT = 12" in plan
assert "return find(level,rack,MAX_DROP,AISLE_DEPTH)" in plan
assert "return find(level,rack,FORKLIFT_MAX_HEIGHT,2)" in plan
assert "BLOCKS=2" in reach
assert "ax*ax+az*az<=BLOCKS*BLOCKS" in reach
assert "bx*bx+bz*bz<=BLOCKS*BLOCKS" in reach
assert "StorageGolemReturnPath.walkable(level,feet)" in reach
assert "WarehouseSortingReach.withinReach(blockPosition(),rack)" in golem
assert "WarehouseSortingReach.accessible(level,rack)" in dispatch
assert "WarehouseSortingReach.groundRack(level,rack)" in carrier
assert "WarehouseSortingReach.groundRack(level,rack)" in read("StoragePalletCrew.java")
assert "liftToForklift(ItemStack jack)" in racks
assert "installFromForklift(ItemStack jack)" in racks
assert "origin.liftToForklift(jack)" in fork and "rack.installFromForklift(jack)" in fork
assert "ForkliftPalletIO.liftGround" in fork and "ForkliftPalletIO.placeGround" in fork
assert "NonNullList.withSize(217" in io
assert "ItemContainerContents.fromItems(payload)" in io
assert "target.restoreJackCargo(stock)" in io
assert "jack.remove(DataComponents.CONTAINER)" in io
assert "StorageGroundPalletTransfer.wrappedController" in io
assert "driver=worker.getUUID()" in fork
assert "worker.assignForkliftDriver" in fork
assert "if(forkliftDriver){tickForkliftDriver(level);return;}" in golem
assert "startRiding(lift)" in golem
assert "finishForkliftDriving()" in golem
assert "world.getEntity(driver)" in fork
assert "private void endTask(ServerLevel world)" in fork
assert '"PARKED - DRIVER ON DUTY"' in fork
assert "public boolean dismissDriver(ServerLevel world)" in fork
assert "player.getItemInHand(hand).is(RackContent.FORKLIFT_CONTROLLER)" in fork
assert "golem.finishForkliftDriving()" in fork
assert "!forkliftEscort&&!builderPalletJob" in golem
assert "WarehouseSortingReach.groundRack(level,rack)" in carrier
assert "if(hasPackedCargo())" in fork
assert "ItemStack packed=jack.copy();" in fork
assert "public boolean readyForPickup()" in fork
assert "if(!enterRackSafely(world,rack,false))return;" in fork
assert "if(!exitRackSafely(world,origin,true))return;" in fork
assert "busy()" in item and "nearest.order(level,rack,selectedSource)" in item
assert "FORKLIFT_CONTROLLER" in read("RackContent.java")
assert "selectedFloorPallet" in fork
assert "StorageGroundPalletTransfer.wrappedController(level,clicked)" in item
assert "WarehouseForkliftModel::layer" in (
 root/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java").read_text()
assert "mastMiddle.y=22f-height*5.5f" in (client/"WarehouseForkliftModel.java").read_text()
assert "carriage.y=22f-height*16f" in (client/"WarehouseForkliftModel.java").read_text()
assert (root/"src/main/resources/assets/caszual_additions/textures/entity/warehouse_forklift.png").is_file()
assert (root/"src/main/resources/data/caszual_additions/recipe/warehouse_forklift.json").is_file()

# Controller rack selection extends to 20 blocks, never through walls.
assert "RACK_SELECTION_REACH = 20.0D" in item
assert "public InteractionResult use(Level level,Player player,InteractionHand hand)" in item
assert "eye.add(player.getViewVector(1.0F).scale(RACK_SELECTION_REACH))" in item
assert "level.clip(new ClipContext(eye,end," in item
assert "ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player" in item
assert "hit.getType()!=HitResult.Type.BLOCK" in item
assert "if(rack==null||rack.hasPallet())return InteractionResult.PASS;" in item
assert "return dispatch(server,player,player.getItemInHand(hand),rack)" in item
assert "return dispatch(level,player,context.getItemInHand(),rack)" in item
assert "level.mayInteract(player,root)" in item

print("PASS: 2-block sort/reach; grounded crews; one owner for forklift cargo; driver boarding and animated mast")
