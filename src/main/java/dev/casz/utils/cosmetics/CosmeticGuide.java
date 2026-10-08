package dev.casz.utils.cosmetics;

import dev.casz.utils.CaszUtils;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

public final class CosmeticGuide {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(CaszUtils.MOD_ID, "cosmetic_guide");
    public static final Item ITEM;

    public record OpenGuide() implements CustomPacketPayload {
        public static final Type<OpenGuide> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(CaszUtils.MOD_ID, "open_cosmetic_guide"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuide> CODEC =
                StreamCodec.unit(new OpenGuide());

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    static {
        var key = ResourceKey.create(Registries.ITEM, ID);
        ITEM = Registry.register(
                BuiltInRegistries.ITEM,
                key,
                new CosmeticGuideItem(new Item.Properties().setId(key).stacksTo(1))
        );
    }

    private CosmeticGuide() {}

    public static void initialize() {
        PayloadTypeRegistry.clientboundPlay().register(OpenGuide.TYPE, OpenGuide.CODEC);
    }

    public static void open(ServerPlayer player) {
        ServerPlayNetworking.send(player, new OpenGuide());
    }
}
