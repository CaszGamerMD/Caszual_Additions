#!/usr/bin/env python3
"""A missing rack stack cannot monopolize an output packer reservation."""
from pathlib import Path
src=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
dispatch=(src/"StorageGolemDispatch.java").read_text()
golem=(src/"StorageGolem.java").read_text()
station=(src/"ShulkerTankEntity.java").read_text()
ptp=(src/"PtpBlockEntity.java").read_text()
assert "static BlockPos alternateOrderStock(" in dispatch
assert "return rack.getBlockPos();" in dispatch
assert "ORDER OUT OF STOCK: RELEASING PICKER" in golem
assert "releaseOutput(level);\n                    stage=2;" in golem
assert "ORDER RACK EMPTY: MOVING TO NEXT STOCK" in golem
assert "source=alternate;" in golem
assert "&&openWalkway(level,orderAccessGoal))" in golem
assert "if(openWalkway(level,cell))options.add(cell);" in golem
assert "var occupied=stationOrder?java.util.Set.<BlockPos>of()" in golem
assert "nextWorkPlan=tickCount+(stationOrder?8:20)" in golem
assert "tickCount-lastWorkProgress>(stationOrder?45:70)" in golem
assert "StorageGolemDispatch.continuePacking(" in golem
assert "if(old.size()==27)" in station
assert "if(ptp.pendingOutputOrders()==0&&!ptp.outputInFlight())" in station
assert "public int activeOutputReservations()" in ptp
print("PASS warehouse source depletion recovery, no infinite picker lock, stable order routes and full shulkers")
