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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public final class QuarterPlacement {
    private static final double TARGET_EPSILON = 0.002;

    private QuarterPlacement() {}

    public static InteractionResult place(UseOnContext context, BlockState source) {
        Level level = context.getLevel();

        // First, allow aiming through an EMPTY octant of an existing quarter
        // container. Vanilla raycasts ignore that empty volume and normally hit
        // the block behind it, which made diagonal/kitty-corner placement
        // impossible.
        QuarterTarget rayTarget = findEmptyQuarterAlongRay(context);
        if (rayTarget != null) {
            if (rayTarget.state().is(QuarterBlocks.QUARTER_BLOCK)) {
                return addToGeneric(context, rayTarget.pos(), rayTarget.state(), rayTarget.index(), source);
            }
            if (rayTarget.state().getBlock() instanceof RgbQuarterBlock) {
                return migrateLegacyAndAdd(context, rayTarget.pos(), rayTarget.state(), rayTarget.index(), source);
            }
        }

        // If an occupied quarter itself was clicked, nudge the hit point through
        // that face. If that exact adjacent octant is unavailable, choose the
        // nearest empty octant to the cursor.
        BlockPos clickedPos = context.getClickedPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        if (isQuarterContainer(clickedState)) {
            int index = directTargetIndex(context, clickedState);
            if (index >= 0) {
                if (clickedState.is(QuarterBlocks.QUARTER_BLOCK)) {
                    return addToGeneric(context, clickedPos, clickedState, index, source);
                }
                return migrateLegacyAndAdd(context, clickedPos, clickedState, index, source);
            }
        }

        // No existing quarter container was targeted, so place the first quarter
        // normally in the block location selected by Minecraft.
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        if (!placeContext.canPlace()) return InteractionResult.FAIL;

        BlockPos pos = placeContext.getClickedPos();
        BlockState existing = level.getBlockState(pos);
        int index = cornerIndex(context.getClickLocation(), pos);

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

    private static QuarterTarget findEmptyQuarterAlongRay(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return null;

        Vec3 from = player.getEyePosition();
        Vec3 rawTo = context.getClickLocation();
        Vec3 ray = rawTo.subtract(from);
        if (ray.lengthSqr() < 1.0E-8) return null;

        Vec3 direction = ray.normalize();
        Vec3 to = rawTo.add(direction.scale(0.1));

        BlockPos clicked = context.getClickedPos();
        QuarterTarget best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos pos = clicked.offset(dx, dy, dz);
                    BlockState state = context.getLevel().getBlockState(pos);
                    if (!isQuarterContainer(state)) continue;

                    AABB fullBlock = new AABB(
                            pos.getX(), pos.getY(), pos.getZ(),
                            pos.getX() + 1.0, pos.getY() + 1.0, pos.getZ() + 1.0
                    );
                    var intersection = fullBlock.clip(from, to);
                    if (intersection.isEmpty()) continue;

                    Vec3 justInside = intersection.get().add(direction.scale(TARGET_EPSILON));
                    int index = cornerIndex(justInside, pos);
                    if (isOccupied(state, index)) continue;

                    double distance = intersection.get().distanceToSqr(from);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = new QuarterTarget(pos, state, index);
                    }
                }
            }
        }

        return best;
    }

    private static int directTargetIndex(UseOnContext context, BlockState state) {
        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        Vec3 hit = context.getClickLocation();

        Vec3 nudged = hit.add(
                face.getStepX() * TARGET_EPSILON,
                face.getStepY() * TARGET_EPSILON,
                face.getStepZ() * TARGET_EPSILON
        );

        int preferred = cornerIndex(nudged, pos);
        if (!isOccupied(state, preferred)) return preferred;

        // Fallback: nearest empty octant center to the cursor. This keeps adding
        // pieces intuitive when the exact face-adjacent octant is already used.
        Vec3 local = local(hit, pos);
        int best = -1;
        double bestDistance = Double.MAX_VALUE;

        for (int i = 0; i < 8; i++) {
            if (isOccupied(state, i)) continue;

            double cx = (i & 1) != 0 ? 0.75 : 0.25;
            double cz = (i & 2) != 0 ? 0.75 : 0.25;
            double cy = i >= 4 ? 0.75 : 0.25;
            double dx = local.x - cx;
            double dy = local.y - cy;
            double dz = local.z - cz;
            double distance = dx * dx + dy * dy + dz * dz;

            if (distance < bestDistance) {
                bestDistance = distance;
                best = i;
            }
        }

        return best;
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

    private record QuarterTarget(BlockPos pos, BlockState state, int index) {}
}
