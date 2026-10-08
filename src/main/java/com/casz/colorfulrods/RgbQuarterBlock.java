package com.casz.colorfulrods;

import com.caszgamermd.caszualadditions.quarter.QuarterBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbQuarterBlock extends Block {
    public static final MapCodec<RgbQuarterBlock> CODEC = simpleCodec(RgbQuarterBlock::new);
    public static final BooleanProperty NWD = BooleanProperty.create("nwd");
    public static final BooleanProperty NED = BooleanProperty.create("ned");
    public static final BooleanProperty SWD = BooleanProperty.create("swd");
    public static final BooleanProperty SED = BooleanProperty.create("sed");
    public static final BooleanProperty NWU = BooleanProperty.create("nwu");
    public static final BooleanProperty NEU = BooleanProperty.create("neu");
    public static final BooleanProperty SWU = BooleanProperty.create("swu");
    public static final BooleanProperty SEU = BooleanProperty.create("seu");
    public static final BooleanProperty[] CORNERS = {NWD, NED, SWD, SED, NWU, NEU, SWU, SEU};

    public RgbQuarterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CORNERS) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NWD, NED, SWD, SED, NWU, NEU, SWU, SEU);
    }

    private static Vec3 relative(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();
        return new Vec3(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
    }

    private static BooleanProperty corner(Vec3 relative) {
        boolean east = relative.x >= 0.5;
        boolean up = relative.y >= 0.5;
        boolean south = relative.z >= 0.5;
        if (!up && !south) return east ? NED : NWD;
        if (!up) return east ? SED : SWD;
        if (!south) return east ? NEU : NWU;
        return east ? SEU : SWU;
    }

    private static boolean allCorners(BlockState state) {
        for (BooleanProperty property : CORNERS) {
            if (!state.getValue(property)) return false;
        }
        return true;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!QuarterBlocks.isQuarterPiece(context.getItemInHand()) || allCorners(state)) return false;
        return !state.getValue(corner(relative(context)));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        BooleanProperty target = corner(relative(context));
        if (existing.is(this)) {
            return existing.getValue(target) ? null : existing.setValue(target, true);
        }
        return defaultBlockState().setValue(target, true);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(NWD)) shape = Shapes.or(shape, box(0, 0, 0, 8, 8, 8));
        if (state.getValue(NED)) shape = Shapes.or(shape, box(8, 0, 0, 16, 8, 8));
        if (state.getValue(SWD)) shape = Shapes.or(shape, box(0, 0, 8, 8, 8, 16));
        if (state.getValue(SED)) shape = Shapes.or(shape, box(8, 0, 8, 16, 8, 16));
        if (state.getValue(NWU)) shape = Shapes.or(shape, box(0, 8, 0, 8, 16, 8));
        if (state.getValue(NEU)) shape = Shapes.or(shape, box(8, 8, 0, 16, 16, 8));
        if (state.getValue(SWU)) shape = Shapes.or(shape, box(0, 8, 8, 8, 16, 16));
        if (state.getValue(SEU)) shape = Shapes.or(shape, box(8, 8, 8, 16, 16, 16));
        return shape;
    }
}
