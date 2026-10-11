package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

public final class StorageGolemRenderer extends MobRenderer<StorageGolem,StorageGolemRenderState,StorageGolemModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("storage_golem"),"main");
    private static final Identifier TEXTURE=CaszualAdditions.id("textures/entity/storage_golem.png");
    public StorageGolemRenderer(EntityRendererProvider.Context ctx){
        super(ctx,new StorageGolemModel(ctx.bakeLayer(LAYER)),.35f);
    }
    @Override public StorageGolemRenderState createRenderState(){
        return new StorageGolemRenderState();
    }
    @Override public Identifier getTextureLocation(StorageGolemRenderState state){
        return TEXTURE;
    }
}
