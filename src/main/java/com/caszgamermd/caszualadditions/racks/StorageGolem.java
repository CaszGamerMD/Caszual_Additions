package com.caszgamermd.caszualadditions.racks;

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
    private int stage=2; // 0 source, 1 destination, 2 home, 3 return source
    private boolean outputJob;
    private int outputTicket, pickupLimit=64;
    private ItemStack requestedSample=ItemStack.EMPTY;
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
    }
    public void assignOutput(BlockPos home,BlockPos source,BlockPos destination,
            ItemStack sample,int ticket,int quantity){
        assign(home,source,destination,sample);
        outputJob=true;outputTicket=ticket;
        pickupLimit=Math.clamp(quantity,1,64);
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
        BlockPos target=stage==0||stage==3?source:stage==1?destination:home;
        BlockPos walkway=approach(level,target);
        if(!level.hasChunkAt(target) || !level.hasChunkAt(walkway))return;
        if(!closeTo(walkway)){
            if(tickCount%30==0)
                getNavigation().moveTo(walkway.getX()+.5,walkway.getY(),
                        walkway.getZ()+.5,.78);
            return;
        }
        getNavigation().stop();
        if(stage==0){
            if(outputJob){
                PtpBlockEntity ptp=outputPoint(level);
                if(ptp==null||!ptp.readyForOutput()||!ptp.hasReservation(outputTicket)){
                    stage=2;return;
                }
            }
            Container from=loaded(level,source);
            if(from==null || from instanceof PtpBlockEntity station
                    &&(!station.input()||station.sealed())){
                stage=2;return;
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
                    if(ptp!=null)ptp.outputDelivered(outputTicket,inserted);
                }
                waitTicks=0;
                if(cargo.isEmpty())stage=2;
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
            // An outbound shipment that cannot be delivered is walked back
            // to its rack. Any remainder is dropped here for recovery.
            if(!cargo.isEmpty()){
                StorageGolemDispatch.insert(level,source,cargo);
                if(!cargo.isEmpty()){
                    Containers.dropItemStack(level,getX(),getY(),getZ(),cargo);
                    cargo=ItemStack.EMPTY;
                }
            }
            releaseOutput(level);
            stage=2;return;
        }
        releaseOutput(level);
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
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        home=in.read("home",BlockPos.CODEC).orElse(BlockPos.ZERO);
        source=in.read("source",BlockPos.CODEC).orElse(BlockPos.ZERO);
        destination=in.read("destination",BlockPos.CODEC).orElse(BlockPos.ZERO);
        cargo=in.read("cargo",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        stage=Math.clamp(in.getIntOr("stage",2),0,3);
        itemId=in.getStringOr("item","");
        waitTicks=Math.max(0,in.getIntOr("wait",0));
        outputJob=in.getBooleanOr("output_job",false);
        outputTicket=Math.max(0,in.getIntOr("output_ticket",0));
        pickupLimit=Math.clamp(in.getIntOr("pickup_limit",64),1,64);
        requestedSample=in.read("requested_sample",ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel server &&!deathDropsReleased){
            deathDropsReleased=true;
            releaseOutput(server);
            if(!cargo.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),cargo);
                cargo=ItemStack.EMPTY;
            }
            Containers.dropItemStack(server,getX(),getY(),getZ(),
                    new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        }
        super.die(source);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(level().isClientSide())return InteractionResult.SUCCESS;
        if(!player.isShiftKeyDown())return InteractionResult.PASS;
        if(level() instanceof ServerLevel server)releaseOutput(server);
        if(!cargo.isEmpty()){
            player.getInventory().placeItemBackInInventory(cargo.copy());
            cargo=ItemStack.EMPTY;
        }
        player.getInventory().placeItemBackInInventory(new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        discard();
        return InteractionResult.SUCCESS;
    }
}
