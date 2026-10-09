package com.caszgamermd.caszualadditions.plushie;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<PlayerPlushieBlock> CODEC = simpleCodec(PlayerPlushieBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 4, 13, 14, 12);
    private static final VoxelShape SITTING_SHAPE = Block.box(2, 0, 3, 14, 11, 13);
    private static final VoxelShape SLEEPING_SHAPE = Block.box(1, 0, 1, 15, 5, 15);

    public PlayerPlushieBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (level.getBlockEntity(pos) instanceof PlayerPlushieBlockEntity plushie) {
            if (plushie.pose() == 9) return SLEEPING_SHAPE;
            if (plushie.pose() == 1) return SITTING_SHAPE;
        }
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PlayerPlushieBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof PlayerPlushieBlockEntity plushie) {
            plushie.setProfile(stack.get(DataComponents.PROFILE));
            plushie.setPose(PlayerPlushies.pose(stack));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        // Only an entirely empty-handed interaction changes poses; using
        // items on the plushie must not unexpectedly cycle its appearance.
        if (!player.getMainHandItem().isEmpty() || !player.getOffhandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof PlayerPlushieBlockEntity plushie) {
            plushie.setPose(plushie.pose() + 1);
            serverPlayer.sendOverlayMessage(net.minecraft.network.chat.Component.literal(
                    "Plushie: " + PlayerPlushieBlockEntity.POSES[plushie.pose()]));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PlayerPlushieBlockEntity plushie) {
            return List.of(PlayerPlushies.createStack(plushie.profile(), plushie.pose()));
        }
        return List.of(new ItemStack(PlayerPlushies.PLAYER_PLUSHIE_ITEM));
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        if (level.getBlockEntity(pos) instanceof PlayerPlushieBlockEntity plushie) {
            return PlayerPlushies.createStack(plushie.profile());
        }
        return new ItemStack(PlayerPlushies.PLAYER_PLUSHIE_ITEM);
    }
}
