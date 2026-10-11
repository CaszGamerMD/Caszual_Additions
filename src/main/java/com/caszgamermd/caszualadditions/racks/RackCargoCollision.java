package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The rack renders up to 27 cargo cells atop its inserted pallet. Give each
 * filled cube a matching collision shape, clipped to the rack's individual
 * world block cell. No extra invisible block proxies or chunk loads needed.
 */
public final class RackCargoCollision {
    private static final double CELL=2.0/3.0;
    private static final double DECK_TOP=5.0/16.0;

    private RackCargoCollision(){}

    public static VoxelShape shape(BlockGetter level,BlockPos pos,BlockState state) {
        if(!(state.getBlock() instanceof RackBlock))return Shapes.empty();
        BlockPos root=RackBlock.root(pos,state);
        if(!(level.getBlockEntity(root) instanceof RackBlockEntity rack)
                || !rack.hasPallet())return Shapes.empty();

        int visible=0;
        for(int i=0;i<rack.getContainerSize();i++)
            if(!rack.getItem(i).isEmpty() && ++visible==27)break;
        if(visible==0)return Shapes.empty();

        Direction facing=state.getValue(RackBlock.FACING);
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        for(int part=0;part<4;part++){
            BlockPos cell=RackBlock.cell(root,facing,part);
            minX=Math.min(minX,cell.getX());
            minZ=Math.min(minZ,cell.getZ());
        }

        VoxelShape combined=Shapes.empty();
        for(int i=0;i<visible;i++){
            int col=i%3,row=(i/3)%3,layer=i/9;
            double x0=minX+col*CELL,x1=x0+CELL;
            double y0=root.getY()+DECK_TOP+layer*CELL,y1=y0+CELL;
            double z0=minZ+row*CELL,z1=z0+CELL;
            double left=Math.max(pos.getX(),x0),right=Math.min(pos.getX()+1,x1);
            double bottom=Math.max(pos.getY(),y0),top=Math.min(pos.getY()+1,y1);
            double front=Math.max(pos.getZ(),z0),back=Math.min(pos.getZ()+1,z1);
            if(right-left<=1e-5||top-bottom<=1e-5||back-front<=1e-5)continue;
            combined=Shapes.or(combined,Block.box(
                    (left-pos.getX())*16,(bottom-pos.getY())*16,(front-pos.getZ())*16,
                    (right-pos.getX())*16,(top-pos.getY())*16,(back-pos.getZ())*16));
        }
        return combined;
    }
}
