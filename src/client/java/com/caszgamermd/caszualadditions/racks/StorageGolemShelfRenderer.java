package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Display the REAL Storage Golem model, scaled down to fit each shelf bay.
 * The docked robots use precisely the same 64x64 slate robot skin and UV
 * coordinates as the independent workers and the four moving-pallet models:
 * cyan optics, torso plates, oily hands and dirty boots all stay in sync.
 *
 * Docked workers are only render models, never additional entities.
 */
public final class StorageGolemShelfRenderer implements
        BlockEntityRenderer<StorageGolemShelfEntity,StorageGolemShelfRenderState> {
    private static final Identifier GOLEM_SKIN =
            CaszualAdditions.id("textures/entity/storage_golem.png");
    private static final String[] PARTS={
            "body","head","left_arm","right_arm","left_leg","right_leg"
    };
    // ModelPart submission already converts model pixels to block units.
    // Do NOT add another 1/16 scale: that shrinks docked robots to specks
    // hovering against the shelf roof instead of standing on its floor.
    // The real Golem spans ~24 model pixels (1.5 blocks). At .44 Y scale,
    // the miniature stands .66 blocks high inside the 11/16-tall bay.
    // Each figure spans 18 model pixels (1.125 blocks) from arm to arm;
    // .32 X scale fits two workers with a small gap between the side beams.
    private static final float FOOT_Y=3f/16f;
    private static final float WIDTH_SCALE=.32f;
    private static final float HEIGHT_SCALE=.44f;
    private final ModelPart miniature;

    public StorageGolemShelfRenderer(BlockEntityRendererProvider.Context context){
        // The same source mesh/UV layout as StorageGolemRenderer.
        miniature=StorageGolemModel.layer().bakeRoot();
    }

    @Override public StorageGolemShelfRenderState createRenderState(){
        return new StorageGolemShelfRenderState();
    }

    @Override public void extractRenderState(StorageGolemShelfEntity shelf,
            StorageGolemShelfRenderState state,float partial,Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(shelf,state,partial,camera,cracks);
        state.docked=Math.clamp(shelf.docked(),0,2);
        state.facing=shelf.getBlockState().getValue(StorageGolemShelfBlock.FACING);
    }

    @Override public void submit(StorageGolemShelfRenderState state,PoseStack poses,
            SubmitNodeCollector collector,CameraRenderState camera){
        if(state.docked==0)return;
        poses.pushPose();
        poses.translate(.5f,0,.5f);
        float yaw=switch(state.facing){
            case EAST->90f;
            case SOUTH->180f;
            case WEST->270f;
            default->0f;
        };
        poses.mulPose(Axis.YP.rotationDegrees(yaw));
        poses.translate(-.5f,0,-.5f);

        for(int i=0;i<state.docked;i++){
            float cx=i==0?.31f:.69f;
            poses.pushPose();
            // The top of the original model is at y=0 and its soles
            // at y=24. Flip its Y exactly as in Minecraft entity renders.
            // That leaves feet resting on the shelf floor and head tucked
            // beneath the upper crossbar; the face points out of the shelf.
            poses.translate(cx,FOOT_Y+1.5f*HEIGHT_SCALE,.45f);
            // submitModelPart applies model-pixel-to-world conversion;
            // multiplying by 1/16 here caused a second, unintended shrink.
            poses.scale(-WIDTH_SCALE,-HEIGHT_SCALE,HEIGHT_SCALE);
            for(String name:PARTS)
                collector.order(2).submitModelPart(
                        miniature.getChild(name),poses,
                        RenderTypes.entityCutout(GOLEM_SKIN),
                        state.lightCoords,OverlayTexture.NO_OVERLAY,
                        null,-1,null,0);
            poses.popPose();
        }
        poses.popPose();
    }
}
