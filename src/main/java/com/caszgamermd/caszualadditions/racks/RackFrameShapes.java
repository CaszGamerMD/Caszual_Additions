package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rack collision generated directly from the 12 visible metal-frame model
 * segments, including the complete bottom pallet grill. No invisible solid
 * blocks in the upper tiers or in the open grill. Index is part=0..11;
 * the horizontal rotation matches pallet_rack.json blockstates.
 */
final class RackFrameShapes {
    private RackFrameShapes(){}
    // (from x,y,z, to x,y,z) in Minecraft model pixel units.
    private static final double[][][] ELEMENTS = new double[][][]{
        {{15, 0, 0, 16, 16, 1}, {0, 0, 0, 15, 1, 1}, {15, 0, 1, 16, 1, 16}, {15, 15, 1, 16, 16, 16}, {3, 1, 0, 4, 2, 16}, {8, 1, 0, 9, 2, 16}, {13, 1, 0, 14, 2, 16}, {0, 1, 7.5, 16, 2, 8.5}, {0, 1, 15, 16, 2, 16}},
        {{0, 0, 0, 1, 16, 1}, {1, 0, 0, 16, 1, 1}, {0, 0, 1, 1, 1, 16}, {0, 15, 1, 1, 16, 16}, {3, 1, 0, 4, 2, 16}, {8, 1, 0, 9, 2, 16}, {13, 1, 0, 14, 2, 16}, {0, 1, 7.5, 16, 2, 8.5}, {0, 1, 15, 16, 2, 16}},
        {{15, 0, 15, 16, 16, 16}, {0, 0, 15, 15, 1, 16}, {15, 0, 0, 16, 1, 15}, {0, 15, 15, 15, 16, 16}, {15, 15, 0, 16, 16, 15}, {3, 1, 0, 4, 2, 16}, {8, 1, 0, 9, 2, 16}, {13, 1, 0, 14, 2, 16}, {0, 1, 7.5, 16, 2, 8.5}, {0, 1, 15, 16, 2, 16}},
        {{0, 0, 15, 1, 16, 16}, {1, 0, 15, 16, 1, 16}, {0, 0, 0, 1, 1, 15}, {1, 15, 15, 16, 16, 16}, {0, 15, 0, 1, 16, 15}, {3, 1, 0, 4, 2, 16}, {8, 1, 0, 9, 2, 16}, {13, 1, 0, 14, 2, 16}, {0, 1, 7.5, 16, 2, 8.5}, {0, 1, 15, 16, 2, 16}},
        {{15, 0, 0, 16, 16, 1}, {15, 0, 1, 16, 1, 16}, {15, 15, 1, 16, 16, 16}},
        {{0, 0, 0, 1, 16, 1}, {0, 0, 1, 1, 1, 16}, {0, 15, 1, 1, 16, 16}},
        {{15, 0, 15, 16, 16, 16}, {0, 0, 15, 15, 1, 16}, {15, 0, 0, 16, 1, 15}, {0, 15, 15, 15, 16, 16}, {15, 15, 0, 16, 16, 15}},
        {{0, 0, 15, 1, 16, 16}, {1, 0, 15, 16, 1, 16}, {0, 0, 0, 1, 1, 15}, {1, 15, 15, 16, 16, 16}, {0, 15, 0, 1, 16, 15}},
        {{15, 0, 0, 16, 16, 1}, {15, 0, 1, 16, 1, 16}, {0, 15, 0, 15, 16, 1}, {15, 15, 1, 16, 16, 16}},
        {{0, 0, 0, 1, 16, 1}, {0, 0, 1, 1, 1, 16}, {1, 15, 0, 16, 16, 1}, {0, 15, 1, 1, 16, 16}},
        {{15, 0, 15, 16, 16, 16}, {0, 0, 15, 15, 1, 16}, {15, 0, 0, 16, 1, 15}, {0, 15, 15, 15, 16, 16}, {15, 15, 0, 16, 16, 15}},
        {{0, 0, 15, 1, 16, 16}, {1, 0, 15, 16, 1, 16}, {0, 0, 0, 1, 1, 15}, {1, 15, 15, 16, 16, 16}, {0, 15, 0, 1, 16, 15}}
    };
    private static final VoxelShape[][] CELLS=new VoxelShape[4][12];
    static {
        for(int orientation=0;orientation<4;orientation++)
            for(int part=0;part<12;part++){
                VoxelShape combined=Shapes.empty();
                for(double[] cube:ELEMENTS[part]){
                    double x1=cube[0],y1=cube[1],z1=cube[2],
                           x2=cube[3],y2=cube[4],z2=cube[5];
                    double a,b,c,d;
                    switch(orientation){
                        case 1 -> {a=16-z2;b=x1;c=16-z1;d=x2;}
                        case 2 -> {a=16-x2;b=16-z2;c=16-x1;d=16-z1;}
                        case 3 -> {a=z1;b=16-x2;c=z2;d=16-x1;}
                        default -> {a=x1;b=z1;c=x2;d=z2;}
                    }
                    combined=Shapes.or(combined,Block.box(a,y1,b,c,y2,d));
                }
                CELLS[orientation][part]=combined.optimize();
            }
    }
    static VoxelShape cell(BlockState state){
        Direction facing=state.getValue(RackBlock.FACING);
        int rotation=switch(facing){
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
        return CELLS[rotation][state.getValue(RackBlock.PART)];
    }
}
