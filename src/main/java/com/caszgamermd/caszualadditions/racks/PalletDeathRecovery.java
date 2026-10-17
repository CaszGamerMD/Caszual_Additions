package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import java.util.ArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Death recovery of a carried pallet is a SINGLE durable inventory transfer:
 *  - install one intact 2x2 pallet + exactly its 216 saved slots nearby, OR
 *  - spawn ONE pallet item with those 216 indexed slots in its container
 *    component, restored on placement by PalletBlockEntity.initialize().
 *
 * Never eject 216 loose stacks or replace someone else's blocks. In
 * particular, never try to stand on the thin deck of an adjacent pallet.
 */
public final class PalletDeathRecovery {
    private PalletDeathRecovery(){}

    public static void release(ServerLevel level,BlockPos deathPosition,
            double x,double y,double z,ItemStack pallet,
            NonNullList<ItemStack> contents,Direction facing,boolean legacy){
        if(pallet.isEmpty())return;
        if(!(pallet.getItem() instanceof BlockItem blockItem)
                ||!(blockItem.getBlock() instanceof PalletBlock block)
                ||contents.size()!=PalletBlockEntity.SLOTS){
            // Non-pallet items are not part of the valid carrier contract.
            // Preserve everything rather than silently deleting unknown
            // or malformed cargo during a bad-save recovery.
            Containers.dropItemStack(level,x,y,z,pallet.copy());
            for(ItemStack item:contents)
                if(!item.isEmpty())Containers.dropItemStack(level,x,y,z,item.copy());
            return;
        }

        // Prefer the actual place of death, then progressively wider
        // nearby rings. Local +/-1 terrain steps may be suitable; do not
        // place floating platforms, underwater, inside cargo, or on racks.
        for(int radius=0;radius<=5;radius++){
            for(int dy:new int[]{0,-1,1,-2,2})
                for(int dx=-radius;dx<=radius;dx++)
                    for(int dz=-radius;dz<=radius;dz++){
                        if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                        BlockPos candidate=deathPosition.offset(dx,dy,dz);
                        if(tryPlace(level,candidate,block,pallet,contents,facing,legacy))
                            return;
                    }
        }

        // A wall, void, crowded rack, or narrow cliff must not force
        // 216 individual drops. This one item is a full pallet escrow.
        ItemStack packed=PalletBlockEntity.packedCarrierDrop(pallet,contents);
        Containers.dropItemStack(level,x,y,z,packed);
    }

    private static boolean tryPlace(ServerLevel level,BlockPos root,
            PalletBlock block,ItemStack pallet,NonNullList<ItemStack> contents,
            Direction facing,boolean legacy){
        BlockState state=block.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing)
                .setValue(PalletBlock.LEGACY,legacy);
        BlockPos[] cells=new BlockPos[4];
        for(int i=0;i<4;i++){
            BlockPos pos=PalletBlock.partPos(root,i,state);
            cells[i]=pos;
            if(level.isOutsideBuildHeight(pos)
                    ||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.hasChunkAt(pos)
                    ||!level.getBlockState(pos).canBeReplaced()
                    ||!level.getFluidState(pos).isEmpty())return false;
            BlockPos below=pos.below();
            // A pallet deck is only 5 pixels thick. Treating it as floor
            // would put the recovered pallet on top of an unrelated one.
            if(!level.getBlockState(below).isFaceSturdy(level,below,Direction.UP))
                return false;
            // The filled pallet can render and collide up to 3 blocks tall.
            // Avoid a wall/roof that would cut through its cargo.
            for(int h=1;h<=2;h++){
                BlockPos above=pos.above(h);
                if(!level.hasChunkAt(above)
                        ||!level.getBlockState(above).getCollisionShape(level,above).isEmpty()
                        ||!level.getFluidState(above).isEmpty())return false;
            }
        }
        ArrayList<BlockPos> placed=new ArrayList<>(4);
        for(int i=0;i<4;i++){
            if(!level.setBlock(cells[i],state.setValue(PalletBlock.PART,i),3)){
                rollback(level,placed,block,state);
                return false;
            }
            placed.add(cells[i]);
        }
        PalletBlockEntity dest=level.getBlockEntity(root)
                instanceof PalletBlockEntity be&&be.isRoot()?be:null;
        if(dest==null){
            rollback(level,placed,block,state);
            return false;
        }
        dest.initialize(root,0,pallet);
        boolean verified=true;
        for(int i=0;i<PalletBlockEntity.SLOTS;i++){
            ItemStack value=contents.get(i);
            if(!value.isEmpty())dest.setItem(i,value.copy());
        }
        for(int i=0;i<PalletBlockEntity.SLOTS;i++){
            ItemStack old=contents.get(i),restored=dest.getItem(i);
            if(old.isEmpty()!=restored.isEmpty()
                    ||!old.isEmpty()&&(!ItemStack.isSameItemSameComponents(old,restored)
                        ||old.getCount()!=restored.getCount())){
                verified=false;
                break;
            }
        }
        if(!verified){
            dest.clearContent();
            rollback(level,placed,block,state);
        }
        return verified;
    }
    private static void rollback(ServerLevel level,ArrayList<BlockPos> placed,
            PalletBlock block,BlockState expected){
        for(int i=placed.size()-1;i>=0;i--){
            BlockPos pos=placed.get(i);
            BlockState actual=level.getBlockState(pos);
            if(actual.getBlock()==block
                    &&actual.getValue(PalletBlock.FACING)==expected.getValue(PalletBlock.FACING)
                    &&actual.getValue(PalletBlock.LEGACY)==expected.getValue(PalletBlock.LEGACY))
                level.removeBlock(pos,false);
        }
    }
}
