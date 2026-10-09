package com.caszgamermd.caszualadditions.headvending;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/** Persisted per player, NOT per vending machine or per client installation. */
public final class HeadVendingBookmarks {
    public static final int MAX_FAVORITES = 64;
    public static final int MAX_HISTORY = 30;
    private static final AttachmentType<List<String>> FAVORITES = AttachmentRegistry.create(
            CaszualAdditions.id("head_vending_favorites"),
            b -> b.initializer(List::of).persistent(Codec.STRING.listOf()).copyOnDeath());
    private static final AttachmentType<List<String>> HISTORY = AttachmentRegistry.create(
            CaszualAdditions.id("head_vending_history"),
            b -> b.initializer(List::of).persistent(Codec.STRING.listOf()).copyOnDeath());

    public record Toggle(BlockPos pos, String kind, String target, String label)
            implements CustomPacketPayload {
        public static final Type<Toggle> TYPE = new Type<>(CaszualAdditions.id("head_vending_favorite"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Toggle> CODEC =
                StreamCodec.composite(
                        BlockPos.STREAM_CODEC, Toggle::pos,
                        ByteBufCodecs.stringUtf8(12), Toggle::kind,
                        ByteBufCodecs.stringUtf8(128), Toggle::target,
                        ByteBufCodecs.stringUtf8(100), Toggle::label,
                        Toggle::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE = new Type<>(CaszualAdditions.id("head_vending_saved"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Snapshot> CODEC =
                StreamCodec.composite(ByteBufCodecs.stringUtf8(16384), Snapshot::json, Snapshot::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private HeadVendingBookmarks() {}

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(Toggle.TYPE, Toggle.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Snapshot.TYPE, Snapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Toggle.TYPE, (payload, context) ->
                context.server().execute(() -> handle(context.player(), payload)));
    }

    public static String encode(String kind, String target, String label) {
        // Avoid separator collisions, unbounded text and invalid input.
        return kind + "|" + target + "|" + label.replace('|', ' ').replace('\n', ' ').strip();
    }

    private static boolean valid(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        return state.is(HeadVendingContent.PLAYER_HEAD_VENDING_MACHINE)
                && HeadVendingBlock.isComplete(player.level(), pos, state)
                && player.distanceToSqr(Vec3.atCenterOf(HeadVendingBlock.basePos(pos, state))) <= 64;
    }

    private static void handle(ServerPlayer player, Toggle toggle) {
        if (!valid(player, toggle.pos())) return;
        String kind = toggle.kind().toLowerCase(Locale.ROOT);
        String target = toggle.target().toLowerCase(Locale.ROOT);
        if (toggle.label().isBlank() || toggle.label().length() > 100) return;
        if (kind.equals("mob")) {
            if (!HeadVendingContent.MOB_HEADS.containsKey(target)) return;
        } else if (kind.equals("player")) {
            if (!target.matches("[a-z0-9_]{1,16}")) return;
        } else if (kind.equals("custom")) {
            if (!target.matches("[a-f0-9]{32,128}")) return;
        } else return;

        String saved = encode(kind, target, toggle.label());
        List<String> favorites = new ArrayList<>(player.getAttachedOrElse(FAVORITES, List.of()));
        // Identify by kind and target even if the item's visible label changed.
        String prefix = kind + "|" + target + "|";
        boolean removed = favorites.removeIf(entry -> entry.startsWith(prefix));
        if (!removed) {
            favorites.add(0, saved);
            if (favorites.size() > MAX_FAVORITES)
                favorites = new ArrayList<>(favorites.subList(0, MAX_FAVORITES));
        }
        player.setAttached(FAVORITES, List.copyOf(favorites));
        sync(player);
    }

    public static void recordPurchase(ServerPlayer player, String kind, String target, String label) {
        String prefix = kind + "|" + target.toLowerCase(Locale.ROOT) + "|";
        List<String> history = new ArrayList<>(player.getAttachedOrElse(HISTORY, List.of()));
        history.removeIf(entry -> entry.startsWith(prefix));
        history.add(0, encode(kind, target.toLowerCase(Locale.ROOT), label));
        if (history.size() > MAX_HISTORY) history = new ArrayList<>(history.subList(0, MAX_HISTORY));
        player.setAttached(HISTORY, List.copyOf(history));
        sync(player);
    }

    /**
     * History is written only after a successful server-validated purchase.
     * It can safely restore a purchased custom texture after server restart.
     * Favorites, which can be set without buying, are intentionally not trusted.
     */
    public static String previouslyPurchasedCustom(ServerPlayer player, String hash) {
        String prefix = "custom|" + hash.toLowerCase(Locale.ROOT) + "|";
        for (String entry : player.getAttachedOrElse(HISTORY, List.of())) {
            if (entry.startsWith(prefix)) return entry.substring(prefix.length());
        }
        return null;
    }

    public static void sync(ServerPlayer player) {
        JsonObject data = new JsonObject();
        JsonArray favorites = new JsonArray();
        for (String entry : player.getAttachedOrElse(FAVORITES, List.of())) favorites.add(entry);
        JsonArray history = new JsonArray();
        for (String entry : player.getAttachedOrElse(HISTORY, List.of())) history.add(entry);
        data.add("favorites", favorites);
        data.add("history", history);
        ServerPlayNetworking.send(player, new Snapshot(data.toString()));
    }
}
