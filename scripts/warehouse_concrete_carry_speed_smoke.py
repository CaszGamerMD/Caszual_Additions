#!/usr/bin/env python3
"""Loaded pallet rig concrete acceleration regression (Fabric build runs next)."""
from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
base=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
speed=(base/"WarehouseCarrySpeed.java").read_text()
rig=(base/"MovingPallet.java").read_text()
registration=(base/"RackContent.java").read_text()
# 26.2 color blocks have dynamic registry IDs rather than Blocks.COLOR_CONCRETE
# constants. All sixteen vanilla ids are recognized by the shared suffix.
assert "BuiltInRegistries.BLOCK.getKey(block)" in speed
assert 'id.getNamespace().equals("minecraft")' in speed
assert 'id.getPath().endsWith("_concrete")' in speed
assert 'concrete_powder' in speed.lower()
assert "instanceof IndustrialGradeConcreteBlock" in speed
assert "CONCRETE_MULTIPLIER=1.35" in speed
assert "CORNER=0.70" in speed
assert "for(double dx:new double[]{-CORNER,CORNER})" in speed
assert "for(double dz:new double[]{-CORNER,CORNER})" in speed
assert "feetY-.125" in speed
assert "level.hasChunkAt(floor)" in speed
assert "CONCRETE_POWDER" not in speed
assert "CARRY_WALK_SPEED=.144" in rig
assert ".add(Attributes.MOVEMENT_SPEED,CARRY_WALK_SPEED)" in rig
assert "fast=!pallet.isEmpty()&&!delivered&&liftTicks>=12&&onGround()" in rig
assert "WarehouseCarrySpeed.onConcrete(level,getX(),getY(),getZ())" in rig
assert "CARRY_WALK_SPEED*(fast?WarehouseCarrySpeed.CONCRETE_MULTIPLIER:1.0)" in rig
assert "attribute.setBaseValue(wanted)" in rig
assert "updateCarrySurfaceSpeed(server);" in rig
assert "class MovingPallet extends PathfinderMob" in rig
assert "MOVING_PALLET_TYPE" in registration
# Entire carrying rig speeds up; don't change independent Golem walking,
# furniture/pallet motion, remote assignments, cargo, or navigation topology.
golem=(base/"StorageGolem.java").read_text()
assert "WarehouseCarrySpeed" not in golem
assert "MovingPalletFloorPath.route(" in rig
assert "navigateRemoteFloor(" in rig
assert "navigateToRackFront(" in rig
print("PASS: +35% on loaded, grounded all-four-corners concrete; baseline elsewhere")
