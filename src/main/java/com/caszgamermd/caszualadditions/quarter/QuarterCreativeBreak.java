package com.caszgamermd.caszualadditions.quarter;

import com.caszgamermd.caszualadditions.rods.RgbBuildingBlocks;
import com.caszgamermd.caszualadditions.rods.RgbQuarterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Handles a quarter at a time, preserving neighboring corners and their materials.
 * The server creates drops; the client only predicts the updated shape.
 */
public final class QuarterCreativeBreak {
    private QuarterCreativeBreak() {}

    public static boolean isQuarterContainer(BlockState state) {
        return state.is(QuarterBlocks.QUARTER_BLOCK)
                || state.getBlock() instanceof RgbQuarterBlock;
    }

    public static boolean breakTargeted(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!isQuarterContainer(state)) return false;

        ItemStack held = player.getMainHandItem();
        BoinkrItem.Mode mode = held.getItem() instanceof BoinkrItem
                ? BoinkrItem.mode(held) : BoinkrItem.Mode.SINGLE;
        // Boink! converts only by right-click. Left-click should not destroy
        // an entire cell in this mode.
        if (mode == BoinkrItem.Mode.BOINK) return true;

        int target = targetedOccupiedCorner(player, pos, state);
        if (target < 0) return false;
        boolean group = mode == BoinkrItem.Mode.GROUP;
        boolean creative = player.getAbilities().instabuild;

        if (state.is(QuarterBlocks.QUARTER_BLOCK)) {
            GenericQuarterBlockEntity be = level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity found
                    ? found : null;
            if (!level.isClientSide() && !creative && be != null) {
                for (int i = 0; i < 8; i++) {
                    if (state.getValue(GenericQuarterBlock.CORNERS[i]) && (group || i == target)) {
                        BlockState source = be.material(i);
                        if (source != null) Block.popResource(level, pos, QuarterBlocks.pieceFor(source, 1));
                    }
                }
            }
            BlockState updated = state;
            for (int i = 0; i < 8; i++) {
                if (state.getValue(GenericQuarterBlock.CORNERS[i]) && (group || i == target))
                    updated = updated.setValue(GenericQuarterBlock.CORNERS[i], false);
            }
            if (count(updated, GenericQuarterBlock.CORNERS) == 0) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, updated, 11);
                if (be != null) {
                    for (int i = 0; i < 8; i++) {
                        if (state.getValue(GenericQuarterBlock.CORNERS[i]) && (group || i == target))
                            be.clearMaterial(i);
                    }
                }
            }
            return true;
        }

        if (!level.isClientSide() && !creative) {
            int pieces = group ? count(state, RgbQuarterBlock.CORNERS) : 1;
            Block.popResource(level, pos, new ItemStack(RgbBuildingBlocks.RGB_QUARTER_ITEM, pieces));
        }
        BlockState updated = state;
        for (int i = 0; i < 8; i++) {
            if (state.getValue(RgbQuarterBlock.CORNERS[i]) && (group || i == target))
                updated = updated.setValue(RgbQuarterBlock.CORNERS[i], false);
        }
        if (count(updated, RgbQuarterBlock.CORNERS) == 0) level.removeBlock(pos, false);
        else level.setBlock(pos, updated, 11);
        return true;
    }

    /** Complete, uniform quarter container to its exact original full block state. */
    public static @Nullable BlockState mergedSource(Level level, BlockPos pos, BlockState state) {
        if (state.is(QuarterBlocks.QUARTER_BLOCK)) {
            if (count(state, GenericQuarterBlock.CORNERS) != 8
                    || !(level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity be)) return null;
            BlockState source = be.material(0);
            if (source == null || source.hasBlockEntity()) return null;
            for (int i = 1; i < 8; i++) {
                BlockState other = be.material(i);
                if (other == null || !other.is(source.getBlock())) return null;
            }
            return source;
        }
        if (state.getBlock() instanceof RgbQuarterBlock
                && count(state, RgbQuarterBlock.CORNERS) == 8)
            return RgbBuildingBlocks.RGB_BLOCK.defaultBlockState();
        return null;
    }

    private static int count(BlockState state, BooleanProperty[] corners) {
        int count = 0;
        for (BooleanProperty corner : corners) if (state.getValue(corner)) count++;
        return count;
    }

    private static int targetedOccupiedCorner(Player player, BlockPos pos, BlockState state) {
        Vec3 from = player.getEyePosition();
        Vec3 to = from.add(player.getViewVector(1.0F)
                .scale(player.blockInteractionRange() + 1.0));
        double bestDistance = Double.MAX_VALUE;
        int best = -1;
        for (int i = 0; i < 8; i++) {
            boolean occupied = state.is(QuarterBlocks.QUARTER_BLOCK)
                    ? state.getValue(GenericQuarterBlock.CORNERS[i])
                    : state.getValue(RgbQuarterBlock.CORNERS[i]);
            if (!occupied) continue;
            var hit = cornerBox(i, pos).clip(from, to);
            if (hit.isEmpty()) continue;
            double distance = hit.get().distanceToSqr(from);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }
        return best;
    }

    public static AABB cornerBox(int index, BlockPos pos) {
        double x = (index & 1) != 0 ? 0.5 : 0.0;
        double z = (index & 2) != 0 ? 0.5 : 0.0;
        double y = index >= 4 ? 0.5 : 0.0;
        return new AABB(pos.getX() + x, pos.getY() + y, pos.getZ() + z,
                pos.getX() + x + 0.5, pos.getY() + y + 0.5, pos.getZ() + z + 0.5);
    }
}
