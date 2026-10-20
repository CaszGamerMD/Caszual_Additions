package com.caszgamermd.caszualadditions.painting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * A true industrial paint layer over the SOURCE MODEL FACES, not over
 * the block's collision/selection shape. A rack's thin posts, stairs,
 * braces, diagonal rails and gaps all retain their actual geometry.
 *
 * Every model face is a very thin tinted, textured plate just outside
 * that element's actual face. Multipart and rotated model pieces use
 * the same transform as the source block model.
 */
public final class IndustrialFinishRenderer extends EntityRenderer<
        IndustrialFinishEntity,IndustrialFinishRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private static final float OFFSET=.0016f,THICK=.001f;
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
        state.elements=IndustrialPaintModelGeometry.elements(target);
        // No voxel-shape fallback for custom renderer blocks. An outline
        // can be a solid cube even when the visible model is open framing.
        // This prevents painting a false solid wall across a rack.
        blocks.update(state.finish,IndustrialPaintContent.SURFACE.defaultBlockState(),DISPLAY);
        state.finish.tintLayers().clear();
        state.finish.tintLayers().add(entity.color());
    }
    @Override public void submit(IndustrialFinishRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(!state.show||state.elements.isEmpty())return;
        pose.pushPose();
        // The cosmetic entity is anchored to the middle of its block in X/Z;
        // y=0 is the base of the block, not the camera.
        for(var part:state.elements){
            pose.pushPose();
            if(part.yaw()!=0)
                pose.mulPose(Axis.YP.rotationDegrees(-part.yaw()));
            if(part.pitch()!=0)
                pose.mulPose(Axis.XP.rotationDegrees(part.pitch()));
            float[] pivot=part.pivot();
            if(part.angle()!=0f){
                pose.translate(pivot[0]-.5f,pivot[1],pivot[2]-.5f);
                switch(part.axis()){
                    case "x" -> pose.mulPose(Axis.XP.rotationDegrees(part.angle()));
                    case "y" -> pose.mulPose(Axis.YP.rotationDegrees(part.angle()));
                    case "z" -> pose.mulPose(Axis.ZP.rotationDegrees(part.angle()));
                    default -> {}
                }
                pose.translate(-pivot[0]+.5f,-pivot[1],-pivot[2]+.5f);
            }
            float[] a=part.from(),b=part.to();
            float sx=b[0]-a[0],sy=b[1]-a[1],sz=b[2]-a[2];
            if(sx<=0||sy<=0||sz<=0){pose.popPose();continue;}
            for(int face=0;face<6;face++){
                if((part.faces()&(1<<face))==0)continue;
                // Each patch is one textured surface of the REAL source
                // element, never a giant cube wrapped around its shape.
                float x=a[0],y=a[1],z=a[2],w=sx,h=sy,d=sz;
                switch(face){
                    case 0 -> {x=a[0]-OFFSET-THICK;w=THICK;} // west
                    case 1 -> {x=b[0]+OFFSET;w=THICK;} // east
                    case 2 -> {y=a[1]-OFFSET-THICK;h=THICK;} // down
                    case 3 -> {y=b[1]+OFFSET;h=THICK;} // up
                    case 4 -> {z=a[2]-OFFSET-THICK;d=THICK;} // north
                    case 5 -> {z=b[2]+OFFSET;d=THICK;} // south
                    default -> {}
                }
                pose.pushPose();
                pose.translate(x-.5f,y,z-.5f);
                pose.scale(w,h,d);
                state.finish.submit(pose,collector,state.lightCoords,
                        OverlayTexture.NO_OVERLAY,0);
                pose.popPose();
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
