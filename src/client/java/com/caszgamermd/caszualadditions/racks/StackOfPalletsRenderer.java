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
 * ONE full-width 2x2 pallet per tier, stacked 12 decks high (3.75 blocks).
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
        blocks.update(state.floorGrill,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        int displaySlot=0;
        for(int slot=0;slot<12;slot++){
            ItemStack palletItem=magazine.getItem(slot);
            if(!StackOfPalletsItem.isPallet(palletItem))continue;
            // Inventory slots may have gaps after GUI transfers. Display
            // contents are COMPACT from bottom to top, so all full-size
            // decks really support each other and their collision matches.
            int index=displaySlot++;
            state.occupied[index]=true;
            state.wrapped[index]=GlassPalletWrap.wrapped(palletItem);
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
                blocks.update(state.boards[index][i],deck[i].defaultBlockState(),DISPLAY);
                state.boards[index][i].tintLayers().clear();
                if(plastic)
                    state.boards[index][i].tintLayers().add(
                            PalletData.plasticColor(palletItem));
            }
        }
        for(int i=displaySlot;i<12;i++){
            state.occupied[i]=false;
            state.wrapped[i]=false;
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
        // A thin 2x2 steel floor grate remains visible EVEN when the
        // dispenser holds zero pallets. It is a physical 1px base, and
        // the lattice is raised only ~.01 blocks above the dark frame.
        var grille=state.floorGrill;
        for(float x:new float[]{.015f,1.93f})
            cube(grille,state,poses,collector,x,.008f,.015f,.055f,.052f,1.97f);
        for(float z:new float[]{.015f,1.93f})
            cube(grille,state,poses,collector,.015f,.008f,z,1.97f,.052f,.055f);
        for(int line=1;line<8;line++){
            float at=line*.25f;
            cube(grille,state,poses,collector,at-.018f,.012f,.07f,
                    .036f,.027f,1.86f);
            cube(grille,state,poses,collector,.07f,.012f,at-.018f,
                    1.86f,.027f,.036f);
        }
        for(int i=0;i<12;i++){
            if(!state.occupied[i])continue;
            // Each individual real pallet fills the ENTIRE 2x2 footprint.
            // PalletBlock collision is 5 pixels high (5/16 blocks): the
            // 12th deck sits 3.4375 blocks up and finishes at y=3.75.
            float py=i*(StackOfPalletsBlock.PALLET_THICKNESS_PIXELS/16f);
            poses.pushPose();
            poses.translate(0f,py,0f);
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
