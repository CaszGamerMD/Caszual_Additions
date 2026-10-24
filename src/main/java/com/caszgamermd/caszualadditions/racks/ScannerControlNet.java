package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Server-owned portable scanner controls; GUI never edits item NBT itself. */
public final class ScannerControlNet {
    public record Open(String json) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(CaszualAdditions.id("scanner_settings_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=
            StreamCodec.composite(ByteBufCodecs.stringUtf8(2048),Open::json,Open::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record SetOption(String key,String value) implements CustomPacketPayload {
        public static final Type<SetOption> TYPE=new Type<>(CaszualAdditions.id("scanner_settings_set"));
        public static final StreamCodec<RegistryFriendlyByteBuf,SetOption> CODEC=
            StreamCodec.composite(ByteBufCodecs.stringUtf8(32),SetOption::key,
                                  ByteBufCodecs.stringUtf8(48),SetOption::value,SetOption::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    private ScannerControlNet(){}
    public static void initialize(){
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetOption.TYPE,SetOption.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetOption.TYPE,(payload,ctx)->
            ctx.server().execute(()->apply(ctx.player(),payload)));
    }
    public static void open(ServerPlayer player,ItemStack scanner){
        if(!scanner.is(RackContent.STORAGE_SCANNER_ITEM))return;
        JsonObject json=StorageScannerItem.settingsSnapshot(scanner,player.level());
        ServerPlayNetworking.send(player,new Open(json.toString()));
    }
    private static void apply(ServerPlayer player,SetOption packet){
        ItemStack scanner=player.getMainHandItem();
        if(!scanner.is(RackContent.STORAGE_SCANNER_ITEM))return;
        // Player must keep the same physical scanner in the main hand.
        StorageScannerItem.applySetting(scanner,packet.key(),packet.value());
        open(player,scanner);
    }
}
