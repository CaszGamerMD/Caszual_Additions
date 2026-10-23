#!/usr/bin/env python3
"""Blocking regression checks for Golem task feedback and 3-wide aisle routes."""
from pathlib import Path
j=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
g=(j/"StorageGolem.java").read_text()
r=(j/"RackPickupPlan.java").read_text()
p=(j/"StorageGolemReturnPath.java").read_text()
m=(j/"MovingPallet.java").read_text()
assert "public static final int AISLE_DEPTH=3" in r
assert "return find(level,rack,MAX_DROP,AISLE_DEPTH);" in r
assert "for(int depth=1;depth<=accessDepth&&clear;depth++)" in r
assert "int depth=worker<2 ? 2 : 3;" in r
assert "accessibleRows(int clearDepth)" in r
assert "depth<=4" not in r
assert "StorageGolemReturnPath.stepsHome(" in g
assert "WarehousePreferredPath.leg(feet,target," in g
assert "followTerrainStep(next,1.08)" in g
assert "steerOnFloor(next,speed)" in g
assert "getJumpControl().jump();" in g
assert "getMoveControl().setWantedPosition" in g
assert "occupiedAisle(level,target)" in g
assert "occupiedAisle(level,shelfPlane)" in g
assert "routeIssue=\"REROUTING\"" in g
assert "routeIssue=\"WAITING FOR SAFE WALKABLE ROUTE\"" in g
assert "private void updateTaskLabel()" in g
assert '"PALLET "+(haulIndex+1)+"/4 | WITH PLAYER"' in g
assert '"PALLET "+(haulIndex+1)+"/4 | WAITING FOR PLAYER"' in g
assert 'label="CREW "+(haulIndex+1)+"/4 | IDLE";' in g
assert "setCustomNameVisible(true)" in g
assert "updateTaskLabel();" in g
assert "String displayed=showDebugStatuses()?golemName+" in g
assert "if(tickCount%2!=0)return" not in g
assert "private void updateCrewLabel()" in m
assert "4 GOLEMS | DELIVERING TO RACK" in m
assert "4 GOLEMS | FOLLOWING PLAYER" in m
assert "if(tickCount%10==0)updateCrewLabel()" in m
print("PASS: three-block rack aisles, nearby robot avoidance, step-by-step steering, and visible work labels")
