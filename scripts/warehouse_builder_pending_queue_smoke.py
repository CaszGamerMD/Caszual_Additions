#!/usr/bin/env python3
"""Builder warehouse order ownership, PTP locking and UI freshness regression."""
from pathlib import Path
root = Path(__file__).resolve().parents[1]
server=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
ui=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
computer=(server/"WarehouseComputerBlockEntity.java").read_text()
orders=(server/"BuilderWarehouseFulfillment.java").read_text()
ptp=(server/"PtpBlockEntity.java").read_text()
net=(server/"RackComputerNet.java").read_text()
screen=(ui/"RackComputerScreen.java").read_text()

# Pending orders live at the computer even before an output point can be
# used, and never create "locked but empty" PTP state.
assert "record PendingBuilder(" in computer
assert "waitingBuilders.put(request,new PendingBuilder(" in computer
assert "builder_waiting_count" in computer
assert "out.store(prefix+\"table\",BlockPos.CODEC,pending.table());" in computer
assert 'in.read(prefix+"table",BlockPos.CODEC)' in computer
assert "queueBuilder(request,owner,table,manifest)" in orders
assert 'new Reply("QUEUED_AT_WAREHOUSE",BlockPos.ZERO' in orders
assert "config.dequeueBuilder(pending.request(),pending.owner(),pending.table())" in orders
assert "ptp.beginBuilderOutput(pending.request(),pending.owner()" in orders
assert orders.index("ptp.readyForOutput()") < orders.index("ptp.beginBuilderOutput(")
assert "public String waitingStatus(" in orders

# Both staged and assigned work are visible in the SAME order feed.
assert "for(var pending:settings.waitingBuilders())" in net
assert 'order.addProperty("status","QUEUED AT WAREHOUSE");' in net
assert '"reason",\n                        BuilderWarehouseFulfillment.waitingStatus' in net
assert "for(PtpBlockEntity point:WarehouseNetwork.fromComputer(level,computer).points())" in net
assert "int priorBuilderCount=builderOrders.size();" in screen
assert "if(priorBuilderCount==0&&builderOrders.size()>0)" in screen
assert "if(++refreshTicks%60==0&&page!=3)sendQuery();" in screen

# A physical PTP can only become reserved after the correct real pallet is
# already ready; pallet loading retains full 3x3x3 air volume.
assert "||!readyForOutput()||!emptyCargo()" in ptp
clearance=ptp[ptp.index("public String palletInstallProblem()"):
              ptp.index("public boolean canInstallPallet()")]
assert "for(int height=1;height<=3;height++)" in clearance
assert "root.offset(x,height,z)" in clearance
print("PASS: persisted warehouse-owned Builder queue, output assignment only when ready, visible live status, full pallet clearance")
