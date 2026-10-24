package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The warehouse ID remains the computer's immutable position, while its
 * player-assigned name survives world reloads and item registry changes. */
public final class WarehouseComputerBlockEntity extends BlockEntity {
    private String name="Warehouse";
    // Server-owned, per-warehouse settings; no spawn/virtual item creation.
    private int orderPackers=1;
    private BlockPos orderOutput=BlockPos.ZERO;
    // Pending construction orders belong to the Rack Computer, NOT a PTP.
    // The latter is reserved only once a configured pallet can actually
    // receive boxes. This queue is saved through unloads and server restarts.
    public record PendingBuilder(java.util.UUID request,java.util.UUID owner,
            BlockPos table,java.util.Map<String,Integer> manifest){
        public PendingBuilder{manifest=java.util.Map.copyOf(manifest);}
    }
    private final java.util.LinkedHashMap<java.util.UUID,PendingBuilder> waitingBuilders=
            new java.util.LinkedHashMap<>();
    public java.util.List<PendingBuilder> waitingBuilders(){
        return java.util.List.copyOf(waitingBuilders.values());
    }
    public PendingBuilder waitingBuilder(java.util.UUID request){
        return waitingBuilders.get(request);
    }
    public boolean queueBuilder(java.util.UUID request,java.util.UUID owner,
            BlockPos table,java.util.Map<String,Integer> manifest){
        if(request==null||owner==null||table==null||manifest==null
                ||manifest.isEmpty()||manifest.size()>16)return false;
        PendingBuilder existing=waitingBuilders.get(request);
        if(existing!=null)return existing.owner().equals(owner)
                &&existing.table().equals(table)
                &&existing.manifest().equals(manifest);
        // A single Planning Table has only one outstanding supply ticket.
        // Refuse an obsolete second ID instead of hiding a duplicate order.
        for(PendingBuilder pending:waitingBuilders.values())
            if(pending.table().equals(table))return false;
        if(waitingBuilders.size()>=12)return false;
        waitingBuilders.put(request,new PendingBuilder(request,owner,
                table.immutable(),manifest));
        setChanged();
        return true;
    }
    public boolean dequeueBuilder(java.util.UUID request,java.util.UUID owner,
            BlockPos table){
        PendingBuilder pending=waitingBuilders.get(request);
        if(pending==null||!pending.owner().equals(owner)
                ||!pending.table().equals(table))return false;
        waitingBuilders.remove(request);
        setChanged();return true;
    }
    public int orderPackers(){return orderPackers;}
    public BlockPos orderOutput(){return orderOutput;}
    public void configurePackers(int count){
        orderPackers=Math.clamp(count,1,4);setChanged();
    }
    public void selectOrderOutput(BlockPos ptp){
        orderOutput=ptp==null?BlockPos.ZERO:ptp.immutable();
        setChanged();
    }
    public WarehouseComputerBlockEntity(BlockPos pos,BlockState state){
        super(RackContent.COMPUTER_ENTITY,pos,state);
    }
    public String warehouseName(){return name;}
    public void rename(String requested){
        String sanitized=requested==null?"":requested.strip()
                .replaceAll("[\\p{Cntrl}]", "");
        if(sanitized.length()>40)sanitized=sanitized.substring(0,40);
        name=sanitized.isBlank()?"Warehouse":sanitized;
        setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),
                getBlockState(),3);
    }
    public static String name(ServerLevel level,BlockPos pos){
        return level.getBlockEntity(pos) instanceof WarehouseComputerBlockEntity be
                ?be.warehouseName():"Warehouse";
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putString("warehouse_name",name);
        out.putInt("order_packers",orderPackers);
        out.store("order_output_ptp",BlockPos.CODEC,orderOutput);
        out.putInt("builder_waiting_count",waitingBuilders.size());
        int index=0;
        for(PendingBuilder pending:waitingBuilders.values()){
            String prefix="builder_waiting_"+index+"_";
            out.putString(prefix+"ticket",pending.request().toString());
            out.putString(prefix+"owner",pending.owner().toString());
            out.store(prefix+"table",BlockPos.CODEC,pending.table());
            out.putInt(prefix+"lines",pending.manifest().size());
            int line=0;
            for(var item:pending.manifest().entrySet()){
                out.putString(prefix+"item_"+line,item.getKey());
                out.putInt(prefix+"qty_"+line,item.getValue());line++;
            }
            index++;
        }
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        name=in.getStringOr("warehouse_name","Warehouse");
        orderPackers=Math.clamp(in.getIntOr("order_packers",1),1,4);
        orderOutput=in.read("order_output_ptp",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        waitingBuilders.clear();
        int count=Math.clamp(in.getIntOr("builder_waiting_count",0),0,12);
        for(int i=0;i<count;i++){
            String prefix="builder_waiting_"+i+"_";
            try{
                var ticket=java.util.UUID.fromString(in.getStringOr(prefix+"ticket",""));
                var owner=java.util.UUID.fromString(in.getStringOr(prefix+"owner",""));
                BlockPos table=in.read(prefix+"table",BlockPos.CODEC).orElse(null);
                if(table==null)continue;
                java.util.Map<String,Integer> manifest=new java.util.LinkedHashMap<>();
                int size=Math.clamp(in.getIntOr(prefix+"lines",0),0,16);
                for(int line=0;line<size;line++){
                    String item=in.getStringOr(prefix+"item_"+line,"");
                    int qty=in.getIntOr(prefix+"qty_"+line,0);
                    if(!item.isBlank()&&qty>=1&&qty<=4096)
                        manifest.put(item,qty);
                }
                if(!manifest.isEmpty())
                    waitingBuilders.put(ticket,new PendingBuilder(ticket,owner,
                            table.immutable(),manifest));
            }catch(IllegalArgumentException ignored){}
        }
    }
}
