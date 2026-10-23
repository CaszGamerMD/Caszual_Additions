#!/usr/bin/env python3
"""Forklift must route around rack banks and square its forks on the pallet midpoint."""
from pathlib import Path
from collections import deque

root=Path(__file__).resolve().parents[1]
base=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
fork=(base/"WarehouseForklift.java").read_text()
route=(base/"ForkliftAislePath.java").read_text()
view=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/WarehouseForkliftRenderer.java").read_text()
model=(root/"src/client/java/com/caszgamermd/caszualadditions/racks/WarehouseForkliftModel.java").read_text()
assert 'private static final int MAX_VISITED=18000,MAX_RANGE=64;' in route
assert 'private static final int[][] DIRECTIONS={{1,0},{-1,0},{0,1},{0,-1}};' in route
assert 'int nextCost=cost.get(current)+10+turnCost;' in route
assert 'if(!world.noCollision(vehicle,box))return false;' in route
assert 'for(double px:new double[]{box.minX+inset,box.maxX-inset})' in route
assert 'for(double pz:new double[]{box.minZ+inset,box.maxZ-inset})' in route
assert 'world.hasChunkAt(below)' in route
assert 'return List.of();' in route # no path means NO driving through racks
assert 'Collections.reverse(path);' in route
assert 'path.add(destination);' in route
assert 'ForkliftAislePath.route(world,this,goal)' in fork
assert 'ForkliftAislePath.canStand(world,this,next)' in fork
assert 'getNavigation().stop();' in fork
assert 'aisleRoute=List.of();' in fork
assert 'routeStep++' in fork
assert 'status="WAITING: NO SAFE FORKLIFT AISLE ROUTE TO DOCK";' in fork
assert 'status="REPLANNING FORKLIFT AISLE AROUND OBSTRUCTION";' in fork
assert 'directLaneClear(' not in fork
assert 'powerAlongSafeRoute(' not in fork

# Physical rendering places a centered load 2.82 blocks in front of mast.
assert 'pose.translate(-.90f,.32f+state.mastHeight,-3.72f);' in view
assert 'addBox(-10,-3,-61,4,3,35)' in model
assert 'addBox(6,-3,-61,4,3,35)' in model
assert 'private static Vec3 rackPalletCenter(RackBlockEntity rack)' in fork
assert 'side.getStepX()*.5' in fork and '-face.getStepX()*.5' in fork
assert 'palletCenter.x+outward.getStepX()*2.82' in fork
assert 'palletCenter.z+outward.getStepZ()*2.82' in fork
assert 'new Dock(position,outward.getOpposite())' in fork
assert 'private boolean alignForTransfer(Dock dock)' in fork
assert 'face.getStepX(),face.getStepZ()' in fork
assert 'groundPalletCenter(world,root,facing)' in fork
assert 'ForkliftAislePath.route(world,this,position)' in fork

# A toy layout of a three-block-clear aisle with a rack bank blocking
# the straight line. A 1.2-block chassis requires at least two open cells
# across; a real route must go around the end of the bank.
W,H=16,11
obstacles={(x,z) for x in range(5,11) for z in range(2,8)}
start=(2,5);destination=(13,5)
def possible(x,z):
    # center-only integer grid: a 1.2-wide vehicle fits only if all
    # neighboring corners remain clear, never through the rack.
    for cx in (x-.6,x+.6):
        for cz in (z-.6,z+.6):
            if not (0<=cx<W and 0<=cz<H):return False
            if (int(cx),int(cz)) in obstacles:return False
    return True
assert all(possible(*p) for p in [start,destination])
assert not all(possible(x,5) for x in range(start[0],destination[0]+1))
queue=deque([start]);parent={start:None}
while queue:
    node=queue.popleft()
    if node==destination:break
    for step in ((1,0),(-1,0),(0,1),(0,-1)):
        nxt=(node[0]+step[0],node[1]+step[1])
        if nxt not in parent and possible(*nxt):
            parent[nxt]=node;queue.append(nxt)
assert destination in parent, 'the aisle bank has a usable detour'
walk=[];point=destination
while point:
    walk.append(point);point=parent[point]
walk.reverse()
assert any(p[1]>=9 or p[1]<=0 for p in walk), 'route must circle the rack bank'
assert all(possible(*p) for p in walk)
print('PASS: pathfinding goes around rack banks; chassis fits 3-block aisle; centered pallet docking')
