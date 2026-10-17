#!/usr/bin/env python3
"""Stack of Pallets is never a navigable floor or shortcut for warehouse workers.

The 2x2x4 structure has count-dependent *physical* collision, including
very low decks that an ordinary entity could step across. Navigation
must instead reject any stack segment at the worker's feet, head, or
beneath its feet, irrespective of whether the magazine is empty.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
racks=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
def src(name):return (racks/name).read_text()
stack=src("StackOfPalletsBlock.java")
goals=src("StorageGolemReturnPath.java")
worker=src("StorageGolem.java")
carrier=src("MovingPallet.java")
floors=src("MovingPalletFloorPath.java")
preferred=src("WarehousePreferredPath.java")
assert 'IntegerProperty.create("part",0,3)' in stack
assert 'IntegerProperty.create("layer",0,3)' in stack
assert 'MAX_PALLETS=12' in stack
assert 'return be==null?0:be.count()*PALLET_THICKNESS_PIXELS;' in stack
assert "boolean isPathfindable(BlockState state," in stack
assert "PathComputationType type" in stack
assert "return false;" in stack[stack.index("boolean isPathfindable("):stack.index("@Override protected VoxelShape getCollisionShape(")]
assert "state.getValue(LAYER)==0" in stack
assert "Block.box(0,0,0,16,1,16)" in stack
assert "Block.box(0,0,0,16,Math.min(16,remaining),16)" in stack
# Never allow a step ONTO the top of a pallet stack, even if it is 1/16
# high or has a collision-less upper satellite section in a saved world.
for state in ("feetState", "headState", "below"):
    assert f"{state}.getBlock() instanceof StackOfPalletsBlock" in goals
assert "&&headState.getCollisionShape(level,feet.above()).isEmpty()" in goals
assert "&& !below.getCollisionShape" in goals.replace("&&!below.getCollisionShape", "&& !below.getCollisionShape")
# ALL warehouse path callers must share the same walkability barrier.
assert "StorageGolemReturnPath.walkable(level,p)" in worker
assert "StorageGolemReturnPath.walkable(level,cell)" in carrier
assert "StorageGolemReturnPath.steps(feet,target,available)" in worker
assert "WarehousePreferredPath.leg(feet,target,available" in worker
assert "WarehousePreferredPath.leg(here,target," in carrier
assert "position->safePlatformFloor(level,position)" in carrier
assert "no diagonal" in floors.lower() or "Cardinal steps only" in floors
assert "if(flat!=null)return flat;" in preferred
# Simple 2x2 obstacle regression for four-direction same-Y grid routing.
blocked={(4,4),(5,4),(4,5),(5,5)}
def shortest(start,goal):
    from collections import deque
    q=deque([(start,[start])]);seen={start}
    while q:
        at,path=q.popleft()
        if at==goal:return path
        for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
            nxt=(at[0]+dx,at[1]+dz)
            if nxt in blocked or nxt in seen or any(abs(v)>12 for v in nxt):continue
            seen.add(nxt);q.append((nxt,path+[nxt]))
    return None
for source,dest in [((2,4),(8,4)),((4,2),(4,8)),((3,3),(7,7))]:
    path=shortest(source,dest)
    assert path and not any(cell in blocked for cell in path)
print("PASS: all 2x2 magazine parts (empty through 12-high) block Golem/rig routing; no over-stack stepping")
