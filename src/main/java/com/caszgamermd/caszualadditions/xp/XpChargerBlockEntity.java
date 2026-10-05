package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
public final class XpChargerBlockEntity extends BlockEntity {
 private ItemStack item=ItemStack.EMPTY;
 public XpChargerBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.CHARGER,p,s);}
 public ItemStack item(){return item;} public void setItem(ItemStack s){item=s.copyWithCount(Math.min(1,s.getCount()));setChanged();}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!item.isEmpty())out.store("item",ItemStack.CODEC,item);}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);item=in.read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY);}
}
