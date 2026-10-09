package com.caszgamermd.caszualadditions.headvending;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One block wide, one deep and two blocks tall. Both halves open the same
 * server-validated vending UI, and an orphan half removes itself.
 */
public final class HeadVendingBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<HeadVendingBlock> CODEC =
            simpleCodec(HeadVendingBlock::new);
    public static final EnumProperty<DoubleBlockHalf> HALF =
            EnumProperty.create("half", DoubleBlockHalf.class);

    public HeadVendingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos top = context.getClickedPos().above();
        if (context.getLevel().isOutsideBuildHeight(top)
                || !context.getLevel().getBlockState(top).canBeReplaced()) {
            return null;
        }
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
        }
    }

    public static BlockPos basePos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    public static boolean isComplete(LevelReader level, BlockPos clicked, BlockState clickedState) {
        if (!(clickedState.getBlock() instanceof HeadVendingBlock)) return false;
        BlockPos base = basePos(clicked, clickedState);
        BlockState lower = level.getBlockState(base);
        BlockState upper = level.getBlockState(base.above());
        return lower.is(clickedState.getBlock())
                && upper.is(clickedState.getBlock())
                && lower.getValue(HALF) == DoubleBlockHalf.LOWER
                && upper.getValue(HALF) == DoubleBlockHalf.UPPER
                && lower.getValue(FACING) == upper.getValue(FACING);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if ((half == DoubleBlockHalf.LOWER && direction == Direction.UP)
                || (half == DoubleBlockHalf.UPPER && direction == Direction.DOWN)) {
            if (!neighborState.is(this)
                    || neighborState.getValue(HALF) == half
                    || neighborState.getValue(FACING) != state.getValue(FACING)) {
                return Blocks.AIR.defaultBlockState();
            }
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && isComplete(level, pos, state)) {
            DoubleBlockHalf half = state.getValue(HALF);
            BlockPos other = half == DoubleBlockHalf.UPPER ? pos.below() : pos.above();

            // The lower half owns the only loot-table drop. Breaking the
            // upper half removes the lower half without loot, so create the
            // single machine item explicitly in survival.
            if (half == DoubleBlockHalf.UPPER && !player.getAbilities().instabuild) {
                popResource(level, pos, new ItemStack(this));
            }
            level.removeBlock(other, false);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && isComplete(level, pos, state)) {
            ServerPlayNetworking.send(serverPlayer,
                    new HeadVendingContent.Open(basePos(pos, state)));
        }
        return InteractionResult.SUCCESS;
    }
}
