package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Actual floor magazine: twelve independent intact empty pallet items. */
public final class StackOfPalletsBlockEntity extends BlockEntity implements Container,ExtendedMenuProvider<BlockPos> {
    private NonNullList<ItemStack> pallets=NonNullList.withSize(12,ItemStack.EMPTY);
    public StackOfPalletsBlockEntity(BlockPos pos,BlockState state){
        super(RackContent.PALLET_STACK_ENTITY,pos,state);
    }
    @Override public Component getDisplayName(){return Component.literal("Stack of Pallets");}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new StackOfPalletsMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
    public void loadMagazine(ItemStack magazine){
        NonNullList<ItemStack> unpacked=NonNullList.withSize(12,ItemStack.EMPTY);
        magazine.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY).copyInto(unpacked);
        for(int i=0;i<12;i++)
            if(StackOfPalletsItem.isPallet(unpacked.get(i)))
                pallets.set(i,unpacked.get(i).copyWithCount(1));
        setChanged();
    }
    public ItemStack asItem(){
        ItemStack stack=new ItemStack(RackContent.STACK_OF_PALLETS);
        // Save all twelve slots INCLUDING empty positions, so breaking
        // and replacing the multiblock reproduces its exact visual layout.
        stack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(pallets));
        return stack;
    }
    public int count(){
        int n=0;for(ItemStack stack:pallets)if(!stack.isEmpty())n++;return n;
    }
    public boolean put(ItemStack pallet){
        if(!StackOfPalletsItem.isPallet(pallet)||pallet.getCount()!=1)return false;
        for(int i=0;i<12;i++)if(pallets.get(i).isEmpty()){
            pallets.set(i,pallet.copyWithCount(1));setChanged();return true;
        }
        return false;
    }
    public ItemStack take(){
        for(int i=11;i>=0;i--)if(!pallets.get(i).isEmpty()){
            ItemStack result=pallets.get(i);
            pallets.set(i,ItemStack.EMPTY);setChanged();return result;
        }
        return ItemStack.EMPTY;
    }
    @Override public int getContainerSize(){return 12;}
    @Override public boolean isEmpty(){return count()==0;}
    @Override public ItemStack getItem(int slot){
        return slot>=0&&slot<12?pallets.get(slot):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot,int count){
        if(slot<0||slot>=12||count<=0)return ItemStack.EMPTY;
        ItemStack result=ContainerHelper.removeItem(pallets,slot,count);
        if(!result.isEmpty())setChanged();return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        if(slot<0||slot>=12)return ItemStack.EMPTY;
        ItemStack result=ContainerHelper.takeItem(pallets,slot);
        if(!result.isEmpty())setChanged();return result;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=12||(!stack.isEmpty()&&!canPlaceItem(slot,stack)))return;
        pallets.set(slot,stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1));
        setChanged();
    }
    @Override public boolean canPlaceItem(int slot,ItemStack item){
        return slot>=0&&slot<12&&StackOfPalletsItem.isPallet(item)
                &&item.getCount()==1;
    }
    @Override public void clearContent(){
        pallets=NonNullList.withSize(12,ItemStack.EMPTY);setChanged();
    }
    @Override public boolean stillValid(Player player){
        return Container.stillValidBlockEntity(this,player,6.0f);
    }
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null){
            BlockState state=getBlockState();
            if(state.getBlock() instanceof StackOfPalletsBlock){
                int height=Math.min(4,(count()+2)/3);
                if(state.getValue(StackOfPalletsBlock.HEIGHT)!=height)
                    level.setBlock(worldPosition,state.setValue(StackOfPalletsBlock.HEIGHT,height),3);
                level.sendBlockUpdated(worldPosition,state,state,3);
            }
        }
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);ContainerHelper.saveAllItems(output,pallets);
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);
        pallets=NonNullList.withSize(12,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,pallets);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){
        return saveWithoutMetadata(provider);
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
