package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.resources.Identifier;

/** Fork assembly and pallet live in the same yaw/pose space. */
public final class WarehouseForkliftRenderer extends MobRenderer<
        WarehouseForklift,WarehouseForkliftRenderState,WarehouseForkliftModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("warehouse_forklift"),"main");
    private static final Identifier TEXTURE=
            CaszualAdditions.id("textures/entity/warehouse_forklift.png");
    private final ItemModelResolver items;
    public WarehouseForkliftRenderer(EntityRendererProvider.Context ctx){
        super(ctx,new WarehouseForkliftModel(ctx.bakeLayer(LAYER)),.65f);
        items=ctx.getItemModelResolver();
    }
    @Override public void extractRenderState(WarehouseForklift vehicle,
            WarehouseForkliftRenderState state,float partialTick){
        super.extractRenderState(vehicle,state,partialTick);
        state.mastHeight=vehicle.mastHeight();
        state.task=vehicle.action();
        if(vehicle.displayPallet().isEmpty())state.pallet.clear();
        else items.updateForLiving(state.pallet,vehicle.displayPallet(),
                ItemDisplayContext.FIXED,vehicle);
    }
    @Override public void submit(WarehouseForkliftRenderState state,
            PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        // The real ItemStack is rendered above the forward fork carriage.
        // Its visual orientation follows the forklift heading and mast lift.
        if(state.pallet.isEmpty())return;
        pose.pushPose();
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180-state.bodyRot));
        pose.translate(0,1.25+state.mastHeight,-1.60);
        pose.scale(1.4f,1.4f,1.4f);
        state.pallet.submit(pose,collector,state.lightCoords,
                net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public WarehouseForkliftRenderState createRenderState(){
        return new WarehouseForkliftRenderState();
    }
    @Override public Identifier getTextureLocation(WarehouseForkliftRenderState state){
        return TEXTURE;
    }
}
