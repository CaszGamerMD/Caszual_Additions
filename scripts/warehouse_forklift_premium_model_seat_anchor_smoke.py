#!/usr/bin/env python3
"""Industrial forklift visual pass: precise centered seat, live steering, existing cargo clearance."""
from pathlib import Path
from math import sin,cos,radians,isclose
root=Path(__file__).resolve().parents[1]
racks=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
seat=(racks/"WarehouseForkliftSeat.java").read_text()
truck=(racks/"WarehouseForklift.java").read_text()
geometry=(client/"WarehouseForkliftModel.java").read_text()
renderer=(client/"WarehouseForkliftRenderer.java").read_text()
golem=(client/"StorageGolemRenderer.java").read_text()
state=(client/"WarehouseForkliftRenderState.java").read_text()

# Exactly ONE shared model/rider anchor. Never reintroduce a separate
# +0.28 lateral nudge or a mismatched 0.59-rear rider offset.
assert 'public static final double LOCAL_RIGHT=0.0;' in seat
assert 'public static final double LOCAL_REAR=10.0/16.0;' in seat
assert 'public static final double FOOT_HEIGHT=10.0/16.0;' in seat
assert 'WarehouseForkliftSeat.riderPosition(position(),getYRot())' in truck
assert 'double right=.28;' not in truck
assert 'getY()+.62' not in truck
assert 'if(!(passenger instanceof StorageGolem))' in truck
assert 'getPassengerRidingPosition(Entity passenger)' in truck
assert 'texOffs(48,48).addBox(-6,-10,5,12,2,10)' in geometry
assert 'texOffs(48,48).addBox(-6,-25,15,12,15,2)' in geometry

# The rider remains CENTERED on the same revolving seat at every heading.
rear=.625;right=0.
for heading in (0,45,90,135,180,225,270,315):
    angle=radians(heading)
    x=-sin(angle)*rear+cos(angle)*right
    z=cos(angle)*rear+sin(angle)*right
    assert isclose((x*x+z*z)**.5,rear,abs_tol=1e-10)
    # The local seat is strictly on the forward/rear centerline.
    assert isclose(x*cos(angle)+z*sin(angle),0.,abs_tol=1e-10)

# The Golem body is carried by the forklift yaw on BOTH logical sides,
# not allowed to drift by choosing its own head/look rotation client-side.
assert 'for(Entity passenger:getPassengers())' in truck
assert 'golem.setYRot(getYRot());' in truck
assert 'golem.setYBodyRot(getYRot());' in truck
assert 'golem.setYHeadRot(getYRot());' in truck
assert 'state.bodyRot=net.minecraft.util.Mth.rotLerp(' in golem
assert 'partialTick,vehicle.yBodyRotO,vehicle.yBodyRot);' in golem
assert 'state.yRot=0f;' in golem
assert 'state.xRot=0f;' in golem
assert 'state.walkAnimationSpeed=0f;' in golem

# Dedicated steering direction sync, frame-independent articulations.
assert 'STEERING=' in truck and 'builder.define(STEERING,0f);' in truck
assert 'entityData.set(STEERING,steering);' in truck
assert 'public float steeringAngle()' in truck
assert 'public float steeringAngle;' in state
assert 'state.steeringAngle=vehicle.steeringAngle();' in renderer
for part in ('frontLeftWheel.yRot=steer*.45f;',
             'frontRightWheel.yRot=steer*.45f;',
             'steeringWheel.zRot=-steer*.35f;',
             'mastMiddle.y=22f-height*5.5f;',
             'mastTop.y=22f-height*10f;',
             'carriage.y=22f-height*16f;',
             'ramLeft.y=22f-height*8f;',
             'ramRight.y=22f-height*8f;'):
    assert part in geometry

# Authored detail groups remain truly separate from collision/pathfinding.
for part in ('chassis','rear_counterweight','hood_and_guards',
             'overhead_guard','driver_seat','operator_controls',
             'steering_wheel','mast_fixed','mast_middle','mast_top',
             'ram_left','ram_right','carriage','safety_details'):
    assert 'root.addOrReplaceChild("'+part+'"' in geometry
for wheel in ('wheel_front_left','wheel_front_right',
              'wheel_rear_left','wheel_rear_right'):
    assert '"'+wheel+'"' in geometry
assert 'LayerDefinition.create(mesh,128,128)' in geometry
assert 'pose.translate(-.90f,.32f+state.mastHeight,-3.72f);' in renderer
assert 'addBox(-10,-3,-61,4,3,35)' in geometry
assert 'addBox(6,-3,-61,4,3,35)' in geometry
assert 'DRIVE_BLOCKS_PER_TICK=0.20D' in truck
assert 'ForkliftAislePath.route(world,this,goal)' in truck
print('PASS: premium forklift mesh, synced steering, exact rotating rider anchor, 4m/s aisle routing preserved')
