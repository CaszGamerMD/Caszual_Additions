package com.caszgamermd.caszualadditions.quarter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/** The Boink'r: precise breaking, whole-cell breaking and lossless splitting/merging. */
public final class BoinkrItem extends Item {
    private static final String MODE_KEY = "boinkr_mode";
    public enum Mode {
        SINGLE("Single"), GROUP("Group"), BOINK("Boink!");
        private final String title;
        Mode(String title) { this.title = title; }
        public String title() { return title; }
    }

    public BoinkrItem(Properties properties) { super(properties); }

    public static Mode mode(ItemStack stack) {
        var tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        String raw = tag.getStringOr(MODE_KEY, Mode.SINGLE.name());
        try { return Mode.valueOf(raw); }
        catch (IllegalArgumentException ignored) { return Mode.SINGLE; }
    }

    private static InteractionResult cycle(Level level, Player player, ItemStack stack) {
        if (!level.isClientSide()) {
            Mode[] modes = Mode.values();
            Mode next = modes[(mode(stack).ordinal() + 1) % modes.length];
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            tag.putString(MODE_KEY, next.name());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            player.sendOverlayMessage(Component.literal("Boink'r: " + next.title()));
            level.playSound(null, player.blockPosition(), SoundEvents.WOODEN_BUTTON_CLICK_ON,
                    SoundSource.PLAYERS, 0.6f, 1.2f + next.ordinal() * 0.1f);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * This is a dedicated baby-block tool, not a pickaxe or universal hammer.
     * In mining modes it removes quarters quickly. Boink! is conversion-only.
     */
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return QuarterCreativeBreak.isQuarterContainer(state) && mode(stack) != Mode.BOINK
                ? 64.0F : 0.0F;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown()) return cycle(level, player, player.getItemInHand(hand));
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (player.isShiftKeyDown()) return cycle(level, player, context.getItemInHand());
        if (mode(context.getItemInHand()) != Mode.BOINK) return InteractionResult.PASS;

        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!player.getAbilities().mayBuild || !level.mayInteract(player, pos)) return InteractionResult.FAIL;

        BlockState merged = QuarterCreativeBreak.mergedSource(level, pos, state);
        if (merged != null) {
            if (!level.isClientSide()) {
                level.setBlock(pos, merged, 3);
                sound(level, pos);
            }
            return InteractionResult.SUCCESS;
        }

        if (QuarterCreativeBreak.isQuarterContainer(state)) {
            if (!level.isClientSide()) {
                player.sendOverlayMessage(Component.literal(
                        "Boink!: fill all eight quarters with the same block first."));
            }
            return InteractionResult.FAIL;
        }

        // Only single-block, full-cube solids without block entities.
        // This excludes inventories, liquids, crops, doors, and stateful machinery.
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0f || state.hasBlockEntity()
                || !state.getFluidState().isEmpty()
                || state.getRenderShape() != RenderShape.MODEL
                || !Block.isShapeFullBlock(state.getCollisionShape(level, pos))
                || state.getBlock().asItem() == net.minecraft.world.item.Items.AIR) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            BlockState quarters = QuarterBlocks.QUARTER_BLOCK.defaultBlockState();
            for (var corner : GenericQuarterBlock.CORNERS)
                quarters = quarters.setValue(corner, true);
            if (!level.setBlock(pos, quarters, 3)) return InteractionResult.FAIL;
            if (level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity entity) {
                for (int i = 0; i < 8; i++) entity.setMaterial(i, state);
            }
            sound(level, pos);
        }
        return InteractionResult.SUCCESS;
    }

    private static void sound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS, 0.8f, 0.95f);
    }
}
