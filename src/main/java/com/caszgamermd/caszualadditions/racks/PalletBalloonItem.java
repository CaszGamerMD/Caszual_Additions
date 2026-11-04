package com.caszgamermd.caszualadditions.racks;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Deployable Pallet Balloon. Right-click places it on open ground. */
public final class PalletBalloonItem extends Item {
    public PalletBalloonItem(Properties properties){super(properties);}
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(!(level instanceof ServerLevel server))return InteractionResult.FAIL;
        if(player.getVehicle()!=null){
            player.sendSystemMessage(Component.literal("Leave your current vehicle before deploying a Pallet Balloon."));
            return InteractionResult.SUCCESS;
        }
        PalletBalloon balloon=RackContent.PALLET_BALLOON_TYPE.create(
                server,EntitySpawnReason.EVENT);
        if(balloon==null)return InteractionResult.FAIL;
        Vec3 look=player.getLookAngle();
        double x=player.getX()+look.x*2.0;
        double z=player.getZ()+look.z*2.0;
        double y=Math.max(64,player.getY());
        balloon.setPos(x,y,z);
        balloon.setYRot(player.getYRot());
        if(!server.addFreshEntity(balloon))return InteractionResult.FAIL;
        if(!player.getAbilities().instabuild)player.getItemInHand(hand).shrink(1);
        player.startRiding(balloon);
        player.sendSystemMessage(Component.literal(
                "Pallet Balloon deployed. W/S fly forward/back, A/D steer, Space rises, Sprint descends. First passenger pilots."));
        return InteractionResult.SUCCESS;
    }
    @Override public InteractionResult useOn(UseOnContext context){
        return use(context.getLevel(),context.getPlayer(),context.getHand());
    }
}
