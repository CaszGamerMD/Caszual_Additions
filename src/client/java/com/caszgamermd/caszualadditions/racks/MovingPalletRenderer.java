package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
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

/** Exactly one client renderer moves the pallet and its four attached
 * animated Storage Golems as an inseparable unit. */
public final class MovingPalletRenderer extends MobRenderer<
        MovingPallet,MovingPalletRenderState,MovingPalletModel> {
    public static final ModelLayerLocation LAYER=
            new ModelLayerLocation(CaszualAdditions.id("moving_pallet"),"main");
    private static final Identifier TEXTURE=
            CaszualAdditions.id("textures/entity/storage_golem.png");
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver resolver;
    public MovingPalletRenderer(EntityRendererProvider.Context context){
        super(context,new MovingPalletModel(context.bakeLayer(LAYER)),1.2f);
        resolver=context.getBlockModelResolver();
    }
    @Override public void extractRenderState(MovingPallet entity,
            MovingPalletRenderState state,float partialTick){
        super.extractRenderState(entity,state,partialTick);
        ItemStack stack=entity.displayPallet();
        state.visible=!stack.isEmpty();
        state.cargo=stack.isEmpty()?0:entity.visibleCargo();
        if(stack.isEmpty())return;
        Block material=Blocks.OAK_PLANKS;
        boolean plastic=false;
        if(stack.getItem() instanceof BlockItem item
                &&item.getBlock() instanceof PalletBlock block){
            material=switch(block.kind()){
                case IRON->Blocks.IRON_BLOCK;
                case COPPER->PalletContent.copperTextureBlock(block.copperAge());
                case PLASTIC->PalletContent.PALLET_RENDER_PROXY;
                default->PalletData.wood(stack).getFirst();
            };
            plastic=block.kind()==PalletKind.PLASTIC;
        }
        resolver.update(state.deck,material.defaultBlockState(),DISPLAY);
        state.deck.tintLayers().clear();
        if(plastic)state.deck.tintLayers().add(PalletData.plasticColor(stack));
        resolver.update(state.foot,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        resolver.update(state.crate,
                PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
        resolver.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
    }
    @Override public void submit(MovingPalletRenderState state,PoseStack pose,
            SubmitNodeCollector collector,CameraRenderState camera){
        super.submit(state,pose,collector,camera);
        if(!state.visible)return;
        pose.pushPose();
        // Origin = geometric middle of the unit. All four Golems have
        // identical corner offsets relative to this one pallet platform.
        pose.translate(-.9f,1.48f,-.9f);
        for(float z:new float[]{.08f,1.48f})
            piece(state.deck,state,pose,collector,.02f,0,z,1.76f,.10f,.24f);
        for(float x:new float[]{.10f,.73f,1.36f})
            piece(state.deck,state,pose,collector,x,.13f,.08f,.32f,.12f,1.64f);
        for(float x:new float[]{.10f,1.60f})
            for(float z:new float[]{.10f,1.60f})
                piece(state.foot,state,pose,collector,x,-.12f,z,.10f,.17f,.10f);
        for(int i=0;i<state.cargo;i++){
            int col=i%3,row=i/3%3,layer=i/9;
            piece(state.crate,state,pose,collector,
                    .14f+col*.54f,.27f+layer*.51f,.14f+row*.54f,
                    .51f,.49f,.51f);
        }
        float wallHeight=Math.max(.64f,
                .29f+(float)Math.ceil(state.cargo/9.0)*.51f);
        piece(state.glass,state,pose,collector,.04f,.23f,.035f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.04f,.23f,1.74f,1.77f,wallHeight,.025f);
        piece(state.glass,state,pose,collector,.035f,.23f,.06f,.025f,wallHeight,1.68f);
        piece(state.glass,state,pose,collector,1.78f,.23f,.06f,.025f,wallHeight,1.68f);
        pose.popPose();
    }
    private static void piece(BlockModelRenderState block,MovingPalletRenderState state,
            PoseStack pose,SubmitNodeCollector collector,
            float x,float y,float z,float sx,float sy,float sz){
        pose.pushPose();
        pose.translate(x,y,z);
        pose.scale(sx,sy,sz);
        block.submit(pose,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        pose.popPose();
    }
    @Override public MovingPalletRenderState createRenderState(){
        return new MovingPalletRenderState();
    }
    @Override public Identifier getTextureLocation(MovingPalletRenderState state){
        return TEXTURE;
    }
}
