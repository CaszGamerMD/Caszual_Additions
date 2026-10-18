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
 * Small warehouse automaton: squared cabinet torso, work-lamp head, short
 * arms and independent walking legs. Designed as a Storage Golem, not a
 * Copper Golem or a resized vanilla Iron Golem.
 */
public final class StorageGolemModel extends EntityModel<StorageGolemRenderState> {
    private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg,leftForearm,rightForearm;
    public StorageGolemModel(ModelPart root){
        super(root);
        head=root.getChild("head");
        leftArm=root.getChild("left_arm");
        rightArm=root.getChild("right_arm");
        leftForearm=leftArm.getChild("forearm");
        rightForearm=rightArm.getChild("forearm");
        leftLeg=root.getChild("left_leg");
        rightLeg=root.getChild("right_leg");
    }
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0,0)
                        .addBox(-5,-6,-3.5f,10,11,7),
                PartPose.offset(0,13,0));
        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0,20)
                        .addBox(-4,-5,-3,8,7,6),
                // Head extends from y=1 to y=8; torso starts at y=7.
                // A one-pixel overlap closes the old floating-head gap.
                PartPose.offset(0,6,0));
        PartDefinition left=root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(0,40)
                        .addBox(-2,-1,-2,4,6,4),
                PartPose.offset(7,11,0));
        left.addOrReplaceChild("forearm",
                CubeListBuilder.create().texOffs(16,40)
                        .addBox(-2,-1,-2,4,6,4),
                PartPose.offset(0,5,0));
        PartDefinition right=root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(0,40)
                        .addBox(-2,-1,-2,4,6,4),
                PartPose.offset(-7,11,0));
        right.addOrReplaceChild("forearm",
                CubeListBuilder.create().texOffs(16,40)
                        .addBox(-2,-1,-2,4,6,4),
                PartPose.offset(0,5,0));
        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create().texOffs(44,0)
                        .addBox(-2,0,-2,4,8,4),
                PartPose.offset(3,16,0));
        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create().texOffs(44,0)
                        .addBox(-2,0,-2,4,8,4),
                PartPose.offset(-3,16,0));
        return LayerDefinition.create(mesh,64,64);
    }
    /** Called from the entity's item render LAYER, after Minecraft has
     * applied the mob's world translation and body rotation. Follow the
     * articulated right forearm into its wrist, then move the real item
     * inward between BOTH hands. No world-space tray or stationary cargo.
     */
    public void translateToCargoHand(com.mojang.blaze3d.vertex.PoseStack pose){
        rightArm.translateAndRotate(pose);
        rightForearm.translateAndRotate(pose);
        // Arm pivot is 7 px right of the chest, so center between wrists.
        // Keep the sample pressed into the cupped gloves.
        pose.translate(7f/16f,4f/16f,-.06f);
    }
    @Override public void setupAnim(StorageGolemRenderState state){
        super.setupAnim(state);
        head.xRot=state.xRot*Mth.DEG_TO_RAD;
        head.yRot=state.yRot*Mth.DEG_TO_RAD;
        float phase=state.walkAnimationPos*.55f;
        // Vanilla can retain a tiny residual limb-swing value even after
        // navigation stops. Zero it out so an idle worker is truly neutral.
        float amount=Mth.clamp(state.walkAnimationSpeed,0f,1f);
        if(amount<.045f)amount=0f;
        leftLeg.xRot=Mth.cos(phase)*1.1f*amount;
        rightLeg.xRot=Mth.cos(phase+Mth.PI)*1.1f*amount;
        // Reset ALL joints each frame. A carrying pose must never linger
        // after unloading, returning to a shelf, or finishing a task.
        leftArm.x=7f;rightArm.x=-7f;
        leftArm.y=rightArm.y=11f;
        leftArm.z=rightArm.z=0f;
        leftArm.xRot=rightLeg.xRot*.6f;
        rightArm.xRot=leftLeg.xRot*.6f;
        leftArm.yRot=rightArm.yRot=0f;
        leftArm.zRot=rightArm.zRot=0f;
        leftForearm.xRot=rightForearm.xRot=0f;
        leftForearm.yRot=rightForearm.yRot=0f;
        leftForearm.zRot=rightForearm.zRot=0f;
        if(state.pushingEmptyPallet){
            // Arms reach forward to physically TOUCH the upright pallet
            // instead of swinging empty-handed through its surface.
            leftArm.xRot=-1.45f;
            rightArm.xRot=-1.45f;
            leftForearm.xRot=-.12f;
            rightForearm.xRot=-.12f;
            leftArm.zRot=-.16f;
            rightArm.zRot=.16f;
        }else if(state.heldSlots>0&&!state.carryingPallet){
            // A runner cups the real picked-up item between both wrists.
            // The model's right forearm is the attachment anchor.
            leftArm.xRot=-1.36f;
            rightArm.xRot=-1.36f;
            leftForearm.xRot=-.38f;
            rightForearm.xRot=-.38f;
            leftArm.zRot=-.14f;
            rightArm.zRot=.14f;
        }else if(state.carryingPallet){
            // Four manual crew workers hold the SAME real pallet by hand.
            leftArm.xRot=-1.85f;
            rightArm.xRot=-1.85f;
            leftForearm.xRot=-.90f;
            rightForearm.xRot=-.90f;
            leftArm.zRot=-.12f;
            rightArm.zRot=.12f;
        }else{
            leftArm.zRot=0;
            rightArm.zRot=0;
        }
    }
}
