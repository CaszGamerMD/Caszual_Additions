package com.caszgamermd.caszualadditions.xp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public final class XpTankBlock extends Block implements EntityBlock {
    public static final MapCodec<XpTankBlock> CODEC = simpleCodec(XpTankBlock::new);

    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 10);
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty UP = BooleanProperty.create("up");
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    public XpTankBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FILL, 0)
                        .setValue(NORTH, false)
                        .setValue(SOUTH, false)
                        .setValue(WEST, false)
                        .setValue(EAST, false)
                        .setValue(UP, false)
                        .setValue(DOWN, false)
        );
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL, NORTH, SOUTH, WEST, EAST, UP, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connections(defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            LevelReader level,
            ScheduledTickAccess ticks,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            RandomSource random
    ) {
        return state.setValue(property(direction), connects(level, neighborPos));
    }

    private static BooleanProperty property(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
            case UP -> UP;
            case DOWN -> DOWN;
        };
    }

    private static BlockState connections(BlockState state, BlockGetter level, BlockPos pos) {
        return state
                .setValue(NORTH, connects(level, pos.north()))
                .setValue(SOUTH, connects(level, pos.south()))
                .setValue(WEST, connects(level, pos.west()))
                .setValue(EAST, connects(level, pos.east()))
                .setValue(UP, connects(level, pos.above()))
                .setValue(DOWN, connects(level, pos.below()));
    }

    private static boolean connects(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof XpTankBlock;
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston
    ) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        XpNetwork.refreshVisuals(level, pos);
    }

    @Override
    protected void affectNeighborsAfterRemoval(
            BlockState state,
            ServerLevel level,
            BlockPos pos,
            boolean movedByPiston
    ) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);

        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.getBlockState(neighbor).getBlock() instanceof XpTankBlock) {
                XpNetwork.refreshVisuals(level, neighbor);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpTankBlockEntity(pos, state);
    }
}
