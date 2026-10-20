package com.caszgamermd.caszualadditions.painting;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Render a ONE-SIDED paint quad on each exposed block model face.
 *
 * Do NOT shrink a six-sided cube into a plane: the resulting near-overlap
 * between its front and back faces creates dark surfaces and flickering
 * seams on solid cubes. One baked, tinted quad per visible model face
 * follows structural beams without filling their open space.
 */
public final class IndustrialFinishRenderer extends EntityRenderer<
        IndustrialFinishEntity,IndustrialFinishRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    // Distance in block units between the actual model and its paint skin.
    // This is applied *before* scaling, so thin rails are offset by the
    // SAME world-space distance as large vanilla cube faces.
    private static final float SURFACE_OFFSET=.004f;
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
        var pos=entity.anchor();
        var target=world.getBlockState(pos);
        state.show=!target.isAir();
        if(!state.show)return;
        state.elements=IndustrialPaintModelGeometry.elements(target);
        for(int face=0;face<state.faces.length;face++){
            blocks.update(state.faces[face],
                    IndustrialPaintContent.SURFACE_FACES[face].defaultBlockState(),DISPLAY);
            state.faces[face].tintLayers().clear();
            state.faces[face].tintLayers().add(entity.color());
        }
    }

    @Override public void submit(IndustrialFinishRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(!state.show||state.elements.isEmpty())return;
        for(var part:state.elements){
            pose.pushPose();

            // Baked blockstate rotation is around the CENTER of a block,
            // including its vertical center for the X axis. Apply element
            // rotation inside it, about the model-provided pivot.
            if(part.yaw()!=0)pose.mulPose(Axis.YP.rotationDegrees(-part.yaw()));
            if(part.pitch()!=0){
                pose.translate(0,.5f,0);
                pose.mulPose(Axis.XP.rotationDegrees(part.pitch()));
                pose.translate(0,-.5f,0);
            }
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

            float[] from=part.from(),to=part.to();
            float x=to[0]-from[0],y=to[1]-from[1],z=to[2]-from[2];
            if(x<=0||y<=0||z<=0){pose.popPose();continue;}
            for(int face=0;face<6;face++){
                if((part.faces()&(1<<face))==0)continue;
                // Map one actual source model face onto one one-sided quad.
                // Offsets are WORLD UNITS, not 1/1000 of a model's size.
                float ox=0,oy=0,oz=0;
                switch(face){
                    case 0 -> ox=-SURFACE_OFFSET;
                    case 1 -> ox=SURFACE_OFFSET;
                    case 2 -> oy=-SURFACE_OFFSET;
                    case 3 -> oy=SURFACE_OFFSET;
                    case 4 -> oz=-SURFACE_OFFSET;
                    case 5 -> oz=SURFACE_OFFSET;
                    default -> {}
                }
                pose.pushPose();
                pose.translate(from[0]-.5f+ox,from[1]+oy,from[2]-.5f+oz);
                pose.scale(x,y,z);
                state.faces[face].submit(pose,collector,state.lightCoords,
                        OverlayTexture.NO_OVERLAY,0);
                pose.popPose();
            }
            pose.popPose();
        }
    }
}
