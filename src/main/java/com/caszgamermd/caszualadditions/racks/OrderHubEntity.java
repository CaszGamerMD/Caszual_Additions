package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Server-authoritative order queue. Does not withdraw items unless the hub has
 * a pallet and destination capacity. At most three workers are active.
 */
public final class OrderHubEntity extends BlockEntity implements Container,ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS=216;
    public static final int MAX_ORDERS=16;
    private NonNullList<ItemStack> output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private ItemStack pallet=ItemStack.EMPTY;
    private String palletFilter="";
    private int collectors;
    private final ArrayDeque<Order> queue=new ArrayDeque<>();
    private record Order(String id,int remaining) {}
    public OrderHubEntity(BlockPos pos,BlockState state){super(RackContent.HUB_ENTITY,pos,state);}

    public boolean hasPallet(){return !pallet.isEmpty();}
    public boolean installPallet(ItemStack stack){
        if(hasPallet())return false;
        pallet=stack.copyWithCount(1);
        palletFilter=com.caszgamermd.caszualadditions.pallet.PalletData.filter(stack);
        setChanged();return true;
    }
    public ItemStack removePallet() {
        if(!isEmpty())return ItemStack.EMPTY;
        return dropPallet();
    }
    /** Breaking the hub separately drops its output inventory and pallet. */
    public ItemStack dropPallet() {
        if(!hasPallet())return ItemStack.EMPTY;
        ItemStack old=com.caszgamermd.caszualadditions.pallet.PalletData.applyFilter(
                pallet,palletFilter);
        pallet=ItemStack.EMPTY;palletFilter="";setChanged();return old;
    }
    public boolean addCollector(){
        if(collectors>=3)return false;
        collectors++;setChanged();return true;
    }
    public void removeCollector(){if(collectors>0){collectors--;setChanged();}}
    public int collectors(){return collectors;}
    public int orders(){return queue.size();}
    public boolean queue(String itemId,int amount){
        if(queue.size()>=MAX_ORDERS||amount<1||amount>4096)return false;
        queue.addLast(new Order(itemId,amount));setChanged();return true;
    }
    public static @Nullable OrderHubEntity nextToComputer(ServerLevel level,BlockPos computer){
        for(Direction side:Direction.values()){
            BlockPos at=computer.relative(side);
            if(!level.hasChunkAt(at))continue;
            OrderHubEntity hub=OrderHubBlock.controller(level,at);
            if(hub!=null)return hub;
        }
        return null;
    }
    private @Nullable ShulkerTankEntity tank(ServerLevel level) {
        for(int i=0;i<4;i++){
            BlockPos part=OrderHubBlock.cell(worldPosition,i);
            for(Direction side:Direction.values()){
                BlockPos p=part.relative(side);
                if(!level.hasChunkAt(p))continue;
                ShulkerTankEntity tank=ShulkerTankBlock.controller(level,p);
                if(tank!=null)return tank;
            }
        }
        return null;
    }
    private @Nullable BlockPos adjacentComputer(ServerLevel level){
        for(int i=0;i<4;i++){
            BlockPos part=OrderHubBlock.cell(worldPosition,i);
            for(Direction side:Direction.values()){
                BlockPos p=part.relative(side);
                if(level.hasChunkAt(p)&&level.getBlockState(p).is(RackContent.RACK_COMPUTER))
                    return p;
            }
        }
        return null;
    }
    private boolean accepts(ItemStack sample) {
        return hasPallet() && (palletFilter.isBlank()
            || BuiltInRegistries.ITEM.getKey(sample.getItem()).toString().equals(palletFilter));
    }
    private int freeSlot(ItemStack sample) {
        if(!accepts(sample))return -1;
        // Reuse matching partial stacks before consuming new pallet slots.
        for(int i=0;i<SLOTS;i++){
            ItemStack stack=output.get(i);
            if(!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack,sample)
                    && stack.getCount()<stack.getMaxStackSize())return i;
        }
        for(int i=0;i<SLOTS;i++)if(output.get(i).isEmpty())return i;
        return -1;
    }
    /** Consume only from the connected rack network and only matching IDs. */
    private static ItemStack take(ServerLevel level,List<RackBlockEntity> racks,Item target,int max) {
        for(RackBlockEntity rack:racks){
            if(!rack.hasPallet())continue;
            for(int i=0;i<rack.getContainerSize();i++){
                ItemStack stack=rack.getItem(i);
                if(stack.isEmpty()||stack.getItem()!=target)continue;
                return rack.removeItem(i,Math.min(max,stack.getCount()));
            }
        }
        return ItemStack.EMPTY;
    }
    /** Server-level inventory transfers, never force chunk loads. */
    public static void serverTick(ServerLevel level,BlockPos pos,OrderHubEntity hub){
        if(level.getGameTime()%10!=0||!hub.hasPallet()||hub.collectors==0||hub.queue.isEmpty())return;
        BlockPos computer=hub.adjacentComputer(level);
        if(computer==null)return;
        List<RackBlockEntity> racks=RackNetwork.fromComputer(level,computer);
        if(racks.isEmpty())return;

        for(int worker=0;worker<hub.collectors && !hub.queue.isEmpty();worker++){
            Order order=hub.queue.peekFirst();
            Identifier id=Identifier.tryParse(order.id());
            if(id==null){hub.queue.removeFirst();hub.setChanged();continue;}
            Item target=BuiltInRegistries.ITEM.getValue(id);
            if(target==null||target==Items.AIR){
                hub.queue.removeFirst();hub.setChanged();continue;
            }
            ShulkerTankEntity tank=hub.tank(level);
            // Any empty named/color shulker can carry up to 27 regular item
            // stacks, retaining every original stack's custom components.
            ItemStack emptyPreview=tank==null?ItemStack.EMPTY:tank.peek();
            boolean pack=!emptyPreview.isEmpty() && target.canFitInsideContainerItems()
                    && hub.freeSlot(emptyPreview)>=0;
            if(pack){
                int slot=hub.freeSlot(emptyPreview);
                if(slot<0 || !hub.output.get(slot).isEmpty())continue;
                ItemStack empty=tank.take();
                if(empty.isEmpty())continue;
                List<ItemStack> cargo=new ArrayList<>(27);
                int withdrawn=0;
                while(withdrawn<order.remaining() && cargo.size()<27){
                    ItemStack stack=take(level,racks,target,
                            Math.min(64,order.remaining()-withdrawn));
                    if(stack.isEmpty())break;
                    withdrawn+=stack.getCount();
                    cargo.add(stack);
                }
                if(withdrawn<=0){
                    tank.add(empty);continue;
                }
                empty.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(cargo));
                hub.output.set(slot,empty);
                hub.setChanged();
                int rest=order.remaining()-withdrawn;
                hub.queue.removeFirst();
                if(rest>0)hub.queue.addFirst(new Order(order.id(),rest));
            } else {
                // FIRST identify the exact source stack, including components.
                // Then reserve a compatible destination slot. Only afterwards
                // remove items; a full pallet can never destroy items in transit.
                RackBlockEntity source=null;
                int sourceSlot=-1;
                ItemStack sample=ItemStack.EMPTY;
                outer: for(RackBlockEntity rack:racks){
                    if(!rack.hasPallet())continue;
                    for(int i=0;i<rack.getContainerSize();i++){
                        ItemStack candidate=rack.getItem(i);
                        if(!candidate.isEmpty()&&candidate.getItem()==target){
                            source=rack;sourceSlot=i;sample=candidate;
                            break outer;
                        }
                    }
                }
                if(source==null)continue;
                int slot=hub.freeSlot(sample);
                if(slot<0)continue;
                ItemStack current=hub.output.get(slot);
                int available=current.isEmpty()?sample.getMaxStackSize()
                        :current.getMaxStackSize()-current.getCount();
                if(available<=0)continue;
                ItemStack received=source.removeItem(sourceSlot,
                        Math.min(available,order.remaining()));
                if(received.isEmpty())continue;
                if(current.isEmpty())hub.output.set(slot,received);
                else current.grow(received.getCount());
                hub.setChanged();
                int rest=order.remaining()-received.getCount();
                hub.queue.removeFirst();
                if(rest>0)hub.queue.addFirst(new Order(order.id(),rest));
            }
        }
    }

    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){for(ItemStack i:output)if(!i.isEmpty())return false;return true;}
    @Override public ItemStack getItem(int slot){return slot>=0&&slot<SLOTS?output.get(slot):ItemStack.EMPTY;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){return accepts(stack);}
    @Override public ItemStack removeItem(int slot,int count){
        ItemStack r=ContainerHelper.removeItem(output,slot,count);
        if(!r.isEmpty())setChanged();return r;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        ItemStack r=ContainerHelper.takeItem(output,slot);
        if(!r.isEmpty())setChanged();return r;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS||(!stack.isEmpty()&&!accepts(stack)))return;
        output.set(slot,stack);setChanged();
    }
    @Override public void clearContent(){output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    @Override public boolean stillValid(Player player){return Container.stillValidBlockEntity(this,player,6.0f);}
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output,this.output);
        if(hasPallet())output.store("output_pallet",ItemStack.CODEC,pallet);
        output.putInt("collectors",collectors);
        output.putString("pallet_filter",palletFilter);
        output.putInt("order_count",queue.size());
        int i=0;
        for(Order order:queue){
            output.putString("order_id_"+i,order.id());
            output.putInt("order_qty_"+i,order.remaining());
            i++;
        }
    }
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        output=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input,output);
        pallet=input.read("output_pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        collectors=Math.clamp(input.getIntOr("collectors",0),0,3);
        palletFilter=input.getStringOr("pallet_filter","");
        queue.clear();
        int count=Math.clamp(input.getIntOr("order_count",0),0,MAX_ORDERS);
        for(int i=0;i<count;i++){
            String id=input.getStringOr("order_id_"+i,"");
            int qty=input.getIntOr("order_qty_"+i,0);
            if(!id.isBlank()&&qty>0&&qty<=4096)queue.add(new Order(id,qty));
        }
    }
    @Override public Component getDisplayName(){return Component.literal("Order Collector Hub");}
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new PalletMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
}
