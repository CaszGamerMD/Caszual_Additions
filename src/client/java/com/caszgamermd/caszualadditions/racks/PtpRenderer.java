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
import net.minecraft.client.renderer.item.ItemStackRenderState;
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
 * A full two-by-two pallet is centered over the three-by-three transfer floor.
 * This renderer runs only on the platform's center controller block entity.
 */
public final class PtpRenderer implements BlockEntityRenderer<PtpBlockEntity,RackRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    private final ItemModelResolver itemModels;
    public PtpRenderer(BlockEntityRendererProvider.Context c){
        blocks=c.blockModelResolver();itemModels=c.itemModelResolver();
    }
    @Override public RackRenderState createRenderState(){return new RackRenderState();}
    @Override public void extractRenderState(PtpBlockEntity ptp,RackRenderState state,
            float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(ptp,state,partial,camera,cracks);
        state.pallet=ptp.hasPallet();
        state.sealed=ptp.sealed();
        state.inputMode=ptp.input();
        blocks.update(state.statusLamp,(ptp.hauling()?Blocks.AMETHYST_BLOCK
                :ptp.input()?Blocks.EMERALD_BLOCK:Blocks.REDSTONE_BLOCK)
                .defaultBlockState(),DISPLAY);
        state.visible=0;
        if(!state.pallet){
            for(var item:state.items)item.clear();
            return;
        }
        ItemStack item=ptp.palletStack();
        Block[] deck=new Block[]{Blocks.OAK_PLANKS,Blocks.OAK_PLANKS,
                Blocks.OAK_PLANKS,Blocks.OAK_PLANKS};
        boolean plastic=false;
        if(item.getItem() instanceof BlockItem bi &&bi.getBlock() instanceof PalletBlock pallet){
            if(pallet.kind()==PalletKind.WOOD){
                var woods=PalletData.wood(item);
                for(int i=0;i<4;i++)deck[i]=woods.get(i);
            } else if(pallet.kind()==PalletKind.PLASTIC){
                plastic=true;
                for(int i=0;i<4;i++)deck[i]=PalletContent.PALLET_RENDER_PROXY;
            } else{
                Block metal=pallet.kind()==PalletKind.IRON?Blocks.IRON_BLOCK
                        :PalletContent.copperTextureBlock(pallet.copperAge());
                for(int i=0;i<4;i++)deck[i]=metal;
            }
        }
        for(int i=0;i<4;i++){
            blocks.update(state.boards[i],deck[i].defaultBlockState(),DISPLAY);
            state.boards[i].tintLayers().clear();
            if(plastic)state.boards[i].tintLayers().add(PalletData.plasticColor(item));
        }
        blocks.update(state.carton,PalletContent.CARDBOARD_RENDER_PROXY.defaultBlockState(),DISPLAY);
        // Use transparent glass panels as the full-height sealed envelope.
        blocks.update(state.nameplate,Blocks.GLASS.defaultBlockState(),DISPLAY);
        for(int slot=0;slot<ptp.getContainerSize() && state.visible<RackRenderState.CARGO;slot++){
            ItemStack stack=ptp.displayItem(slot);
            if(stack.isEmpty())continue;
            int i=state.visible++;
            state.boxed[i]=!(stack.getItem() instanceof BlockItem);
            itemModels.updateForTopItem(state.items[i],stack,
                    state.boxed[i]?ItemDisplayContext.GUI:ItemDisplayContext.NONE,
                    ptp.getLevel(),null,slot);
        }
        for(int i=state.visible;i<RackRenderState.CARGO;i++)state.items[i].clear();
    }
    @Override public void submit(RackRenderState state,PoseStack poses,
            SubmitNodeCollector collector,CameraRenderState camera){
        if(!state.pallet)return;
        poses.pushPose();
        // The controller is at the CENTER block of the 3x3 floor.
        // A 2x2 pallet centered over it must extend half a block each way.
        poses.translate(-.5,.25,-.5);
        // Four indicator lamps occupy the exposed 3x3 ring, not the
        // centered pallet. Green = input, red = output.
        for(float x:new float[]{-.32f,2.15f})
            for(float z:new float[]{-.32f,2.15f})
                box(state.statusLamp,state,poses,collector,x,.015f,z,.17f,.09f,.17f);
        for(int i=0;i<4;i++){
            float x=.04f+i*.49f;
            box(state.boards[i],state,poses,collector,x,.19f,.015f,.43f,.105f,.985f);
            box(state.boards[i],state,poses,collector,x,.19f,1.0f,.43f,.105f,.985f);
        }
        for(float x:new float[]{.035f,1.0f}){
            box(state.boards[2],state,poses,collector,x,.035f,.12f,.965f,.15f,.23f);
            box(state.boards[3],state,poses,collector,x,.035f,.88f,.965f,.15f,.23f);
            box(state.boards[0],state,poses,collector,x,.035f,1.65f,.965f,.15f,.23f);
        }
        double cell=RackRenderState.CELL;
        for(int i=0;i<state.visible;i++){
            int col=i%3,row=i/3%3,layer=i/9;
            double xx=col*cell,zz=row*cell,yy=5.0/16.0+layer*cell;
            poses.pushPose();
            if(state.boxed[i]){
                poses.translate(xx,yy,zz);
                poses.scale((float)cell,(float)cell,(float)cell);
                state.carton.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                poses.pushPose();
                poses.translate(.5,.5,-.016);
                poses.scale(.44f,.44f,.44f);
                state.items[i].submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
                poses.popPose();
            }else{
                poses.translate(xx+cell*.5,yy+cell*.5,zz+cell*.5);
                poses.scale((float)cell,(float)cell,(float)cell);
                state.items[i].submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
            }
            poses.popPose();
        }
        if(state.sealed){
            // Four thin GLASS panes, not iron bands or crossed straps.
            // Panels enclose the displayed cargo and sit just outside its
            // two-block footprint. Split corners to avoid overlapping faces.
            float height=Math.max(.8f,(float)(5.0/16.0
                    +Math.ceil(state.visible/9.0)*RackRenderState.CELL+.08));
            height=Math.min(5.1f,height);
            float bottom=.29f,wall=height-bottom;
            box(state.nameplate,state,poses,collector,-.04f,bottom,-.045f,2.08f,wall,.033f);
            box(state.nameplate,state,poses,collector,-.04f,bottom,2.012f,2.08f,wall,.033f);
            box(state.nameplate,state,poses,collector,-.045f,bottom,-.01f,.033f,wall,2.02f);
            box(state.nameplate,state,poses,collector,2.012f,bottom,-.01f,.033f,wall,2.02f);
        }
        poses.popPose();
    }
    private static void box(net.minecraft.client.renderer.block.BlockModelRenderState model,
            RackRenderState state,PoseStack poses,SubmitNodeCollector collector,
            float x,float y,float z,float sx,float sy,float sz){
        poses.pushPose();poses.translate(x,y,z);poses.scale(sx,sy,sz);
        model.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        poses.popPose();
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 96;}
}
