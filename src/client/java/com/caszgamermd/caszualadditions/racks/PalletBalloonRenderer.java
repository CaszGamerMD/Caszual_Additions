package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class PalletBalloonRenderer extends MobRenderer<
        PalletBalloon,PalletBalloonRenderState,PalletBalloonModel>{
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("pallet_balloon"),"main");
    private static final Identifier TEXTURE=
            CaszualAdditions.id("textures/entity/warehouse_forklift.png");
    private final ItemModelResolver items;
    public PalletBalloonRenderer(EntityRendererProvider.Context context){
        super(context,new PalletBalloonModel(context.bakeLayer(LAYER)),1.4f);
        items=context.getItemModelResolver();
    }
    @Override public void extractRenderState(PalletBalloon balloon,
            PalletBalloonRenderState state,float partialTick){
        super.extractRenderState(balloon,state,partialTick);
        int count=0;
        for(int i=0;i<4;i++){
            ItemStack stack=balloon.palletAt(i);
            if(stack.isEmpty()){state.cargo[i].clear();continue;}
            items.updateForLiving(state.cargo[i],stack,ItemDisplayContext.FIXED,balloon);
            count++;
        }
        state.cargoCount=count;
    }
    @Override public void submit(PalletBalloonRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        // Four cargo positions align with the four 2x2 deck pads.
        float[][] positions={{-.95f,.10f,-.95f},{.95f,.10f,-.95f},
                {-.95f,.10f,.95f},{.95f,.10f,.95f}};
        for(int i=0;i<4;i++){
            if(state.cargo[i].isEmpty())continue;
            pose.pushPose();
            pose.translate(positions[i][0],positions[i][1],positions[i][2]);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(1.65f,1.65f,1.65f);
            state.cargo[i].submit(pose,collector,state.lightCoords,
                    OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
    }
    @Override public PalletBalloonRenderState createRenderState(){
        return new PalletBalloonRenderState();
    }
    @Override public Identifier getTextureLocation(PalletBalloonRenderState state){
        return TEXTURE;
    }
}
