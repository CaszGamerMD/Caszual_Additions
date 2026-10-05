package dev.casz.utils;

import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public final class FireflyGlassItem extends BlockItem {
    public FireflyGlassItem(Block block, Properties properties) { super(block, properties); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.FIREFLY_BUSH)) return super.useOn(context);
        var player = context.getPlayer();
        var stack = context.getItemInHand();
        if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), stack))) return InteractionResult.FAIL;
        if (!level.isClientSide()) {
            var covered = getBlock().defaultBlockState().setValue(FireflyGlassBlock.CONTAINS_BUSH, true);
            if (!level.setBlock(pos, covered, Block.UPDATE_ALL)) return InteractionResult.FAIL;
            level.playSound(null, pos, SoundEvents.GLASS_PLACE, SoundSource.BLOCKS, 1f, 1f);
            if (player instanceof ServerPlayer serverPlayer) CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
            if (player == null || !player.getAbilities().instabuild) stack.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}