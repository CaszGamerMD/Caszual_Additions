#!/usr/bin/env python3
"""PTP regressions: Builder OUTPUT toggles and realistic empty-pallet clearance."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
server = root / "src/main/java/com/caszgamermd/caszualadditions/racks"
client = root / "src/client/java/com/caszgamermd/caszualadditions/racks"
ptp = (server / "PtpBlock.java").read_text()
state = (server / "PtpBlockEntity.java").read_text()
net = (server / "RackComputerNet.java").read_text()
screen = (client / "RackComputerScreen.java").read_text()
builder = (server / "BuilderWarehouseFulfillment.java").read_text()

# Shift+empty-hand now changes input/output regardless of pallet occupancy.
# Shift+held-pallet still recovers an empty unreserved pallet for the player.
assert "if(player.isShiftKeyDown()){" in ptp
assert "if(ptp.changeModeFromComputer())" in ptp
assert "retrieveEmptyPallet(ptp,player)" in ptp
assert "player.isShiftKeyDown()" in ptp
assert "Cannot change PTP mode: " in ptp

# Every control shares the same live-order safety conditions; never orphan a
# builder shipment, output picker ticket or hauling job by reversing a lane.
assert "public String modeChangeProblem()" in state
for condition in ("builderReserved()","hauling()","outputInFlight()",
                  "pendingOutputOrders()>0"):
    assert condition in state[state.index("public String modeChangeProblem()"):
                              state.index("public boolean changeModeFromComputer()")]
assert "if(!modeChangeProblem().isEmpty())return false;" in state
assert "case 0->target.changeModeFromComputer();" in net
assert "cell.addProperty(\"modeLocked\"," in net
assert 'point.get("modeLocked").getAsBoolean()' in screen
assert 'answer(player,new Query(computer,"",BlockPos.ZERO));' in net

# Empty pallet requires only a usable access layer, NOT an empty 3x3x3
# cargo tower. If a real obstacle remains, show its exact block position/type
# instead of accusing an already-empty whole PTP.
start = state.index("public String palletInstallProblem()")
end = state.index("public boolean canInstallPallet()")
clearance = state[start:end]
assert "root.offset(x,1,z)" in clearance
assert "for(int y=1;y<=3;y++)" not in clearance
assert "p.toShortString()" in clearance
assert "BuiltInRegistries.BLOCK" in clearance
assert "obstruction.canBeReplaced()" in clearance
assert "return palletInstallProblem().isEmpty()" in state
assert '"Cannot load pallet: "+ptp.palletInstallProblem()' in ptp

# Builder output requires a real unsealed pallet in OUTPUT mode.
assert "public boolean readyForOutput(){return !input&&!sealed&&hasPallet();}" in state
assert "ptp.readyForOutput()" in builder
print("PASS: existing-pallet INPUT/OUTPUT toggles, safe Builder reservations, precise one-layer clearance diagnostics")
