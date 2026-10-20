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
     * Phase 1: three Golems take the wrapped TARGET pallet into the ONE
     * MovingPallet entity. The old 216 slots never visit an inventory,
     * the ground is cleared, and the empty-pallet courier keeps ownership
     * of its own unwrapped item until the footprint is vacant.
     */
    public static boolean liftForRemoteExchange(ServerLevel level,BlockPos root,
            MovingPallet carrier){
        PalletBlockEntity source=wrappedController(level,root);
        if(source==null||!source.getBlockPos().equals(root)
                ||!carrier.acceptsRemoteLift(root))return false;
        BlockState previous=level.getBlockState(root);
        ItemStack oldDeck=source.createDropStack();
        NonNullList<ItemStack> stock=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)stock.set(i,source.getItem(i).copy());
        if(!carrier.acceptRemoteLift(root,previous,oldDeck,stock))return false;
        // The client sees only the existing 3-Golem loaded entity once the
        // ground cargo has been cleared, not duplicate physical inventory.
        source.clearContent();
        PalletCargoCollisionBlock.clear(level,root,previous);
        for(int i=1;i<4;i++)
            level.removeBlock(PalletBlock.partPos(root,i,previous),false);
        level.removeBlock(root,false);
        return true;
    }

    /**
     * Phase 2: the courier deposits its real empty ItemStack only AFTER the
     * three loaded Golems are visibly clear of the entire old 2x2 footprint.
     * A temporary obstruction leaves the empty item in the courier's
     * persisted cargo; no replacement or original pallet is lost.
     */
    public static boolean placeRemoteReplacement(ServerLevel level,BlockPos root,
            MovingPallet carrier,StorageGolem courier,ItemStack fresh){
        if(!carrier.readyForCourierPlacement()
                ||!courier.remoteCourierActive()||fresh.isEmpty()
                ||!(fresh.getItem() instanceof net.minecraft.world.item.BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return false;
        // No more placing a replacement remotely from an unrelated
        // pallet's service aisle, even if the rig is already clear of the
        // actual destination. Courier must stand beside its SAVED 2x2
        // target footprint with a real clear two-wide pushing lane.
        BlockPos[] assignedFootprint=RemotePalletCourierRoute.targetFootprint(
                level,root,carrier,fresh);
        if(!RemotePalletCourierRoute.nearTargetService(
                    courier.blockPosition(),assignedFootprint)
                ||!RemotePalletCourierRoute.clear(
                    level,courier.blockPosition()))
            return false;
        BlockState placed=block.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,carrier.remoteOldFacing())
                .setValue(PalletBlock.LEGACY,carrier.remoteOldLegacy());
        // Never overwrite the target or whatever is now occupying its cells.
        BlockPos[] cells=new BlockPos[4];
        for(int i=0;i<4;i++){
            cells[i]=PalletBlock.partPos(root,i,placed);
            if(!level.hasChunkAt(cells[i])
                    ||!level.getBlockState(cells[i]).canBeReplaced())return false;
        }
        // Geometry checked in the carrier's original-orientation 2x2
        // footprint (not root+1, which can point in the WRONG direction
        // on rotated pallets). The coordinator only enables placement
        // after the three-Golem entity has left the actual volume.
        if(!carrier.remoteFootprintClear())return false;
        for(int i=0;i<4;i++){
            if(level.setBlock(cells[i],placed.setValue(PalletBlock.PART,i),3))continue;
            for(int j=i-1;j>=0;j--)level.removeBlock(cells[j],false);
            return false;
        }
        PalletBlockEntity installed=level.getBlockEntity(root)
                instanceof PalletBlockEntity p?p:null;
        if(installed==null){
            for(BlockPos pos:cells)level.removeBlock(pos,false);
            return false;
        }
        installed.initialize(root,0,fresh);
        if(!carrier.confirmCourierPlacement(courier.getUUID())){
            for(BlockPos pos:cells)level.removeBlock(pos,false);
            return false;
        }
        return true;
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
