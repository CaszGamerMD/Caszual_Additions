package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Authoritative pilot controls, sent while the first passenger is steering. */
public record PalletBalloonPilotPayload(float forward,float turn,float lift)
        implements CustomPacketPayload {
    public static final Type<PalletBalloonPilotPayload> TYPE =
            new Type<>(CaszualAdditions.id("pallet_balloon_pilot"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PalletBalloonPilotPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.FLOAT,PalletBalloonPilotPayload::forward,
                    ByteBufCodecs.FLOAT,PalletBalloonPilotPayload::turn,
                    ByteBufCodecs.FLOAT,PalletBalloonPilotPayload::lift,
                    PalletBalloonPilotPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
