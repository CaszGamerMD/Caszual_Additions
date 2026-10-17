#!/usr/bin/env python3
"""Regression: four real Golems depart rather than standing/dancing at the warehouse.

The courier uses an actual 2x2 clear area, not an impossible 3x3 perimeter
around a shelf exit. Members of the same mission must never count one
another as changing blocks in the path planner. Their three goals are
perpendicular to the wrapped pallet face, including east/west orientation.
"""
from pathlib import Path
from collections import deque
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
def src(name):return (root/name).read_text()
courier=src("RemotePalletCourierRoute.java")
worker=src("StorageGolem.java")
squad=src("RemotePalletPickupCrew.java")
dispatch=src("RemotePalletDispatch.java")
assert "for(int xSide:new int[]{-1,1})" in courier
assert "for(int zSide:new int[]{-1,1})" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(xSide,0,zSide))" in courier
assert "StorageGolemReturnPath.walkable(level,center.offset(0,0,zSide))" in courier
assert "if(remotePickup&&other.remotePickup" in worker
assert "remotePickupMission.equals(other.remotePickupMission)" in worker
assert "if(remotePickup&&other.remoteCourier" in worker
assert "remotePickupMission.equals(other.remoteCarrierId)" in worker
assert "remotePickupRally,remotePickupTarget,haulIndex" in worker
assert "individualGoal(BlockPos rally,BlockPos target," in squad
assert "rally.offset(0,0,lane)" in squad
assert "rally.offset(lane,0,0)" in squad
assert "individualGoal(aisle,leader.remotePickupTarget(),index)" in squad
assert "StorageGolemReturnPath.steps(from,center," in squad
assert "worker.beginRemotePickup(" in dispatch
assert "level.addFreshEntity(worker)" in dispatch
assert "level.addFreshEntity(carrier)" not in dispatch
assert "remoteCourierPhase==0" in worker
assert "if(!followCourierRoute(level,remoteCourierAisle,true))" in worker

# A real Golem shelf exit frequently has a shelf wall just behind it.
# A full 3x3 footprint wrongly considers that unwalkable even though
# the Golem with its side-on pallet has two unobstructed rows ahead.
blocked={(x,-1) for x in range(-5,6)}
def clear3(x,z):
    return all((x+dx,z+dz) not in blocked
               for dx in (-1,0,1) for dz in (-1,0,1))
def clear2(x,z):
    if (x,z) in blocked:return False
    return any(all((x+dx,z+dz) not in blocked
                   for dx in (0,side_x) for dz in (0,side_z))
               for side_x in (-1,1) for side_z in (-1,1))
assert not clear3(0,0) and clear2(0,0), "Two-wide shelf exit must be traversable"

def slot(rally,target,i):
    dx, dz=abs(rally[0]-target[0]),abs(rally[1]-target[1])
    return (rally[0],rally[1]+i-1) if dx>dz else (rally[0]+i-1,rally[1])
for rally,target in [((10,5),(5,5)),((5,10),(5,5)),
                     ((0,5),(5,5)),((5,0),(5,5))]:
    goals={slot(rally,target,i) for i in range(3)}
    assert len(goals)==3
    assert all(g!=target for g in goals)
    assert all(abs(g[0]-rally[0])+abs(g[1]-rally[1])<=1 for g in goals)

# The outer pushing lane must admit a cardinal path where a 3x3
# approximation would fail at the departure doorway.
def reachable(start,dest,open_cell):
    q=deque([start]);seen={start}
    while q:
        x,z=q.popleft()
        if (x,z)==dest:return True
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            p=(x+dx,z+dz)
            if p not in seen and abs(p[0])<20 and abs(p[1])<20 and open_cell(*p):
                seen.add(p);q.append(p)
    return False
assert reachable((0,0),(0,7),clear2)
assert not reachable((0,0),(0,7),clear3)
print("PASS: courier can depart a two-wide shelf aisle; trio goals are"
      " perpendicular and do not dynamically obstruct one another")
