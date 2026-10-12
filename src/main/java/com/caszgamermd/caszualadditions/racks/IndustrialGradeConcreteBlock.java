package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Full-strength colored warehouse flooring. All colors conduct exactly
 * the same network signal through their six adjacent block faces. */
public final class IndustrialGradeConcreteBlock extends Block {
    private final MapCodec<IndustrialGradeConcreteBlock> codec=MapCodec.unit(this);
    public IndustrialGradeConcreteBlock(BlockBehaviour.Properties properties){
        super(properties);
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
}
