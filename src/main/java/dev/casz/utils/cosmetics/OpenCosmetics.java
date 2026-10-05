package dev.casz.utils.cosmetics;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record OpenCosmetics() implements CustomPacketPayload {
    public static final OpenCosmetics INSTANCE = new OpenCosmetics();
    public static final Type<OpenCosmetics> TYPE = new Type<>(Identifier.fromNamespaceAndPath("caszutils", "open_cosmetics"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCosmetics> CODEC = StreamCodec.unit(INSTANCE);
    @Override public Type<OpenCosmetics> type() { return TYPE; }
}