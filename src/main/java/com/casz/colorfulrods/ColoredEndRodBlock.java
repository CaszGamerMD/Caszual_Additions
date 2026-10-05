package com.casz.colorfulrods;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
public final class ColoredEndRodBlock extends EndRodBlock {
    public static final MapCodec<EndRodBlock> CODEC = simpleCodec(ColoredEndRodBlock::new);
    public ColoredEndRodBlock(BlockBehaviour.Properties properties) { super(properties); }
    @Override public MapCodec<EndRodBlock> codec() { return CODEC; }
}
