package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class PalletBalloonRenderer extends MobRenderer<
        PalletBalloon,LivingEntityRenderState,PalletBalloonModel>{
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("pallet_balloon"),"main");
    private static final Identifier TEXTURE=
            CaszualAdditions.id("textures/entity/warehouse_forklift.png");
    public PalletBalloonRenderer(EntityRendererProvider.Context context){
        super(context,new PalletBalloonModel(context.bakeLayer(LAYER)),1.4f);
    }
    @Override public LivingEntityRenderState createRenderState(){
        return new LivingEntityRenderState();
    }
    @Override public Identifier getTextureLocation(LivingEntityRenderState state){
        return TEXTURE;
    }
}
