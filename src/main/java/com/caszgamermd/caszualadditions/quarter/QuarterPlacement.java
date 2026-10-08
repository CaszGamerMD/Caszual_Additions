package com.caszgamermd.caszualadditions.quarter;

import com.casz.colorfulrods.RgbBuildingBlocks;
import com.casz.colorfulrods.RgbQuarterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class QuarterPlacement {
    private QuarterPlacement() {}

    public static InteractionResult place(UseOnContext context, BlockState source) {
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!placeContext.canPlace()) return InteractionResult.FAIL;

        Level level = context.getLevel();
        BlockPos pos = placeContext.getClickedPos();
        BlockState existing = level.getBlockState(pos);
        int index = cornerIndex(context.getClickLocation(), pos);

        if (existing.is(QuarterBlocks.QUARTER_BLOCK)) {
            if (existing.getValue(GenericQuarterBlock.CORNERS[index])) return InteractionResult.FAIL;
            return addToGeneric(context, pos, existing, index, source);
        }

        if (existing.getBlock() instanceof RgbQuarterBlock) {
            return migrateLegacyAndAdd(context, pos, existing, index, source);
        }

        BlockState placed = QuarterBlocks.QUARTER_BLOCK.defaultBlockState()
                .setValue(GenericQuarterBlock.CORNERS[index], true);

        if (!existing.canBeReplaced(placeContext)) return InteractionResult.FAIL;
        if (!level.isUnobstructed(
                placed,
                pos,
                CollisionContext.placementContext(context.getPlayer())
        )) {
            return InteractionResult.FAIL;
        }

        if (!level.setBlock(pos, placed, 11)) return InteractionResult.FAIL;

        if (level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity) {
            blockEntity.setMaterial(index, source);
        }

        finishPlacement(context, pos, placed);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult addToGeneric(
            UseOnContext context,
            BlockPos pos,
            BlockState state,
            int index,
            BlockState source
    ) {
        BlockState updated = state.setValue(GenericQuarterBlock.CORNERS[index], true);
        if (!context.getLevel().setBlock(pos, updated, 11)) return InteractionResult.FAIL;

        if (context.getLevel().getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity) {
            blockEntity.setMaterial(index, source);
        }

        finishPlacement(context, pos, updated);
        return InteractionResult.SUCCESS;
    }

    private static InteractionResult migrateLegacyAndAdd(
            UseOnContext context,
            BlockPos pos,
            BlockState legacy,
            int index,
            BlockState source
    ) {
        if (legacy.getValue(RgbQuarterBlock.CORNERS[index])) return InteractionResult.FAIL;

        BlockState migrated = QuarterBlocks.QUARTER_BLOCK.defaultBlockState();
        for (int i = 0; i < 8; i++) {
            if (legacy.getValue(RgbQuarterBlock.CORNERS[i])) {
                migrated = migrated.setValue(GenericQuarterBlock.CORNERS[i], true);
            }
        }
        migrated = migrated.setValue(GenericQuarterBlock.CORNERS[index], true);

        if (!context.getLevel().setBlock(pos, migrated, 11)) return InteractionResult.FAIL;

        if (context.getLevel().getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity) {
            BlockState rgb = RgbBuildingBlocks.RGB_BLOCK.defaultBlockState();
            for (int i = 0; i < 8; i++) {
                if (legacy.getValue(RgbQuarterBlock.CORNERS[i])) {
                    blockEntity.setMaterial(i, rgb);
                }
            }
            blockEntity.setMaterial(index, source);
        }

        finishPlacement(context, pos, migrated);
        return InteractionResult.SUCCESS;
    }

    private static int cornerIndex(Vec3 hit, BlockPos pos) {
        double x = Math.max(0.0, Math.min(0.999999, hit.x - pos.getX()));
        double y = Math.max(0.0, Math.min(0.999999, hit.y - pos.getY()));
        double z = Math.max(0.0, Math.min(0.999999, hit.z - pos.getZ()));
        return GenericQuarterBlock.cornerIndex(new Vec3(x, y, z));
    }

    private static void finishPlacement(UseOnContext context, BlockPos pos, BlockState state) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var sound = state.getSoundType();

        level.playSound(
                player,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F
        );
        level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(player, state));
        context.getItemInHand().consume(1, player);
    }
}
