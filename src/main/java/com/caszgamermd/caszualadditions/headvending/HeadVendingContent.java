package com.caszgamermd.caszualadditions.headvending;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    /** One response per mob; verified HeadDB hash, never arbitrary client texture. */
    public record MobPreviewRequest(BlockPos pos, String key) implements CustomPacketPayload {
        public static final Type<MobPreviewRequest> TYPE =
                new Type<>(CaszualAdditions.id("mob_head_preview_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobPreviewRequest> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, MobPreviewRequest::pos,
                        ByteBufCodecs.stringUtf8(48), MobPreviewRequest::key,
                        MobPreviewRequest::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record MobPreviewReply(BlockPos pos, String key, String hash) implements CustomPacketPayload {
        public static final Type<MobPreviewReply> TYPE =
                new Type<>(CaszualAdditions.id("mob_head_preview_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, MobPreviewReply> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, MobPreviewReply::pos,
                        ByteBufCodecs.stringUtf8(48), MobPreviewReply::key,
                        ByteBufCodecs.stringUtf8(128), MobPreviewReply::hash,
                        MobPreviewReply::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Checkout(BlockPos pos, String json) implements CustomPacketPayload {
        public static final Type<Checkout> TYPE =
                new Type<>(CaszualAdditions.id("checkout_heads"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Checkout> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, Checkout::pos,
                        ByteBufCodecs.stringUtf8(8192), Checkout::json, Checkout::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record CheckoutReply(boolean success, String message) implements CustomPacketPayload {
        public static final Type<CheckoutReply> TYPE =
                new Type<>(CaszualAdditions.id("checkout_heads_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CheckoutReply> CODEC =
                StreamCodec.composite(ByteBufCodecs.BOOL, CheckoutReply::success,
                        ByteBufCodecs.stringUtf8(180), CheckoutReply::message,
                        CheckoutReply::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final Map<String, CompletableFuture<CustomHeadCatalog.Head>> MOB_TEXTURES =
            new ConcurrentHashMap<>();

    private static CompletableFuture<CustomHeadCatalog.Head> mobTexture(String key) {
        CompletableFuture<CustomHeadCatalog.Head> future = MOB_TEXTURES.computeIfAbsent(key,
                id -> HeadDbSearch.findMobAsync(displayName(id)));
        // Don't permanently cache a temporary API outage or an empty search.
        future.whenComplete((head, failure) -> {
            if (failure != null || head == null) MOB_TEXTURES.remove(key, future);
        });
        return future;
    }

    public static void initialize() {
        HeadVendingBookmarks.initialize();
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
        PayloadTypeRegistry.serverboundPlay().register(MobPreviewRequest.TYPE, MobPreviewRequest.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MobPreviewReply.TYPE, MobPreviewReply.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Checkout.TYPE, Checkout.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CheckoutReply.TYPE, CheckoutReply.CODEC);

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
        ServerPlayNetworking.registerGlobalReceiver(MobPreviewRequest.TYPE,
                (payload, context) -> context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!validMachine(player, payload.pos())) return;
                    String key = payload.key().toLowerCase(Locale.ROOT);
                    if (MOB_HEADS.get(key) != Items.PLAYER_HEAD) return;
                    mobTexture(key).whenComplete((head, error) ->
                            context.server().execute(() -> {
                                ServerPlayer current = context.server().getPlayerList()
                                        .getPlayer(player.getUUID());
                                if (current != null && validMachine(current, payload.pos())) {
                                    ServerPlayNetworking.send(current, new MobPreviewReply(
                                            payload.pos(), key, head == null ? "" : head.hash()));
                                }
                            }));
                }));
        ServerPlayNetworking.registerGlobalReceiver(Checkout.TYPE,
                (payload, context) -> context.server().execute(() ->
                        handleCheckout(context.server(), context.player(), payload)));
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
                    completePurchase(current, head, resolved.name() + "'s Head", "player", name);
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
            completePurchase(player, new ItemStack(item), displayName(key) + " Head", "mob", key);
            return;
        }

        mobTexture(key).whenComplete((head, error) ->
                server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, payload.pos())) return;
                    if (head == null) {
                        status(current, "No matching " + displayName(key)
                                + " head is available from HeadDB.", false);
                        return;
                    }
                    completePurchase(current, CustomHeadCatalog.createHead(
                            displayName(key) + " Head", head.hash()),
                            displayName(key) + " Head", "mob", key);
                }));
    }


    private static void handleCustomSearch(
            MinecraftServer server, ServerPlayer player, SearchCustom request
    ) {
        if (!validMachine(player, request.pos())) return;
        HeadDbSearch.searchAsync(request.query(), request.category(), request.page())
                .whenComplete((data, error) -> server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, request.pos())) return;
                    String answer = error == null ? data : CustomHeadCatalog.Catalog.failure(
                            request.query(), request.category(), request.page());
                    ServerPlayNetworking.send(current, new CustomResults(request.pos(), answer));
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

        // Only hashes previously returned by HeadDB on this server are valid.
        // Clients cannot submit arbitrary texture URLs or forged hash values.
        CustomHeadCatalog.Head head = HeadDbSearch.verified(hash);
        if (head == null) {
            String label = HeadVendingBookmarks.previouslyPurchasedCustom(player, hash);
            if (label == null) {
                status(player, "Search for this head again before purchasing.", false);
                return;
            }
            // This hash was already validated by the server and recorded
            // as a successful purchase in this player's persistent history.
            completePurchase(player, CustomHeadCatalog.createHead(label, hash),
                    label, "custom", hash);
            return;
        }
        completePurchase(player, CustomHeadCatalog.createHead(head.name(), head.hash()),
                head.name(), "custom", head.hash());
    }

    private record CartLine(String kind, String target, int quantity) {}
    private record Prepared(ItemStack stack, String label, String kind, String target) {}

    private static void checkoutMessage(ServerPlayer player, boolean success, String message) {
        ServerPlayNetworking.send(player, new CheckoutReply(success, message));
    }

    private static void handleCheckout(MinecraftServer server, ServerPlayer player, Checkout request) {
        if (!validMachine(player, request.pos())) return;
        List<CartLine> entries = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int units = 0;
        try {
            JsonArray array = JsonParser.parseString(request.json()).getAsJsonArray();
            if (array.size() < 1 || array.size() > 64)
                throw new IllegalArgumentException("Cart must contain 1-64 different heads.");
            for (JsonElement element : array) {
                JsonObject row = element.getAsJsonObject();
                String kind = row.get("kind").getAsString().toLowerCase(Locale.ROOT);
                String target = row.get("target").getAsString().toLowerCase(Locale.ROOT);
                int qty = row.get("qty").getAsInt();
                if (qty < 1 || qty > 64 || target.length() > 128
                        || !seen.add(kind + "|" + target)
                        || !(kind.equals("mob") || kind.equals("player") || kind.equals("custom")))
                    throw new IllegalArgumentException("Invalid cart entry.");
                if (kind.equals("mob") && !MOB_HEADS.containsKey(target)
                        || kind.equals("player") && !PLAYER_NAME.matcher(target).matches()
                        || kind.equals("custom") && !target.matches("[a-f0-9]{32,128}"))
                    throw new IllegalArgumentException("Unknown head in cart.");
                units += qty;
                if (units > 64) throw new IllegalArgumentException("64 heads maximum per checkout.");
                entries.add(new CartLine(kind, target, qty));
            }
        } catch (RuntimeException invalid) {
            checkoutMessage(player, false, "Invalid cart: " + invalid.getMessage());
            return;
        }

        List<CompletableFuture<Prepared>> pending = new ArrayList<>();
        for (CartLine entry : entries) {
            String key = entry.target();
            CompletableFuture<Prepared> future;
            switch (entry.kind()) {
                case "player" -> future = CompletableFuture.supplyAsync(() ->
                        server.services().profileResolver().fetchByName(key)
                                .map(profile -> {
                                    ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
                                    stack.set(DataComponents.PROFILE,
                                            ResolvableProfile.createResolved(profile));
                                    String label = profile.name() + "'s Head";
                                    stack.set(DataComponents.ITEM_NAME, Component.literal(label));
                                    return new Prepared(stack, label, "player", key);
                                }).orElse(null), Util.nonCriticalIoPool());
                case "mob" -> {
                    Item item = MOB_HEADS.get(key);
                    String label = displayName(key) + " Head";
                    future = item != Items.PLAYER_HEAD
                            ? CompletableFuture.completedFuture(new Prepared(
                                    new ItemStack(item), label, "mob", key))
                            : mobTexture(key).thenApply(head -> head == null ? null
                                    : new Prepared(CustomHeadCatalog.createHead(label, head.hash()),
                                            label, "mob", key));
                }
                case "custom" -> {
                    CustomHeadCatalog.Head head = HeadDbSearch.verified(key);
                    String label = head != null ? head.name()
                            : HeadVendingBookmarks.previouslyPurchasedCustom(player, key);
                    future = CompletableFuture.completedFuture(label == null ? null
                            : new Prepared(CustomHeadCatalog.createHead(label, key),
                                    label, "custom", key));
                }
                default -> throw new IllegalStateException();
            }
            pending.add(future);
        }

        final int totalUnits = units;
        CompletableFuture.allOf(pending.toArray(new CompletableFuture[0]))
                .whenComplete((ignored, failure) -> server.execute(() -> {
                    ServerPlayer current = server.getPlayerList().getPlayer(player.getUUID());
                    if (current == null || !validMachine(current, request.pos())) return;
                    if (failure != null) {
                        checkoutMessage(current, false, "Could not load head textures; retry.");
                        return;
                    }
                    List<Prepared> output = new ArrayList<>();
                    for (int i = 0; i < entries.size(); i++) {
                        Prepared item = pending.get(i).getNow(null);
                        if (item == null || item.stack().isEmpty()) {
                            checkoutMessage(current, false,
                                    "Missing head: " + entries.get(i).target()
                                            + ". Please try again later.");
                            return;
                        }
                        for (int q = 0; q < entries.get(i).quantity(); q++) output.add(item);
                    }
                    finalizeCheckout(current, output, totalUnits);
                }));
    }

    /**
     * Validate entire checkout against the inventory snapshot BEFORE removing
     * emeralds. Roll back inventory if something unexpectedly fails to insert.
     */
    private static void finalizeCheckout(ServerPlayer player, List<Prepared> heads, int amount) {
        Inventory inventory = player.getInventory();
        boolean creative = player.getAbilities().instabuild;
        List<ItemStack> original = new ArrayList<>();
        for (int i = 0; i < inventory.getContainerSize(); i++)
            original.add(inventory.getItem(i).copy());
        List<ItemStack> simulated = new ArrayList<>();
        for (ItemStack slot : original) simulated.add(slot.copy());
        int emeralds = 0;
        for (int i = 0; i < Math.min(36, simulated.size()); i++)
            if (simulated.get(i).is(Items.EMERALD)) emeralds += simulated.get(i).getCount();
        if (!creative && emeralds < amount) {
            checkoutMessage(player, false, "Need " + amount + " emeralds (have " + emeralds + ").");
            return;
        }
        if (!creative) {
            int remaining = amount;
            for (int i = 0; i < Math.min(36, simulated.size()) && remaining > 0; i++) {
                ItemStack stack = simulated.get(i);
                if (!stack.is(Items.EMERALD)) continue;
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }

        for (Prepared prepared : heads) {
            ItemStack item = prepared.stack();
            boolean placed = false;
            for (int i = 0; i < Math.min(36, simulated.size()); i++) {
                ItemStack slot = simulated.get(i);
                if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, item)
                        && slot.getCount() < slot.getMaxStackSize()) {
                    slot.grow(1);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                for (int i = 0; i < Math.min(36, simulated.size()); i++) {
                    if (simulated.get(i).isEmpty()) {
                        simulated.set(i, item.copyWithCount(1));
                        placed = true;
                        break;
                    }
                }
            }
            if (!placed) {
                checkoutMessage(player, false, "Not enough inventory space for " + amount + " heads.");
                return;
            }
        }

        // Commit the simulated inventory once, as one transaction.
        for (int i = 0; i < Math.min(36, simulated.size()); i++)
            inventory.setItem(i, simulated.get(i));
        inventory.setChanged();
        List<String> purchased = new ArrayList<>();
        for (Prepared item : heads)
            purchased.add(HeadVendingBookmarks.encode(
                    item.kind(), item.target(), item.label()));
        HeadVendingBookmarks.recordPurchases(player, purchased);
        checkoutMessage(player, true, creative
                ? "Created " + amount + " heads for free."
                : "Purchased " + amount + " heads for " + amount + " emeralds.");
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
            String label,
            String kind,
            String target
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

        HeadVendingBookmarks.recordPurchase(player, kind, target, label);
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
