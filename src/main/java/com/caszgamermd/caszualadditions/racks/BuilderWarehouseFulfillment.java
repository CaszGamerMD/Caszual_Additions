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
    /** An order is accepted by the WAREHOUSE before touching the PTP.
     * A ticket in this saved queue never locks a pallet or changes its mode.
     * Every retry uses the same UUID; assignment is atomic on the main thread.
     */
    /** Migrate a broken old-world PTP lock back to the durable warehouse
     * queue ONLY if its pallet is missing and no physical package, shulker
     * contents or picker is active anywhere in this warehouse. Never
     * destroy actual items to repair an unready Builder order.
     */
    public static int recoverMissingPalletReservations(ServerLevel level,
            BlockPos computer){
        if(!(level.getBlockEntity(computer) instanceof WarehouseComputerBlockEntity cfg))
            return 0;
        var network=WarehouseNetwork.fromComputer(level,computer);
        for(var station:network.tanks())
            if(station.activeCount()>0||station.finishedCount()>0
                    ||station.bufferedItems()>0)return 0;
        int migrated=0;
        for(PtpBlockEntity ptp:network.points()){
            if(!ptp.builderReserved()||ptp.hasPallet()||!ptp.emptyCargo()
                    ||ptp.outputInFlight()||ptp.hauling())continue;
            var ticket=ptp.builderRequestId();
            var owner=ptp.builderOwnerId();
            var table=ptp.builderTablePos();
            Map<String,Integer> remaining=ptp.pendingOutputManifest();
            if(ticket==null||owner==null||table.equals(BlockPos.ZERO)
                    ||remaining.isEmpty())continue;
            if(cfg.queueBuilder(ticket,owner,table,remaining)
                    &&ptp.releaseUnpreparedBuilder())migrated++;
        }
        return migrated;
    }

    public static Reply request(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,Map<String,Integer> manifest){
        if(owner==null||request==null||table==null||computer==null
                ||!level.hasChunkAt(table)
                ||!BuiltInRegistries.BLOCK.getKey(level.getBlockState(table).getBlock())
                        .toString().equals("golems_plus:builder_table")
                ||!valid(level,computer)
                ||!(level.getBlockEntity(computer) instanceof WarehouseComputerBlockEntity config))
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
        recoverMissingPalletReservations(level,computer);
        var network=WarehouseNetwork.fromComputer(level,computer);
        // Idempotent live reservation, including old worlds where the PTP
        // already owns a job from before the computer queue was introduced.
        for(PtpBlockEntity ptp:network.points())
            if(ptp.builderMatches(request,owner,table,computer))
                return inspect(level,computer,table,owner,request,ptp.getBlockPos());
        if(!config.queueBuilder(request,owner,table,manifest))
            return new Reply("WAREHOUSE_ORDER_CONFLICT",BlockPos.ZERO,Map.of(),0,0);
        // The exact queued UUID survives world reload even when no pallet,
        // Hopper Adapter or shulker station is currently available.
        return assignWaiting(level,computer,config,
                config.waitingBuilder(request),lines,network);
    }

    private static Reply assignWaiting(ServerLevel level,BlockPos computer,
            WarehouseComputerBlockEntity config,
            WarehouseComputerBlockEntity.PendingBuilder pending,
            List<PtpBlockEntity.OutputOrder> lines,WarehouseNetwork.Result network){
        if(pending==null)
            return new Reply("RESERVATION_MISSING",BlockPos.ZERO,Map.of(),0,0);
        BlockPos selected=config.orderOutput();
        if(!network.shelves().isEmpty()&&!network.racks().isEmpty())
            for(PtpBlockEntity ptp:network.points()){
                if(!selected.equals(BlockPos.ZERO)
                        &&!selected.equals(ptp.getBlockPos()))continue;
                if(!ptp.readyForOutput()||!ptp.emptyCargo()
                        ||ptp.pendingOutputOrders()!=0||ptp.builderReserved()
                        ||ptp.outputInFlight()||ptp.hauling())continue;
                var station=tank(level,computer,ptp);
                if(station==null||station.activeCount()!=0
                        ||station.finishedCount()!=0||station.bufferedItems()!=0)
                    continue;
                if(!ptp.beginBuilderOutput(pending.request(),pending.owner(),
                        pending.table(),computer,lines))continue;
                config.dequeueBuilder(pending.request(),pending.owner(),pending.table());
                for(StorageGolemShelfEntity shelf:network.shelves())
                    if(shelf.docked()>0&&StorageGolemDispatch
                            .tryDispatchQueuedOutput(level,shelf))break;
                return inspect(level,computer,pending.table(),pending.owner(),
                        pending.request(),ptp.getBlockPos());
            }
        return new Reply("QUEUED_AT_WAREHOUSE",BlockPos.ZERO,Map.of(),
                lines.size(),0);
    }

    /** Human-facing diagnostic for a staged but not yet PTP-assigned order. */
    public static String waitingStatus(ServerLevel level,BlockPos computer){
        var network=WarehouseNetwork.fromComputer(level,computer);
        if(network.shelves().isEmpty())return "WAITING: CONNECT STORAGE GOLEM SHELVES";
        if(network.racks().isEmpty())return "WAITING: CONNECT STOCK RACKS";
        BlockPos selected=level.getBlockEntity(computer)
                instanceof WarehouseComputerBlockEntity cfg
                    ?cfg.orderOutput():BlockPos.ZERO;
        boolean output=false;
        boolean ready=false;
        for(PtpBlockEntity ptp:network.points()){
            if(!selected.equals(BlockPos.ZERO)
                    &&!selected.equals(ptp.getBlockPos()))continue;
            if(ptp.input())continue;
            output=true;
            if(!ptp.readyForOutput()||ptp.builderReserved()
                    ||ptp.outputInFlight()||ptp.hauling()
                    ||ptp.pendingOutputOrders()!=0||!ptp.emptyCargo())continue;
            ready=true;
            var station=tank(level,computer,ptp);
            if(station!=null&&station.activeCount()==0
                    &&station.finishedCount()==0&&station.bufferedItems()==0)
                return "WAITING: OUTPUT PALLET READY; ASSIGNING NEXT TICKET";
        }
        if(!output)return "WAITING: SET A CONNECTED PTP TO OUTPUT";
        if(!ready)return "WAITING: OUTPUT PTP NEEDS PALLET OR IS BUSY";
        return "WAITING: CONNECT SHULKER STATION TO CORNER HOPPER ADAPTER";
    }

    /** Human-readable state of the actual reserved packing lane, shown
     * on the Rack Computer beside the Builder manifest. All checks are
     * read-only and never withdraw stock or create worker entities. */
    public static String workStatus(ServerLevel level,BlockPos computer,
            PtpBlockEntity ptp){
        if(!valid(level,computer)||ptp==null)return "WAREHOUSE OFFLINE";
        if(!ptp.readyForOutput())return "OUTPUT PTP NEEDS AN UNSEALED PALLET";
        var network=WarehouseNetwork.fromComputer(level,computer);
        var station=tank(level,computer,ptp);
        if(station==null)return "NO SHULKER STATION HOPPER ADAPTER LINK";
        if(ptp.outputInFlight())return "PICKER DISPATCHED / STOCK IN TRANSIT";
        if(ptp.pendingOutputOrders()==0)
            return station.activeCount()>0||station.finishedCount()>0
                    ?"PACKING SHULKERS":"WAITING FOR BUILDER COLLECTION";
        if(station.count()==0&&station.activeCount()==0
                &&station.finishedCount()==0&&station.bufferedItems()==0)
            return "SHULKER STATION NEEDS EMPTY SHULKERS";
        int workers=0;
        int clearExits=0;
        for(StorageGolemShelfEntity shelf:network.shelves()){
            if(shelf.docked()<=0)continue;
            workers+=shelf.docked();
            if(StorageGolemShelfDoor.exit(level,shelf)!=null)clearExits++;
        }
        if(workers==0)return "NO DOCKED PICKER GOLEMS";
        if(clearExits==0)return "PICKER SHELF EXITS BLOCKED";
        var order=ptp.nextOutputOrder();
        if(order==null)return "WAITING FOR MATERIALS";
        boolean stocked=false;
        boolean reachable=false;
        for(RackBlockEntity rack:network.racks()){
            if(!rack.hasPallet())continue;
            for(int slot=0;slot<rack.getContainerSize();slot++){
                ItemStack stack=rack.getItem(slot);
                if(!stack.isEmpty()&&itemId(stack).equals(order.itemId())){
                    stocked=true;
                    if(WarehouseSortingReach.accessible(level,rack)
                            &&stack.getItem().canFitInsideContainerItems())
                        reachable=true;
                    break;
                }
            }
            if(reachable)break;
        }
        if(!stocked)return "REQUESTED BLOCKS NOT IN CONNECTED RACKS";
        if(!reachable)return "PICKER CANNOT REACH SOURCE RACK";
        return "QUEUED FOR PICKING";
    }

    public static Reply inspect(ServerLevel level,BlockPos computer,BlockPos table,
            UUID owner,UUID request,BlockPos ptpPos){
        if(valid(level,computer))recoverMissingPalletReservations(level,computer);
        if(ptpPos==null||!level.hasChunkAt(ptpPos)||!valid(level,computer))
            return new Reply("WAREHOUSE_OFFLINE",ptpPos==null?BlockPos.ZERO:ptpPos,
                    Map.of(),0,0);
        if(!(level.getBlockEntity(ptpPos) instanceof PtpBlockEntity ptp)
                ||!ptp.builderMatches(request,owner,table,computer)){
            // If an old empty PTP was safely migrated, keep the same UUID
            // and tell the Builder to clear its old PTP assignment and
            // resume polling the computer queue instead of creating copies.
            if(level.getBlockEntity(computer)
                    instanceof WarehouseComputerBlockEntity cfg){
                var pending=cfg.waitingBuilder(request);
                if(pending!=null&&pending.owner().equals(owner)
                        &&pending.table().equals(table))
                    return new Reply("QUEUED_AT_WAREHOUSE",
                            BlockPos.ZERO,Map.of(),pending.manifest().size(),0);
            }
            return new Reply("RESERVATION_MISSING",ptpPos,Map.of(),0,0);
        }
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
