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
    private final ModelPart head,leftArm,rightArm,leftLeg,rightLeg;
    public StorageGolemModel(ModelPart root){
        super(root);
        head=root.getChild("head");
        leftArm=root.getChild("left_arm");
        rightArm=root.getChild("right_arm");
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
        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create().texOffs(28,20)
                        .addBox(-2,-1,-2,4,10,4),
                PartPose.offset(7,11,0));
        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create().texOffs(28,20)
                        .addBox(-2,-1,-2,4,10,4),
                PartPose.offset(-7,11,0));
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
    @Override public void setupAnim(StorageGolemRenderState state){
        super.setupAnim(state);
        head.xRot=state.xRot*Mth.DEG_TO_RAD;
        head.yRot=state.yRot*Mth.DEG_TO_RAD;
        float phase=state.walkAnimationPos*.55f,amount=state.walkAnimationSpeed;
        leftLeg.xRot=Mth.cos(phase)*1.1f*amount;
        rightLeg.xRot=Mth.cos(phase+Mth.PI)*1.1f*amount;
        leftArm.xRot=rightLeg.xRot*.6f;
        rightArm.xRot=leftLeg.xRot*.6f;
        if(state.heldSlots>0&&!state.carryingPallet){
            // Load kept up on both forearms, tucked against the chest.
            leftArm.xRot=-1.05f;
            rightArm.xRot=-1.05f;
            leftArm.zRot=-.14f;
            rightArm.zRot=.14f;
        }else{
            leftArm.zRot=0;
            rightArm.zRot=0;
        }
        if(state.carryingPallet){
            // Both hands remain elevated during the loaded walk.
            // The actual pallet remains escrowed on the source PTP.
            leftArm.xRot=-3.02f;
            rightArm.xRot=-3.02f;
            leftArm.zRot=-.12f;
            rightArm.zRot=.12f;
        }
    }
}
