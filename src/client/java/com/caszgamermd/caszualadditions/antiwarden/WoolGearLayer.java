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

        // Every box has its OWN 48x32 UV cell on the 256x256 atlas.
        // Sharing old 64x64 offsets made large armor surfaces sample
        // unrelated colors and appear as huge pink/untextured panels.
        // The head stays visible: earmuffs cushion the ears while goggles
        // cover only the eyes; no opaque cube over the whole face.
        root.addOrReplaceChild("helmet",CubeListBuilder.create()
                // Steel band resting on the top of the head
                .texOffs(0,0).addBox(-4.45f,-8.7f,-3.2f,8.9f,1.3f,6.4f)
                .texOffs(48,0).addBox(-3.65f,-9.0f,-2.8f,7.3f,.75f,5.6f)
                // Cushioned earcups, outer shell + wool pads
                .texOffs(96,0).addBox(-5.45f,-7f,-3f,1.7f,4.5f,6f)
                .texOffs(144,0).addBox(3.75f,-7f,-3f,1.7f,4.5f,6f)
                .texOffs(192,0).addBox(-5.7f,-6.5f,-2.8f,1.7f,3.5f,5.6f)
                .texOffs(0,32).addBox(4f,-6.5f,-2.8f,1.7f,3.5f,5.6f)
                // Band from each ear to the dual night-vision eyepieces
                .texOffs(48,32).addBox(-4.2f,-6.0f,-4.05f,8.4f,1f,.8f)
                .texOffs(96,32).addBox(-3.9f,-6.5f,-4.4f,7.8f,.8f,1.25f)
                // Small dark metal goggle housings in front of the face
                .texOffs(144,32).addBox(-3.65f,-6.25f,-5.5f,3.3f,3.1f,1.65f)
                .texOffs(192,32).addBox(.35f,-6.25f,-5.5f,3.3f,3.1f,1.65f)
                // Inset green glass fronts, not a full-face visor
                .texOffs(0,64).addBox(-3.15f,-5.8f,-5.72f,2.3f,2.1f,.28f)
                .texOffs(48,64).addBox(.85f,-5.8f,-5.72f,2.3f,2.1f,.28f)
                .texOffs(96,64).addBox(-.55f,-5.5f,-5.2f,1.1f,1.05f,.95f)
                .texOffs(144,64).addBox(-2.7f,-5.5f,-5.79f,.65f,.5f,.12f)
                .texOffs(192,64).addBox(1.3f,-5.5f,-5.79f,.65f,.5f,.12f),
                PartPose.ZERO);

        // Vanilla player legs: feet at y=12; each slipper is about five
        // pixels wide, with a flexible 1-pixel sole, a round wool toe,
        // and a soft cuff at the ankle. NO full-height leg box.
        CubeListBuilder slipper=CubeListBuilder.create()
                .texOffs(0,96).addBox(-2.4f,10.95f,-4.1f,4.8f,1.3f,6.6f)
                .texOffs(48,96).addBox(-2.2f,9.65f,-3.8f,4.4f,1.5f,5.8f)
                .texOffs(96,96).addBox(-2.3f,9.55f,-4.2f,4.6f,1.4f,2.7f)
                .texOffs(144,96).addBox(-2.45f,8.8f,-.35f,4.9f,1.5f,2.8f)
                .texOffs(192,96).addBox(-1.75f,9.4f,-4.25f,3.5f,.35f,.2f);
        root.addOrReplaceChild("left_slipper",slipper,PartPose.ZERO);
        root.addOrReplaceChild("right_slipper",slipper,PartPose.ZERO);
        return LayerDefinition.create(mesh,256,256);
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
