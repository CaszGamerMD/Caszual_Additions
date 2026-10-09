package com.caszgamermd.caszualadditions.utils.cosmetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record ToggleArmorVisibility(boolean hidden) implements CustomPacketPayload {
    public static final Type<ToggleArmorVisibility> TYPE = new Type<>(Identifier.fromNamespaceAndPath("caszual_additions","toggle_armor_visibility"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleArmorVisibility> CODEC = StreamCodec.composite(
        net.minecraft.network.codec.ByteBufCodecs.BOOL, ToggleArmorVisibility::hidden, ToggleArmorVisibility::new);
    @Override public Type<? extends CustomPacketPayload> type(){ return TYPE; }
}
