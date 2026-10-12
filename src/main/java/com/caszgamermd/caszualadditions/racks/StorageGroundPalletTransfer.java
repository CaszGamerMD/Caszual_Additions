package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/** Server-only handoff for free-standing 2x2 pallets.
 * Validate all four pieces before transferring the contents to the one
 * carrier; remove the floor inventory only AFTER the leader accepted it.
 * The pallet is not dropped as an item or placed in the player's inventory. */
public final class StorageGroundPalletTransfer {
    private StorageGroundPalletTransfer(){}
    public static PalletBlockEntity wrappedController(ServerLevel level,BlockPos anyPart){
        if(!level.hasChunkAt(anyPart))return null;
        BlockState state=level.getBlockState(anyPart);
        if(!(state.getBlock() instanceof PalletBlock))return null;
        BlockPos root=PalletBlock.rootPos(anyPart,state);
        if(!level.hasChunkAt(root))return null;
        BlockState home=level.getBlockState(root);
        if(!(home.getBlock() instanceof PalletBlock)
                ||home.getValue(PalletBlock.PART)!=0||!level.hasChunkAt(root)
                ||!level.getBlockState(root).is(state.getBlock()))return null;
        if(!(level.getBlockEntity(root) instanceof PalletBlockEntity pallet)
                ||!pallet.isRoot()||!pallet.glassWrapped())return null;
        for(int i=0;i<4;i++){
            BlockPos part=PalletBlock.partPos(root,i,home);
            if(!level.hasChunkAt(part))return null;
            BlockState actual=level.getBlockState(part);
            if(actual.getBlock()!=home.getBlock()
                    ||actual.getValue(PalletBlock.PART)!=i
                    ||actual.getValue(PalletBlock.FACING)!=home.getValue(PalletBlock.FACING)
                    ||actual.getValue(PalletBlock.LEGACY)!=home.getValue(PalletBlock.LEGACY))
                return null;
        }
        return pallet;
    }
    public static boolean lift(ServerLevel level,BlockPos root,StorageGolem leader){
        PalletBlockEntity pallet=wrappedController(level,root);
        if(pallet==null||!pallet.getBlockPos().equals(root)||leader.holdsPallet())
            return false;
        ItemStack deck=pallet.createDropStack();
        if(deck.isEmpty())return false;
        NonNullList<ItemStack> contents=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)contents.set(i,pallet.getItem(i).copy());
        if(!leader.loadPallet(deck,contents))return false;
        BlockState original=level.getBlockState(root);
        // Source contents are now cleared; only the lead Golem owns them.
        pallet.clearContent();
        PalletCargoCollisionBlock.clear(level,root,original);
        for(int i=1;i<4;i++)
            level.removeBlock(PalletBlock.partPos(root,i,original),false);
        level.removeBlock(root,false);
        return true;
    }
}
