package com.caszgamermd.caszualadditions.utils;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BuddingAmethystBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Silk Touch pickup for normally unobtainable special blocks.
 *
 * Only a successfully broken, correctly harvested block can drop an item. We
 * take the spawner's data BEFORE removal and emit the item AFTER a successful
 * server-side break; a canceled/protected break cannot create a free item.
 */
public final class SilkTouchSpecialBlocks {
    public static final TagKey<Block> BUDDING_BLOCKS = TagKey.create(
            Registries.BLOCK, CaszualAdditions.id("silk_touch_budding_blocks"));

    private static final Map<UUID, PendingDrop> PENDING = new HashMap<>();

    private SilkTouchSpecialBlocks() {
    }

    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, blockEntity) -> {
            if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
                return true;
            }

            // A different attempt must never reuse the previous pending item.
            PENDING.remove(serverPlayer.getUUID());
            if (serverPlayer.getAbilities().instabuild
                    || !serverPlayer.hasCorrectToolForDrops(state)
                    || !hasSilkTouch(serverLevel, serverPlayer.getMainHandItem())) {
                return true;
            }

            Block block = state.getBlock();
            ItemStack drop;
            if (state.is(Blocks.SPAWNER) && blockEntity instanceof SpawnerBlockEntity spawner) {
                // Includes SpawnData/SpawnPotentials, delays, ranges and modded entity IDs.
                drop = new ItemStack(Blocks.SPAWNER);
                drop.set(DataComponents.BLOCK_ENTITY_DATA,
                        TypedEntityData.of(spawner.getType(), spawner.saveCustomOnly(serverLevel.registryAccess())));
            } else if (isBuddingBlock(state)) {
                if (block.asItem() == net.minecraft.world.item.Items.AIR) {
                    return true;
                }
                drop = new ItemStack(block);
            } else {
                return true;
            }

            // Many modded budding blocks already have Silk Touch loot. Never
            // create a second copy when their own loot table provides one.
            if (Block.getDrops(state, serverLevel, pos, blockEntity, serverPlayer,
                    serverPlayer.getMainHandItem()).stream().anyMatch(stack -> stack.is(block.asItem()))) {
                return true;
            }

            PENDING.put(serverPlayer.getUUID(), new PendingDrop(serverLevel, pos.immutable(), block, drop));
            return true;
        });

        PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return;
            }

            PendingDrop pending = PENDING.remove(serverPlayer.getUUID());
            if (pending != null
                    && level == pending.level()
                    && pos.equals(pending.pos())
                    && state.is(pending.block())) {
                Block.popResource(level, pos, pending.stack());
            }
        });

        PlayerBlockBreakEvents.CANCELED.register((level, player, pos, state, blockEntity) -> {
            if (player instanceof ServerPlayer serverPlayer) {
                PENDING.remove(serverPlayer.getUUID());
            }
        });
    }

    private static boolean isBuddingBlock(BlockState state) {
        Block block = state.getBlock();
        // The tag is the explicit compatibility path for blocks with unusual
        // names. Also recognize other mods' budding subclasses / IDs.
        return state.is(BUDDING_BLOCKS)
                || block instanceof BuddingAmethystBlock
                || BuiltInRegistries.BLOCK.getKey(block).getPath().contains("budding");
    }

    public static boolean hasSilkTouch(ServerLevel level, ItemStack tool) {
        if (tool.isEmpty()) {
            return false;
        }
        var enchantment = level.registryAccess()
                .lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, tool) > 0;
    }

    private record PendingDrop(ServerLevel level, BlockPos pos, Block block, ItemStack stack) {
    }
}
