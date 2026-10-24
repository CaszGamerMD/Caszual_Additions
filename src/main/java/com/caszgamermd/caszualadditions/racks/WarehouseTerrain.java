package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;

/**
 * Exact collision-floor height for walking robots and loaded pallet teams.
 * Bottom slabs are half a block ABOVE their own Y origin. At a bottom slab
 * the Golem's blockPosition is the slab block, not the AIR block above it.
 * Treating its y-coordinate as feet height causes a half-block waypoint
 * error, repeated "stalls", and unnecessary jumps when walking over slabs.
 *
 * Never infer half-steps from pallets, racks, water or other thin collision
 * objects: only genuine bottom slabs receive the smooth-step exception.
 */
public final class WarehouseTerrain {
    private WarehouseTerrain(){}

    public static boolean bottomSlab(ServerLevel level,BlockPos feet){
        BlockState state=level.getBlockState(feet);
        return state.getBlock() instanceof SlabBlock
                &&state.getValue(SlabBlock.TYPE)==SlabType.BOTTOM
                &&level.getFluidState(feet).isEmpty();
    }

    /** A stair's center-path node can be inside the stair block itself.
     * Like a bottom slab, this is an actual collision floor rather than a
     * full-block obstacle. Pallets and thin warehouse structures are NEVER
     * treated as architectural steps.
     */
    public static boolean stairFloor(ServerLevel level,BlockPos feet){
        BlockState state=level.getBlockState(feet);
        return state.getBlock() instanceof StairBlock
                &&level.getFluidState(feet).isEmpty();
    }
    public static double standingY(ServerLevel level,BlockPos feet){
        if(bottomSlab(level,feet))return feet.getY()+.5;
        if(stairFloor(level,feet)){
            var shape=level.getBlockState(feet).getCollisionShape(level,feet);
            return feet.getY()+(shape.isEmpty()?0:
                    shape.max(Direction.Axis.Y));
        }
        return feet.getY();
    }

    public static boolean smoothHalfStep(ServerLevel level,BlockPos from,
            BlockPos to){
        if(Math.abs(from.getX()-to.getX())
                +Math.abs(from.getZ()-to.getZ())!=1)return false;
        return (bottomSlab(level,from)||bottomSlab(level,to)
                    ||stairFloor(level,from)||stairFloor(level,to))
                &&Math.abs(standingY(level,to)-standingY(level,from))<=.501;
    }

    public static boolean safeRise(ServerLevel level,BlockPos from,
            BlockPos to){
        return standingY(level,to)-standingY(level,from)<=1.001
                &&standingY(level,to)-standingY(level,from)>=-1.001;
    }

    /** A two-block-wide pallet can transition across adjacent half slabs.
     * Evaluate each support cell at ITS collision surface height, rather
     * than assuming every footprint block has identical integer Y. */
    public static boolean wideFloor(ServerLevel level,BlockPos center){
        if(!StorageGolemReturnPath.walkable(level,center))return false;
        double centerY=standingY(level,center);
        for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            BlockPos cell=center.offset(x,0,z);
            boolean supported=false;
            for(int dy:new int[]{0,-1,1}){
                BlockPos trial=cell.offset(0,dy,0);
                if(Math.abs(standingY(level,trial)-centerY)>.501)continue;
                if(StorageGolemReturnPath.walkable(level,trial)){
                    supported=true;break;
                }
            }
            if(!supported)return false;
        }
        return true;
    }
}
