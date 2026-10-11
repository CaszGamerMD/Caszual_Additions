package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletKind;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
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
    private final Font font;
    public RackBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        blocks=context.blockModelResolver();
        items=context.itemModelResolver();
        font=context.font();
    }
    @Override public RackRenderState createRenderState(){return new RackRenderState();}

    @Override public void extractRenderState(RackBlockEntity rack,RackRenderState state,
            float partial,Vec3 camera,ModelFeatureRenderer.@Nullable CrumblingOverlay cracks){
        BlockEntityRenderer.super.extractRenderState(rack,state,partial,camera,cracks);
        state.facing=rack.facing();
        var world=rack.getLevel();
        String aisle=world==null?"":RackNetwork.aisle(world,rack);
        state.rackLabel=rack.hasRackPlate()
                ?(aisle.isBlank()||rack.rackName().isBlank()
                    ?rack.displayName():aisle+"-"+rack.rackName()):"";
        state.aisleLabel=aisle;
        // The aisle sign belongs at the outer ends of the whole row,
        // not over each rack's front. A new bay hides the old interior
        // end plate and moves the label to the newly exposed end.
        for(Direction side:Direction.values())state.aisleEndFaces[side.get3DDataValue()]=false;
        if(world!=null&&!aisle.isBlank()){
            for(Direction side:new Direction[]{
                    rack.facing().getClockWise(),rack.facing().getCounterClockWise()}){
                BlockPos neighboring=rack.getBlockPos().relative(side,2);
                RackBlockEntity adjacent=world.hasChunkAt(neighboring)
                        ?RackBlock.controller(world,neighboring):null;
                state.aisleEndFaces[side.get3DDataValue()]=
                        adjacent==null||!RackNetwork.aisleNeighbors(world,rack,adjacent);
            }
        }
        blocks.update(state.crossBeam,Blocks.IRON_BLOCK.defaultBlockState(),DISPLAY);
        blocks.update(state.nameplate,Blocks.POLISHED_DEEPSLATE.defaultBlockState(),DISPLAY);
        for(Direction side:Direction.values()){
            boolean connected=false;
            // Only a pair of connectors across an ACTUAL shared rack face
            // produces a cross brace. A single connector is not a network link.
            if(rack.hasConnector(side) && rack.getLevel()!=null){
                for(int i=0;i<12&&!connected;i++){
                    BlockPos current=RackBlock.cell(rack.getBlockPos(),rack.facing(),i);
                    BlockPos adjacent=current.relative(side);
                    if(!rack.getLevel().hasChunkAt(adjacent))continue;
                    RackBlockEntity other=RackBlock.controller(rack.getLevel(),adjacent);
                    if(other!=null && !other.getBlockPos().equals(rack.getBlockPos())
                            && other.hasConnector(side.getOpposite()))
                        connected=true;
                }
            }
            // Draw the shared brace only on its positive-axis owner to
            // prevent two touching rack renderers creating coplanar beams.
            state.linkedFaces[side.get3DDataValue()]=connected
                    && (side==Direction.EAST || side==Direction.SOUTH || side==Direction.UP);
        }
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
        submitRackAccessories(state,poses,collector);
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

    /** Metal X-braces, front rack ID, and aisle signs on the exposed row ends. */
    private void submitRackAccessories(RackRenderState state,PoseStack poses,
            SubmitNodeCollector collector) {
        Direction front=state.facing;
        Direction right=front.getCounterClockWise();
        Direction back=front.getOpposite();
        float x=(float)Math.min(0,Math.min(right.getStepX(),
                Math.min(back.getStepX(),right.getStepX()+back.getStepX())));
        float z=(float)Math.min(0,Math.min(right.getStepZ(),
                Math.min(back.getStepZ(),right.getStepZ()+back.getStepZ())));

        for(Direction side:Direction.values()){
            if(!state.linkedFaces[side.get3DDataValue()])continue;
            poses.pushPose();
            if(side==Direction.UP){
                // Vertical connectors join the matching upper/lower frame
                // edges without occupying any pallet cargo cells.
                poses.translate(x,0,z);
                for(float dx:new float[]{.10f,1.84f})
                    for(float dz:new float[]{.10f,1.84f})
                        board(state.crossBeam,state,poses,collector,dx,2.77f,dz,
                                .06f,.46f,.06f);
            }else{
                boolean alongZ=side==Direction.EAST;
                double cx=alongZ ? x+2.0 : x+1.0;
                double cz=alongZ ? z+1.0 : z+2.0;
                poses.translate(cx,1.48,cz);
                if(alongZ)poses.mulPose(Axis.YP.rotationDegrees(90));
                // Two braces in an X on the *actual* shared boundary.
                for(float degrees:new float[]{-31f,31f}){
                    poses.pushPose();
                    poses.mulPose(Axis.ZP.rotationDegrees(degrees));
                    board(state.crossBeam,state,poses,collector,
                            -.97f,-.047f,-.038f,1.94f,.094f,.076f);
                    poses.popPose();
                }
            }
            poses.popPose();
        }

        if(!state.rackLabel.isBlank())
            submitFacingPlate(state,poses,collector,state.rackLabel,
                    front,x,z,2.31f,0xffe4f0f5);
        // Label both accessible ends of the aisle. Interior faces are
        // automatically suppressed, even when the Aisle Tag was attached
        // to a rack in the middle of the row.
        if(!state.aisleLabel.isBlank()){
            for(Direction side:new Direction[]{front.getClockWise(),
                    front.getCounterClockWise()}){
                if(state.aisleEndFaces[side.get3DDataValue()])
                    submitFacingPlate(state,poses,collector,state.aisleLabel,
                            side,x,z,2.61f,0xffffde9b);
            }
        }
    }

    private void submitFacingPlate(RackRenderState state,PoseStack poses,
            SubmitNodeCollector collector,String label,Direction face,
            float x,float z,float y,int color) {
        poses.pushPose();
        poses.translate(x+1.0,0,z+1.0);
        float angle=switch(face){
            case NORTH -> 0f;
            case EAST -> -90f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        };
        poses.mulPose(Axis.YP.rotationDegrees(angle));
        poses.translate(-1.0,0,-1.0);
        submitPlate(state,poses,collector,label,y,color);
        poses.popPose();
    }

    private void submitPlate(RackRenderState state,PoseStack poses,
            SubmitNodeCollector collector,String label,float y,int color) {
        // Plate face projects only 0.06 blocks into the empty access aisle;
        // offset text farther out by 0.003 to avoid Z-fighting with its metal.
        board(state.nameplate,state,poses,collector,
                .20f,y,-.060f,1.60f,.255f,.055f);
        String visible=label;
        while(visible.length()>1 && font.width(visible)>113)
            visible=visible.substring(0,visible.length()-1);
        if(!visible.equals(label)&&visible.length()>1)
            visible=visible.substring(0,visible.length()-1)+"…";
        poses.pushPose();
        poses.translate(1.0f,y+.13f,-.066f);
        poses.scale(-.013f,-.013f,.013f);
        collector.submitText(poses,-font.width(visible)/2f,-4.0f,
                Component.literal(visible).getVisualOrderText(),false,
                Font.DisplayMode.POLYGON_OFFSET,state.lightCoords,color,0,0);
        poses.popPose();
    }

    private static void board(BlockModelRenderState model,RackRenderState state,
            PoseStack poses,SubmitNodeCollector collector,
            float x,float y,float z,float width,float height,float depth){
        poses.pushPose();poses.translate(x,y,z);poses.scale(width,height,depth);
        model.submit(poses,collector,state.lightCoords,OverlayTexture.NO_OVERLAY,0);
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
