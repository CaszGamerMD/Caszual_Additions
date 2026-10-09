package com.caszgamermd.caszualadditions.quarter;

import com.caszgamermd.caszualadditions.rods.RgbQuarterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class QuarterCreativeBreak {
    private QuarterCreativeBreak() {}

    public static boolean removeTargetedCorner(Level level, Player player, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        int index = targetedOccupiedCorner(player, pos, state);
        if (index < 0) return false;

        if (state.is(QuarterBlocks.QUARTER_BLOCK)) {
            int count = occupiedCount(state, GenericQuarterBlock.CORNERS);
            if (count <= 1) {
                level.removeBlock(pos, false);
            } else {
                BlockState updated = state.setValue(GenericQuarterBlock.CORNERS[index], false);
                level.setBlock(pos, updated, 11);
                if (level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity) {
                    blockEntity.clearMaterial(index);
                }
            }
            return true;
        }

        if (state.getBlock() instanceof RgbQuarterBlock) {
            int count = occupiedCount(state, RgbQuarterBlock.CORNERS);
            if (count <= 1) {
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(RgbQuarterBlock.CORNERS[index], false), 11);
            }
            return true;
        }

        return false;
    }

    private static int targetedOccupiedCorner(Player player, BlockPos pos, BlockState state) {
        Vec3 from = player.getEyePosition();
        Vec3 direction = player.getViewVector(1.0F);
        Vec3 to = from.add(direction.scale(player.blockInteractionRange() + 1.0));

        double bestDistance = Double.MAX_VALUE;
        int best = -1;

        for (int i = 0; i < 8; i++) {
            boolean occupied;
            if (state.is(QuarterBlocks.QUARTER_BLOCK)) {
                occupied = state.getValue(GenericQuarterBlock.CORNERS[i]);
            } else if (state.getBlock() instanceof RgbQuarterBlock) {
                occupied = state.getValue(RgbQuarterBlock.CORNERS[i]);
            } else {
                return -1;
            }

            if (!occupied) continue;

            AABB box = cornerBox(i, pos);
            var hit = box.clip(from, to);
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
        double minX = (index & 1) != 0 ? 0.5 : 0.0;
        double minZ = (index & 2) != 0 ? 0.5 : 0.0;
        double minY = index >= 4 ? 0.5 : 0.0;

        return new AABB(
                pos.getX() + minX,
                pos.getY() + minY,
                pos.getZ() + minZ,
                pos.getX() + minX + 0.5,
                pos.getY() + minY + 0.5,
                pos.getZ() + minZ + 0.5
        );
    }

    private static int occupiedCount(
            BlockState state,
            net.minecraft.world.level.block.state.properties.BooleanProperty[] corners
    ) {
        int count = 0;
        for (var corner : corners) {
            if (state.getValue(corner)) count++;
        }
        return count;
    }
}
