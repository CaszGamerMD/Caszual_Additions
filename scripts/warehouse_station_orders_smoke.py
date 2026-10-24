#!/usr/bin/env python3
"""Warehouse orders: static safety/regression checks before Java tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
j=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
c=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
def src(p):return p.read_text()
station=src(j/"ShulkerTankEntity.java")
station_block=src(j/"ShulkerTankBlock.java")
golem=src(j/"StorageGolem.java")
dispatch=src(j/"StorageGolemDispatch.java")
net=src(j/"RackComputerNet.java")
screen=src(c/"RackComputerScreen.java")
ptp=src(j/"PtpBlockEntity.java")
router=src(j/"StorageGolemReturnPath.java")
assert "public static final int STATIONS=3;" in station
assert "NonNullList.withSize(STATIONS,ItemStack.EMPTY)" in station
assert "NonNullList.withSize(CAPACITY,ItemStack.EMPTY)" in station
assert 'output.store("station_active_"+i' in station
assert 'output.store("station_ready_"+i' in station
assert 'input.read("station_active_"+i' in station
assert 'input.read("station_ready_"+i' in station
assert "attachedOutput(ServerLevel level)" in station
assert "instanceof PtpHopperPort" in station
assert "attachedOutput(level)!=requested" in station
assert "StorageGolemDispatch.insert(level,ptp.getBlockPos(),moving)==1" in station
assert "takePackedOnBreak()" in station_block
assert "BlockEntityTicker<T> getTicker" in station_block
assert "orderCargo=NonNullList.withSize(3,ItemStack.EMPTY)" in golem
assert "continuePacking(" in golem
assert "packerSlotFree(" in dispatch
assert "orderPackers()" in src(j/"WarehouseComputerBlockEntity.java")
assert "out.store(\"order_slot_\"+i,ItemStack.CODEC" in golem
assert "in.read(\"order_slot_\"+i" in golem
assert "StorageGolemDispatch.insert(level,source,item)" in golem
assert "station.accept(delivery,level,ptp)" in golem
assert "station.finishOrder(level,ptp)" in golem
assert "assignStationOrder" in golem
assert "station.canAccept(preview,level,output)" in dispatch
assert "amount<order.remaining()" in dispatch and "preview.size()<3" in dispatch
assert "queueOutputList" in ptp and "outputOrders.addAll(lines)" in ptp
assert 'new OrderListRequest(computer,rows.toString())' in screen or 'new RackComputerNet.OrderListRequest(' in screen
assert "orderList.put(selectedItemId,total)" in screen
assert "ORDER LIST" in screen and "SUBMIT ORDER LIST" in screen
assert "acceptOrderList(ServerPlayer player,OrderListRequest order)" in net
assert "requested.containsKey(id.toString())" in net
assert "output.queueOutputList(lines)" in net
assert "return floorApproach(level,place);" in golem
# Explicit Builder orders must dispatch even when automatic INPUT sorting is off.
builder=src(j/"BuilderWarehouseFulfillment.java")
shelf=src(j/"StorageGolemShelfEntity.java")
ui=src(c/"RackComputerScreen.java")
assert "tryDispatchQueuedOutput(level,shelf)" in shelf
assert "if(shelf.sortingEnabled)StorageGolemDispatch.tryDispatch(level,shelf);" in shelf
assert "public static boolean tryDispatchQueuedOutput(" in dispatch
assert "tryDispatchQueuedOutput(level,shelf))break;" in builder
assert 'order.addProperty("reason",BuilderWarehouseFulfillment.workStatus(' in net
assert "STATUS: " in ui
assert "NO SHULKER STATION HOPPER ADAPTER LINK" in builder
assert "PICKER CANNOT REACH SOURCE RACK" in builder
assert "public void flushBuilderBatch(ServerLevel level,PtpBlockEntity ptp)" in station
assert "if(ptp.builderReserved())" in golem
assert "station.flushBuilderBatch(level,ptp);" in golem
assert "if(ptp==null||!ptp.builderReserved()||attachedOutput(level)!=ptp)return;" in station
assert "flushFinished(level,ptp,true);" in station

assert "StorageGolemReturnPath.walkable(level,p)" in golem
assert "feetState.getBlock() instanceof RackBlock" in router
print("PASS: atomic order list, three-slot packer, persisted packer count and PTP, full/finished shulkers, adapter-only transfer")

assert "if(ptp.pendingOutputOrders()==0&&!ptp.outputInFlight())" in station
assert "public record PackingControl(" in net
assert "configurePackers(" in net
