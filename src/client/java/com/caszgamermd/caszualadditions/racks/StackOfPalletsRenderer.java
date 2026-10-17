package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.GlassPalletWrap;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * One controller renders the EXACT twelve saved pallet ItemStacks as a
 * 2-by-2 array of mini pallet decks in each of 3 tiers: 4 x 3 = 12.
 * Material for each deck board is derived from that particular saved item:
 * mixed plank selections, plastic dye tint, iron, copper ageing and wrap.
 */
public final class StackOfPalletsRenderer implements
        BlockEntityRenderer<StackOfPalletsBlockEntity,StackOfPalletsRenderState> {
    private static final BlockDisplayContext DISPLAY=BlockDisplayContext.create();
    private final BlockModelResolver blocks;
    public StackOfPalletsRenderer(BlockEntityRendererProvider.Context context){
        blocks=context.blockModelResolver();
    }
    @Override public StackOfPalletsRenderState createRenderState(){
        return new StackOfPalletsRenderState();
    }
    @Override public void extractRenderState(StackOfPalletsBlockEntity magazine,
            StackOfPalletsRenderState state,float partial,Vec3 camera,
            ModelFeatureRenderer.@org.jspecify.annotations.Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(magazine,state,partial,camera,cracks);
        blocks.update(state.glass,Blocks.GLASS.defaultBlockState(),DISPLAY);
        for(int slot=0;slot<12;slot++){
            ItemStack palletItem=magazine.getItem(slot);
            state.occupied[slot]=StackOfPalletsItem.isPallet(palletItem);
            state.wrapped[slot]=state.occupied[slot]&&GlassPalletWrap.wrapped(palletItem);
            if(!state.occupied[slot])continue;
            Block[] deck={Blocks.OAK_PLANKS,Blocks.OAK_PLANKS,
                    Blocks.OAK_PLANKS,Blocks.OAK_PLANKS};
            boolean plastic=false;
            if(palletItem.getItem() instanceof BlockItem item
                    &&item.getBlock() instanceof PalletBlock pallet){
                if(pallet.kind()==PalletKind.WOOD){
                    var woods=PalletData.wood(palletItem);
                    for(int i=0;i<4;i++)deck[i]=woods.get(i);
                }else if(pallet.kind()==PalletKind.PLASTIC){
                    plastic=true;
                    for(int i=0;i<4;i++)deck[i]=PalletContent.PALLET_RENDER_PROXY;
                }else{
                    Block metal=pallet.kind()==PalletKind.IRON?Blocks.IRON_BLOCK
                            :PalletContent.copperTextureBlock(pallet.copperAge());
                    for(int i=0;i<4;i++)deck[i]=metal;
                }
            }
            for(int i=0;i<4;i++){
                blocks.update(state.boards[slot][i],deck[i].defaultBlockState(),DISPLAY);
                state.boards[slot][i].tintLayers().clear();
                if(plastic)
                    state.boards[slot][i].tintLayers().add(
                            PalletData.plasticColor(palletItem));
            }
        }
    }
    private static void cube(net.minecraft.client.renderer.block.BlockModelRenderState model,
            StackOfPalletsRenderState state,PoseStack poses,SubmitNodeCollector collector,
            float x,float y,float z,float w,float h,float d){
        poses.pushPose();
        poses.translate(x,y,z);
        poses.scale(w,h,d);
        model.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
        poses.popPose();
    }
    @Override public void submit(StackOfPalletsRenderState state,PoseStack poses,
            SubmitNodeCollector collector,CameraRenderState camera){
        for(int i=0;i<12;i++){
            if(!state.occupied[i])continue;
            int layer=i/4,slot=i%4;
            // The real pallet spans 2x2 blocks. Here its EXACT deck mesh
            // is uniformly scaled to .46, so four fit within the 2x2 crate.
            float px=(slot%2)*1.0f+.04f;
            float pz=(slot/2)*1.0f+.04f;
            float py=.04f+layer*.275f;
            poses.pushPose();
            poses.translate(px,py,pz);
            poses.scale(.46f,.46f,.46f);
            for(int board=0;board<4;board++){
                float x=.04f+board*.49f;
                cube(state.boards[i][board],state,poses,collector,
                        x,.19f,.015f,.43f,.105f,.985f);
                cube(state.boards[i][board],state,poses,collector,
                        x,.19f,1.0f,.43f,.105f,.985f);
            }
            for(float x:new float[]{.035f,1.0f}){
                cube(state.boards[i][2],state,poses,collector,x,.035f,.12f,.965f,.15f,.23f);
                cube(state.boards[i][3],state,poses,collector,x,.035f,.88f,.965f,.15f,.23f);
                cube(state.boards[i][0],state,poses,collector,x,.035f,1.65f,.965f,.15f,.23f);
            }
            if(state.wrapped[i]){
                // Each wrapped stored pallet keeps its own little glass
                // envelope, rather than globally wrapping the whole stack.
                cube(state.glass,state,poses,collector,-.025f,.03f,-.03f,
                        2.05f,.31f,.018f);
                cube(state.glass,state,poses,collector,-.025f,.03f,2.01f,
                        2.05f,.31f,.018f);
                cube(state.glass,state,poses,collector,-.03f,.03f,-.015f,
                        .018f,.31f,2.03f);
                cube(state.glass,state,poses,collector,2.01f,.03f,-.015f,
                        .018f,.31f,2.03f);
            }
            poses.popPose();
        }
    }
    @Override public boolean shouldRenderOffScreen(){return true;}
    @Override public int getViewDistance(){return 96;}
}
