package com.caszgamermd.caszualadditions.quarter;

import com.caszgamermd.caszualadditions.rods.RgbBuildingBlocks;
import com.caszgamermd.caszualadditions.rods.RgbQuarterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class QuarterPlacement {
    private static final double TARGET_EPSILON = 0.002;

    private QuarterPlacement() {}

    /**
     * Face-accurate placement. The hit face, not the nearest corner to the
     * player, determines which half-block cell receives the piece:
     *
     * - Clicking an INTERIOR face of an existing quarter adds the quarter
     *   directly beyond the cursor inside that same block space.
     * - Clicking an OUTER face creates/extends the neighboring block space.
     * - Sneaking while clicking a quarter forces the neighboring block
     *   space even when the clicked face is inside the partial block.
     *
     * Never search nearby containers before honoring the actual raycast hit,
     * and never substitute the "nearest empty" corner when the selected
     * position is occupied. Either the requested cell is free or placement
     * is rejected. This also keeps placement deterministic in multiplayer.
     */
    public static InteractionResult place(UseOnContext context, BlockState source) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        Vec3 hit = context.getClickLocation();

        // Step just beyond the SURFACE at the crosshair. An internal 0.5
        // face stays within the 1x1x1 container; an external 0/1 face
        // crosses into the next world block along the clicked face.
        Vec3 beyond = hit.add(
                face.getStepX() * TARGET_EPSILON,
                face.getStepY() * TARGET_EPSILON,
                face.getStepZ() * TARGET_EPSILON
        );
        BlockState clickedState = level.getBlockState(clicked);
        if (isQuarterContainer(clickedState)) {
            Player player = context.getPlayer();
            BlockPos destination = player != null && player.isShiftKeyDown()
                    ? clicked.relative(face)
                    : BlockPos.containing(beyond.x, beyond.y, beyond.z);
            return placeInCell(context, destination, cornerIndex(beyond, destination),
                    source, new BlockPlaceContext(context));
        }

        // Honor Minecraft's ordinary replaceable-block behavior (grass,
        // water, etc.) when the actual hit is not a quarter container.
        // A neighboring existing partial container can still receive the
        // exact selected octant, but cannot steal a click elsewhere.
        BlockPlaceContext placement = new BlockPlaceContext(context);
        BlockPos destination = placement.getClickedPos();
        return placeInCell(context, destination, cornerIndex(beyond, destination),
                source, placement);
    }

    private static InteractionResult placeInCell(UseOnContext context,
            BlockPos pos, int index, BlockState source, BlockPlaceContext placement) {
        Level level = context.getLevel();
        BlockState existing = level.getBlockState(pos);
        if (existing.is(QuarterBlocks.QUARTER_BLOCK)) {
            return addToGeneric(context, pos, existing, index, source);
        }
        if (existing.getBlock() instanceof RgbQuarterBlock) {
            return migrateLegacyAndAdd(context, pos, existing, index, source);
        }

        // Never overwrite an occupied solid block just because the requested
        // quarter is adjacent to a different, partly filled container.
        if (!existing.canBeReplaced(placement)) return InteractionResult.FAIL;

        BlockState placed = QuarterBlocks.QUARTER_BLOCK.defaultBlockState()
                .setValue(GenericQuarterBlock.CORNERS[index], true);
        if (!level.isUnobstructed(placed, pos,
                CollisionContext.placementContext(context.getPlayer()))) {
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
        if (state.getValue(GenericQuarterBlock.CORNERS[index])) return InteractionResult.FAIL;

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

    private static boolean isQuarterContainer(BlockState state) {
        return state.is(QuarterBlocks.QUARTER_BLOCK) || state.getBlock() instanceof RgbQuarterBlock;
    }

    private static boolean isOccupied(BlockState state, int index) {
        if (state.is(QuarterBlocks.QUARTER_BLOCK)) {
            return state.getValue(GenericQuarterBlock.CORNERS[index]);
        }
        if (state.getBlock() instanceof RgbQuarterBlock) {
            return state.getValue(RgbQuarterBlock.CORNERS[index]);
        }
        return true;
    }

    private static int cornerIndex(Vec3 hit, BlockPos pos) {
        return GenericQuarterBlock.cornerIndex(local(hit, pos));
    }

    private static Vec3 local(Vec3 hit, BlockPos pos) {
        double x = Math.max(0.0, Math.min(0.999999, hit.x - pos.getX()));
        double y = Math.max(0.0, Math.min(0.999999, hit.y - pos.getY()));
        double z = Math.max(0.0, Math.min(0.999999, hit.z - pos.getZ()));
        return new Vec3(x, y, z);
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
