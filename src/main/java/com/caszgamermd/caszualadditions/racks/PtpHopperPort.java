package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A directional, zero-storage corner adapter to the 3x3 point's one inventory. */
public final class PtpHopperPort extends BlockEntity implements Container {
    public PtpHopperPort(BlockPos pos,BlockState state){
        super(RackContent.PTP_PORT_ENTITY,pos,state);
    }
    private PtpBlockEntity owner(){
        return level==null?null:PtpBlock.controller(level,worldPosition);
    }
    private boolean canInput(){
        PtpBlockEntity c=owner();
        return c!=null&&c.hasPallet()&&!c.sealed()&&c.input();
    }
    private boolean canOutput(){
        PtpBlockEntity c=owner();
        return c!=null&&c.hasPallet()&&!c.sealed()&&!c.input();
    }
    @Override public int getContainerSize(){return owner()==null?0:PtpBlockEntity.SLOTS;}
    @Override public boolean isEmpty(){
        PtpBlockEntity c=owner();
        return c==null||c.sealed()||c.isEmpty();
    }
    @Override public ItemStack getItem(int slot){
        PtpBlockEntity c=owner();return c==null?ItemStack.EMPTY:c.getItem(slot);
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        PtpBlockEntity c=owner();return canInput()&&c!=null&&c.canPlaceItem(slot,stack);
    }
    @Override public ItemStack removeItem(int slot,int count){
        PtpBlockEntity c=owner();
        return canOutput()&&c!=null?c.removeItem(slot,count):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        PtpBlockEntity c=owner();
        return canOutput()&&c!=null?c.removeItemNoUpdate(slot):ItemStack.EMPTY;
    }
    @Override public boolean canTakeItem(Container destination,int slot,ItemStack stack){
        return canOutput();
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(canInput()){PtpBlockEntity c=owner();if(c!=null)c.setItem(slot,stack);}
    }
    @Override public void clearContent(){/* adapters must never void pallet cargo */}
    @Override public boolean stillValid(Player player){
        PtpBlockEntity c=owner();return c!=null&&c.stillValid(player);
    }
}
