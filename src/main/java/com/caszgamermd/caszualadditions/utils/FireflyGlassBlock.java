package com.caszgamermd.caszualadditions.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class FireflyGlassBlock extends Block {
    public static final BooleanProperty CONTAINS_BUSH = BooleanProperty.create("contains_bush");
    private static final VoxelShape SHELL = Shapes.or(
        Block.box(0, 0, 0, 1, 15, 16), Block.box(15, 0, 0, 16, 15, 16),
        Block.box(1, 0, 0, 15, 15, 1), Block.box(1, 0, 15, 15, 15, 16),
        Block.box(0, 15, 0, 16, 16, 16));
    private final DyeColor color;
    public FireflyGlassBlock(Properties properties, DyeColor color) {
        super(properties);
        this.color = color;
        registerDefaultState(stateDefinition.any().setValue(CONTAINS_BUSH, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CONTAINS_BUSH); }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CONTAINS_BUSH)) Blocks.FIREFLY_BUSH.animateTick(state, level, pos, random);
    }
    public DyeColor color() { return color; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHELL; }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHELL; }
    @Override protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
    @Override protected boolean propagatesSkylightDown(BlockState state) { return true; }
}