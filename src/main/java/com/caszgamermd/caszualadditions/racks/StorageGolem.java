package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A real pathfinding warehouse worker; it carries its actual cargo as entity
 * save data between SOURCE and DESTINATION. Never teleports inventory or
 * duplicates its shelf token on spawn, unload, death or redock.
 */
public final class StorageGolem extends PathfinderMob {
    private BlockPos home=BlockPos.ZERO, source=BlockPos.ZERO,destination=BlockPos.ZERO;
    private ItemStack cargo=ItemStack.EMPTY;
    private String itemId="";
    // 0 source, 1 destination, 2 home, 3 return source,
    // 4 collect an empty shulker from the magazine, 5 return an empty box
    private int stage=2;
    private boolean outputJob;
    private int outputTicket, pickupLimit=64;
    private ItemStack requestedSample=ItemStack.EMPTY;
    private boolean packedJob;
    private BlockPos magazine=BlockPos.ZERO;
    private ItemStack emptyBox=ItemStack.EMPTY;
    private int packedItemCount;
    // Four workers are independently saved and physically navigate both
    // checkpoints; their sealed pallet remains escrowed on the source PTP.
    private boolean haulJob;
    private int haulTicket,haulIndex;
    private boolean deathDropsReleased;
    private int waitTicks;
    public StorageGolem(EntityType<? extends StorageGolem> type,Level level){
        super(type,level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,20)
                .add(Attributes.MOVEMENT_SPEED,0.22)
                .add(Attributes.FOLLOW_RANGE,28);
    }
    @Override protected void registerGoals() {
        // Jobs choose destinations server-side. Random strolling would
        // interfere with deliberate pallet-to-rack navigation.
    }
    public void assign(BlockPos home,BlockPos source,BlockPos dest,ItemStack sample){
        this.home=home.immutable();
        this.source=source.immutable();
        this.destination=dest.immutable();
        this.itemId=net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(sample.getItem()).toString();
        this.cargo=ItemStack.EMPTY;
        this.stage=0;this.waitTicks=0;
        this.outputJob=false;this.outputTicket=0;this.pickupLimit=64;
        this.requestedSample=sample.copyWithCount(1);
        this.packedJob=false;this.magazine=BlockPos.ZERO;
        this.emptyBox=ItemStack.EMPTY;this.packedItemCount=0;
    }
    public void assignOutput(BlockPos home,BlockPos source,BlockPos destination,
            ItemStack sample,int ticket,int quantity){
        assign(home,source,destination,sample);
        outputJob=true;outputTicket=ticket;
        pickupLimit=Math.clamp(quantity,1,64);
    }
    /** A packed worker physically visits its shelf-connected magazine. */
    public void assignPackedOutput(BlockPos tank,int amount){
        packedJob=true;magazine=tank.immutable();
        stage=4;
        pickupLimit=Math.clamp(amount,1,27*64);
    }
    public void assignHaul(BlockPos shelf,BlockPos transfer,BlockPos rack,
            int ticket,int index){
        home=shelf.immutable();source=transfer.immutable();destination=rack.immutable();
        cargo=ItemStack.EMPTY;emptyBox=ItemStack.EMPTY;requestedSample=ItemStack.EMPTY;
        outputJob=false;outputTicket=0;packedJob=false;packedItemCount=0;
        haulJob=true;haulTicket=ticket;haulIndex=index;stage=0;waitTicks=0;
    }
    private PtpBlockEntity haulSource(ServerLevel level){
        return level.hasChunkAt(source)
                &&level.getBlockEntity(source) instanceof PtpBlockEntity point?point:null;
    }
    private void cancelHaul(ServerLevel level){
        if(haulJob&&haulTicket!=0){
            PtpBlockEntity sourcePoint=haulSource(level);
            if(sourcePoint!=null&&sourcePoint.validHaul(haulTicket,destination))
                sourcePoint.cancelHaul();
            haulTicket=0;
        }
    }
    /**
     * Unlike ordinary item jobs, the workers never withdraw cargo. The
     * wrapped source pallet stays sealed and locked until all four stand at
     * the destination, where the PTP commits one validated rack handoff.
     */
    private void tickHaul(ServerLevel level){
        if(stage==2){
            BlockPos target=home;
            if(!level.hasChunkAt(target))return;
            if(!closeTo(target)){
                if(tickCount%30==0)getNavigation().moveTo(target.getX()+.5,
                        target.getY()+1,target.getZ()+.5,.78);
                return;
            }
            getNavigation().stop();
            if(level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                    &&shelf.add())discard();
            return;
        }
        if(!level.hasChunkAt(source))return;
        PtpBlockEntity ptp=haulSource(level);
        if(ptp==null||!ptp.validHaul(haulTicket,destination)){
            stage=2;return;
        }
        ptp.renewHaul(haulTicket);
        if(stage==0&&ptp.haulStage(haulTicket)==1)stage=1;
        BlockPos target=stage==0?source:destination;
        if(!level.hasChunkAt(target))return;
        BlockPos walkway=approach(level,target);
        if(!level.hasChunkAt(walkway))return;
        if(!closeTo(walkway)){
            if(tickCount%30==0)getNavigation().moveTo(walkway.getX()+.5,
                    walkway.getY(),walkway.getZ()+.5,.78);
            return;
        }
        getNavigation().stop();
        if(stage==0){
            ptp.assembleHaul(haulTicket,haulIndex);
            if(ptp.haulStage(haulTicket)==1)stage=1;
        }else if(stage==1){
            // It is safe to call this repeatedly while waiting for the other
            // three. Bitmasks prevent duplicate arrival/commit operations.
            if(ptp.arriveHaul(haulTicket,haulIndex,level))stage=2;
        }
    }
    private PtpBlockEntity outputPoint(ServerLevel level){
        return level.hasChunkAt(destination)
                &&level.getBlockEntity(destination) instanceof PtpBlockEntity ptp?ptp:null;
    }
    private void releaseOutput(ServerLevel level){
        if(outputJob&&outputTicket!=0){
            PtpBlockEntity ptp=outputPoint(level);
            if(ptp!=null)ptp.releaseOutput(outputTicket);
            outputTicket=0;
        }
    }
    public BlockPos home(){return home;}
    private BlockPos approach(ServerLevel level,BlockPos place) {
        BlockEntity be=level.hasChunkAt(place)?level.getBlockEntity(place):null;
        if(be instanceof PtpBlockEntity)return place.south(2);
        if(be instanceof ShulkerTankEntity)return place.south();
        if(be instanceof RackBlockEntity rack)
            return RackBlock.cell(place,rack.facing(),0).relative(rack.facing());
        return place;
    }
    private boolean closeTo(BlockPos p){
        return distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)<3.5;
    }
    private Container loaded(ServerLevel level,BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Container c
                ?c:null;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel level)||tickCount%10!=0)return;
        if(haulJob){tickHaul(level);return;}
        if(outputJob&&outputTicket!=0){
            PtpBlockEntity ptp=outputPoint(level);
            if(ptp!=null)ptp.renewOutput(outputTicket);
        }
        BlockPos target=stage==0||stage==3?source
                :stage==1?destination:stage==4||stage==5?magazine:home;
        BlockPos walkway=approach(level,target);
        if(!level.hasChunkAt(target) || !level.hasChunkAt(walkway))return;
        if(!closeTo(walkway)){
            if(tickCount%30==0)
                getNavigation().moveTo(walkway.getX()+.5,walkway.getY(),
                        walkway.getZ()+.5,.78);
            return;
        }
        getNavigation().stop();
        if(stage==4){
            PtpBlockEntity output=outputPoint(level);
            if(output==null||!output.readyForOutput()
                    ||!output.hasReservation(outputTicket)){
                stage=2;return;
            }
            ShulkerTankEntity tank=level.getBlockEntity(magazine)
                    instanceof ShulkerTankEntity t?t:null;
            if(tank==null||tank.isEmpty()){stage=2;return;}
            emptyBox=tank.take();
            if(emptyBox.isEmpty()){stage=2;return;}
            stage=0;return;
        }
        if(stage==5){
            if(!emptyBox.isEmpty()){
                ShulkerTankEntity tank=level.getBlockEntity(magazine)
                        instanceof ShulkerTankEntity t?t:null;
                if(tank==null||!tank.add(emptyBox))
                    Containers.dropItemStack(level,getX(),getY(),getZ(),emptyBox);
                emptyBox=ItemStack.EMPTY;
            }
            stage=2;return;
        }
        if(stage==0){
            if(outputJob){
                PtpBlockEntity ptp=outputPoint(level);
                if(ptp==null||!ptp.readyForOutput()||!ptp.hasReservation(outputTicket)){
                    stage=emptyBox.isEmpty()?2:5;return;
                }
            }
            Container from=loaded(level,source);
            if(from==null || from instanceof PtpBlockEntity station
                    &&(!station.input()||station.sealed())){
                stage=emptyBox.isEmpty()?2:5;return;
            }
            if(packedJob){
                if(emptyBox.isEmpty()){stage=2;return;}
                // Preview at most 27 same-item stacks, from this rack only,
                // before withdrawing anything from the source inventory.
                List<ItemStack> expected=new ArrayList<>(27);
                List<Integer> slots=new ArrayList<>(27);
                int requested=0;
                for(int i=0;i<from.getContainerSize()&&expected.size()<27
                        &&requested<pickupLimit;i++){
                    ItemStack sample=from.getItem(i);
                    if(sample.isEmpty()||!sample.getItem().canFitInsideContainerItems()
                            ||!net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(sample.getItem()).toString().equals(itemId))continue;
                    int n=Math.min(sample.getCount(),pickupLimit-requested);
                    expected.add(sample.copyWithCount(n));
                    slots.add(i);
                    requested+=n;
                }
                ItemStack preview=StorageGolemPacking.fill(emptyBox,expected);
                if(preview.isEmpty()
                        ||!StorageGolemDispatch.canInsert(level,destination,preview)){
                    stage=5;return;
                }
                List<ItemStack> actual=new ArrayList<>(expected.size());
                for(int i=0;i<slots.size();i++){
                    ItemStack retrieved=from.removeItem(slots.get(i),expected.get(i).getCount());
                    if(!retrieved.isEmpty())actual.add(retrieved);
                }
                ItemStack packed=StorageGolemPacking.fill(emptyBox,actual);
                if(packed.isEmpty()
                        ||!StorageGolemDispatch.canInsert(level,destination,packed)){
                    for(ItemStack item:actual){
                        StorageGolemDispatch.insert(level,source,item);
                        if(!item.isEmpty())
                            Containers.dropItemStack(level,getX(),getY(),getZ(),item);
                    }
                    stage=5;return;
                }
                cargo=packed;
                packedItemCount=StorageGolemPacking.itemCount(packed);
                emptyBox=ItemStack.EMPTY;
                stage=1;waitTicks=0;return;
            }
            for(int i=0;i<from.getContainerSize();i++){
                ItemStack sample=from.getItem(i);
                if(sample.isEmpty() || !net.minecraft.core.registries.BuiltInRegistries.ITEM
                        .getKey(sample.getItem()).toString().equals(itemId)
                        || !requestedSample.isEmpty()
                        && !ItemStack.isSameItemSameComponents(sample,requestedSample))continue;
                if(StorageGolemDispatch.canInsert(level,destination,sample)){
                    cargo=from.removeItem(i,Math.min(sample.getCount(),pickupLimit));
                    if(!cargo.isEmpty()){stage=1;waitTicks=0;return;}
                }
            }
            stage=2;return;
        }
        if(stage==1){
            if(cargo.isEmpty()){stage=2;return;}
            if(outputJob){
                PtpBlockEntity ptp=outputPoint(level);
                if(ptp==null||!ptp.readyForOutput()||!ptp.hasReservation(outputTicket)){
                    stage=3;return; // carry it physically back to the source rack
                }
            }
            int inserted=StorageGolemDispatch.insert(level,destination,cargo);
            if(inserted>0){
                if(outputJob){
                    PtpBlockEntity ptp=outputPoint(level);
                    if(ptp!=null)ptp.outputDelivered(outputTicket,
                            packedJob?packedItemCount:inserted);
                }
                waitTicks=0;
                if(cargo.isEmpty()){
                    packedItemCount=0;
                    stage=2;
                }
            } else if(++waitTicks>80){
                if(outputJob)stage=3;
                else {
                    // Input sorting may reroute to another compatible rack.
                    BlockPos alternate=StorageGolemDispatch.findRackFor(level,home,cargo);
                    if(alternate!=null)destination=alternate;
                    else stage=2;
                }
                waitTicks=0;
            }
            return;
        }
        if(stage==3){
            // A rejected packed delivery goes back to the SAME source rack.
            // Recover the contained item stacks first, then return the one
            // emptied shulker to the shelf magazine; never clone its contents.
            if(packedJob&&!cargo.isEmpty()&&packedItemCount>0){
                ItemStack unpackedEmpty=StorageGolemPacking.emptyVersion(cargo);
                List<ItemStack> stacks=StorageGolemPacking.unpack(cargo);
                cargo=ItemStack.EMPTY;
                packedItemCount=0;
                for(ItemStack item:stacks){
                    StorageGolemDispatch.insert(level,source,item);
                    if(!item.isEmpty())
                        Containers.dropItemStack(level,getX(),getY(),getZ(),item);
                }
                emptyBox=unpackedEmpty;
            }else if(!cargo.isEmpty()){
                StorageGolemDispatch.insert(level,source,cargo);
                if(!cargo.isEmpty()){
                    Containers.dropItemStack(level,getX(),getY(),getZ(),cargo);
                    cargo=ItemStack.EMPTY;
                }
            }
            releaseOutput(level);
            stage=emptyBox.isEmpty()?2:5;return;
        }
        releaseOutput(level);
        if(!emptyBox.isEmpty()){
            // Worker was interrupted after withdrawal but before packing.
            Containers.dropItemStack(level,getX(),getY(),getZ(),emptyBox);
            emptyBox=ItemStack.EMPTY;
        }
        if(!cargo.isEmpty()){
            // A destroyed/filtered destination is not a deletion path:
            // physically drop the real item for recovery before redocking.
            Containers.dropItemStack(level,getX(),getY(),getZ(),cargo);
            cargo=ItemStack.EMPTY;
        }
        if(level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf &&shelf.add()){
            discard(); // one entity becomes exactly one docked golem token
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.store("home",BlockPos.CODEC,home);
        out.store("source",BlockPos.CODEC,source);
        out.store("destination",BlockPos.CODEC,destination);
        if(!cargo.isEmpty())out.store("cargo",ItemStack.CODEC,cargo);
        out.putInt("stage",stage);out.putString("item",itemId);out.putInt("wait",waitTicks);
        out.putBoolean("output_job",outputJob);
        out.putInt("output_ticket",outputTicket);
        out.putInt("pickup_limit",pickupLimit);
        if(!requestedSample.isEmpty())out.store("requested_sample",ItemStack.CODEC,requestedSample);
        out.putBoolean("packed_job",packedJob);
        out.store("magazine",BlockPos.CODEC,magazine);
        out.putInt("packed_item_count",packedItemCount);
        if(!emptyBox.isEmpty())out.store("empty_box",ItemStack.CODEC,emptyBox);
        out.putBoolean("haul_job",haulJob);
        out.putInt("haul_ticket",haulTicket);
        out.putInt("haul_worker_index",haulIndex);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        home=in.read("home",BlockPos.CODEC).orElse(BlockPos.ZERO);
        source=in.read("source",BlockPos.CODEC).orElse(BlockPos.ZERO);
        destination=in.read("destination",BlockPos.CODEC).orElse(BlockPos.ZERO);
        cargo=in.read("cargo",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        stage=Math.clamp(in.getIntOr("stage",2),0,5);
        itemId=in.getStringOr("item","");
        waitTicks=Math.max(0,in.getIntOr("wait",0));
        outputJob=in.getBooleanOr("output_job",false);
        outputTicket=Math.max(0,in.getIntOr("output_ticket",0));
        pickupLimit=Math.clamp(in.getIntOr("pickup_limit",64),1,27*64);
        requestedSample=in.read("requested_sample",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        packedJob=in.getBooleanOr("packed_job",false);
        magazine=in.read("magazine",BlockPos.CODEC).orElse(BlockPos.ZERO);
        packedItemCount=Math.max(0,in.getIntOr("packed_item_count",0));
        emptyBox=in.read("empty_box",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        haulJob=in.getBooleanOr("haul_job",false);
        haulTicket=Math.max(0,in.getIntOr("haul_ticket",0));
        haulIndex=Math.clamp(in.getIntOr("haul_worker_index",0),0,3);
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel server &&!deathDropsReleased){
            deathDropsReleased=true;
            releaseOutput(server);
            cancelHaul(server);
            if(!cargo.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),cargo);
                cargo=ItemStack.EMPTY;
            }
            if(!emptyBox.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),emptyBox);
                emptyBox=ItemStack.EMPTY;
            }
            Containers.dropItemStack(server,getX(),getY(),getZ(),
                    new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        }
        super.die(source);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(level().isClientSide())return InteractionResult.SUCCESS;
        if(!player.isShiftKeyDown())return InteractionResult.PASS;
        if(level() instanceof ServerLevel server){
            releaseOutput(server);
            cancelHaul(server);
        }
        if(!cargo.isEmpty()){
            player.getInventory().placeItemBackInInventory(cargo.copy());
            cargo=ItemStack.EMPTY;
        }
        if(!emptyBox.isEmpty()){
            player.getInventory().placeItemBackInInventory(emptyBox.copy());
            emptyBox=ItemStack.EMPTY;
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        discard();
        return InteractionResult.SUCCESS;
    }
}
