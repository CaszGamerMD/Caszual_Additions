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
    /**
     * Remote swap: the moving crew owns one fresh, EMPTY pallet. Capture the
     * wrapped original + all 216 stacks, put the fresh pallet in its exact
     * four-block footprint, then exchange the crew's cargo identity. A failed
     * placement restores the old pallet and inventory, never creating a
     * duplicate or dropping either payload.
     */
    public static boolean exchange(ServerLevel level,BlockPos root,MovingPallet carrier){
        PalletBlockEntity source=wrappedController(level,root);
        if(source==null||!source.getBlockPos().equals(root)
                ||!carrier.freshPalletOnly()||!carrier.remoteExchangeActive())
            return false;
        ItemStack fresh=carrier.displayPallet();
        if(!(fresh.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)
                ||!(blockItem.getBlock() instanceof PalletBlock newBlock))
            return false;
        BlockState previous=level.getBlockState(root);
        ItemStack oldDeck=source.createDropStack();
        NonNullList<ItemStack> stock=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)stock.set(i,source.getItem(i).copy());
        BlockState[] oldStates=new BlockState[4];
        for(int i=0;i<4;i++){
            BlockPos position=PalletBlock.partPos(root,i,previous);
            if(!level.hasChunkAt(position))return false;
            oldStates[i]=level.getBlockState(position);
        }
        BlockState replacement=newBlock.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,previous.getValue(PalletBlock.FACING))
                .setValue(PalletBlock.LEGACY,previous.getValue(PalletBlock.LEGACY));
        // Old cargo belongs to the transaction until commit. Clear the
        // existing inventory before changing its root block entity.
        source.clearContent();
        PalletCargoCollisionBlock.clear(level,root,previous);
        boolean success=true;
        for(int i=0;i<4;i++){
            BlockPos position=PalletBlock.partPos(root,i,previous);
            if(!level.setBlock(position,replacement.setValue(PalletBlock.PART,i),3)){
                success=false;break;
            }
        }
        PalletBlockEntity placed=level.getBlockEntity(root)
                instanceof PalletBlockEntity p?p:null;
        if(placed==null)success=false;
        if(success){
            placed.initialize(root,0,fresh);
            success=placed.isEmpty();
        }
        if(success&&carrier.acceptRecoveredPallet(oldDeck,stock))return true;

        // Rebuild the original exactly if even one part of the empty
        // replacement failed. Ownership has not transferred to the rig.
        for(int i=0;i<4;i++){
            BlockPos part=PalletBlock.partPos(root,i,previous);
            level.setBlock(part,oldStates[i],3);
        }
        PalletBlockEntity restored=level.getBlockEntity(root)
                instanceof PalletBlockEntity p?p:null;
        if(restored!=null){
            restored.initialize(root,0,oldDeck);
            for(int i=0;i<216;i++)
                if(!stock.get(i).isEmpty())restored.setItem(i,stock.get(i).copy());
            PalletCargoCollisionBlock.refresh(level,root);
        }else{
            // A severely damaged world can refuse the old block as well.
            // Never delete cargo: release recoverable drops at this site.
            net.minecraft.world.Containers.dropItemStack(level,root.getX()+.5,
                    root.getY()+.5,root.getZ()+.5,oldDeck);
            for(ItemStack stack:stock)if(!stack.isEmpty())
                net.minecraft.world.Containers.dropItemStack(level,root.getX()+.5,
                        root.getY()+.5,root.getZ()+.5,stack);
        }
        return false;
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
