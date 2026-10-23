package com.caszgamermd.caszualadditions.racks;

import net.minecraft.world.phys.Vec3;

/**
 * Single physical seat anchor shared by the truck model and riding Golem.
 *
 * Local truck model coordinates: +Z is toward the rear counterweight,
 * +X is passenger-right, and the model's origin is centered over the axle.
 * The seat cushion spans X=-6..6, Z=5..15 model pixels, making the center
 * X=0, Z=10px (0.625 block behind the model origin).
 *
 * Keep these values aligned with WarehouseForkliftModel.seat: independent
 * "nudge" constants cause the occupant to float beside the seat in turns.
 */
public final class WarehouseForkliftSeat {
    public static final double LOCAL_RIGHT=0.0;
    public static final double LOCAL_REAR=10.0/16.0;
    /** Feet touch the top of the cushion (10 px above the local ground). */
    public static final double FOOT_HEIGHT=10.0/16.0;
    private WarehouseForkliftSeat(){}

    public static Vec3 riderPosition(Vec3 forkliftBase,float yaw){
        double angle=Math.toRadians(yaw);
        // Rotate both seat offsets with the truck's actual world yaw.
        return new Vec3(
                forkliftBase.x-Math.sin(angle)*LOCAL_REAR
                        +Math.cos(angle)*LOCAL_RIGHT,
                forkliftBase.y+FOOT_HEIGHT,
                forkliftBase.z+Math.cos(angle)*LOCAL_REAR
                        +Math.sin(angle)*LOCAL_RIGHT);
    }
}
