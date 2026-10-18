package com.caszgamermd.caszualadditions.racks;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Surface bonus for the ONE moving pallet carrying its attached Golem team.
 *  The bonus applies only if all four Golem-foot corners are supported by
 *  actual poured concrete (vanilla dyed concrete or industrial-grade).
 *  Powder, slabs and neighboring concrete never count as concrete flooring.
 */
public final class WarehouseCarrySpeed {
    /** Only loaded pallets get this multiplier; empty/pushing Golems don't. */
    public static final double CONCRETE_MULTIPLIER=1.35;
    private static final double CORNER=0.70;
    private static final Set<Block> CONCRETE=Set.of(
            Blocks.WHITE_CONCRETE,Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE,Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE,Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE,Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE,Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE,Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE,Blocks.BLACK_CONCRETE);

    private WarehouseCarrySpeed(){}

    public static boolean isConcrete(Block block){
        return CONCRETE.contains(block)||block instanceof IndustrialGradeConcreteBlock;
    }

    /** Sample all four physical Golem positions, not only the pallet center.
     *  The carrier's Y is the actual standing surface; one eighth block below
     *  samples its support, rather than the air/structure above the feet.
     */
    public static boolean onConcrete(ServerLevel level,double x,double feetY,double z){
        for(double dx:new double[]{-CORNER,CORNER})
            for(double dz:new double[]{-CORNER,CORNER}){
                BlockPos floor=BlockPos.containing(x+dx,feetY-.125,z+dz);
                if(!level.hasChunkAt(floor)
                        ||!isConcrete(level.getBlockState(floor).getBlock()))
                    return false;
            }
        return true;
    }
}
