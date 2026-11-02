#!/usr/bin/env python3
"""Warehouse worker traffic regression: yield, side-step, detour, no lost cargo."""
from pathlib import Path
from collections import deque
root=Path(__file__).resolve().parents[1]
racks=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
g=(racks/"StorageGolem.java").read_text()
walk=(racks/"StorageGolemReturnPath.java").read_text()
prefer=(racks/"WarehousePreferredPath.java").read_text()

assert "private String trafficAt(ServerLevel level,BlockPos next)" in g
for name in ("StorageGolem.class","MovingPallet.class",
             "PalletJackVehicle.class","WarehouseForklift.class"):
    assert name in g[g.index("private String trafficAt("):
                     g.index("private double routeDistanceSq(",g.index("private String trafficAt("))]
assert "private boolean yieldForTraffic(ServerLevel level,BlockPos next,boolean homeRoute)" in g
assert "tickCount-trafficWaitingSince<24" in g
assert "avoidWorkUntil=tickCount+100;" in g
assert "avoidHomeUntil=tickCount+100;" in g
assert "if(yieldForTraffic(level,next,false))return;" in g
assert "if(yieldForTraffic(level,next,true))return;" in g
assert "forward.getClockWise(),forward.getCounterClockWise()" in g
assert "WarehouseTerrain.safeRise(level,here,pullout)" in g
assert "followTerrainStep(pullout,.82);" in g
assert "trafficPulloutExpires=tickCount+35;" in g
assert "if(stationOrder&&next.equals(orderAccessGoal))" in g
assert "orderAccessExpires=0;" in g
assert "if(!StorageGolemReturnPath.walkable(level,next))" in g
assert "REROUTING AROUND NEW PALLET / BLOCKED AISLE" in g
assert "WarehousePreferredPath.leg(feet,target," in g
assert "&& (tickCount" not in g  # unwanted per-node expensive search guard
assert "&&openWalkway(level,orderAccessGoal))" in g
assert "stage=2;routeIssue=\"ORDER PACKED: RETURNING HOME\";" in g
assert "PalletJackItem.unpack(jack)" not in g  # worker paths never alter jack contents
# Physical pallets/racks remain forbidden as stepping surfaces.
for obstacle in ("RackBlock","StackOfPalletsBlock","PalletBlock"):
    assert "instanceof "+obstacle in walk or (
        "instanceof com.caszgamermd.caszualadditions.pallet."+obstacle in walk)
assert "private static java.util.List<BlockPos> find(" in prefer

# Two workers meeting in an open 3-wide aisle should have a legal alternate
# corridor, but a 2x2 deck placed in the center cannot be crossed.
start=(0,0);finish=(7,0)
pallet={(3,-1),(4,-1),(3,0),(4,0)}
worker={(2,0)}
def route(avoid):
    q=deque([start]);prev={start:None}
    while q:
        x,z=q.popleft()
        if (x,z)==finish:
            result=[]
            while (x,z)!=start:
                result.append((x,z))
                x,z=prev[x,z]
            return result[::-1]
        for dx,dz in ((1,0),(0,1),(-1,0),(0,-1)):
            n=(x+dx,z+dz)
            if n in prev or n in pallet or n in avoid:
                continue
            if not(-2<=n[0]<=9 and -2<=n[1]<=2):
                continue
            prev[n]=(x,z)
            q.append(n)
    return None
r=route(worker)
assert r is not None and finish==r[-1]
assert not any(p in pallet or p in worker for p in r)
# If every lateral escape is walled, waiting is better than clipping.
def pullout(center,forward,blocked):
    right=(forward[1],-forward[0])
    left=(-right[0],-right[1])
    return next(((center[0]+d[0],center[1]+d[1])
                 for d in (right,left)
                 if (center[0]+d[0],center[1]+d[1]) not in blocked),None)
assert pullout((1,0),(1,0),{(1,-1)})==(1,1)
assert pullout((1,0),(1,0),{(1,-1),(1,1)}) is None

print("PASS: warehouse Golems yield 24 ticks, pull aside if safe, reroute past workers/pallets, preserve jobs")
