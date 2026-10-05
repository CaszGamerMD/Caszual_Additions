package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
public final class XpChargerBlockEntity extends BlockEntity implements WorldlyContainer {
 private static final int[] SLOT={0};
 private ItemStack item=ItemStack.EMPTY;
 public XpChargerBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.CHARGER,p,s);}
 public ItemStack item(){return item;} public void setItem(ItemStack s){item=s.copyWithCount(Math.min(1,s.getCount()));setChanged();}
 @Override public int getContainerSize(){return 1;}
 @Override public boolean isEmpty(){return item.isEmpty();}
 @Override public ItemStack getItem(int slot){return slot==0?item:ItemStack.EMPTY;}
 @Override public ItemStack removeItem(int slot,int amount){if(slot!=0||item.isEmpty()||amount<=0)return ItemStack.EMPTY;var out=item.copy();out.setCount(1);item=ItemStack.EMPTY;setChanged();return out;}
 @Override public ItemStack removeItemNoUpdate(int slot){if(slot!=0)return ItemStack.EMPTY;var out=item;item=ItemStack.EMPTY;return out;}
 @Override public void setItem(int slot,ItemStack stack){if(slot==0)setItem(stack);}
 @Override public int getMaxStackSize(){return 1;}
 @Override public boolean stillValid(Player player){return level!=null&&level.getBlockEntity(worldPosition)==this&&player.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
 @Override public void clearContent(){item=ItemStack.EMPTY;setChanged();}
 @Override public int[] getSlotsForFace(Direction side){return SLOT;}
 @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0&&item.isEmpty()&&XpRepair.hasMending(stack)&&stack.isDamaged();}
 @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return slot==0&&!stack.isDamaged();}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);if(!item.isEmpty())out.store("item",ItemStack.CODEC,item);}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);item=in.read("item",ItemStack.CODEC).orElse(ItemStack.EMPTY);}
}
