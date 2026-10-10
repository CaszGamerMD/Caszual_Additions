package com.caszgamermd.caszualadditions.quarter;

import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side quarter break prediction must also tell the authoritative
 * server to remove the actual quarter, otherwise it is restored on resync.
 * The server checks distance, interaction permission and the target block.
 */
public final class QuarterBreakNetworking {
    private QuarterBreakNetworking() {}

    public record BreakQuarter(BlockPos pos) implements CustomPacketPayload {
        public static final Type<BreakQuarter> TYPE =
                new Type<>(CaszualAdditions.id("break_quarter"));
        public static final StreamCodec<RegistryFriendlyByteBuf, BreakQuarter> CODEC =
                StreamCodec.composite(BlockPos.STREAM_CODEC, BreakQuarter::pos, BreakQuarter::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public static void initialize() {
        PayloadTypeRegistry.serverboundPlay().register(BreakQuarter.TYPE, BreakQuarter.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BreakQuarter.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    BlockPos pos = payload.pos();
                    var level = player.level();
                    // No remote breaking, unauthorized mining or ordinary
                    // block destruction through this packet.
                    double reach = player.blockInteractionRange() + 1.5;
                    if (!player.getAbilities().mayBuild
                            || !level.mayInteract(player, pos)
                            || Vec3.atCenterOf(pos).distanceToSqr(player.getEyePosition()) > reach * reach
                            || !QuarterCreativeBreak.isQuarterContainer(level.getBlockState(pos))) {
                        return;
                    }
                    // Boink! mode is for conversion by right-click, never
                    // breaking by left-click.
                    if (player.getMainHandItem().getItem() instanceof BoinkrItem
                            && BoinkrItem.mode(player.getMainHandItem()) == BoinkrItem.Mode.BOINK) {
                        return;
                    }
                    QuarterCreativeBreak.breakTargeted(level, player, pos);
                }));
    }
}
