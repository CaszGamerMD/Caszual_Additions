package com.caszgamermd.caszualadditions.headvending;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import net.minecraft.world.entity.MobCategory;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.Vec3;

public final class HeadVendingContent {
    private static final Pattern PLAYER_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    public static final Map<String, Item> MOB_HEADS = new LinkedHashMap<>();

    public static HeadVendingBlock PLAYER_HEAD_VENDING_MACHINE;
    public static Item PLAYER_HEAD_VENDING_MACHINE_ITEM;

    private HeadVendingContent() {}

    public record Open(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Open> TYPE =
                new Type<>(CaszualAdditions.id("open_head_vending"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Open::pos, Open::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record Purchase(BlockPos pos, boolean playerHead, String query)
            implements CustomPacketPayload {
        public static final Type<Purchase> TYPE =
                new Type<>(CaszualAdditions.id("buy_head_vending"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Purchase> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC,
                        Purchase::pos,
                        ByteBufCodecs.BOOL,
                        Purchase::playerHead,
                        ByteBufCodecs.stringUtf8(32),
                        Purchase::query,
                        Purchase::new
                );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }


    /** Search/filter requests are processed against the server's catalog only. */
    public record SearchCustom(BlockPos pos, String query, String category, int page)
            implements CustomPacketPayload {
        public static final Type<SearchCustom> TYPE =
                new Type<>(CaszualAdditions.id("search_custom_heads"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SearchCustom> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, SearchCustom::pos,
                        ByteBufCodecs.stringUtf8(48), SearchCustom::query,
                        ByteBufCodecs.stringUtf8(40), SearchCustom::category,
                        ByteBufCodecs.INT, SearchCustom::page,
                        SearchCustom::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record CustomResults(BlockPos pos, String json) implements CustomPacketPayload {
        public static final Type<CustomResults> TYPE =
                new Type<>(CaszualAdditions.id("custom_head_results"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CustomResults> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, CustomResults::pos,
                        ByteBufCodecs.stringUtf8(16384), CustomResults::json,
                        CustomResults::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record BuyCustom(BlockPos pos, String hash) implements CustomPacketPayload {
        public static final Type<BuyCustom> TYPE =
                new Type<>(CaszualAdditions.id("buy_custom_head"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BuyCustom> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, BuyCustom::pos,
                        ByteBufCodecs.stringUtf8(128), BuyCustom::hash,
                        BuyCustom::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void initialize() {
        Identifier id = CaszualAdditions.id("player_head_vending_machine");
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);

        PLAYER_HEAD_VENDING_MACHINE = Registry.register(
                BuiltInRegistries.BLOCK,
                blockKey,
                new HeadVendingBlock(
                        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
                                .strength(3.5F)
                                .noOcclusion()
                                .lightLevel(state -> state.getValue(HeadVendingBlock.HALF)
                                        == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER
                                        ? 8 : 3)
                                .setId(blockKey)
                )
        );

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        PLAYER_HEAD_VENDING_MACHINE_ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                itemKey,
                new BlockItem(
                        PLAYER_HEAD_VENDING_MACHINE,
                        new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()
                )
        );

        MOB_HEADS.put("skeleton", Items.SKELETON_SKULL);
        MOB_HEADS.put("wither_skeleton", Items.WITHER_SKELETON_SKULL);
        MOB_HEADS.put("zombie", Items.ZOMBIE_HEAD);
        MOB_HEADS.put("creeper", Items.CREEPER_HEAD);
        MOB_HEADS.put("piglin", Items.PIGLIN_HEAD);
        MOB_HEADS.put("dragon", Items.DRAGON_HEAD);
        MOB_HEADS.put("ender_dragon", Items.DRAGON_HEAD);

        // Resolve the full vanilla mob roster from Minecraft's entity registry.
        // Native heads use their real vanilla item; the rest use validated HeadDB skins.
        BuiltInRegistries.ENTITY_TYPE.forEach(type -> {
            Identifier entityId = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (entityId != null && "minecraft".equals(entityId.getNamespace())
                    && type.canSummon() && type.getCategory() != MobCategory.MISC) {
                MOB_HEADS.putIfAbsent(entityId.getPath(), Items.PLAYER_HEAD);
            }
        });

        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Purchase.TYPE, Purchase.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SearchCustom.TYPE, SearchCustom.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(BuyCustom.TYPE, BuyCustom.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CustomResults.TYPE, CustomResults.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(
                Purchase.TYPE,
                (payload, context) -> context.server().execute(() ->
                        handlePurchase(context.server(), context.player(), payload))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                SearchCustom.TYPE,
                (payload, context) -> context.server().execute(() ->
                        handleCustomSearch(context.server(), context.player(), payload))
        );

        ServerPlayNetworking.registerGlobalReceiver(
                BuyCustom.TYPE,
                (payload, context) -> context.server().execute(() ->
                        handleCustomPurchase(context.server(), context.player(), payload))
        );
    }

    public static String displayName(String key) {
        return switch (key) {
            case "wither_skeleton" -> "Wither Skeleton";
            case "skeleton" -> "Skeleton";
            case "zombie" -> "Zombie";
            case "creeper" -> "Creeper";
            case "piglin" -> "Piglin";
            case "dragon" -> "Dragon";
            default -> {
                StringBuilder name = new StringBuilder();
                for (String part : key.split("_")) {
                    if (!name.isEmpty()) name.append(' ');
                    if (!part.isEmpty()) name.append(Character.toUpperCase(part.charAt(0)))
                            .append(part.substring(1));
                }
                yield name.toString();
            }
        };
    }

    private static void handlePurchase(
            MinecraftServer server,
            ServerPlayer player,
            Purchase payload
    ) {
        if (!validMachine(player, payload.pos())) return;

        if (payload.playerHead()) {
            String name = payload.query().trim();
            if (!PLAYER_NAME.matcher(name).matches()) {
                status(player, "Enter a valid Minecraft username.", false);
                return;
            }

            status(player, "Looking up " + name + "...", true);
            Util.nonCriticalIoPool().execute(() -> {
                Optional<GameProfile> profile =
                        server.services().profileResolver().fetchByName(name);

                server.execute(() -> {
                    ServerPlayer current =
                            server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, payload.pos())) return;

                    if (profile.isEmpty()) {
                        status(current, "No player profile found for " + name + ".", false);
                        return;
                    }

                    GameProfile resolved = profile.get();
                    ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                    head.set(
                            DataComponents.PROFILE,
                            ResolvableProfile.createResolved(resolved)
                    );
                    head.set(
                            DataComponents.ITEM_NAME,
                            Component.literal(resolved.name() + "'s Head")
                    );
                    completePurchase(current, head, resolved.name() + "'s Head");
                });
            });
            return;
        }

        String key = payload.query().trim().toLowerCase(java.util.Locale.ROOT);
        Item item = MOB_HEADS.get(key);
        if (item == null) {
            status(player, "That mob head is not sold here.", false);
            return;
        }

        if (item != Items.PLAYER_HEAD) {
            completePurchase(player, new ItemStack(item), displayName(key) + " Head");
            return;
        }

        CustomHeadCatalog.load().whenComplete((catalog, error) ->
                server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, payload.pos())) return;
                    if (error != null) {
                        status(current, "Mob-head textures are temporarily unavailable.", false);
                        return;
                    }
                    String desired = key.replace('_', ' ');
                    CustomHeadCatalog.Head match = catalog.heads().stream()
                            .filter(head -> head.name().equalsIgnoreCase(desired)
                                    || head.name().equalsIgnoreCase(desired + " head"))
                            .findFirst().orElse(null);
                    if (match == null) {
                        status(current, "No verified " + displayName(key) + " head texture found.", false);
                        return;
                    }
                    completePurchase(current, CustomHeadCatalog.createHead(
                            displayName(key) + " Head", match.hash()), displayName(key) + " Head");
                }));
    }


    private static void handleCustomSearch(
            MinecraftServer server, ServerPlayer player, SearchCustom request
    ) {
        if (!validMachine(player, request.pos())) return;
        CustomHeadCatalog.load().whenComplete((catalog, error) ->
                server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, request.pos())) return;
                    String data;
                    if (error != null) {
                        data = CustomHeadCatalog.Catalog.failure(
                                request.query(), request.category(), request.page());
                    } else {
                        data = catalog.search(request.query(), request.category(), request.page());
                    }
                    ServerPlayNetworking.send(current, new CustomResults(request.pos(), data));
                }));
    }

    private static void handleCustomPurchase(
            MinecraftServer server, ServerPlayer player, BuyCustom request
    ) {
        if (!validMachine(player, request.pos())) return;
        String hash = request.hash().toLowerCase(java.util.Locale.ROOT);
        if (!hash.matches("[a-f0-9]{32,128}")) {
            status(player, "That custom head is invalid.", false);
            return;
        }

        CustomHeadCatalog.load().whenComplete((catalog, error) ->
                server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, request.pos())) return;
                    if (error != null) {
                        status(current, "Custom heads are temporarily unavailable.", false);
                        return;
                    }
                    CustomHeadCatalog.Head head = catalog.find(hash);
                    if (head == null) {
                        status(current, "That head is not in the catalog.", false);
                        return;
                    }
                    completePurchase(
                            current,
                            CustomHeadCatalog.createHead(head.name(), head.hash()),
                            head.name()
                    );
                }));
    }

    private static boolean validMachine(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        if (!state.is(PLAYER_HEAD_VENDING_MACHINE)
                || !HeadVendingBlock.isComplete(player.level(), pos, state)) {
            status(player, "The vending machine is no longer there.", false);
            return false;
        }

        BlockPos base = HeadVendingBlock.basePos(pos, state);
        if (player.distanceToSqr(Vec3.atCenterOf(base)) > 64.0) {
            status(player, "Move closer to the vending machine.", false);
            return false;
        }

        return true;
    }

    private static void completePurchase(
            ServerPlayer player,
            ItemStack head,
            String label
    ) {
        Inventory inventory = player.getInventory();

        if (inventory.getFreeSlot() < 0
                && inventory.getSlotWithRemainingSpace(head) < 0) {
            status(player, "Your inventory is full.", false);
            return;
        }

        boolean creative = player.getAbilities().instabuild;
        if (!creative) {
            int emeraldSlot = findEmerald(inventory);
            if (emeraldSlot < 0) {
                status(player, "You need 1 emerald.", false);
                return;
            }
            ItemStack emeralds = inventory.getItem(emeraldSlot);
            emeralds.shrink(1);
            if (emeralds.isEmpty()) {
                inventory.setItem(emeraldSlot, ItemStack.EMPTY);
            }
            inventory.setChanged();
        }

        ItemStack toInsert = head.copy();
        boolean inserted = inventory.add(toInsert);
        if (!inserted || !toInsert.isEmpty()) {
            if (!creative) inventory.add(new ItemStack(Items.EMERALD));
            status(player, creative ? "Could not deliver the head." : "Could not deliver the head; your emerald was returned.", false);
            return;
        }

        status(player, creative ? "Created " + label + " for free." : "Purchased " + label + " for 1 emerald.", true);
    }

    private static int findEmerald(Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (inventory.getItem(slot).is(Items.EMERALD)) {
                return slot;
            }
        }
        return -1;
    }

    private static void status(ServerPlayer player, String message, boolean success) {
        player.sendOverlayMessage(
                Component.literal((success ? "✓ " : "✗ ") + message)
        );
    }
}
