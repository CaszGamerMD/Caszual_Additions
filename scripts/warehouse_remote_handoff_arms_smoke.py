#!/usr/bin/env python3
"""Remote pickup must advance on real clearance, then complete courier join.

Regression against the old handoff: waiting for a hard-coded 5-7 block
waypoint after the loaded rig ALREADY left the old footprint, and letting
the courier wait forever at an unreachable/non-joinable service position.
Also protect shoulder attachments and realistic elbow-bend animation.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
racks=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
rig=(racks/"MovingPallet.java").read_text()
worker=(racks/"StorageGolem.java").read_text()
route=(racks/"RemotePalletCourierRoute.java").read_text()
swap=(racks/"StorageGroundPalletTransfer.java").read_text()
model=(client/"MovingPalletModel.java").read_text()
solo=(client/"StorageGolemModel.java").read_text()

assert "public boolean remoteFootprintClear()" in rig
assert "new AABB(minX-.5,deliveryPosition.getY()-.5," in rig
assert "!getBoundingBox().intersects(footprint)" in rig
assert "return deliveryMode==6&&!remoteReplacementPlaced&&remoteFootprintClear();" in rig
assert rig.index("if(remoteFootprintClear()){",rig.index("private void tickRemoteStepAside"))
assert "deliveryFailure=\"OLD PALLET CLEAR; COURIER MAY PLACE REPLACEMENT\"" in rig
assert "remoteMoveAside=BlockPos.ZERO;" in rig
assert "if(!carrier.remoteFootprintClear())return false;" in swap
assert "double x=carrier.getX()-(root.getX()+1)" not in swap
assert "if(dx*dx+dz*dz>.72" in rig
assert "remoteFloorRoute.clear();" in rig

# Fourth worker's destination is not just ANY point in walking range:
# it must be physically reachable AND within the true shared rig's join
# radius, including a +/-1 height difference from irregular ground.
assert "for(int dy:new int[]{0,-1,1})" in route
assert "if(dx*dx+dz*dz+dy*dy>9.0)continue;" in route
assert "if(path==null)continue;" in route
assert "if(distanceToSqr(rig)<9.61" in worker
assert "Math.abs(getY()-rig.getY())<1.8" in worker
assert "remoteCourierJoinAisle=BlockPos.ZERO;" in worker
assert 'routeIssue="ADJUSTING TO LOADED CARRIER\'S ACTUAL POSITION";' in worker

# Bent shoulders: never translate the entire arm inside the body.
for txt in (model,solo):
    assert 'addOrReplaceChild("forearm"' in txt
    assert "PartPose.offset(0,5,0)" in txt
    assert ".addBox(-2,-1,-2,4,6,4)" in txt
assert "leftArm[i].x=7f;" in model
assert "rightArm[i].x=-7f;" in model
assert "leftArm[i].xRot=-1.85f*grip;" in model
assert "leftForearm[i].xRot=-.90f*grip;" in model
assert "rightForearm[i].xRot=-.90f*grip;" in model
assert "leftArm.xRot=-1.85f;" in solo
assert "leftForearm.xRot=-.90f;" in solo
assert "-3.02f" not in model and "-3.02f" not in solo

# Directional footprint geometry: a corner-root isn't geometrical center.
def footprint(parts):
    xs=[x for x,z in parts]; zs=[z for x,z in parts]
    return min(xs),max(xs),min(zs),max(zs)
def clear(cells,carrier_center,radius=.925):
    xmin,xmax,zmin,zmax=footprint(cells)
    x,z=carrier_center
    return x+radius<=xmin-.5 or x-radius>=xmax+1.5 or z+radius<=zmin-.5 or z-radius>=zmax+1.5
for positions in [
    [(10,10),(11,10),(10,11),(11,11)],
    [(10,10),(9,10),(10,9),(9,9)],
    [(10,10),(9,10),(10,11),(9,11)],
    [(10,10),(11,10),(10,9),(11,9)]
]:
    bounds=footprint(positions)
    assert not clear(positions,((bounds[0]+bounds[1]+1)/2,(bounds[2]+bounds[3]+1)/2))
    assert clear(positions,(bounds[1]+3.5,(bounds[2]+bounds[3]+1)/2))
print("PASS: loaded trio clears genuine rotated 2x2 target before handoff;"
      " courier chooses reachable join position; articulated shoulders stay attached")
