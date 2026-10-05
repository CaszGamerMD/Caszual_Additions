package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class XpChargerBlockEntity extends BlockEntity {
 private ItemStack item=ItemStack.EMPTY;
 public XpChargerBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.CHARGER,p,s);}
 public ItemStack item(){return item;} public void setItem(ItemStack s){item=s.copyWithCount(Math.min(1,s.getCount()));setChanged();}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);if(!item.isEmpty())tag.store("item",ItemStack.CODEC,item);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);item=tag.read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY);}
}
