package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.util.Mth;

/** Jack fork assembly and an optional REAL pallet in transport position.
 * Draws from block model materials; no static inventory item floating in air. */
public final class PalletJackRenderer extends EntityRenderer<
        PalletJackVehicle,PalletJackRenderState> {
    private static final Identifier TEXTURE=CaszualAdditions.id(
            "textures/entity/warehouse_forklift.png");
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    public PalletJackRenderer(EntityRendererProvider.Context context){
        super(context);
        shadowRadius=.52f;
        blocks=context.getBlockModelResolver();
    }
    @Override public PalletJackRenderState createRenderState(){return new PalletJackRenderState();}
    @Override public void extractRenderState(PalletJackVehicle vehicle,
            PalletJackRenderState state,float partialTick){
        super.extractRenderState(vehicle,state,partialTick);
        state.heading=Mth.rotLerp(partialTick,vehicle.yRotO,vehicle.getYRot());
        ItemStack pallet=vehicle.displayPallet();
        state.loaded=!pallet.isEmpty();
        state.cargo=state.loaded?vehicle.displayedCargoCrates():0;
        blocks.update(state.metal,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        blocks.update(state.handle,Blocks.POLISHED_BLACKSTONE.defaultBlockState(),DISPLAY);
        blocks.update(state.wheel,Blocks.POLISHED_BLACKSTONE.defaultBlockState(),DISPLAY);
        if(!state.loaded)return;
        Block material=Blocks.OAK_PLANKS;
        boolean plastic=false;
        if(pallet.getItem() instanceof BlockItem item&&item.getBlock() instanceof PalletBlock block){
            material=switch(block.kind()){
                case IRON->Blocks.IRON_BLOCK;
                case COPPER->PalletContent.copperTextureBlock(block.copperAge());
                case PLASTIC->PalletContent.PALLET_RENDER_PROXY;
                default->PalletData.wood(pallet).getFirst();
            };
            plastic=block.kind()==PalletKind.PLASTIC;
        }
        blocks.update(state.deck,material.defaultBlockState(),DISPLAY);
        state.deck.tintLayers().clear();
        if(plastic)state.deck.tintLayers().add(PalletData.plasticColor(pallet));
        for(int i=0;i<4;i++){
            Block board=material;
            if(pallet.getItem() instanceof BlockItem item
                    &&item.getBlock() instanceof PalletBlock block
                    &&block.kind()==PalletKind.WOOD)
                board=PalletData.wood(pallet).get(i);
            blocks.update(state.boards[i],board.defaultBlockState(),DISPLAY);
            state.boards[i].tintLayers().clear();
            if(plastic)state.boards[i].tintLayers().add(PalletData.plasticColor(pallet));
        }
        blocks.update(state.crate,
                PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
        blocks.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
    }
    private static void piece(BlockModelRenderState model,PalletJackRenderState state,
            PoseStack pose,SubmitNodeCollector collector,
            float x,float y,float z,float w,float h,float d){
        pose.pushPose();pose.translate(x,y,z);pose.scale(w,h,d);
        model.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public void submit(PalletJackRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180-state.heading));
        // Two forward fork tines, cross beam, four black wheels.
        for(float x:new float[]{-.55f,.40f}){
            piece(state.metal,state,pose,collector,x,.13f,-2.05f,.16f,.10f,2.22f);
            piece(state.wheel,state,pose,collector,x-.07f,.02f,-.48f,.31f,.22f,.29f);
            piece(state.wheel,state,pose,collector,x-.07f,.02f,.09f,.31f,.22f,.29f);
        }
        piece(state.metal,state,pose,collector,-.70f,.20f,-.36f,1.40f,.24f,.36f);
        // U-shaped push handle enclosing the player's rearward walking stance.
        for(float x:new float[]{-.66f,.58f})
            piece(state.handle,state,pose,collector,x,.30f,.33f,.09f,1.00f,.11f);
        piece(state.handle,state,pose,collector,-.66f,1.24f,.33f,1.33f,.10f,.12f);
        if(state.loaded){
            // Pallet on the FRONT of the jack, not behind or inside the rider.
            pose.pushPose();
            pose.translate(-.86f,.30f,-2.26f);
            for(int i=0;i<4;i++)
                piece(state.boards[i],state,pose,collector,
                    .02f,.13f,.03f+i*.43f,1.77f,.12f,.42f);
            for(float x:new float[]{.10f,.75f,1.61f})
                piece(state.deck,state,pose,collector,
                    x,.02f,.08f,.10f,.15f,1.70f);
            for(float x:new float[]{.15f,1.61f})for(float z:new float[]{.12f,1.64f})
                piece(state.metal,state,pose,collector,x,-.04f,z,.11f,.12f,.10f);
            for(int i=0;i<state.cargo;i++){
                int col=i%3,row=(i/3)%3,layer=i/9;
                piece(state.crate,state,pose,collector,
                    .14f+col*.53f,.30f+layer*.50f,.12f+row*.53f,
                    .50f,.47f,.50f);
            }
            float wallH=Math.max(.62f,.28f+((state.cargo+8)/9)*.51f);
            piece(state.glass,state,pose,collector,.04f,.26f,.035f,1.76f,wallH,.025f);
            piece(state.glass,state,pose,collector,.04f,.26f,1.76f,1.76f,wallH,.025f);
            piece(state.glass,state,pose,collector,.035f,.26f,.06f,.025f,wallH,1.68f);
            piece(state.glass,state,pose,collector,1.79f,.26f,.06f,.025f,wallH,1.68f);
            pose.popPose();
        }
        pose.popPose();
    }
}
