package com.caszgamermd.caszualadditions.painting;
import com.caszgamermd.caszualadditions.CaszualAdditions;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public final class IndustrialPaintNet {
    private IndustrialPaintNet(){}
    public record Open(BlockPos can,int color) implements CustomPacketPayload {
        public static final Type<Open> TYPE=new Type<>(CaszualAdditions.id("industrial_paint_open"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,Open::can,
                        ByteBufCodecs.INT,Open::color,Open::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    /** action 0 = save color, action 1 = take reusable brush. */
    public record Request(BlockPos can,int rgb,int action) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(CaszualAdditions.id("industrial_paint_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=
                StreamCodec.composite(BlockPos.STREAM_CODEC,Request::can,
                        ByteBufCodecs.INT,Request::rgb,
                        ByteBufCodecs.INT,Request::action,Request::new);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public static void initialize(){
        PayloadTypeRegistry.clientboundPlay().register(Open.TYPE,Open.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(Request.TYPE,Request.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(request,context)->
                context.server().execute(()->handle(context.player(),request)));
    }
    private static void handle(ServerPlayer player,Request request){
        BlockPos pos=request.can();
        if(player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>64
                ||!player.level().hasChunkAt(pos)
                ||!(player.level().getBlockEntity(pos) instanceof IndustrialPaintCanBlockEntity can))
            return;
        int color=request.rgb()&0xffffff;
        if(request.action()==0){
            can.setColor(color);
            player.sendSystemMessage(Component.literal(
                    "Industrial Paint Can set to #"+String.format("%06X",color)),true);
        }else if(request.action()==1){
            // Save before dipping so the brush always matches the displayed HEX.
            can.setColor(color);
            if(!player.getInventory().add(IndustrialBrushItem.dipped(false,color))){
                player.sendSystemMessage(Component.literal("Inventory full."),true);
                return;
            }
            player.sendSystemMessage(Component.literal(
                    "Industrial Paint Brush loaded with #"+String.format("%06X",color)),true);
        }
    }
}
