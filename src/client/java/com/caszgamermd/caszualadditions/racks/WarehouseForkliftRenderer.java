package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** The forklift carries the same FULL-SIZE pallet platform and carton stack
 * shown between four carrying Storage Golems, not a tiny inventory icon.
 * Movement and mast translation use the real vehicle's rendering yaw. */
public final class WarehouseForkliftRenderer extends MobRenderer<
        WarehouseForklift,WarehouseForkliftRenderState,WarehouseForkliftModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("warehouse_forklift"),"main");
    private static final Identifier TEXTURE=
            CaszualAdditions.id("textures/entity/warehouse_forklift.png");
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    public WarehouseForkliftRenderer(EntityRendererProvider.Context ctx){
        super(ctx,new WarehouseForkliftModel(ctx.bakeLayer(LAYER)),.65f);
        blocks=ctx.getBlockModelResolver();
    }
    @Override public void extractRenderState(WarehouseForklift vehicle,
            WarehouseForkliftRenderState state,float partialTick){
        super.extractRenderState(vehicle,state,partialTick);
        state.mastHeight=vehicle.mastHeight();
        state.task=vehicle.action();
        ItemStack deck=vehicle.displayPallet();
        state.hasPallet=!deck.isEmpty();
        state.visibleCargo=deck.isEmpty()?0:vehicle.displayedCargoCrates();
        if(deck.isEmpty())return;
        Block material=Blocks.OAK_PLANKS;
        boolean plastic=false;
        if(deck.getItem() instanceof BlockItem item
                &&item.getBlock() instanceof PalletBlock pallet){
            material=switch(pallet.kind()){
                case IRON->Blocks.IRON_BLOCK;
                case COPPER->PalletContent.copperTextureBlock(pallet.copperAge());
                case PLASTIC->PalletContent.PALLET_RENDER_PROXY;
                default->PalletData.wood(deck).getFirst();
            };
            plastic=pallet.kind()==PalletKind.PLASTIC;
        }
        blocks.update(state.palletDeck,material.defaultBlockState(),DISPLAY);
        state.palletDeck.tintLayers().clear();
        if(plastic)state.palletDeck.tintLayers().add(PalletData.plasticColor(deck));
        for(int index=0;index<4;index++){
            Block sample=material;
            if(deck.getItem() instanceof BlockItem item
                    &&item.getBlock() instanceof PalletBlock pallet
                    &&pallet.kind()==PalletKind.WOOD)
                sample=PalletData.wood(deck).get(index);
            blocks.update(state.boards[index],sample.defaultBlockState(),DISPLAY);
            state.boards[index].tintLayers().clear();
            if(plastic)state.boards[index].tintLayers().add(PalletData.plasticColor(deck));
        }
        blocks.update(state.support,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        blocks.update(state.crate,PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
        blocks.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
    }
    @Override public void submit(WarehouseForkliftRenderState state,
            PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(!state.hasPallet)return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180-state.bodyRot));
        // The REAL forks sit just above wheel height. Keep a 2x2 pallet
        // low for driving and lift together with the telescoping carriage.
        pose.translate(-.90f,.32f+state.mastHeight,-1.72f);
        // Four material-accurate long boards and transverse pallet runners.
        for(int board=0;board<4;board++)
            piece(state.boards[board],state,pose,collector,
                    0,.10f,.03f+board*.43f,1.80f,.12f,.42f);
        for(float x:new float[]{.08f,.73f,1.58f})
            piece(state.palletDeck,state,pose,collector,
                    x,.015f,.07f,.14f,.13f,1.73f);
        for(float x:new float[]{.12f,1.60f})
            for(float z:new float[]{.10f,1.60f})
                piece(state.support,state,pose,collector,
                        x,-.03f,z,.10f,.12f,.10f);
        // Exactly the same 3x3x3 visible carton capacity as the four-Golem
        // pallet rendering. The underlying 216 slots never leave the Jack.
        for(int i=0;i<state.visibleCargo;i++){
            int col=i%3,row=(i/3)%3,layer=i/9;
            piece(state.crate,state,pose,collector,
                    .13f+col*.53f,.28f+layer*.49f,.13f+row*.53f,
                    .50f,.47f,.50f);
        }
        float wallHeight=Math.max(.62f,
                .27f+(float)Math.ceil(state.visibleCargo/9.0)*.49f);
        piece(state.glass,state,pose,collector,0,.23f,0,1.79f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,0,.23f,1.76f,1.79f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,0,.23f,.02f,.025f,wallHeight,1.74f);
        piece(state.glass,state,pose,collector,1.77f,.23f,.02f,.025f,wallHeight,1.74f);
        pose.popPose();
    }
    private static void piece(BlockModelRenderState model,WarehouseForkliftRenderState state,
            PoseStack pose,SubmitNodeCollector collector,
            float x,float y,float z,float width,float height,float depth){
        pose.pushPose();
        pose.translate(x,y,z);
        pose.scale(width,height,depth);
        model.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public WarehouseForkliftRenderState createRenderState(){
        return new WarehouseForkliftRenderState();
    }
    @Override public Identifier getTextureLocation(WarehouseForkliftRenderState state){
        return TEXTURE;
    }
}
