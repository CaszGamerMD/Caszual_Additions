package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * Non-destructive, loaded-chunk-only pallet inspection and virtual rack-fit
 * simulation. Scan results are advisory; all actual item transfers remain
 * server-validated by the Storage Golem inventory path.
 */
public final class StorageScanner {
    public record Report(BlockPos point,int items,int occupiedSlots,int itemTypes,
            int storable,int overflow,int racks,boolean sealed,boolean loaded) {
        public String summary(){
            if(!loaded)return "SCANNER: no pallet on transfer point";
            return "SCANNER "+(sealed?"SEALED ":"")+items+" items / "+itemTypes
                    +" types / "+occupiedSlots+" slots | capacity "
                    +storable+" fit, "+overflow+" unassigned / "+racks+" racks";
        }
    }
    private StorageScanner(){}

    /**
     * Resolve a PTP without loading its controller chunk if a 3x3 structure
     * happens to straddle a chunk boundary.
     */
    private static PtpBlockEntity loadedPoint(ServerLevel level,BlockPos cell){
        if(!level.hasChunkAt(cell))return null;
        var state=level.getBlockState(cell);
        if(!(state.getBlock() instanceof PtpBlock))return null;
        int part=state.getValue(PtpBlock.PART);
        if(part==0)return null;
        BlockPos center=PtpBlock.root(cell,part).offset(1,0,1);
        return level.hasChunkAt(center)
                &&level.getBlockEntity(center) instanceof PtpBlockEntity ptp?ptp:null;
    }
    /** The scanner can touch any of a PTP's nine platform blocks. */
    public static PtpBlockEntity nearbyPoint(ServerLevel level,BlockPos scanner){
        for(Direction direction:Direction.values()){
            BlockPos adjacent=scanner.relative(direction);
            if(!level.hasChunkAt(adjacent))continue;
            PtpBlockEntity point=loadedPoint(level,adjacent);
            if(point!=null)return point;
        }
        return null;
    }
    private static BlockPos connectedComputer(ServerLevel level,
            BlockPos scanner,PtpBlockEntity point) {
        if(point==null)return null;
        List<BlockPos> contacts=new ArrayList<>(12);
        contacts.add(scanner);
        // Look at every physical platform part, not just the center.
        BlockPos root=point.getBlockPos().offset(-1,0,-1);
        for(int part=1;part<=9;part++)contacts.add(PtpBlock.cell(root,part));
        Set<BlockPos> visited=new HashSet<>();
        for(BlockPos cell:contacts){
            for(Direction direction:Direction.values()){
                BlockPos candidate=cell.relative(direction);
                if(!visited.add(candidate)||!level.hasChunkAt(candidate))continue;
                if(level.getBlockState(candidate).is(RackContent.RACK_COMPUTER))
                    return candidate;
                if(level.getBlockEntity(candidate) instanceof StorageGolemShelfEntity){
                    for(StorageGolemShelfEntity shelf:StorageGolemShelfEntity.linked(level,candidate)){
                        for(Direction face:Direction.values()){
                            BlockPos computer=shelf.getBlockPos().relative(face);
                            if(level.hasChunkAt(computer)
                                    &&level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
                                return computer;
                        }
                    }
                }
            }
        }
        return null;
    }
    private static final class VirtualRack {
        final RackBlockEntity rack;
        final ItemStack[] slots;
        VirtualRack(RackBlockEntity rack){
            this.rack=rack;
            this.slots=new ItemStack[rack.getContainerSize()];
            for(int i=0;i<slots.length;i++)slots[i]=rack.getItem(i).copy();
        }
        int takeRoom(ItemStack sample,int count,int pass){
            if(!rack.canPlaceItem(0,sample))return count;
            for(int i=0;i<slots.length && count>0;i++){
                ItemStack previous=slots[i];
                if(pass==0){
                    if(previous.isEmpty()
                            ||!ItemStack.isSameItemSameComponents(previous,sample))continue;
                }else if(!previous.isEmpty())continue;
                int room=previous.isEmpty()?sample.getMaxStackSize()
                        :previous.getMaxStackSize()-previous.getCount();
                if(room<=0)continue;
                int transferred=Math.min(room,count);
                if(previous.isEmpty())slots[i]=sample.copyWithCount(transferred);
                else previous.grow(transferred);
                count-=transferred;
            }
            return count;
        }
    }
    public static Report scan(ServerLevel level,BlockPos scanner){
        PtpBlockEntity point=nearbyPoint(level,scanner);
        if(point==null)return null;
        List<ItemStack> cargo=new ArrayList<>();
        Set<String> itemTypes=new HashSet<>();
        int count=0,occupied=0;
        if(point.hasPallet()){
            // getItem() intentionally hides sealed inventories from automation.
            // The scanner must not unseal or mutate a pallet to inspect it.
            for(int i=0;i<point.getContainerSize();i++){
                ItemStack stack=point.displayItem(i);
                if(stack.isEmpty())continue;
                cargo.add(stack);
                occupied++;
                count+=stack.getCount();
                itemTypes.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        BlockPos computer=connectedComputer(level,scanner,point);
        List<VirtualRack> racks=new ArrayList<>();
        if(computer!=null)
            for(RackBlockEntity rack:RackNetwork.fromComputer(level,computer))
                if(rack.hasPallet())racks.add(new VirtualRack(rack));
        int room=0;
        for(ItemStack stack:cargo){
            int remaining=stack.getCount();
            // Snapshot partial stacks first; then allocate empty slots.
            for(int pass=0;pass<2 && remaining>0;pass++)
                for(VirtualRack rack:racks){
                    remaining=rack.takeRoom(stack,remaining,pass);
                    if(remaining==0)break;
                }
            room+=stack.getCount()-remaining;
        }
        return new Report(point.getBlockPos(),count,occupied,itemTypes.size(),
                room,count-room,racks.size(),point.sealed(),point.hasPallet());
    }
    public static String reportForComputer(ServerLevel level,BlockPos computer,
            List<StorageGolemShelfEntity> shelves) {
        Set<BlockPos> cells=new HashSet<>();
        cells.add(computer);
        for(StorageGolemShelfEntity shelf:shelves)cells.add(shelf.getBlockPos());
        Set<BlockPos> seenPoints=new HashSet<>();
        for(BlockPos base:cells)
            for(Direction side:Direction.values()){
                BlockPos neighbor=base.relative(side);
                if(!level.hasChunkAt(neighbor))continue;
                PtpBlockEntity point=loadedPoint(level,neighbor);
                if(point==null||!seenPoints.add(point.getBlockPos()))continue;
                BlockPos origin=point.getBlockPos().offset(-1,0,-1);
                for(int part=1;part<=9;part++){
                    BlockPos at=PtpBlock.cell(origin,part);
                    for(Direction scanSide:Direction.values()){
                        BlockPos scanner=at.relative(scanSide);
                        if(!level.hasChunkAt(scanner)
                                ||!level.getBlockState(scanner).is(RackContent.STORAGE_SCANNER))
                            continue;
                        Report report=scan(level,scanner);
                        if(report!=null)return report.summary();
                    }
                }
            }
        return "";
    }
}
