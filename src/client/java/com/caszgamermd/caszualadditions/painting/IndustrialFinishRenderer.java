package com.caszgamermd.caszualadditions.painting;

import java.util.List;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/** Shape-attached finish: each visible voxel/model outline element is
 * rendered with a tinted fine-grain surface. No cubic full-block shell on
 * fences, rails, stairs or other non-cubic target blocks.
 */
public final class IndustrialFinishRenderer extends EntityRenderer<
        IndustrialFinishEntity,IndustrialFinishRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    public IndustrialFinishRenderer(EntityRendererProvider.Context ctx){
        super(ctx);
        blocks=ctx.getBlockModelResolver();
    }
    @Override public IndustrialFinishRenderState createRenderState(){
        return new IndustrialFinishRenderState();
    }
    @Override public void extractRenderState(IndustrialFinishEntity entity,
            IndustrialFinishRenderState state,float partialTick){
        super.extractRenderState(entity,state,partialTick);
        var world=entity.level();
        var at=entity.anchor();
        var target=world.getBlockState(at);
        state.show=!target.isAir();
        if(!state.show)return;
        // VoxelShape represents the geometric outline, including railing
        // shafts, grates and many multiblock parts. Bound geometry to avoid
        // potentially enormous render lists from other mods.
        List<AABB> boxes=target.getShape(world,at).toAabbs();
        state.shapes=boxes.size()>64?boxes.subList(0,64):boxes;
        blocks.update(state.finish,IndustrialPaintContent.SURFACE.defaultBlockState(),DISPLAY);
        state.finish.tintLayers().clear();
        state.finish.tintLayers().add(entity.color());
    }
    @Override public void submit(IndustrialFinishRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(!state.show)return;
        for(AABB box:state.shapes){
            if(box.getXsize()<=0||box.getYsize()<=0||box.getZsize()<=0)continue;
            // An inset/expanded 1/1024 block skin avoids depth-fighting with
            // the underlying model, without altering interaction/collision.
            double pad=.0014;
            pose.pushPose();
            pose.translate(box.minX-.5-pad,box.minY-pad,box.minZ-.5-pad);
            pose.scale((float)(box.getXsize()+2*pad),
                    (float)(box.getYsize()+2*pad),(float)(box.getZsize()+2*pad));
            state.finish.submit(pose,collector,state.lightCoords,
                    OverlayTexture.NO_OVERLAY,0);
            pose.popPose();
        }
    }
    @Override public Identifier getTextureLocation(IndustrialFinishRenderState state){
        return Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    }
}