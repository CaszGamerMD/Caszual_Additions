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
 * All four Storage Golems are attached MODELS, not entities. Their feet
 * animate from the ONE pallet entity's movement; no separate AI can drift
 * away from its assigned corner or cause visual z-clipping.
 */
public final class MovingPalletModel extends EntityModel<MovingPalletRenderState> {
    private final ModelPart[] leftLeg=new ModelPart[4],rightLeg=new ModelPart[4];
    private final ModelPart[] leftArm=new ModelPart[4],rightArm=new ModelPart[4];
    public MovingPalletModel(ModelPart root){
        super(root);
        for(int i=0;i<4;i++){
            ModelPart corner=root.getChild("corner_"+i);
            leftLeg[i]=corner.getChild("left_leg");
            rightLeg[i]=corner.getChild("right_leg");
            leftArm[i]=corner.getChild("left_arm");
            rightArm[i]=corner.getChild("right_arm");
        }
    }
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        for(int i=0;i<4;i++){
            float x=i%2==0?-14:14;
            float z=i<2?-14:14;
            PartDefinition corner=root.addOrReplaceChild("corner_"+i,
                    CubeListBuilder.create(),PartPose.offset(x,0,z));
            corner.addOrReplaceChild("body",CubeListBuilder.create()
                    .texOffs(0,0).addBox(-5,-6,-3.5f,10,11,7),
                    PartPose.offset(0,13,0));
            corner.addOrReplaceChild("head",CubeListBuilder.create()
                    .texOffs(0,20).addBox(-4,-5,-3,8,7,6),
                    // Head extends from y=1 to y=8; torso starts at y=7.
                // A one-pixel overlap closes the old floating-head gap.
                PartPose.offset(0,6,0));
            corner.addOrReplaceChild("left_arm",CubeListBuilder.create()
                    .texOffs(28,20).addBox(-2,-1,-2,4,10,4),
                    PartPose.offset(7,11,0));
            corner.addOrReplaceChild("right_arm",CubeListBuilder.create()
                    .texOffs(28,20).addBox(-2,-1,-2,4,10,4),
                    PartPose.offset(-7,11,0));
            corner.addOrReplaceChild("left_leg",CubeListBuilder.create()
                    .texOffs(44,0).addBox(-2,0,-2,4,8,4),
                    PartPose.offset(3,16,0));
            corner.addOrReplaceChild("right_leg",CubeListBuilder.create()
                    .texOffs(44,0).addBox(-2,0,-2,4,8,4),
                    PartPose.offset(-3,16,0));
        }
        return LayerDefinition.create(mesh,64,64);
    }
    @Override public void setupAnim(MovingPalletRenderState state){
        super.setupAnim(state);
        float phase=state.walkAnimationPos*.55f;
        float amount=state.walkAnimationSpeed;
        for(int i=0;i<4;i++){
            // Alternating diagonal steps; all four animate from identical
            // entity velocity, while each remains fixed to its corner.
            float offset=(i==0||i==3)?0:Mth.PI;
            leftLeg[i].xRot=Mth.cos(phase+offset)*1.15f*amount;
            rightLeg[i].xRot=Mth.cos(phase+offset+Mth.PI)*1.15f*amount;
            // Minecraft's arm boxes point DOWN from their shoulder.
            // -1.4 radians pointed the arms forward horizontally; rotate
            // almost half a turn to put the HANDS above the heads and under
            // the deck. Hold this pose while carrying, not only lifting.
            float reach=-3.02f*state.liftProgress;
            leftArm[i].xRot=reach;
            rightArm[i].xRot=reach;
            leftArm[i].zRot=-.12f*state.liftProgress;
            rightArm[i].zRot=.12f*state.liftProgress;
        }
    }
}
