#!/usr/bin/env python3
"""Warehouse Builder picker must fill shulkers from all stocked order lines."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
ptp=(root/"PtpBlockEntity.java").read_text()
dispatch=(root/"StorageGolemDispatch.java").read_text()
station=(root/"ShulkerTankEntity.java").read_text()
worker=(root/"StorageGolem.java").read_text()
assert "public OutputOrder nextStockedBuilderOrder(" in ptp
assert "if(!builderReserved()||outputInFlight()||outputOrders.size()<2)" in ptp
assert "outputOrders.addLast(outputOrders.removeFirst());" in ptp
assert "private static PtpBlockEntity.OutputOrder nextStockedBuilderOrder(" in dispatch
assert "needed.containsKey(id)" in dispatch
assert "return output.nextStockedBuilderOrder(present);" in dispatch
assert "PtpBlockEntity.OutputOrder order=nextStockedBuilderOrder(" in dispatch
assert "output.reserveOutput(order.itemId(),amount)" in dispatch
assert "StorageGolemDispatch.continuePacking(" in worker
assert "ptp.outputDelivered(outputTicket,count);" in worker
assert "if(ptp.pendingOutputOrders()==0&&!ptp.outputInFlight())" in station
assert "finishOrder(level,ptp);" in station
assert "if(old.size()==27)" in station
assert "flushFinished(level,ptp,true);" in station
print("PASS: packer can rotate unstocked FIFO head without shipment loss, fill boxes, release only full/finished")
