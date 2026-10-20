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
    private final boolean spawner;
    public WarehouseForkliftItem(Properties p){this(p,true);}
    public WarehouseForkliftItem(Properties p,boolean spawner){
        super(p);this.spawner=spawner;
    }
    private static BlockPos selected(net.minecraft.world.item.ItemStack tool,ServerLevel world){
        var data=tool.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data==null)return null;
        var tag=data.copyTag();
        if(!tag.getStringOr("forklift_source_dimension","")
                .equals(world.dimension().toString()))return null;
        if(!tag.contains("forklift_source_x")||!tag.contains("forklift_source_y")
                ||!tag.contains("forklift_source_z"))return null;
        return new BlockPos(tag.getIntOr("forklift_source_x",0),
                tag.getIntOr("forklift_source_y",0),tag.getIntOr("forklift_source_z",0));
    }
    private static void select(net.minecraft.world.item.ItemStack tool,ServerLevel world,BlockPos root){
        var data=tool.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var tag=data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
        tag.putString("forklift_source_dimension",world.dimension().toString());
        tag.putInt("forklift_source_x",root.getX());
        tag.putInt("forklift_source_y",root.getY());
        tag.putInt("forklift_source_z",root.getZ());
        tool.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;
        BlockPos clicked=context.getClickedPos();
        var player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        if(!spawner){
            var staged=StorageGroundPalletTransfer.wrappedController(level,clicked);
            if(staged!=null){
                select(context.getItemInHand(),level,staged.getBlockPos());
                player.sendSystemMessage(Component.literal(
                        "Forklift Controller: source pallet selected at "
                        +staged.getBlockPos().toShortString()+". Click an empty stacked rack."));
                return InteractionResult.SUCCESS;
            }
        }
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
            BlockPos selectedSource=spawner?null:selected(context.getItemInHand(),level);
            boolean started=nearest.order(level,rack,selectedSource);
            if(started&&!spawner)
                context.getItemInHand().remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            player.sendSystemMessage(Component.literal(started
                    ?"Forklift dispatched: "+(rack.hasPallet()?"retrieve rack pallet to aisle":
                        "collect nearest wrapped ground pallet and place into rack")
                    :"Forklift could not start: check driver Golem, aisle, wrapped pallet, "
                            +"and 12-block lift limit."));
            return InteractionResult.SUCCESS;
        }
        if(!spawner||context.getClickedFace()!=net.minecraft.core.Direction.UP)
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
