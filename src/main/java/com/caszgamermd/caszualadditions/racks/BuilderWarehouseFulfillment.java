package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.ItemTags;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Public cross-mod warehouse contract. The Builder invokes these methods
 * through an optional adapter; no dependency on Golems Plus in this mod.
 * Warehouse golems and the existing physical Shulker Station do the picking.
 */
public final class BuilderWarehouseFulfillment {
    public record Reply(String state,BlockPos ptp,Map<String,Integer> missing,
                        int pendingOrders,int packedBoxes) {
        public Reply {
            missing=Map.copyOf(missing);
        }
    }
    private BuilderWarehouseFulfillment(){}

    private static String itemId(ItemStack stack){
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
    private static boolean valid(ServerLevel level,BlockPos computer){
        return level.hasChunkAt(computer)
                &&level.getBlockState(computer).is(RackContent.RACK_COMPUTER);
    }
    private static ShulkerTankEntity tank(ServerLevel level,BlockPos computer,
            PtpBlockEntity ptp){
        if(!valid(level,computer))return null;
        for(var station:WarehouseNetwork.fromComputer(level,computer).tanks())
            if(station.attachedOutput(level)==ptp)return station;
        return null;
    }
    private static Map<String,Integer> unstocked(ServerLevel level,BlockPos computer,
            Map<String,Integer> demand){
        Map<String,Integer> left=new LinkedHashMap<>(demand);
        for(var rack:WarehouseNetwork.fromComputer(level,computer).racks()){
            if(!rack.hasPallet()||rack.isEmpty())continue;
            for(int i=0;i<rack.getContainerSize();i++){
                ItemStack stack=rack.getItem(i);
                if(!stack.isEmpty()&&left.containsKey(itemId(stack)))
                    left.merge(itemId(stack),-stack.getCount(),Integer::sum);
            }
        }
        left.entrySet().removeIf(entry->entry.getValue()<=0);
        return left;
    }
    /**
     * Read-only availability for a Builder's material checklist.
     * This never reserves, removes or creates items. Returns null while
     * the warehouse computer is unavailable, so the UI can show unchecked
     * white instead of falsely reporting zero stock.
     */
    public static Map<String,Integer> available(ServerLevel level,BlockPos computer,
                                                 Map<String,Long> requested){
        if(!valid(level,computer)||requested==null||requested.size()>1024)return null;
        Map<String,Integer> stock=new LinkedHashMap<>();
        for(String id:requested.keySet()){
            Identifier parsed=Identifier.tryParse(id);
            if(parsed!=null&&BuiltInRegistries.ITEM.containsKey(parsed))
                stock.put(id,0);
        }
        for(var rack:WarehouseNetwork.fromComputer(level,computer).racks()){
            if(!rack.hasPallet()||rack.isEmpty())continue;
            for(int slot=0;slot<rack.getContainerSize();slot++){
                ItemStack item=rack.getItem(slot);
                if(!item.isEmpty()){
                    String id=itemId(item);
                    if(stock.containsKey(id))stock.merge(id,item.getCount(),
                            (a,b)->a>Integer.MAX_VALUE-b?Integer.MAX_VALUE:a+b);
                }
            }
        }
        return stock;
    }
    private static Map<String,Integer> remaining(PtpBlockEntity ptp){
        return ptp.pendingOutputManifest();
    }
    private static int packed(PtpBlockEntity ptp){
        int boxes=0;
        for(int i=0;i<ptp.getContainerSize();i++){
            ItemStack s=ptp.getItem(i);
            if(!s.isEmpty()&&s.is(ItemTags.SHULKER_BOXES)
                    &&s.has(DataComponents.CONTAINER))boxes++;
        }
        return boxes;
    }
    public static Reply request(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,Map<String,Integer> manifest){
        if(owner==null||request==null||table==null||computer==null
                ||!level.hasChunkAt(table)
                ||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(table).getBlock())
                        .toString().equals("golems_plus:builder_table")
                ||!valid(level,computer))
            return new Reply("WAREHOUSE_OFFLINE",BlockPos.ZERO,Map.of(),0,0);
        if(manifest==null||manifest.isEmpty()||manifest.size()>16)
            return new Reply("INVALID_MANIFEST",BlockPos.ZERO,Map.of(),0,0);
        List<PtpBlockEntity.OutputOrder> lines=new ArrayList<>();
        for(var entry:manifest.entrySet()){
            var id=Identifier.tryParse(entry.getKey());
            int qty=entry.getValue()==null?0:entry.getValue();
            if(id==null||qty<1||qty>4096
                    ||!BuiltInRegistries.ITEM.containsKey(id))
                return new Reply("INVALID_MANIFEST",BlockPos.ZERO,Map.of(),0,0);
            lines.add(new PtpBlockEntity.OutputOrder(id.toString(),qty));
        }
        var network=WarehouseNetwork.fromComputer(level,computer);
        if(network.shelves().isEmpty()||network.racks().isEmpty())
            return new Reply("NO_WORKERS_OR_RACKS",BlockPos.ZERO,Map.of(),0,0);
        // Idempotent retry: same request always recovers its prior PTP,
        // and cannot add duplicate output orders after a server restart.
        for(PtpBlockEntity ptp:network.points())
            if(ptp.builderMatches(request,owner,table,computer))
                return inspect(level,computer,table,owner,request,ptp.getBlockPos());
        for(PtpBlockEntity ptp:network.points()){
            if(!ptp.readyForOutput()||!ptp.emptyCargo()
                    ||ptp.pendingOutputOrders()!=0||ptp.builderReserved()
                    ||ptp.outputInFlight()||ptp.hauling())continue;
            var station=tank(level,computer,ptp);
            if(station==null||station.activeCount()!=0
                    ||station.finishedCount()!=0||station.bufferedItems()!=0)
                continue;
            if(!ptp.beginBuilderOutput(request,owner,table,computer,lines))
                continue;
            return inspect(level,computer,table,owner,request,ptp.getBlockPos());
        }
        return new Reply("NO_EMPTY_OUTPUT_PTP",BlockPos.ZERO,Map.of(),0,0);
    }
    public static Reply inspect(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,BlockPos ptpPos){
        if(ptpPos==null||!level.hasChunkAt(ptpPos)||!valid(level,computer))
            return new Reply("WAREHOUSE_OFFLINE",ptpPos==null?BlockPos.ZERO:ptpPos,
                    Map.of(),0,0);
        if(!(level.getBlockEntity(ptpPos) instanceof PtpBlockEntity ptp)
                ||!ptp.builderMatches(request,owner,table,computer))
            return new Reply("RESERVATION_MISSING",ptpPos,Map.of(),0,0);
        int boxed=packed(ptp);
        var station=tank(level,computer,ptp);
        int pending=ptp.pendingOutputOrders();
        if(station==null)
            return new Reply("SHULKER_STATION_MISSING",ptpPos,Map.of(),pending,boxed);
        Map<String,Integer> missing=ptp.outputInFlight() ? Map.of()
                :unstocked(level,computer,remaining(ptp));
        String state=boxed>0?"READY_FOR_PICKUP":
                !missing.isEmpty()?"MISSING_STOCK":
                pending>0&&station.count()==0&&station.activeCount()==0
                        &&station.finishedCount()==0&&station.bufferedItems()==0
                        &&!ptp.outputInFlight()?"MISSING_EMPTY_SHULKERS":
                pending>0?"FULFILLING":
                station.activeCount()>0||station.finishedCount()>0
                        ||station.bufferedItems()>0?"PACKING":"DELIVERED";
        return new Reply(state,ptpPos,missing,pending,boxed);
    }
    /** Physical worker calls only after it has navigated next to the PTP. */
    public static ItemStack takeBox(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,BlockPos ptpPos){
        if(!level.hasChunkAt(ptpPos)
                ||!(level.getBlockEntity(ptpPos) instanceof PtpBlockEntity ptp)
                ||!ptp.builderMatches(request,owner,table,computer)
                ||!ptp.readyForOutput())return ItemStack.EMPTY;
        for(int i=0;i<ptp.getContainerSize();i++){
            ItemStack stack=ptp.getItem(i);
            if(stack.isEmpty()||!stack.is(ItemTags.SHULKER_BOXES)
                    ||!stack.has(DataComponents.CONTAINER)
                    ||stack.get(DataComponents.CONTAINER).nonEmptyItemCopyStream()
                            .findAny().isEmpty())continue;
            return ptp.removeItem(i,1);
        }
        return ItemStack.EMPTY;
    }
    /** Can release only after all worker deliveries and boxed inventory clear. */
    public static boolean release(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,BlockPos ptpPos){
        if(!level.hasChunkAt(ptpPos)
                ||!(level.getBlockEntity(ptpPos) instanceof PtpBlockEntity ptp)
                ||!ptp.builderMatches(request,owner,table,computer)
                ||!ptp.emptyCargo()||ptp.pendingOutputOrders()!=0
                ||ptp.outputInFlight())return false;
        var station=tank(level,computer,ptp);
        if(station==null||station.activeCount()>0
                ||station.finishedCount()>0||station.bufferedItems()>0)return false;
        return ptp.releaseBuilderOutput(request,owner,table,computer);
    }
}
