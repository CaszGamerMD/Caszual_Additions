package com.caszgamermd.caszualadditions.xp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class XpShowerBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING =
            EnumProperty.create("facing", Direction.class, direction -> direction.getAxis().isHorizontal());

    // Canonical shape is the NORTH JSON model. Other directions use the exact
    // same quarter-turn rotation as the blockstate model.
    private static final VoxelShape NORTH = Shapes.or(
            Block.box(6.5, 11, 0, 9.5, 13, 12),
            Block.box(6.5, 9, 9, 9.5, 12, 12),
            Block.box(4.5, 8, 7.5, 11.5, 9, 14.5)
    );
    private static final VoxelShape EAST = rotateY(NORTH, 1);
    private static final VoxelShape SOUTH = rotateY(NORTH, 2);
    private static final VoxelShape WEST = rotateY(NORTH, 3);

    public static final MapCodec<XpShowerBlock> CODEC = simpleCodec(XpShowerBlock::new);

    public XpShowerBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    private static VoxelShape shape(BlockState state) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case EAST -> EAST;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> NORTH;
        };
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shape(state);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        return shape(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        Direction support = face.getAxis().isHorizontal()
                ? face.getOpposite()
                : context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING, support);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new XpShowerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide()
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof XpShowerBlockEntity shower) {
            shower.toggle(serverPlayer);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {
        if (level.isClientSide()) return null;

        return type == XpBlockEntities.SHOWER
                ? (l, p, s, be) -> XpShowerBlockEntity.tick(
                        (net.minecraft.server.level.ServerLevel) l,
                        p,
                        (XpShowerBlockEntity) be
                )
                : null;
    }

    private static VoxelShape rotateY(VoxelShape source, int quarterTurns) {
        VoxelShape result = source;

        for (int turn = 0; turn < Math.floorMod(quarterTurns, 4); turn++) {
            VoxelShape rotated = Shapes.empty();

            for (AABB box : result.toAabbs()) {
                rotated = Shapes.or(
                        rotated,
                        Shapes.box(
                                1.0 - box.maxZ,
                                box.minY,
                                box.minX,
                                1.0 - box.minZ,
                                box.maxY,
                                box.maxX
                        )
                );
            }

            result = rotated;
        }

        return result;
    }
}
