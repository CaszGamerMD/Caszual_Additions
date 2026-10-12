package com.caszgamermd.caszualadditions.racks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Small animated-like docked worker silhouettes show 0, 1 or 2 workers. */
public final class StorageGolemShelfRenderer implements
        BlockEntityRenderer<StorageGolemShelfEntity,StorageGolemShelfRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    public StorageGolemShelfRenderer(BlockEntityRendererProvider.Context context){
        blocks=context.blockModelResolver();
    }
    @Override public StorageGolemShelfRenderState createRenderState(){
        return new StorageGolemShelfRenderState();
    }
    @Override public void extractRenderState(StorageGolemShelfEntity shelf,
            StorageGolemShelfRenderState state,float partial,Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(shelf,state,partial,camera,cracks);
        state.docked=shelf.docked();
        state.facing=shelf.getBlockState().getValue(StorageGolemShelfBlock.FACING);
        blocks.update(state.frame,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        blocks.update(state.lamp,Blocks.DIAMOND_BLOCK.defaultBlockState(),DISPLAY);
    }
    @Override public void submit(StorageGolemShelfRenderState state,PoseStack poses,
            SubmitNodeCollector collector,CameraRenderState camera){
        poses.pushPose();
        poses.translate(.5,0,.5);
        float degrees=switch(state.facing){
            case EAST->90f;case SOUTH->180f;case WEST->270f;default->0f;
        };
        poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(degrees));
        poses.translate(-.5,0,-.5);
        for(int i=0;i<state.docked;i++){
            float cx=i==0?.31f:.69f;
            piece(state.frame,state,poses,collector,cx-.15f,.22f,.29f,.30f,.37f,.35f);
            piece(state.frame,state,poses,collector,cx-.12f,.61f,.30f,.24f,.18f,.27f);
            piece(state.lamp,state,poses,collector,cx-.065f,.67f,.277f,.13f,.045f,.04f);
            piece(state.frame,state,poses,collector,cx-.11f,.08f,.35f,.08f,.16f,.15f);
            piece(state.frame,state,poses,collector,cx+.03f,.08f,.35f,.08f,.16f,.15f);
        }
        poses.popPose();
    }
    private static void piece(net.minecraft.client.renderer.block.BlockModelRenderState model,
            StorageGolemShelfRenderState state,PoseStack poses,
            SubmitNodeCollector collector,float x,float y,float z,
            float sx,float sy,float sz){
        poses.pushPose();poses.translate(x,y,z);poses.scale(sx,sy,sz);
        model.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        poses.popPose();
    }
}
