#!/usr/bin/env python3
"""Remote empty-pallet courier must avoid neighboring full/empty pallet decks.

The ordinary two-by-two pallet deck has a low (5/16 block) collision shape.
It is an obstacle, NOT a solid full-height floor for a Golem to climb onto.
A nearby pallet must not change the UUID/BlockPos of the saved remote target.
"""
from collections import deque
from pathlib import Path

root = Path(__file__).resolve().parents[1]
racks = root / "src/main/java/com/caszgamermd/caszualadditions/racks"
pallet = root / "src/main/java/com/caszgamermd/caszualadditions/pallet"
walk = (racks / "StorageGolemReturnPath.java").read_text()
golem = (racks / "StorageGolem.java").read_text()
courier = (racks / "RemotePalletCourierRoute.java").read_text()
pallet_block = (pallet / "PalletBlock.java").read_text()

assert "Block.box(0, 0, 0, 16, 5, 16)" in pallet_block
# Each footprint part blocks the feet/body and cannot serve as a floor,
# regardless of its thin collision or whether it is carrying cargo.
for state in ("feetState", "headState", "below"):
    assert f"{state}.getBlock() instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock" in walk
    assert f"{state}.is(com.caszgamermd.caszualadditions.pallet.PalletContent.PALLET_CARGO_COLLISION)" in walk
# Do not regress the requirement that ice/stone remain legitimate floors.
assert "return !below.getCollisionShape(level,ground).isEmpty();" in walk
assert "StorageGolemReturnPath.walkable(level,center)" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(0,0,zSide))" in courier

# Blocked cached waypoints must be avoided and trigger an alternate route,
# not restart the same waypoint against the side of another pallet.
assert "courierBlockedCell=cell.immutable();" in golem
assert "courierAvoidUntil=tickCount+140;" in golem
assert "REROUTING AROUND NEARBY PALLET / BLOCKED AISLE" in golem
assert 'pauseRemotePath("NEARBY PALLET BLOCKS COURIER ROUTE")' in golem

# The courier always uses the UUID-linked mission's original pallet root.
assert "remoteCourierTarget=target.immutable();" in golem
assert "remoteCourierAisle=RemotePalletCourierRoute.chooseServiceAisle(" in golem
assert "level,blockPosition(),remoteCourierTarget,trio," in golem
assert "remoteCourierTarget,rig,this,remoteFresh" in golem
assert 'out.store("remote_courier_target",BlockPos.CODEC,remoteCourierTarget)' in golem
assert '"Scanner: courier is locked to pallet at "' in golem

# Simple grid example: an unrelated pallet with a low deck must block its
# four occupied cells, but a different aisle stays accessible. A route over
# y+1 above the unrelated deck must NOT be accepted as a shortcut.
neighbor = {(4,2), (5,2), (4,3), (5,3)}
target = {(10,2), (11,2), (10,3), (11,3)}
blocked = neighbor | target
start, service = (0,2), (9,0)

def full_size_clear(x, z):
    if (x,z) in blocked: return False
    return any(all((x+dx,z+dz) not in blocked
                   for dx in (0, sx) for dz in (0, sz))
               for sx in (-1,1) for sz in (-1,1))

def route(begin, end):
    todo = deque([begin]); prev={begin:None}
    while todo:
        p = todo.popleft()
        if p == end:
            result=[]
            while p != begin:
                result.append(p); p=prev[p]
            return result[::-1]
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            nxt=(p[0]+dx,p[1]+dz)
            if nxt in prev or not (-3<=nxt[0]<=14 and -4<=nxt[1]<=8): continue
            if not full_size_clear(*nxt): continue
            prev[nxt]=p;todo.append(nxt)
    return None

waypoints=route(start, service)
assert waypoints is not None
assert not any(p in blocked for p in waypoints)
assert not any(p in neighbor for p in waypoints)
print("PASS: unrelated 2x2 pallet is no walkable surface; remote target stays locked; "
      "blocked waypoint triggers detour and scanner diagnostics")
