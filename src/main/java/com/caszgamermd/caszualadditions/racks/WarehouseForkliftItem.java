package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Place a physical forklift, or use this item on a rack to assign the
 * nearest parked forklift one specific job. No unrequested rack shuffling.
 */
public final class WarehouseForkliftItem extends Item {
    public WarehouseForkliftItem(Properties p){super(p);}
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;
        BlockPos clicked=context.getClickedPos();
        var player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        RackBlockEntity rack=RackBlock.controller(level,clicked);
        if(rack!=null){
            WarehouseForklift nearest=null;
            double best=24*24;
            for(WarehouseForklift lift:level.getEntitiesOfClass(
                    WarehouseForklift.class,new AABB(clicked).inflate(24),
                    f->!f.busy())){
                double d=lift.blockPosition().distSqr(clicked);
                if(d<best){best=d;nearest=lift;}
            }
            if(nearest==null){
                player.sendSystemMessage(Component.literal(
                        "No idle forklift within 24 blocks of this rack."));
                return InteractionResult.SUCCESS;
            }
            player.sendSystemMessage(Component.literal(nearest.order(level,rack)
                    ?"Forklift dispatched: "+(rack.hasPallet()?"retrieve rack pallet to aisle":
                        "collect nearest wrapped ground pallet and place into rack")
                    :"Forklift could not start: check driver Golem, aisle, wrapped pallet, "
                            +"and 12-block lift limit."));
            return InteractionResult.SUCCESS;
        }
        if(context.getClickedFace()!=net.minecraft.core.Direction.UP)
            return InteractionResult.PASS;
        BlockPos at=clicked.above();
        if(!level.getBlockState(at).canBeReplaced()
                ||!level.getBlockState(at.above()).canBeReplaced())
            return InteractionResult.FAIL;
        WarehouseForklift lift=RackContent.FORKLIFT_TYPE.create(level,EntitySpawnReason.EVENT);
        if(lift==null)return InteractionResult.FAIL;
        lift.setPos(at.getX()+.5,at.getY(),at.getZ()+.5);
        lift.setYRot(player.getYRot());
        if(!level.addFreshEntity(lift))return InteractionResult.FAIL;
        if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
        player.sendSystemMessage(Component.literal(
                "Forklift parked. Keep Golems docked nearby; use another Forklift item "
                +"on a rack to request pickup or storage."));
        return InteractionResult.SUCCESS;
    }
}
