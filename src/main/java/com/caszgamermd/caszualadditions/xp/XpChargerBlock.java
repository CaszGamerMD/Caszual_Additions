package com.caszgamermd.caszualadditions.xp;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Up to three independently repairing Mending tools per connected XP network. */
public final class XpChargerBlock extends Block implements EntityBlock {
    public static final MapCodec<XpChargerBlock> CODEC=simpleCodec(XpChargerBlock::new);
    public XpChargerBlock(BlockBehaviour.Properties p){super(p);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new XpChargerBlockEntity(pos,state);
    }
    private static void returnItem(Player player,ItemStack stack){
        if(!stack.isEmpty()&&!player.getInventory().add(stack))
            player.drop(stack,false);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof XpChargerBlockEntity charger){
            if(player.isShiftKeyDown()){
                // Sneak + empty hand: retrieve all three safely.
                for(int i=0;i<charger.getContainerSize();i++)
                    returnItem(player,charger.removeItem(i,1));
            }else{
                int slot=charger.firstReady();
                if(slot<0)slot=charger.firstOccupied();
                if(slot>=0)returnItem(player,charger.removeItem(slot,1));
            }
            if(level instanceof ServerLevel server)status(player,server,pos,charger);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level level,BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,
            BlockHitResult hit){
        if(!level.isClientSide()&&level.getBlockEntity(pos) instanceof XpChargerBlockEntity charger){
            // Insert into a free slot. Never eject a different person's item
            // merely because a third repair tool was right-clicked.
            if(XpRepair.hasMending(held)&&held.isDamaged()){
                int slot=charger.firstFree();
                if(slot>=0){
                    charger.setItem(slot,held.copyWithCount(1));
                    if(!player.getAbilities().instabuild)held.shrink(1);
                }
            }else if(player.isShiftKeyDown()){
                for(int i=0;i<charger.getContainerSize();i++)
                    returnItem(player,charger.removeItem(i,1));
            }else{
                int slot=charger.firstReady();
                if(slot<0)slot=charger.firstOccupied();
                if(slot>=0)returnItem(player,charger.removeItem(slot,1));
            }
            if(level instanceof ServerLevel server)status(player,server,pos,charger);
        }
        return InteractionResult.SUCCESS;
    }
    private static void status(Player player,ServerLevel level,BlockPos pos,
            XpChargerBlockEntity charger){
        int stored=XpNetwork.stored(level,pos),capacity=XpNetwork.capacity(level,pos);
        StringBuilder summary=new StringBuilder();
        for(int i=0;i<charger.getContainerSize();i++){
            ItemStack item=charger.getItem(i);
            if(i>0)summary.append(" | ");
            summary.append(i+1).append(": ");
            summary.append(item.isEmpty()?"empty":item.getHoverName().getString()
                    +(item.isDamaged()?" ("+item.getDamageValue()+" damage)":" (ready)"));
        }
        if(player instanceof ServerPlayer server)
            server.sendSystemMessage(Component.literal("XP Charger "+
                    charger.occupied()+"/3 | "+summary+
                    " | XP "+stored+"/"+capacity),true);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,
            ServerLevel level,BlockPos pos,boolean moved){
        if(level.getBlockEntity(pos) instanceof XpChargerBlockEntity charger){
            for(int i=0;i<charger.getContainerSize();i++){
                ItemStack item=charger.removeItem(i,1);
                if(!item.isEmpty())popResource(level,pos,item);
            }
        }
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide())return null;
        return type==XpBlockEntities.CHARGER
                ?(world,pos,block,be)->XpRepair.tick((ServerLevel)world,
                        (XpChargerBlockEntity)be):null;
    }
}
