package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletFilterCarrier;
import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import java.util.ArrayDeque;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** One 216-slot pallet and filter controller for all nine transfer cells. */
public final class PtpBlockEntity extends BlockEntity
        implements Container, ExtendedMenuProvider<BlockPos>, PalletFilterCarrier {
    public static final int SLOTS=216;
    private NonNullList<ItemStack> contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private ItemStack pallet=ItemStack.EMPTY,filterUpgrade=ItemStack.EMPTY;
    private int filterMode,filterScope;
    private String filterTarget="",filterSignature="";
    private boolean input=true,sealed;
    // Orders belong to the output endpoint, not a remote/global virtual hub.
    // A persisted reservation prevents multiple walking workers from
    // withdrawing more than the head order requests.
    public record OutputOrder(String itemId,int remaining) {}
    public static final int MAX_OUTPUT_ORDERS=16;
    private final ArrayDeque<OutputOrder> outputOrders=new ArrayDeque<>();
    private int activeTicket,ticketSequence,reservedOutput;
    // Leases survive reload. A missing worker cannot permanently block FIFO.
    private long ticketDeadline;
    private static final long TICKET_LEASE_TICKS=2400; // two minutes
    // A wrapped pallet is owned by this sealed PTP during the entire haul.
    // The four walking Golems are an escort, NEVER a second inventory owner.
    private int haulSequence,haulTicket,haulSourceMask,haulArrivalMask;
    private BlockPos haulRack=BlockPos.ZERO;
    private String haulPlayer="";
    private boolean playerHaul,haulCargoMoved;
    private long haulDeadline;
    private static final long HAUL_LEASE_TICKS=6000; // five minutes

    private void expireHaul(){
        if(haulTicket!=0&&level!=null&&level.getGameTime()>haulDeadline)
            cancelHaul();
    }
    public boolean hauling(){expireHaul();return haulTicket!=0;}
    public BlockPos haulDestination(){return haulRack;}
    public int haulWorkersAtSource(){return hauling()?WarehouseTeamProgress.count(haulSourceMask):0;}
    public int haulWorkersAtDestination(){return hauling()?WarehouseTeamProgress.count(haulArrivalMask):0;}
    public boolean playerHaul(){return hauling()&&playerHaul;}
    public String haulPhase(){return !hauling()?"IDLE":
            WarehouseTeamProgress.complete(haulSourceMask)?"DELIVERING":"ASSEMBLING";}
    public boolean validHaul(int ticket,BlockPos rack){
        expireHaul();
        return ticket!=0&&ticket==haulTicket&&haulRack.equals(rack)
                &&sealed&&hasPallet();
    }
    public void renewHaul(int ticket){
        if(haulTicket!=0&&ticket==haulTicket&&level!=null)
            haulDeadline=level.getGameTime()+HAUL_LEASE_TICKS;
    }
    /** Only dispatch on an assembled wrapped pallet, never an OUTPUT order. */
    public int beginPlayerHaul(java.util.UUID owner){
        int ticket=beginHaul(worldPosition);
        if(ticket!=0){
            haulPlayer=owner.toString();
            playerHaul=true;
            haulCargoMoved=false;
            setChanged();
        }
        return ticket;
    }
    public boolean playerHaulValid(int ticket,java.util.UUID owner){
        expireHaul();
        return ticket!=0&&ticket==haulTicket&&playerHaul
                &&haulPlayer.equals(owner.toString())&&!haulCargoMoved
                &&sealed&&hasPallet();
    }
    public boolean lifted(int ticket){return playerHaul&&haulCargoMoved&&haulTicket==ticket;}
    /** The leader becomes the ONLY inventory owner when the entire team
     * reaches the wrapped pallet. The original PTP empties in this tick. */
    public boolean giveToCarrier(ServerLevel server,StorageGolem leader,int ticket,
            java.util.UUID owner){
        if(!playerHaulValid(ticket,owner)||haulStage(ticket)!=1
                ||leader==null||!leader.isPlayerCarrier(owner,ticket))return false;
        ItemStack deck=com.caszgamermd.caszualadditions.pallet.GlassPalletWrap
                .withWrap(PalletData.applyFilterSettings(pallet,this),true);
        if(deck.isEmpty()||!leader.loadPallet(deck,contents))return false;
        contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        pallet=ItemStack.EMPTY;
        filterUpgrade=ItemStack.EMPTY;
        filterTarget="";filterSignature="";filterMode=0;filterScope=0;
        sealed=false;
        haulCargoMoved=true;
        // No hard lock on the empty PTP; surviving team state is persisted
        // on its four real walking Golems, including the one cargo owner.
        cancelHaul();
        setChanged();
        return true;
    }
    public int beginHaul(BlockPos destination){
        expireHaul();
        if(level==null||haulTicket!=0||!sealed||!hasPallet()
                ||destination==null||!outputOrders.isEmpty()||activeTicket!=0)
            return 0;
        haulSequence=haulSequence==Integer.MAX_VALUE?1:haulSequence+1;
        haulTicket=haulSequence;haulRack=destination.immutable();
        haulSourceMask=0;haulArrivalMask=0;
        haulDeadline=level.getGameTime()+HAUL_LEASE_TICKS;
        setChanged();
        return haulTicket;
    }
    public int haulStage(int ticket){
        expireHaul();
        if(haulTicket==0||haulTicket!=ticket)return -1;
        return WarehouseTeamProgress.complete(haulSourceMask)?1:0;
    }
    public void assembleHaul(int ticket,int worker){
        if(haulStage(ticket)<0||worker<0||worker>3)return;
        int old=haulSourceMask;
        haulSourceMask=WarehouseTeamProgress.arrive(haulSourceMask,worker);
        if(old!=haulSourceMask)setChanged();
    }
    /** Idempotent: fourth worker's arrival alone attempts the pallet commit. */
    public boolean arriveHaul(int ticket,int worker,ServerLevel server){
        if(haulStage(ticket)!=1||worker<0||worker>3)return false;
        int old=haulArrivalMask;
        haulArrivalMask=WarehouseTeamProgress.arrive(haulArrivalMask,worker);
        if(old!=haulArrivalMask)setChanged();
        if(WarehouseTeamProgress.complete(haulArrivalMask))return completeHaul(server);
        return false;
    }
    public void cancelHaul(){
        if(haulTicket==0)return;
        haulTicket=0;haulRack=BlockPos.ZERO;
        haulSourceMask=0;haulArrivalMask=0;haulDeadline=0;
        playerHaul=false;haulPlayer="";haulCargoMoved=false;
        setChanged();
    }
    /**
     * Commit on the server thread after all four reach the destination.
     * Until this method succeeds, the source alone owns all 216 item slots.
     * Failed validation leaves its wrapped pallet completely untouched.
     */
    private boolean completeHaul(ServerLevel server){
        if(!validHaul(haulTicket,haulRack)||!server.hasChunkAt(haulRack))return false;
        if(!(server.getBlockEntity(haulRack) instanceof RackBlockEntity rack)
                ||rack.hasPallet()||!rack.isEmpty())return false;
        ItemStack movedPallet=PalletData.applyFilterSettings(pallet,this);
        if(movedPallet.isEmpty())return false;
        // Validate cargo against the final transferred pallet filter before
        // touching the destination; don't silently reject incompatible slots.
        rack.install(movedPallet);
        if(!rack.hasPallet())return false;
        for(int i=0;i<SLOTS;i++){
            ItemStack sample=contents.get(i);
            if(!sample.isEmpty()&&!rack.canPlaceItem(i,sample)){
                rack.dropPallet();
                return false;
            }
        }
        for(int i=0;i<SLOTS;i++){
            ItemStack sample=contents.get(i);
            if(sample.isEmpty())continue;
            rack.setItem(i,sample.copy());
            ItemStack stored=rack.getItem(i);
            if(!ItemStack.isSameItemSameComponents(stored,sample)
                    ||stored.getCount()!=sample.getCount()){
                rack.clearContent();
                rack.dropPallet();
                return false;
            }
        }
        // Destination is verified: ownership moves exactly once, after all
        // 216 slots and the pallet stack were installed successfully.
        contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        pallet=ItemStack.EMPTY;filterUpgrade=ItemStack.EMPTY;
        filterTarget="";filterSignature="";filterMode=0;filterScope=0;
        sealed=false;
        cancelHaul();
        setChanged();
        return true;
    }
    private void expireTicket(){
        if(activeTicket!=0 && level!=null && level.getGameTime()>ticketDeadline){
            activeTicket=0;reservedOutput=0;ticketDeadline=0;
            setChanged();
        }
    }
    public void renewOutput(int ticket){
        if(ticket!=0&&ticket==activeTicket&&level!=null){
            ticketDeadline=level.getGameTime()+TICKET_LEASE_TICKS;
            // Heartbeats refresh in memory. Avoid chunk saves every 10 ticks;
            // the persisted lease is also refreshed at pickup/delivery.
        }
    }
    public PtpBlockEntity(BlockPos pos,BlockState state){super(RackContent.PTP_ENTITY,pos,state);}
    public boolean input(){return input;}
    public void setInput(boolean v){if(!hauling()){input=v;setChanged();}}
    public boolean sealed(){return sealed;}
    public void setSealed(boolean v){if(hasPallet()&&!hauling()){sealed=v;setChanged();}}
    public boolean hasPallet(){return !pallet.isEmpty();}
    public boolean readyForOutput(){return !input&&!sealed&&hasPallet();}
    /** Explicit workstation controls refuse to interrupt live deliveries or
     * invalidate a wrapped pallet's four-worker escrow. */
    public boolean changeModeFromComputer(){
        expireTicket();
        if(hauling()||activeTicket!=0||pendingOutputOrders()>0)return false;
        setInput(!input);
        return true;
    }
    public boolean changeWrapFromComputer(){
        expireTicket();
        if(!hasWrap()||!hasPallet()||hauling()||activeTicket!=0)return false;
        setSealed(!sealed);
        return true;
    }
    public int pendingOutputOrders(){return outputOrders.size();}
    public int pendingOutputItems(){
        int count=0;
        for(OutputOrder order:outputOrders)count+=order.remaining();
        return count;
    }
    public OutputOrder nextOutputOrder(){return outputOrders.peekFirst();}
    /** A multi-item order is one atomic submission: accept ALL lines or none,
     * so a partially full queue cannot silently drop the end of an order. */
    public boolean queueOutputList(java.util.List<OutputOrder> lines){
        if(input||hauling()||lines.isEmpty()
                ||outputOrders.size()+lines.size()>MAX_OUTPUT_ORDERS)return false;
        for(OutputOrder line:lines)
            if(line.itemId()==null||line.itemId().isBlank()
                    ||line.remaining()<1||line.remaining()>4096)return false;
        outputOrders.addAll(lines);
        setChanged();
        return true;
    }
    public boolean queueOutput(String itemId,int quantity){
        if(input||hauling()||itemId==null||itemId.isBlank()||quantity<1||quantity>4096
                ||outputOrders.size()>=MAX_OUTPUT_ORDERS)return false;
        outputOrders.addLast(new OutputOrder(itemId,quantity));
        setChanged();return true;
    }
    /** At most one outbound job per output point, across all linked shelves. */
    public int reserveOutput(String itemId,int quantity){
        expireTicket();
        OutputOrder next=outputOrders.peekFirst();
        if(!readyForOutput()||activeTicket!=0||next==null||quantity<1
                ||quantity>next.remaining()||!next.itemId().equals(itemId))return 0;
        ticketSequence=ticketSequence==Integer.MAX_VALUE?1:ticketSequence+1;
        if(ticketSequence==0)ticketSequence=1;
        activeTicket=ticketSequence;
        reservedOutput=quantity;
        ticketDeadline=level==null?0:level.getGameTime()+TICKET_LEASE_TICKS;
        setChanged();
        return activeTicket;
    }
    public boolean hasReservation(int ticket){
        expireTicket();
        return ticket!=0&&ticket==activeTicket&&reservedOutput>0;
    }
    /** Commit only items that actually reached this output pallet. */
    public void outputDelivered(int ticket,int quantity){
        if(!hasReservation(ticket)||quantity<=0)return;
        OutputOrder order=outputOrders.peekFirst();
        if(order==null)return;
        int done=Math.min(quantity,Math.min(reservedOutput,order.remaining()));
        reservedOutput-=done;
        outputOrders.removeFirst();
        if(order.remaining()>done)
            outputOrders.addFirst(new OutputOrder(order.itemId(),order.remaining()-done));
        if(reservedOutput==0){activeTicket=0;ticketDeadline=0;}
        else if(level!=null)ticketDeadline=level.getGameTime()+TICKET_LEASE_TICKS;
        setChanged();
    }
    /** A failed, recalled or killed worker must free the unfulfilled quantity. */
    public void releaseOutput(int ticket){
        if(ticket!=0&&ticket==activeTicket){
            activeTicket=0;reservedOutput=0;ticketDeadline=0;setChanged();
        }
    }
    public ItemStack palletStack(){return pallet.copy();}
    public ItemStack displayItem(int slot){return slot>=0&&slot<SLOTS?contents.get(slot).copy():ItemStack.EMPTY;}
    public boolean emptyCargo(){for(ItemStack s:contents)if(!s.isEmpty())return false;return true;}
    public boolean hasAdapter(){
        if(level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int p:new int[]{1,3,7,9})
            if(level.getBlockState(PtpBlock.cell(root,p)).is(RackContent.PTP_HOPPER))return true;
        return false;
    }
    public boolean hasWrap(){
        if(level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int p:new int[]{2,4,6,8})
            if(level.getBlockState(PtpBlock.cell(root,p)).is(RackContent.PTP_WRAP))return true;
        return false;
    }
    public boolean installPallet(ItemStack stack){
        if(hasPallet()||level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int y=1;y<=3;y++)for(int z=0;z<3;z++)for(int x=0;x<3;x++){
            BlockPos p=root.offset(x,y,z);
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)
                    ||!level.getBlockState(p).canBeReplaced())return false;
        }
        pallet=stack.copyWithCount(1);
        filterTarget=PalletData.filter(stack);
        filterMode=PalletData.filterMode(stack);
        filterScope=PalletData.filterScope(stack);
        filterSignature=PalletData.filterSignature(stack);
        filterUpgrade=PalletData.filterInstalled(stack)
                ?new ItemStack(RackContent.PALLET_FILTER):ItemStack.EMPTY;
        setChanged();return true;
    }
    public ItemStack removePallet(){
        return !sealed&&emptyCargo()?dropPallet():ItemStack.EMPTY;
    }
    public ItemStack dropPallet(){
        if(!hasPallet()||hauling())return ItemStack.EMPTY;
        ItemStack result=PalletData.applyFilterSettings(pallet,this);
        pallet=ItemStack.EMPTY;filterUpgrade=ItemStack.EMPTY;
        filterTarget="";filterSignature="";sealed=false;
        setChanged();return result;
    }
    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){return emptyCargo();}
    @Override public ItemStack getItem(int slot){
        return !sealed&&slot>=0&&slot<SLOTS?contents.get(slot):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot,int count){
        if(sealed)return ItemStack.EMPTY;
        ItemStack out=ContainerHelper.removeItem(contents,slot,count);
        if(!out.isEmpty())setChanged();return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        if(sealed)return ItemStack.EMPTY;
        ItemStack out=ContainerHelper.takeItem(contents,slot);
        if(!out.isEmpty())setChanged();return out;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return !sealed&&hasPallet()&&allows(stack);
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS||sealed||(!stack.isEmpty()&&!canPlaceItem(slot,stack)))return;
        contents.set(slot,stack.copy());setChanged();
    }
    @Override public void clearContent(){
        if(!sealed){contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    }
    @Override public boolean stillValid(Player player){
        return !sealed&&Container.stillValidBlockEntity(this,player,6.0F);
    }
    @Override public Component getDisplayName(){
        return Component.literal("Pallet Transfer Point - "+(input?"Input":"Output"));
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inv,Player player){
        return hasPallet()&&!sealed?new PalletMenu(id,inv,this):null;
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
    @Override public ItemStack filterUpgrade(){return filterUpgrade.copy();}
    @Override public void setFilterUpgrade(ItemStack value){
        filterUpgrade=value.is(RackContent.PALLET_FILTER)?value.copyWithCount(1):ItemStack.EMPTY;setChanged();
    }
    @Override public int filterMode(){return filterMode;}
    @Override public void setFilterMode(int v){filterMode=Math.clamp(v,0,1);setChanged();}
    @Override public int filterScope(){return filterScope;}
    @Override public void setFilterScope(int v){filterScope=Math.clamp(v,0,3);setChanged();}
    @Override public String filterTarget(){return filterTarget;}
    @Override public void setFilterTarget(String v){filterTarget=v;setChanged();}
    @Override public String filterComponentSignature(){return filterSignature;}
    @Override public void setFilterComponentSignature(String v){filterSignature=v;setChanged();}
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null){BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,3);}
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out,contents);
        if(hasPallet())out.store("pallet",ItemStack.CODEC,pallet);
        if(!filterUpgrade.isEmpty())out.store("filter_upgrade",ItemStack.CODEC,filterUpgrade);
        out.putString("filter_target",filterTarget);
        out.putString("filter_signature",filterSignature);
        out.putInt("filter_mode",filterMode);
        out.putInt("filter_scope",filterScope);
        out.putBoolean("input",input);out.putBoolean("sealed",sealed);
        out.putInt("output_orders",outputOrders.size());
        int orderIndex=0;
        for(OutputOrder order:outputOrders){
            out.putString("output_order_id_"+orderIndex,order.itemId());
            out.putInt("output_order_qty_"+orderIndex,order.remaining());
            orderIndex++;
        }
        out.putInt("output_ticket_sequence",ticketSequence);
        out.putInt("output_active_ticket",activeTicket);
        out.putInt("output_reserved",reservedOutput);
        out.putLong("output_ticket_deadline",ticketDeadline);
        out.putInt("haul_sequence",haulSequence);
        out.putInt("haul_ticket",haulTicket);
        out.putInt("haul_source_mask",haulSourceMask);
        out.putInt("haul_arrival_mask",haulArrivalMask);
        out.store("haul_rack",BlockPos.CODEC,haulRack);
        out.putLong("haul_deadline",haulDeadline);
        out.putBoolean("player_haul",playerHaul);
        out.putString("haul_player",haulPlayer);
        out.putBoolean("haul_cargo_moved",haulCargoMoved);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        pallet=in.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        filterUpgrade=in.read("filter_upgrade",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        filterTarget=in.getStringOr("filter_target","");
        filterSignature=in.getStringOr("filter_signature","");
        filterMode=Math.clamp(in.getIntOr("filter_mode",0),0,1);
        filterScope=Math.clamp(in.getIntOr("filter_scope",0),0,3);
        input=in.getBooleanOr("input",true);
        sealed=in.getBooleanOr("sealed",false)&&hasPallet();
        outputOrders.clear();
        int orders=Math.clamp(in.getIntOr("output_orders",0),0,MAX_OUTPUT_ORDERS);
        for(int i=0;i<orders;i++){
            String itemId=in.getStringOr("output_order_id_"+i,"");
            int quantity=in.getIntOr("output_order_qty_"+i,0);
            if(!itemId.isBlank()&&quantity>0&&quantity<=4096)
                outputOrders.addLast(new OutputOrder(itemId,quantity));
        }
        ticketSequence=Math.max(0,in.getIntOr("output_ticket_sequence",0));
        activeTicket=Math.max(0,in.getIntOr("output_active_ticket",0));
        reservedOutput=Math.max(0,in.getIntOr("output_reserved",0));
        ticketDeadline=in.getLongOr("output_ticket_deadline",0);
        if(outputOrders.isEmpty()||reservedOutput==0||activeTicket==0){
            activeTicket=0;reservedOutput=0;ticketDeadline=0;
        }else reservedOutput=Math.min(reservedOutput,outputOrders.peekFirst().remaining());
        haulSequence=Math.max(0,in.getIntOr("haul_sequence",0));
        haulTicket=Math.max(0,in.getIntOr("haul_ticket",0));
        haulSourceMask=WarehouseTeamProgress.checkedMask(in.getIntOr("haul_source_mask",0));
        haulArrivalMask=WarehouseTeamProgress.checkedMask(in.getIntOr("haul_arrival_mask",0));
        haulRack=in.read("haul_rack",BlockPos.CODEC).orElse(BlockPos.ZERO);
        haulDeadline=in.getLongOr("haul_deadline",0);
        playerHaul=in.getBooleanOr("player_haul",false);
        haulPlayer=in.getStringOr("haul_player","");
        haulCargoMoved=in.getBooleanOr("haul_cargo_moved",false);
        if(!sealed||!hasPallet()||haulRack.equals(BlockPos.ZERO)){
            haulTicket=0;haulSourceMask=0;haulArrivalMask=0;haulDeadline=0;
        }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
