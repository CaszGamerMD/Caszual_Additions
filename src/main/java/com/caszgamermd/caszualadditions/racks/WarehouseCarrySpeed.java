package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

/** Surface bonus for the ONE moving pallet carrying its attached Golem team.
 *  The bonus applies only if all four Golem-foot corners are supported by
 *  actual poured concrete (vanilla dyed concrete or industrial-grade).
 *  Powder, slabs and neighboring concrete never count as concrete flooring.
 */
public final class WarehouseCarrySpeed {
    /** Only loaded pallets get this multiplier; empty/pushing Golems don't. */
    public static final double CONCRETE_MULTIPLIER=1.35;
    private static final double CORNER=0.70;


    private WarehouseCarrySpeed(){}

    public static boolean isConcrete(Block block){
        if(block instanceof IndustrialGradeConcreteBlock)return true;
        // In Minecraft 26.2 the old Blocks.COLOR_CONCRETE fields are gone.
        // Registered vanilla IDs still have the exact suffix "_concrete".
        // Concrete powder (_concrete_powder) deliberately does not count.
        var id=BuiltInRegistries.BLOCK.getKey(block);
        return id!=null&&id.getNamespace().equals("minecraft")
                &&id.getPath().endsWith("_concrete");
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
