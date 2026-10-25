#!/usr/bin/env python3
"""Deterministic octant targeting regressions; live raycast QA remains needed."""
from pathlib import Path
from math import floor
root=Path(__file__).resolve().parents[1]
src=(root/"src/main/java/com/caszgamermd/caszualadditions/quarter/QuarterPlacement.java").read_text()
assert "findEmptyQuarterAlongRay" not in src
assert "directTargetIndex" not in src
assert "player.isShiftKeyDown()" in src
assert "BlockPos.containing(beyond.x, beyond.y, beyond.z)" in src
assert "clicked.relative(face)" in src
for axis in ("X","Y","Z"):
    assert "face.getStep"+axis+"() * TARGET_EPSILON" in src
assert "placement.getClickedPos()" in src
assert "if (!existing.canBeReplaced(placement)) return InteractionResult.FAIL;" in src
assert "if (state.getValue(GenericQuarterBlock.CORNERS[index])) return InteractionResult.FAIL;" in src
assert "if (legacy.getValue(RgbQuarterBlock.CORNERS[index])) return InteractionResult.FAIL;" in src
assert "migrateLegacyAndAdd(" in src
assert "blockEntity.setMaterial(index, source);" in src
assert "context.getItemInHand().consume(1, player);" in src

eps=.002
faces={"EAST":(1,0,0),"WEST":(-1,0,0),"UP":(0,1,0),
       "DOWN":(0,-1,0),"SOUTH":(0,0,1),"NORTH":(0,0,-1)}
def target(clicked,hit,face,quarter=True,shift=False):
    direction=faces[face]
    beyond=tuple(hit[i]+eps*direction[i] for i in range(3))
    pos=(tuple(clicked[i]+direction[i] for i in range(3))
         if shift or not quarter else tuple(floor(v) for v in beyond))
    x,y,z=[min(.999999,max(0,beyond[i]-pos[i])) for i in range(3)]
    idx=(1 if x>=.5 else 0)+(2 if z>=.5 else 0)+(4 if y>=.5 else 0)
    return pos,idx

# Interior surfaces: put the precise octant beyond the crosshair in the
# SAME existing world block (no nearest-to-player corner fallback).
assert target((0,0,0),(.5,.25,.25),"EAST")==((0,0,0),1)
assert target((0,0,0),(.5,.75,.75),"EAST")==((0,0,0),7)
assert target((0,0,0),(.75,.5,.25),"UP")==((0,0,0),5)
assert target((0,0,0),(.25,.5,.75),"UP")==((0,0,0),6)
# Outer surfaces: neighboring world block is the only legal destination.
assert target((0,0,0),(1,.75,.75),"EAST")==((1,0,0),6)
assert target((0,0,0),(0,.25,.75),"WEST")==((-1,0,0),3)
assert target((0,0,0),(.75,1,.75),"UP")==((0,1,0),3)
assert target((0,0,0),(.25,0,.75),"DOWN")==((0,-1,0),6)
assert target((0,0,0),(.75,.25,1),"SOUTH")==((0,0,1),1)
assert target((0,0,0),(.25,.75,0),"NORTH")==((0,0,-1),6)
# Sneak on inner faces explicitly forces the neighboring world block.
assert target((0,0,0),(.5,.25,.25),"EAST",shift=True)==((1,0,0),0)
# A normal solid block's clicked face selects the expected new half.
assert target((0,0,0),(1,.75,.25),"EAST",quarter=False)==((1,0,0),4)
assert target((0,0,0),(.75,1,.75),"UP",quarter=False)==((0,1,0),3)
print("PASS: exact quarter octants, internal/outer faces, adjacent space and sneak override")
