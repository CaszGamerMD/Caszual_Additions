package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Render a true pallet deck and the first 27 cargo items INSIDE the 2x2 rack
 * frame. No negative-scale mirroring and no internal support cubes.
 */
public final class RackBlockEntityRenderer implements BlockEntityRenderer<RackBlockEntity,RackRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    private final ItemModelResolver items;
    public RackBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blocks=context.blockModelResolver();
        items=context.itemModelResolver();
    }
    @Override public RackRenderState createRenderState(){return new RackRenderState();}

    @Override public void extractRenderState(RackBlockEntity rack,RackRenderState state,
            float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(rack,state,partial,camera,cracks);
        state.facing=rack.facing();
        state.pallet=rack.hasPallet();
        state.visible=0;
        if(!state.pallet){
            for(var item:state.items)item.clear();
            return;
        }
        ItemStack pallet=rack.palletStack();
        Block base=Blocks.OAK_PLANKS;
        Block[] chosen=new Block[]{base,base,base,base};
        boolean plastic=false;
        if(pallet.getItem() instanceof BlockItem palletItem
                && palletItem.getBlock() instanceof PalletBlock block){
            if(block.kind()==PalletKind.WOOD){
                var woods=PalletData.wood(pallet);
                for(int i=0;i<4;i++)chosen[i]=woods.get(i);
            } else if(block.kind()==PalletKind.PLASTIC){
                plastic=true;
                for(int i=0;i<4;i++)chosen[i]=PalletContent.PALLET_RENDER_PROXY;
            } else {
                Block material=block.kind()==PalletKind.IRON?Blocks.IRON_BLOCK
                        :PalletContent.copperTextureBlock(block.copperAge());
                for(int i=0;i<4;i++)chosen[i]=material;
            }
        }
        for(int i=0;i<4;i++){
            blocks.update(state.boards[i],chosen[i].defaultBlockState(),DISPLAY);
            state.boards[i].tintLayers().clear();
            if(plastic)state.boards[i].tintLayers().add(PalletData.plasticColor(pallet));
        }
        blocks.update(state.carton,PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
        for(int slot=0;slot<rack.getContainerSize()&&state.visible<RackRenderState.CARGO;slot++){
            ItemStack stack=rack.getItem(slot);
            if(stack.isEmpty())continue;
            int i=state.visible++;
            state.boxed[i]=!(stack.getItem() instanceof BlockItem);
            items.updateForTopItem(state.items[i],stack,
                    state.boxed[i]?ItemDisplayContext.GUI:ItemDisplayContext.NONE,
                    rack.getLevel(),null,slot);
        }
        for(int i=state.visible;i<RackRenderState.CARGO;i++)state.items[i].clear();
    }

    @Override public void submit(RackRenderState state,PoseStack poses,
            SubmitNodeCollector collector,CameraRenderState camera){
        if(!state.pallet)return;
        // Four rotated rack footprints all fit exactly inside the two-by-two
        // world blocks owned by the rack's front-left root.
        var right=state.facing.getCounterClockWise();
        var back=state.facing.getOpposite();
        double x=Math.min(0,Math.min(right.getStepX(),
                Math.min(back.getStepX(),right.getStepX()+back.getStepX())));
        double z=Math.min(0,Math.min(right.getStepZ(),
                Math.min(back.getStepZ(),right.getStepZ()+back.getStepZ())));
        poses.pushPose();
        poses.translate(x,0,z);
        for(int i=0;i<4;i++){
            float offset=.04f+i*.49f;
            board(state,poses,collector,i,offset,.19f,.015f,.43f,.105f,.985f);
            board(state,poses,collector,i,offset,.19f,1.0f,.43f,.105f,.985f);
        }
        for(float run:new float[]{.035f,1.0f}){
            board(state,poses,collector,2,run,.035f,.12f,.965f,.15f,.23f);
            board(state,poses,collector,3,run,.035f,.88f,.965f,.15f,.23f);
            board(state,poses,collector,0,run,.035f,1.65f,.965f,.15f,.23f);
        }
        for(int i=0;i<state.visible;i++){
            int row=(i%9)/3,col=i%3,layer=i/9;
            float cell=RackRenderState.CELL;
            double xx=col*cell, zz=row*cell;
            double yy=5.0/16.0+layer*cell;
            poses.pushPose();
            if(state.boxed[i]){
                poses.translate(xx,yy,zz);
                poses.scale(cell,cell,cell);
                state.carton.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                poses.pushPose();
                poses.translate(.5,.5,-.016);
                poses.scale(.44f,.44f,.44f);
                state.items[i].submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                poses.popPose();
            }else{
                poses.translate(xx+cell*.5,yy+cell*.5,zz+cell*.5);
                poses.scale(cell,cell,cell);
                state.items[i].submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            }
            poses.popPose();
        }
        poses.popPose();
    }

    private static void board(RackRenderState state,PoseStack poses,SubmitNodeCollector collector,
            int index,float x,float y,float z,float w,float h,float depth){
        poses.pushPose();poses.translate(x,y,z);poses.scale(w,h,depth);
        state.boards[index].submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        poses.popPose();
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 80;}
}
