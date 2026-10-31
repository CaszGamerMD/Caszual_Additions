package com.caszgamermd.caszualadditions.enchantprinter;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class EnchantmentPrinterMenu extends AbstractContainerMenu {
    public static final int PAGE_SIZE=7,PLAYER_START=4;
    private final Container machine;
    private final EnchantmentPrinterBlockEntity entity;
    private final Player viewer;
    private int page;
    private String lastState="";
    public EnchantmentPrinterMenu(int id,Inventory player,BlockPos pos){
        this(id,player,resolve(player,pos));
    }
    private static Container resolve(Inventory player,BlockPos pos){
        BlockEntity found=player.player.level().getBlockEntity(pos);
        return found instanceof EnchantmentPrinterBlockEntity printer
            ?printer:new SimpleContainer(EnchantmentPrinterBlockEntity.SLOTS);
    }
    public EnchantmentPrinterMenu(int id,Inventory player,Container backing){
        super(EnchantmentPrinterContent.MENU,id);
        machine=backing;
        entity=backing instanceof EnchantmentPrinterBlockEntity printer?printer:null;
        viewer=player.player;
        // Feed source, target item, lapis and emerald supplies.
        for(int i=0;i<4;i++){
            final int slot=i;
            addSlot(new Slot(machine,i,31+i*55,70){
                @Override public boolean mayPlace(ItemStack stack){
                    return machine.canPlaceItem(slot,stack)
                        && (slot!=EnchantmentPrinterBlockEntity.TARGET || stack.getCount()==1);
                }
                @Override public int getMaxStackSize(){return slot<2?1:64;}
            });
        }
        // Compact inventory at the bottom of the larger modern console.
        addStandardInventorySlots(player,169,302);
    }
    public void updatePage(int value){page=Math.max(0,value);}
    @Override public void broadcastChanges(){
        super.broadcastChanges();
        if(entity!=null && viewer instanceof ServerPlayer player
                && player.level() instanceof ServerLevel world){
            entity.refreshCooldown(world);
            String state=entity.snapshot(world,page);
            if(!state.equals(lastState)){
                lastState=state;
                ServerPlayNetworking.send(player,new EnchantmentPrinterStatePayload(state));
            }
        }
    }
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player)||!(player.level() instanceof ServerLevel world))return false;
        if(entity==null)return false;
        if(id==0){page=Math.max(0,page-1);}
        else if(id==1){page++;}
        else if(id==2)entity.clearSelection();
        else if(id==3)player.sendSystemMessage(entity.feed(world));
        else if(id==4)player.sendSystemMessage(entity.print(world,player));
        else if(id>=100&&id<100+PAGE_SIZE){
            var keys=entity.keys();
            int index=page*PAGE_SIZE+id-100;
            if(index>=0&&index<keys.size())entity.toggle(world,keys.get(index));
        }else return false;
        lastState="";
        broadcastChanges();
        return true;
    }
    @Override public boolean stillValid(Player player){return machine.stillValid(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        boolean moved;
        if(index<PLAYER_START)moved=moveItemStackTo(stack,PLAYER_START,slots.size(),true);
        else{
            moved=false;
            for(int i=0;i<4;i++){
                if(machine.canPlaceItem(i,stack)
                        && (i!=EnchantmentPrinterBlockEntity.TARGET||stack.getCount()==1)
                        && moveItemStackTo(stack,i,i+1,false)){
                    moved=true;break;
                }
            }
        }
        if(!moved)return ItemStack.EMPTY;
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        return copy;
    }
}
