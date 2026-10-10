package com.caszgamermd.caszualadditions.funbarrel;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A creative-mode exclusive sampler, with no recipe or survival drops. */
public final class FunBarrelBlock extends Block implements EntityBlock {
    public static final MapCodec<FunBarrelBlock> CODEC = simpleCodec(FunBarrelBlock::new);

    public FunBarrelBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Player player = context.getPlayer();
        return player != null && player.getAbilities().instabuild
                ? defaultBlockState() : null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FunBarrelBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FunBarrelBlockEntity barrel) {
            barrel.refreshCatalog();
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (!player.getAbilities().instabuild) return InteractionResult.FAIL;
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof FunBarrelBlockEntity barrel) {
            // Newly installed Caszual mods extend the barrel without refilling
            // anything the player deliberately removed from its inventory.
            barrel.refreshCatalog();
            serverPlayer.openMenu(barrel);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        // Creative can pick a fresh barrel from the creative inventory at any time.
        return List.of();
    }
}
