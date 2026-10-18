package com.caszgamermd.caszualadditions.antiwarden;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Real wearable 3D silhouettes: padded earmuffs with projecting green
 * goggles, plus distinct low wool slippers that follow animated legs.
 * This is attached to the player's head/legs (not world-space) so it
 * stays registered through swimming, crouching and walking.
 */
public final class WoolGearLayer extends RenderLayer<AvatarRenderState,PlayerModel>{
    public static final ModelLayerLocation LAYER=new ModelLayerLocation(
            CaszualAdditions.id("wearable_wool_gear"),"main");
    private static final Identifier TEXTURE=CaszualAdditions.id(
            "textures/entity/wool_sensory_gear.png");
    private final ModelPart helmet,leftSlipper,rightSlipper;
    public WoolGearLayer(RenderLayerParent<AvatarRenderState,PlayerModel> parent,
            EntityRendererProvider.Context context){
        super(parent);
        ModelPart root=context.bakeLayer(LAYER);
        helmet=root.getChild("helmet");
        leftSlipper=root.getChild("left_slipper");
        rightSlipper=root.getChild("right_slipper");
    }
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        var root=mesh.getRoot();
        // A slender strap; fluffy earcups float to either side of the
        // skull. Two goggles stand in front of the eyes, with a bridge,
        // wide green lens faces and tiny dark steel housings.
        root.addOrReplaceChild("helmet",CubeListBuilder.create()
                .texOffs(0,0).addBox(-4.5f,-8.7f,-3.5f,9,1.3f,7)
                .texOffs(0,16).addBox(-5.7f,-6.5f,-2.7f,2,4.5f,5.4f)
                .texOffs(24,16).addBox(3.7f,-6.5f,-2.7f,2,4.5f,5.4f)
                .texOffs(48,16).addBox(-4.2f,-5.8f,-5.3f,8.4f,1.3f,2.2f)
                .texOffs(0,32).addBox(-4.3f,-6.8f,-6.2f,3.8f,3.6f,2.7f)
                .texOffs(24,32).addBox(.5f,-6.8f,-6.2f,3.8f,3.6f,2.7f)
                .texOffs(42,32).addBox(-3.6f,-6.2f,-6.55f,2.4f,2.4f,.5f)
                .texOffs(42,32).addBox(1.2f,-6.2f,-6.55f,2.4f,2.4f,.5f),
                PartPose.ZERO);
        // Geometry in vanilla leg coordinates: feet are near Y = 12.
        root.addOrReplaceChild("left_slipper",CubeListBuilder.create()
                .texOffs(24,46).addBox(-2.55f,10.85f,-4.5f,5.1f,1.35f,7.0f)
                .texOffs(0,46).addBox(-2.4f,9.25f,-4.3f,4.8f,1.7f,6.4f)
                .texOffs(48,46).addBox(-2.25f,9.9f,-4.7f,4.5f,1.0f,3.2f)
                .texOffs(0,16).addBox(-2.5f,8.8f,-.5f,5,1.2f,3.6f),
                PartPose.ZERO);
        root.addOrReplaceChild("right_slipper",CubeListBuilder.create()
                .texOffs(24,46).addBox(-2.55f,10.85f,-4.5f,5.1f,1.35f,7.0f)
                .texOffs(0,46).addBox(-2.4f,9.25f,-4.3f,4.8f,1.7f,6.4f)
                .texOffs(48,46).addBox(-2.25f,9.9f,-4.7f,4.5f,1.0f,3.2f)
                .texOffs(0,16).addBox(-2.5f,8.8f,-.5f,5,1.2f,3.6f),
                PartPose.ZERO);
        return LayerDefinition.create(mesh,64,64);
    }
    private static void draw(ModelPart part,PoseStack pose,
            SubmitNodeCollector collector,int light,int outline){
        collector.order(1).submitModelPart(part,pose,RenderTypes.entityCutout(TEXTURE),
                light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);
    }
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,
            int light,AvatarRenderState state,float yaw,float pitch){
        if(state.isSpectator||state.isInvisible)return;
        var model=getParentModel();
        if(!state.headEquipment.is(AntiWardenContent.WOOL_HELMET)
                &&!state.feetEquipment.is(AntiWardenContent.WOOL_BOOTS))return;
        pose.pushPose();
        model.root().translateAndRotate(pose);
        if(state.headEquipment.is(AntiWardenContent.WOOL_HELMET)){
            pose.pushPose();
            model.head.translateAndRotate(pose);
            draw(helmet,pose,collector,light,state.outlineColor);
            pose.popPose();
        }
        if(state.feetEquipment.is(AntiWardenContent.WOOL_BOOTS)){
            pose.pushPose();
            model.leftLeg.translateAndRotate(pose);
            draw(leftSlipper,pose,collector,light,state.outlineColor);
            pose.popPose();
            pose.pushPose();
            model.rightLeg.translateAndRotate(pose);
            draw(rightSlipper,pose,collector,light,state.outlineColor);
            pose.popPose();
        }
        pose.popPose();
    }
}
