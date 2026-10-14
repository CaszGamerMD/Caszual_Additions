#!/usr/bin/env python3
"""Prevent return-home navigation regressions."""
from pathlib import Path
s=(Path(__file__).resolve().parents[1]/
   "src/main/java/com/caszgamermd/caszualadditions/racks/StorageGolem.java").read_text()
tick=s[s.index("    @Override public void tick()"):s.index("    @Override protected void addAdditionalSaveData(")]
assert tick.index("if(stage==2){") < tick.index("if(haulJob){")
assert "releaseOutput(level);" in tick
assert "returnToShelf(level);" in tick
assert "if(stationOrder&&!orderItems().isEmpty())dropOrderCargo(level);" in tick
method=s[s.index("    private void releaseOutput("):s.index("    public BlockPos home()")]
assert "returnToShelf(level)" not in method
assert "releaseOutput(level);" not in method
home=s[s.index("    private void returnToShelf("):s.index("    private Container loaded(")]
assert "StorageGolemReturnPath.route(from,shelfPlane," in home
assert "StorageGolemReturnPath.walkable(level,p)" in home
assert "No level path? WAIT and retry" in home
assert "StorageGolemReturnPath.canDock(feet,shelfPlane," in s
assert "Math.abs(feet.getY()-home.getY())>1" in s
assert "StorageGolemReturnPath.walkable(level,p)" in home
print("PASS: return-home jobs use flat routes and output release does not recurse")
