package com.caszgamermd.caszualadditions.pallet;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Inventory-free satellite. Every read/write resolves the live controller,
 * including ordinary machines that only inspect block-entity Containers.
 * There is no independent cargo to save, spill or duplicate.
 */
public final class PalletInventoryPort extends BlockEntity implements Container {
 public PalletInventoryPort(BlockPos pos,BlockState state){super(PalletContent.PALLET_INVENTORY_PORT,pos,state);}
 private PalletBlockEntity root(){return level==null?null:PalletBlock.rootEntity(level,worldPosition,getBlockState());}
 @Override public int getContainerSize(){return root()==null?0:PalletBlockEntity.SLOTS;}
 @Override public boolean isEmpty(){var r=root();return r==null||r.isEmpty();}
 @Override public ItemStack getItem(int slot){var r=root();return r==null?ItemStack.EMPTY:r.getItem(slot);}
 @Override public ItemStack removeItem(int slot,int amount){var r=root();return r==null?ItemStack.EMPTY:r.removeItem(slot,amount);}
 @Override public ItemStack removeItemNoUpdate(int slot){var r=root();return r==null?ItemStack.EMPTY:r.removeItemNoUpdate(slot);}
 @Override public void setItem(int slot,ItemStack stack){var r=root();if(r!=null)r.setItem(slot,stack);}
 @Override public boolean canPlaceItem(int slot,ItemStack stack){var r=root();return r!=null&&r.canPlaceItem(slot,stack);}
 @Override public void clearContent(){var r=root();if(r!=null)r.clearContent();}
 @Override public boolean stillValid(Player player){var r=root();return r!=null&&r.stillValid(player);}
 @Override public void setChanged(){super.setChanged();var r=root();if(r!=null)r.setChanged();}
}
