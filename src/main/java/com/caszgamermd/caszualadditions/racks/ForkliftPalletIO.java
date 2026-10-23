package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.GlassPalletWrap;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletCargoCollisionBlock;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.state.BlockState;

/** Transactional, full-inventory pallet exchanges for a forklift.
 * The 217-slot pallet-jack container is an intermediate OWNER, never
 * a copy: slot 0 is the wrapped deck, 1..216 are original cargo slots. */
public final class ForkliftPalletIO {
    private ForkliftPalletIO(){}
    public static boolean fits(ServerLevel world,BlockPos root,Direction face){
        if(world.isOutsideBuildHeight(root)||!world.getWorldBorder().isWithinBounds(root))
            return false;
        // A real 2x2 ground pallet is allowed to occupy an otherwise empty
        // rack-approach row temporarily. In a three-block aisle, EVERY
        // possible 2-deep footprint overlaps one of the opposite racks'
        // reserved access rows, even when there is no physical obstruction.
        // This fit test is for actual forklift staging, not rack placement.
        for(int part=0;part<4;part++){
            BlockPos at=PalletBlock.partPos(root,part,
                    com.caszgamermd.caszualadditions.pallet.PalletContent.WOODEN_PALLET.defaultBlockState()
                            .setValue(PalletBlock.PART,0)
                            .setValue(PalletBlock.FACING,face));
            if(!world.hasChunkAt(at)||!world.hasChunkAt(at.below())
                    ||!world.getBlockState(at).canBeReplaced()
                    ||!world.getBlockState(at.below()).isCollisionShapeFullBlock(world,at.below()))
                return false;
        }
        return true;
    }
    public static boolean liftGround(ServerLevel world,BlockPos root,ItemStack jack){
        if(!jack.is(RackContent.PALLET_JACK)||PalletJackItem.loaded(jack))return false;
        PalletBlockEntity pallet=StorageGroundPalletTransfer.wrappedController(world,root);
        if(pallet==null||!pallet.getBlockPos().equals(root))return false;
        BlockState state=world.getBlockState(root);
        ItemStack deck=pallet.createDropStack();
        if(deck.isEmpty()||!GlassPalletWrap.wrapped(deck))return false;
        NonNullList<ItemStack> payload=NonNullList.withSize(217,ItemStack.EMPTY);
        payload.set(0,deck.copyWithCount(1));
        for(int i=0;i<216;i++)payload.set(i+1,pallet.getItem(i).copy());
        // The Jack becomes the one saved owner before any world blocks go.
        jack.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(payload));
        pallet.clearContent();
        PalletCargoCollisionBlock.clear(world,root,state);
        for(int i=3;i>=0;i--){
            BlockPos at=PalletBlock.partPos(root,i,state);
            if(world.getBlockState(at).is(state.getBlock()))
                world.removeBlock(at,false);
        }
        return true;
    }
    public static boolean placeGround(ServerLevel world,BlockPos root,Direction face,
            ItemStack jack){
        if(!PalletJackItem.loaded(jack))return false;
        NonNullList<ItemStack> payload=PalletJackItem.unpack(jack);
        ItemStack deck=payload.get(0);
        if(!(deck.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block)
                ||!GlassPalletWrap.wrapped(deck)||!fits(world,root,face))return false;
        BlockState initial=block.defaultBlockState().setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.LEGACY,false).setValue(PalletBlock.FACING,face);
        int made=0;
        for(int i=0;i<4;i++){
            if(!world.setBlock(PalletBlock.partPos(root,i,initial),
                    initial.setValue(PalletBlock.PART,i),3))break;
            made++;
        }
        if(made!=4){
            for(int i=made-1;i>=0;i--)
                world.removeBlock(PalletBlock.partPos(root,i,initial),false);
            return false;
        }
        PalletBlockEntity target=world.getBlockEntity(root)
                instanceof PalletBlockEntity p?p:null;
        if(target==null){
            for(int i=3;i>=0;i--)
                world.removeBlock(PalletBlock.partPos(root,i,initial),false);
            return false;
        }
        NonNullList<ItemStack> stock=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)stock.set(i,payload.get(i+1).copy());
        target.initialize(root,0,deck);
        target.restoreJackCargo(stock);
        // Only the placed pallet owns inventory after a successful commit.
        jack.remove(DataComponents.CONTAINER);
        return true;
    }
}
