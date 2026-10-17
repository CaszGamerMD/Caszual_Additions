package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class StorageGolemRenderer
        extends MobRenderer<StorageGolem,StorageGolemRenderState,StorageGolemModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("storage_golem"),"main");
    private static final Identifier TEXTURE=CaszualAdditions.id("textures/entity/storage_golem.png");
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;

    public StorageGolemRenderer(EntityRendererProvider.Context ctx){
        super(ctx,new StorageGolemModel(ctx.bakeLayer(LAYER)),.35f);
        blocks=ctx.getBlockModelResolver();
    }
    @Override public void extractRenderState(StorageGolem golem,
            StorageGolemRenderState state,float partialTick){
        super.extractRenderState(golem,state,partialTick);
        var name=golem.getCustomName();
        String label=name==null?"":name.getString();
        state.carryingPallet=label.startsWith("PALLET ");
        state.pushingEmptyPallet=label.startsWith("EMPTY PALLET")&&!golem.displayPallet().isEmpty();
        state.showSharedPallet=state.carryingPallet
                &&label.startsWith("PALLET 1/4")
                &&!golem.displayPallet().isEmpty();
        state.visibleCargo=state.showSharedPallet?golem.visibleCargoCount():0;
        state.heldSlots=state.carryingPallet?0:golem.displayedLoadSlots();
        for(int i=0;i<state.heldSlots&&i<9;i++){
            ItemStack sample=golem.displayedLoad(i);
            if(sample.isEmpty())continue;
            Block material=PalletContent.CARDBOARD_RENDER_PROXY;
            if(sample.getItem() instanceof BlockItem blockItem
                    &&blockItem.getBlock().defaultBlockState().isSolid())
                material=blockItem.getBlock();
            blocks.update(state.held[i],material.defaultBlockState(),DISPLAY);
        }
        if(state.showSharedPallet||state.pushingEmptyPallet){
            Block material=Blocks.OAK_PLANKS;
            ItemStack carried=golem.displayPallet();
            boolean plastic=false;
            if(carried.getItem() instanceof BlockItem item
                    &&item.getBlock() instanceof PalletBlock pallet){
                material=switch(pallet.kind()){
                    case IRON->Blocks.IRON_BLOCK;
                    case COPPER->PalletContent.copperTextureBlock(pallet.copperAge());
                    case PLASTIC->PalletContent.PALLET_RENDER_PROXY;
                    default->PalletData.wood(carried).getFirst();
                };
            }
            plastic=carried.getItem() instanceof BlockItem bi
                    &&bi.getBlock() instanceof PalletBlock pb
                    &&pb.kind()==PalletKind.PLASTIC;
            blocks.update(state.palletDeck,material.defaultBlockState(),DISPLAY);
            state.palletDeck.tintLayers().clear();
            if(plastic)state.palletDeck.tintLayers().add(PalletData.plasticColor(carried));
            blocks.update(state.palletSupport,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
            blocks.update(state.crate,
                    PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
            blocks.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
        }
    }
    /** Render one actual two-by-two pallet platform above the raised hands
     * of all four Golems. Index-0 carrier is the only renderer submitting
     * the pallet: cargo remains owned by the lead server-side Golem. */
    @Override public void submit(StorageGolemRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(state.heldSlots>0&&!state.carryingPallet){
            // Nine real cargo slots become a visible tray of matching
            // block textures or cardboard cartons for non-solid items.
            // This is a rendering preview ONLY; no stacks are spawned.
            pose.pushPose();
            for(int i=0;i<state.heldSlots&&i<9;i++){
                int col=i%3, row=i/3;
                piece(state.held[i],state,pose,collector,
                        -.37f+col*.255f,.90f+row*.23f,-.68f,
                        .23f,.21f,.23f);
            }
            pose.popPose();
        }
        if(state.pushingEmptyPallet){
            // A single Golem pushes the replacement vertically on its edge.
            pose.pushPose();
            for(float y:new float[]{.23f,.58f,.91f})
                piece(state.palletDeck,state,pose,collector,
                        -.54f,y,-.77f,1.08f,.13f,.13f);
            for(float x:new float[]{-.42f,.33f})
                piece(state.palletSupport,state,pose,collector,
                        x,.18f,-.82f,.11f,.89f,.13f);
            pose.popPose();
        }
        if(!state.showSharedPallet)return;
        pose.pushPose();
        // Crew index 0 is NW of a 1.8 x 1.8 formation. Four corners
        // remain directly beneath this deck as workers trail the player.
        pose.translate(.04f,1.46f,.04f);
        for(float z:new float[]{.08f,1.48f})
            piece(state.palletDeck,state,pose,collector,.02f,0,z,1.76f,.10f,.24f);
        for(float x:new float[]{.10f,.73f,1.36f})
            piece(state.palletDeck,state,pose,collector,x,.13f,.08f,.32f,.12f,1.64f);
        for(float x:new float[]{.10f,1.60f})
            for(float z:new float[]{.10f,1.60f})
                piece(state.palletSupport,state,pose,collector,x,-.12f,z,.10f,.17f,.10f);
        // The first 27 visible stacks are represented as 3x3x3 cartons,
        // retaining an accurate filled-slot count without client-side
        // duplication of the 216-slot server inventory.
        for(int i=0;i<state.visibleCargo;i++){
            int col=i%3,row=(i/3)%3,layer=i/9;
            piece(state.crate,state,pose,collector,
                    .14f+col*.54f,.27f+layer*.51f,.14f+row*.54f,
                    .51f,.49f,.51f);
        }
        float wallHeight=Math.max(.64f,
                .29f+(float)Math.ceil(state.visibleCargo/9.0)*.51f);
        // Clear, 1px-ish glass panels contain raised cargo on all sides.
        piece(state.glass,state,pose,collector,.04f,.23f,.035f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.04f,.23f,1.74f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.035f,.23f,.06f,.025f,wallHeight,1.68f);
        piece(state.glass,state,pose,collector,1.78f,.23f,.06f,.025f,wallHeight,1.68f);
        pose.popPose();
    }
    private static void piece(BlockModelRenderState block,StorageGolemRenderState state,
            PoseStack pose,SubmitNodeCollector collector,
            float x,float y,float z,float width,float height,float depth){
        pose.pushPose();
        pose.translate(x,y,z);
        pose.scale(width,height,depth);
        block.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public StorageGolemRenderState createRenderState(){
        return new StorageGolemRenderState();
    }
    @Override public Identifier getTextureLocation(StorageGolemRenderState state){
        return TEXTURE;
    }
}
