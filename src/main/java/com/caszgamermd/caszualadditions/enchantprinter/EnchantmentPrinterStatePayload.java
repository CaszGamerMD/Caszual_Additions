package com.caszgamermd.caszualadditions.enchantprinter;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record EnchantmentPrinterStatePayload(String state) implements CustomPacketPayload {
    public static final Type<EnchantmentPrinterStatePayload> TYPE=
        new Type<>(CaszualAdditions.id("enchantment_printer_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,EnchantmentPrinterStatePayload> CODEC=
        StreamCodec.composite(ByteBufCodecs.STRING_UTF8,
            EnchantmentPrinterStatePayload::state,EnchantmentPrinterStatePayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
