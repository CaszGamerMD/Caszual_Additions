package com.casz.colorfulrods;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RgbVerticalStairsBlock extends HorizontalDirectionalBlock {
    public enum Side implements StringRepresentable {
        LEFT, RIGHT;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final MapCodec<RgbVerticalStairsBlock> CODEC = simpleCodec(RgbVerticalStairsBlock::new);
    public static final EnumProperty<Side> SIDE = EnumProperty.create("side", Side.class);

    // These two shapes exactly match the two NORTH-facing JSON models.
    // Every other orientation is derived from these with the same Y rotation
    // used by the blockstate JSON so the visual model and hitbox cannot drift.
    private static final VoxelShape NORTH_LEFT = Shapes.or(
            box(0, 0, 0, 8, 16, 16),
            box(8, 0, 8, 16, 16, 16)
    );
    private static final VoxelShape NORTH_RIGHT = Shapes.or(
            box(8, 0, 0, 16, 16, 16),
            box(0, 0, 8, 8, 16, 16)
    );

    private static final VoxelShape EAST_LEFT = rotateY(NORTH_LEFT, 1);
    private static final VoxelShape EAST_RIGHT = rotateY(NORTH_RIGHT, 1);
    private static final VoxelShape SOUTH_LEFT = rotateY(NORTH_LEFT, 2);
    private static final VoxelShape SOUTH_RIGHT = rotateY(NORTH_RIGHT, 2);
    private static final VoxelShape WEST_LEFT = rotateY(NORTH_LEFT, 3);
    private static final VoxelShape WEST_RIGHT = rotateY(NORTH_RIGHT, 3);

    public RgbVerticalStairsBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(SIDE, Side.LEFT)
        );
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder
    ) {
        builder.add(FACING, SIDE);
    }

    private static Side sideFor(Direction facing, double x, double z) {
        return switch (facing) {
            case NORTH -> x < 0.5 ? Side.LEFT : Side.RIGHT;
            case SOUTH -> x > 0.5 ? Side.LEFT : Side.RIGHT;
            case EAST -> z < 0.5 ? Side.LEFT : Side.RIGHT;
            case WEST -> z > 0.5 ? Side.LEFT : Side.RIGHT;
            default -> Side.LEFT;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction facing = clickedFace.getAxis().isHorizontal()
                ? clickedFace.getOpposite()
                : context.getHorizontalDirection().getOpposite();

        var hit = context.getClickLocation();
        double x = hit.x - Math.floor(hit.x);
        double z = hit.z - Math.floor(hit.z);

        return defaultBlockState()
                .setValue(FACING, facing)
                .setValue(SIDE, sideFor(facing, x, z));
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        boolean left = state.getValue(SIDE) == Side.LEFT;
        return switch (state.getValue(FACING)) {
            case NORTH -> left ? NORTH_LEFT : NORTH_RIGHT;
            case EAST -> left ? EAST_LEFT : EAST_RIGHT;
            case SOUTH -> left ? SOUTH_LEFT : SOUTH_RIGHT;
            case WEST -> left ? WEST_LEFT : WEST_RIGHT;
            default -> NORTH_LEFT;
        };
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
