package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * High-detail vanilla-styled counterbalance warehouse forklift.
 *
 * All dimensions are authored in Minecraft model pixels (16px per block).
 * The cab's physical SEAT IS CENTERED at (X=0, Z=+8) and its cushion top
 * is 10px above the ground, exactly matching WarehouseForkliftSeat.
 *
 * Local -Z is the front of the vehicle. The fully supported two-tine
 * carriage spans local -Z 23..64; the real 1.8x1.8 cargo rendering remains
 * where it was on the forks, never intersecting the golem or cage.
 *
 * Cosmetic geometry has no effect on the server collision box, 3-block
 * aisle pathing, pallet saving, mast height or player interactions.
 */
public final class WarehouseForkliftModel extends EntityModel<WarehouseForkliftRenderState> {
    private final ModelPart frontLeftWheel,frontRightWheel,rearLeftWheel,rearRightWheel;
    private final ModelPart mastMiddle,mastTop,carriage;
    private final ModelPart ramLeft,ramRight,steeringWheel;

    public WarehouseForkliftModel(ModelPart root){
        super(root);
        frontLeftWheel=root.getChild("wheel_front_left");
        frontRightWheel=root.getChild("wheel_front_right");
        rearLeftWheel=root.getChild("wheel_rear_left");
        rearRightWheel=root.getChild("wheel_rear_right");
        mastMiddle=root.getChild("mast_middle");
        mastTop=root.getChild("mast_top");
        carriage=root.getChild("carriage");
        ramLeft=root.getChild("ram_left");
        ramRight=root.getChild("ram_right");
        steeringWheel=root.getChild("steering_wheel");
    }

    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();

        // Structural lower frame and steel protective belly, distinct from
        // the orange body panels. The inset toe step makes cab entry obvious.
        root.addOrReplaceChild("chassis",CubeListBuilder.create()
                .texOffs(70,0).addBox(-10,-7,-19,20,5,36)
                .texOffs(0,0).addBox(-10,-9,-18,20,3,33)
                .texOffs(70,0).addBox(-11,-3,-17,22,2,28)
                .texOffs(70,18).addBox(-12,-8,-16,2,4,28)
                .texOffs(70,18).addBox(10,-8,-16,2,4,28)
                .texOffs(48,48).addBox(-10,-8,-21,20,3,4)
                .texOffs(70,18).addBox(-10,-6,15,20,5,3),
                PartPose.offset(0,24,0));

        // Rounded-in-blocks counterweight: sloped shoulder approximated by
        // stepped chamfers, bevel end caps and realistic rear service panel.
        root.addOrReplaceChild("rear_counterweight",CubeListBuilder.create()
                .texOffs(0,0).addBox(-10,-18,8,20,11,8)
                .texOffs(0,0).addBox(-9,-21,10,18,4,6)
                .texOffs(0,23).addBox(-7,-22,11,14,2,5)
                .texOffs(70,0).addBox(-9,-16,16,18,6,2)
                .texOffs(70,0).addBox(-7,-18,17,14,4,2)
                .texOffs(48,48).addBox(-10,-11,17,20,2,2)
                .texOffs(98,0).addBox(-5,-19,18,10,1,1),
                PartPose.offset(0,24,0));

        // Visible tapered nose, curved-looking wheel arches, footboard and
        // side panels. Everything sits behind the mast and forks.
        root.addOrReplaceChild("hood_and_guards",CubeListBuilder.create()
                .texOffs(0,0).addBox(-9,-16,-17,18,3,12)
                .texOffs(0,23).addBox(-8,-19,-15,16,3,7)
                .texOffs(48,48).addBox(-9,-18,-18,18,2,3)
                .texOffs(0,0).addBox(-13,-11,-16,3,4,12)
                .texOffs(0,0).addBox(10,-11,-16,3,4,12)
                .texOffs(70,18).addBox(-13,-11,9,3,5,8)
                .texOffs(70,18).addBox(10,-11,9,3,5,8)
                .texOffs(70,0).addBox(-9,-7,-4,18,1,12)
                .texOffs(48,58).addBox(-9,-8,-4,18,1,12)
                .texOffs(70,18).addBox(-14,-8,0,4,3,12)
                .texOffs(70,18).addBox(10,-8,0,4,3,12),
                PartPose.offset(0,24,0));

        // Four individual wheels. Tread shoulder/cap relief gives them
        // depth while the hub remains centered on each rotating axle.
        frontWheel(root,"wheel_front_left",-11.5f,-9.5f);
        frontWheel(root,"wheel_front_right",11.5f,-9.5f);
        rearWheel(root,"wheel_rear_left",-11.5f,13.5f);
        rearWheel(root,"wheel_rear_right",11.5f,13.5f);

        // Upright overhead guard: hollow cab with tapered shoulder posts,
        // full canopy, cross braces, front/rear grab rails, and no solid wall
        // that would visually hide a centered seated operator.
        root.addOrReplaceChild("overhead_guard",CubeListBuilder.create()
                .texOffs(70,18).addBox(-12,-49,7,3,42,3)
                .texOffs(70,18).addBox(9,-49,7,3,42,3)
                .texOffs(70,18).addBox(-12,-49,-10,3,39,3)
                .texOffs(70,18).addBox(9,-49,-10,3,39,3)
                .texOffs(70,0).addBox(-13,-52,-11,26,3,22)
                .texOffs(0,80).addBox(-13,-54,-12,26,2,24)
                .texOffs(70,18).addBox(-11,-51,-11,22,2,2)
                .texOffs(70,18).addBox(-11,-51,9,22,2,2)
                .texOffs(70,18).addBox(-11,-51,-8,2,2,17)
                .texOffs(70,18).addBox(9,-51,-8,2,2,17)
                .texOffs(70,18).addBox(-12,-28,7,2,3,3)
                .texOffs(70,18).addBox(10,-28,7,2,3,3)
                .texOffs(98,0).addBox(-10,-52,0,20,1,2),
                PartPose.offset(0,24,0));

        // Actual fitted seat. Cushion TOP at model local y=-10 = world
        // height 10/16, center X=0 and center Z=8 pixels. The backrest is
        // behind the golem's shoulders and the modest armrests do not
        // obscure its steering pose.
        root.addOrReplaceChild("driver_seat",CubeListBuilder.create()
                .texOffs(48,48).addBox(-6,-10,3,12,3,10)
                .texOffs(70,0).addBox(-6,-8,3,12,5,10)
                .texOffs(48,48).addBox(-6,-25,12,12,16,3)
                .texOffs(70,18).addBox(-6,-26,13,12,2,3)
                .texOffs(70,18).addBox(-8,-17,8,2,3,6)
                .texOffs(70,18).addBox(6,-17,8,2,3,6)
                .texOffs(70,0).addBox(-4,-3,7,8,2,4),
                PartPose.offset(0,24,0));

        // Steering column points toward -Z/front. The rotatable wheel
        // has its own pivot, so its motion remains in place, not orbiting
        // around the vehicle origin. Dual foot pedals are visible below.
        root.addOrReplaceChild("operator_controls",CubeListBuilder.create()
                .texOffs(70,18).addBox(-2,-24,-5,4,12,4)
                .texOffs(70,0).addBox(-4,-14,-7,8,3,5)
                .texOffs(48,48).addBox(-5,-4,-10,3,1,5)
                .texOffs(48,48).addBox(2,-4,-10,3,1,5)
                .texOffs(70,18).addBox(7,-15,-5,2,12,2)
                .texOffs(70,18).addBox(-9,-15,-5,2,12,2)
                .texOffs(104,50).addBox(7,-17,-6,2,3,3)
                .texOffs(98,0).addBox(7,-11,-1,2,2,2),
                PartPose.offset(0,24,0));
        root.addOrReplaceChild("steering_wheel",CubeListBuilder.create()
                .texOffs(70,18).addBox(-8,-1,-1,16,2,2)
                .texOffs(70,18).addBox(-8,-6,-1,2,12,2)
                .texOffs(70,18).addBox(6,-6,-1,2,12,2)
                .texOffs(70,18).addBox(-7,-7,-1,14,2,2)
                .texOffs(70,18).addBox(-7,5,-1,14,2,2)
                .texOffs(48,58).addBox(-2,-2,-2,4,4,4),
                PartPose.offset(0,1,-7));

        // Two fixed dark guide channels with protective slats and bolts.
        root.addOrReplaceChild("mast_fixed",CubeListBuilder.create()
                .texOffs(70,18).addBox(-12,-46,-22,3,46,4)
                .texOffs(70,18).addBox(9,-46,-22,3,46,4)
                .texOffs(70,0).addBox(-12,-44,-22,24,3,3)
                .texOffs(70,0).addBox(-12,-7,-22,24,3,3)
                .texOffs(70,18).addBox(-11,-25,-21,22,2,3)
                .texOffs(70,18).addBox(-11,-15,-21,22,2,3)
                .texOffs(92,31).addBox(-8,-30,-21,3,21,3)
                .texOffs(92,31).addBox(5,-30,-21,3,21,3)
                .texOffs(70,0).addBox(-12,-46,-24,4,2,3)
                .texOffs(70,0).addBox(8,-46,-24,4,2,3),
                PartPose.offset(0,22,0));

        // Telescoping inner mast with independent smooth extension. These
        // do not intersect the pallet carrying channel in front.
        root.addOrReplaceChild("mast_middle",CubeListBuilder.create()
                .texOffs(70,18).addBox(-9,-44,-23,2,43,3)
                .texOffs(70,18).addBox(7,-44,-23,2,43,3)
                .texOffs(70,0).addBox(-9,-43,-24,18,2,3)
                .texOffs(70,0).addBox(-9,-8,-24,18,2,3),
                PartPose.offset(0,22,0));
        root.addOrReplaceChild("mast_top",CubeListBuilder.create()
                .texOffs(70,18).addBox(-6,-43,-25,2,43,3)
                .texOffs(70,18).addBox(4,-43,-25,2,43,3)
                .texOffs(70,0).addBox(-6,-42,-26,12,2,3)
                .texOffs(70,18).addBox(-6,-9,-26,12,2,3),
                PartPose.offset(0,22,0));

        // Left/right hydraulic lift cylinders; chrome center rods extend
        // with the carriage, no impossible side-to-side displacement.
        root.addOrReplaceChild("ram_left",CubeListBuilder.create()
                .texOffs(92,31).addBox(-7,-18,-19,3,19,3)
                .texOffs(70,0).addBox(-6,-28,-18,1,12,1)
                .texOffs(104,50).addBox(-7,-18,-19,3,2,3),
                PartPose.offset(0,22,0));
        root.addOrReplaceChild("ram_right",CubeListBuilder.create()
                .texOffs(92,31).addBox(4,-18,-19,3,19,3)
                .texOffs(70,0).addBox(5,-28,-18,1,12,1)
                .texOffs(104,50).addBox(4,-18,-19,3,2,3),
                PartPose.offset(0,22,0));

        // Heavy back plate, two real extended steel forks with 1px beveled
        // tips, supporting the complete visible 1.8x1.8 wrapped pallet.
        // Forks end at -64px: the existing cargo mesh ends at -59.5px.
        root.addOrReplaceChild("carriage",CubeListBuilder.create()
                .texOffs(70,0).addBox(-12,-15,-28,24,4,3)
                .texOffs(70,18).addBox(-11,-12,-31,4,12,4)
                .texOffs(70,18).addBox(7,-12,-31,4,12,4)
                .texOffs(92,31).addBox(-10,-3,-61,4,3,35)
                .texOffs(92,31).addBox(6,-3,-61,4,3,35)
                .texOffs(70,0).addBox(-9,-2,-64,2,2,3)
                .texOffs(70,0).addBox(7,-2,-64,2,2,3)
                .texOffs(70,18).addBox(-11,-16,-30,22,2,2)
                .texOffs(70,18).addBox(-10,-9,-29,20,2,2)
                .texOffs(104,50).addBox(-12,-11,-29,2,3,2)
                .texOffs(104,50).addBox(10,-11,-29,2,3,2),
                PartPose.offset(0,22,0));

        // Work lamps, hazard stripes, amber beacon, tail lamps, brackets,
        // exposed exhaust, and rear warning plate.
        root.addOrReplaceChild("safety_details",CubeListBuilder.create()
                .texOffs(98,90).addBox(-12,-42,-25,5,4,3)
                .texOffs(98,90).addBox(7,-42,-25,5,4,3)
                .texOffs(104,50).addBox(-11,-44,-26,3,1,2)
                .texOffs(104,50).addBox(8,-44,-26,3,1,2)
                .texOffs(108,70).addBox(-10,-20,18,5,3,2)
                .texOffs(108,70).addBox(5,-20,18,5,3,2)
                .texOffs(0,80).addBox(-6,-18,19,12,3,1)
                .texOffs(104,50).addBox(6,-55,7,5,3,5)
                .texOffs(70,18).addBox(7,-50,8,3,3,3)
                .texOffs(70,18).addBox(-10,-40,12,3,20,3)
                .texOffs(70,0).addBox(-11,-42,11,5,3,5)
                .texOffs(48,48).addBox(-7,-12,19,14,2,1),
                PartPose.offset(0,24,0));

        return LayerDefinition.create(mesh,128,128);
    }

    private static void frontWheel(PartDefinition root,String name,float x,float z){
        root.addOrReplaceChild(name,CubeListBuilder.create()
                .texOffs(0,67).addBox(-2.6f,-4.5f,-4.5f,5.2f,9f,9f)
                .texOffs(21,66).addBox(-2.8f,-3.5f,-3.5f,5.6f,7f,7f)
                .texOffs(70,18).addBox(-3.0f,-2f,-2f,6f,4f,4f)
                .texOffs(70,0).addBox(-3.1f,-.8f,-.8f,6.2f,1.6f,1.6f),
                PartPose.offset(x,20f,z));
    }

    private static void rearWheel(PartDefinition root,String name,float x,float z){
        root.addOrReplaceChild(name,CubeListBuilder.create()
                .texOffs(0,67).addBox(-2.5f,-4f,-4f,5f,8f,8f)
                .texOffs(21,66).addBox(-2.7f,-3f,-3f,5.4f,6f,6f)
                .texOffs(70,18).addBox(-2.9f,-1.8f,-1.8f,5.8f,3.6f,3.6f)
                .texOffs(70,0).addBox(-3.0f,-.7f,-.7f,6f,1.4f,1.4f),
                PartPose.offset(x,20f,z));
    }

    @Override public void setupAnim(WarehouseForkliftRenderState state){
        super.setupAnim(state);
        float height=Mth.clamp(state.mastHeight,0f,12f);
        mastMiddle.y=22f-height*5.5f;
        mastTop.y=22f-height*10f;
        carriage.y=22f-height*16f;
        ramLeft.y=22f-height*8f;
        ramRight.y=22f-height*8f;

        // Wheel spin reflects actual travel. Front wheels also swivel as
        // the player sees the steering wheel turn inside the cab.
        float wheelSpin=state.walkAnimationPos*.8f;
        frontLeftWheel.xRot=wheelSpin;
        frontRightWheel.xRot=wheelSpin;
        rearLeftWheel.xRot=wheelSpin;
        rearRightWheel.xRot=wheelSpin;
        float steer=Mth.clamp(state.steeringAngle,-1f,1f);
        frontLeftWheel.yRot=steer*.45f;
        frontRightWheel.yRot=steer*.45f;
        rearLeftWheel.yRot=0f;
        rearRightWheel.yRot=0f;
        steeringWheel.zRot=-steer*.35f;
    }
}
