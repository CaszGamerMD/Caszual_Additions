package com.caszgamermd.caszualadditions.pallet;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Edits the target string of the currently OPEN pallet menu, not an arbitrary block. */
public final class PalletFilterNet {
    public record SetTarget(int containerId,String value) implements CustomPacketPayload {
        public static final Type<SetTarget> TYPE=
                new Type<>(CaszualAdditions.id("pallet_filter_target"));
        public static final StreamCodec<RegistryFriendlyByteBuf,SetTarget> CODEC=
                StreamCodec.composite(ByteBufCodecs.INT,SetTarget::containerId,
                        ByteBufCodecs.stringUtf8(100),SetTarget::value,SetTarget::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }

    private PalletFilterNet(){}
    public static void initialize(){
        PayloadTypeRegistry.serverboundPlay().register(SetTarget.TYPE,SetTarget.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(SetTarget.TYPE,(payload,context)->
                context.server().execute(()->{
                    var player=context.player();
                    if(player.containerMenu.containerId!=payload.containerId()
                            ||!(player.containerMenu instanceof PalletMenu menu)
                            ||!menu.stillValid(player))return;
                    menu.applyFilterTarget(payload.value());
                }));
    }

    public static boolean validTarget(int scope,String target){
        if(target.length()>100)return false;
        if(target.isBlank())return true;
        if(scope==1)return target.matches("[a-z0-9_.-]{2,64}");
        if(scope==3)return false; // EXACT requires a live sample, not typed components
        Identifier id=Identifier.tryParse(target);
        return id!=null && target.equals(id.toString());
    }
}
