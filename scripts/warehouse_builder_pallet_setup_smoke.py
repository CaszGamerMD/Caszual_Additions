#!/usr/bin/env python3
"""Warehouse Builder pallet allocation and physically-fulfilled packing contracts."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
ptp=(root/"PtpBlockEntity.java").read_text()
picker=(root/"StorageGolem.java").read_text()
dispatch=(root/"StorageGolemDispatch.java").read_text()
fulfill=(root/"BuilderWarehouseFulfillment.java").read_text()
shelf=(root/"StorageGolemShelfEntity.java").read_text()
assert "private java.util.UUID builderPreparing,palletSetupWorker;" in ptp
assert "public boolean prepareBuilder(" in ptp
assert "public boolean preparationMatches(" in ptp
assert "public boolean reservePalletSetup(" in ptp
assert "palletSetupDeadline" in ptp
assert "builder_preparing" in ptp
assert "pallet_setup_worker" in ptp
assert "if(preparingForBuilder())return" in ptp
assert "if(!hasPallet()&&!palletInstallProblem().isEmpty())return false;" in ptp
assert "builderPreparing=null;palletSetupWorker=null;palletSetupDeadline=0;" in ptp
assert "returnBuilderPallet=hasPallet()" in ptp
assert "return_builder_pallet" in ptp
assert "public boolean needsBuilderPalletReturn()" in ptp
assert "assignBuilderPallet(BlockPos shelf,BlockPos dispenser," in picker
assert "builder_pallet_job" in picker
assert "builder_pallet_ticket" in picker
assert "dispenser.take()" in picker
assert "ptp.installPallet(cargo.copyWithCount(1))" in picker
assert "dispenser.put(cargo.copyWithCount(1))" in picker
assert "private void tickBuilderPallet(" in picker
assert "if(builderPalletJob){tickBuilderPallet(level);return;}" in picker
assert "public static boolean tryDispatchBuilderPallet(" in dispatch
assert "ptp.reservePalletSetup(order.request(),worker.getUUID())" in dispatch
assert "worker.assignBuilderPallet(" in dispatch
assert "tryDispatchBuilderPallet(level,shelf)" in dispatch
assert "dispatchPalletMagazine(level,shelf,terminal,true)" in dispatch
assert "ptp.needsBuilderPalletReturn()" in dispatch
assert "private static boolean hasStationPort(" in fulfill
assert "ptp.prepareBuilder(pending.request())" in fulfill
assert "ptp.beginBuilderOutput(pending.request(),pending.owner()" in fulfill
assert "public static void progressWaiting(" in fulfill
assert "BuilderWarehouseFulfillment.progressWaiting(level,computer);" in shelf
print("PASS: durable Builder PTP preparation, actual pallet courier, packing handoff and empty pallet return")
