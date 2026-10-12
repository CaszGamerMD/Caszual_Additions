package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.ContainerHelper;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;
import net.minecraft.core.Direction;
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
    // Client-only representation; real contents remain in palletContents.
    // This synchronizes just one material/item sample and visible crate count.
    private static final EntityDataAccessor<ItemStack> DISPLAY_PALLET=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DISPLAY_CARGO=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.INT);
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(DISPLAY_PALLET,ItemStack.EMPTY);
        builder.define(DISPLAY_CARGO,0);
    }
    public ItemStack displayPallet(){return entityData.get(DISPLAY_PALLET);}
    public int visibleCargoCount(){return entityData.get(DISPLAY_CARGO);}
    private void syncPalletVisual(){
        entityData.set(DISPLAY_PALLET,carryingPallet.copy());
        int filled=0;
        for(ItemStack item:palletContents)if(!item.isEmpty())filled++;
        entityData.set(DISPLAY_CARGO,Math.min(27,filled));
    }
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
    private boolean playerHaul;
    private boolean groundPickup;
    private String haulOwner="";
    private ItemStack carryingPallet=ItemStack.EMPTY;
    private NonNullList<ItemStack> palletContents=NonNullList.withSize(216,ItemStack.EMPTY);
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
        setCustomName(Component.literal("RALLY "+(index+1)+"/4"));
        setCustomNameVisible(true);
    }
    public void assignPlayerCrew(BlockPos shelf,int index,UUID owner){
        home=shelf.immutable();source=BlockPos.ZERO;destination=BlockPos.ZERO;
        cargo=ItemStack.EMPTY;emptyBox=ItemStack.EMPTY;
        outputJob=false;outputTicket=0;packedJob=false;
        haulJob=true;playerHaul=true;haulOwner=owner.toString();
        haulTicket=0;haulIndex=index;stage=4;waitTicks=0;
        setCustomName(Component.literal("CREW "+(index+1)+"/4"));
        setCustomNameVisible(true);
    }
    public boolean ownedBy(UUID owner){
        return playerHaul&&haulOwner.equals(owner.toString());
    }
    public boolean isPlayerCarrier(UUID owner,int ticket){
        return ownedBy(owner)&&haulIndex==0&&haulTicket==ticket;
    }
    public boolean escorting(UUID owner){return ownedBy(owner)&&stage==1;}
    public boolean pickupPending(UUID owner){return ownedBy(owner)&&stage==0;}
    public void beginGroundPickup(BlockPos groundRoot){
        if(playerHaul&&stage==4&&!holdsPallet()){
            source=groundRoot.immutable();
            destination=groundRoot.immutable();
            groundPickup=true;
            haulTicket=0;
            stage=0;
            setCustomName(Component.literal("RALLY "+(haulIndex+1)+"/4"));
        }
    }
    public void cancelPendingPickup(ServerLevel level){
        if(playerHaul&&stage==0){
            cancelHaul(level);
            groundPickup=false;
            stage=4;
            setCustomName(Component.literal("CREW "+(haulIndex+1)+"/4"));
        }
    }
    public boolean holdsPallet(){return !carryingPallet.isEmpty();}
    /** Handoff is copied into the moving pallet before the crew is
     * despawned. If entity spawning fails, the old lead keeps its cargo. */
    public ItemStack palletForTransfer(){return carryingPallet.copy();}
    public NonNullList<ItemStack> palletContentsForTransfer(){
        NonNullList<ItemStack> copy=NonNullList.withSize(216,ItemStack.EMPTY);
        for(int i=0;i<216;i++)copy.set(i,palletContents.get(i).copy());
        return copy;
    }
    public void relinquishPalletToRig(){releaseCarriedPallet();}
    /** Read-only server diagnostics for Rack Computer Golem task dashboard. */
    public BlockPos shelfHome(){return home;}
    public BlockPos workSource(){return source;}
    public BlockPos workTarget(){return destination;}
    public String assignedOwner(){return playerHaul?haulOwner:"";}
    public String currentWork(){
        if(playerHaul)return switch(stage){
            case 0->"LIFTING WRAPPED PALLET";
            case 1->"FOLLOWING PLAYER / "+(holdsPallet()?"CARRYING PALLET":"ESCORT");
            case 2->"RETURNING TO SHELF";
            default->"FOLLOWING PLAYER / READY";
        };
        if(haulJob)return switch(stage){
            case 0->"ASSEMBLING HAUL";case 1->"MOVING PALLET";
            default->"RETURNING TO SHELF";
        };
        if(packedJob)return switch(stage){
            case 4->"COLLECTING EMPTY SHULKER";
            case 5->"RETURNING EMPTY SHULKER";
            case 0->"PACKING ORDER";case 1->"DELIVERING PACKED ORDER";
            case 3->"RETURNING REJECTED ORDER";
            default->"RETURNING TO SHELF";
        };
        if(outputJob)return switch(stage){
            case 0->"PICKING OUTPUT ORDER";case 1->"DELIVERING OUTPUT ORDER";
            case 3->"RETURNING REJECTED ORDER";
            default->"RETURNING TO SHELF";
        };
        return switch(stage){
            case 0->"PICKING INPUT STOCK";case 1->"SORTING TO RACK";
            default->"RETURNING TO SHELF";
        };
    }
    public String carriedDescription(){
        if(holdsPallet())return "WRAPPED PALLET";
        if(!cargo.isEmpty())return cargo.getHoverName().getString()
                +" x"+cargo.getCount();
        return emptyBox.isEmpty()?"EMPTY":"EMPTY SHULKER";
    }
    public int crewIndex(){return haulIndex;}
    public void beginPlayerPickup(BlockPos ptp,int ticket){
        if(playerHaul&&stage==4){
            source=ptp.immutable();destination=ptp.immutable();
            haulTicket=ticket;groundPickup=false;stage=0;
            setCustomName(Component.literal("RALLY "+(haulIndex+1)+"/4"));
        }
    }
    public void startFollowing(){
        if(playerHaul&&stage==0){
            stage=1;
            groundPickup=false;
            setCustomName(Component.literal("PALLET "+(haulIndex+1)+"/4"));
        }
    }
    public void clearDelivery(){
        if(playerHaul&&stage==1){stage=4;haulTicket=0;
            setCustomName(Component.literal("CREW "+(haulIndex+1)+"/4"));}
    }
    public void dismissCrew(){
        if(playerHaul&&stage==4){stage=2;setCustomNameVisible(false);}
    }
    public boolean loadPallet(ItemStack stack,NonNullList<ItemStack> stock){
        if(!playerHaul||haulIndex!=0||!carryingPallet.isEmpty()
                ||stack.isEmpty()||stock.size()!=216)return false;
        carryingPallet=stack.copyWithCount(1);
        for(int i=0;i<216;i++)palletContents.set(i,stock.get(i).copy());
        syncPalletVisual();
        return true;
    }
    private boolean allCargoFits(Container dest){
        for(int i=0;i<216;i++){
            ItemStack item=palletContents.get(i);
            if(!item.isEmpty()&&!dest.canPlaceItem(i,item))return false;
        }
        return true;
    }
    private boolean verifyCargo(Container dest){
        for(int i=0;i<216;i++){
            ItemStack old=palletContents.get(i),now=dest.getItem(i);
            if(old.isEmpty()!=now.isEmpty())return false;
            if(!old.isEmpty()&&(!ItemStack.isSameItemSameComponents(old,now)
                    ||old.getCount()!=now.getCount()))return false;
        }
        return true;
    }
    /** After validation, one server tick installs exactly the one pallet
     * and all 216 stacks; the carrier is cleared only after verification. */
    public boolean placeIntoRack(RackBlockEntity rack){
        if(!playerHaul||stage!=1||carryingPallet.isEmpty()
                ||rack.hasPallet()||!rack.isEmpty())return false;
        rack.install(carryingPallet);
        if(!rack.hasPallet()||!allCargoFits(rack)){
            rack.dropPallet();return false;
        }
        for(int i=0;i<216;i++)if(!palletContents.get(i).isEmpty())
            rack.setItem(i,palletContents.get(i).copy());
        if(!verifyCargo(rack)){
            rack.clearContent();rack.dropPallet();return false;
        }
        releaseCarriedPallet();
        return true;
    }
    public boolean placeOnGround(ServerLevel level,BlockPos pos,Direction facing){
        if(!playerHaul||stage!=1||carryingPallet.isEmpty()
                ||!(carryingPallet.getItem() instanceof BlockItem bi)
                ||!(bi.getBlock() instanceof PalletBlock palletBlock))return false;
        BlockState root=palletBlock.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing)
                .setValue(PalletBlock.LEGACY,false);
        for(int part=0;part<4;part++){
            BlockPos where=PalletBlock.partPos(pos,part,root);
            if(level.isOutsideBuildHeight(where)||!level.getWorldBorder().isWithinBounds(where)
                    ||!level.hasChunkAt(where)||!level.getBlockState(where).canBeReplaced())
                return false;
        }
        if(!level.setBlock(pos,root,3))return false;
        boolean complete=true;
        for(int part=1;part<4;part++){
            BlockPos where=PalletBlock.partPos(pos,part,root);
            if(!level.setBlock(where,root.setValue(PalletBlock.PART,part),3)){
                complete=false;break;
            }
        }
        PalletBlockEntity dest=level.getBlockEntity(pos)
                instanceof PalletBlockEntity p?p:null;
        if(dest==null)complete=false;
        if(complete){
            dest.initialize(pos,0,carryingPallet);
            if(!allCargoFits(dest))complete=false;
        }
        if(complete){
            for(int i=0;i<216;i++)if(!palletContents.get(i).isEmpty())
                dest.setItem(i,palletContents.get(i).copy());
            complete=verifyCargo(dest);
        }
        if(!complete){
            if(dest!=null)dest.clearContent();
            for(int part=0;part<4;part++){
                BlockPos where=PalletBlock.partPos(pos,part,root);
                BlockState found=level.getBlockState(where);
                if(found.getBlock()==palletBlock
                        &&found.getValue(PalletBlock.PART)==part)
                    level.removeBlock(where,false);
            }
            return false;
        }
        releaseCarriedPallet();
        return true;
    }
    private void releaseCarriedPallet(){
        carryingPallet=ItemStack.EMPTY;
        palletContents=NonNullList.withSize(216,ItemStack.EMPTY);
        syncPalletVisual();
    }
    private void dropCarriedPallet(ServerLevel level){
        if(carryingPallet.isEmpty())return;
        Containers.dropItemStack(level,getX(),getY(),getZ(),carryingPallet);
        for(ItemStack stack:palletContents)
            if(!stack.isEmpty())Containers.dropItemStack(level,getX(),getY(),getZ(),stack);
        releaseCarriedPallet();
    }
    private UUID ownerId(){
        try{return UUID.fromString(haulOwner);}catch(IllegalArgumentException ex){return null;}
    }
    private void tickPlayerHaul(ServerLevel level){
        if(stage==2){
            setCustomNameVisible(false);
            if(!level.hasChunkAt(home))return;
            if(!closeTo(home)){
                if(tickCount%20==0)getNavigation().moveTo(
                        home.getX()+.5,home.getY()+1,home.getZ()+.5,.88);
                return;
            }
            getNavigation().stop();
            if(level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                    &&shelf.add())discard();
            return;
        }
        UUID owner=ownerId();
        if(owner==null){stage=2;return;}
        if(stage==0){
            if(!level.hasChunkAt(source))return;
            if(groundPickup){
                UUID ownerId=ownerId();
                if(StorageGroundPalletTransfer.wrappedController(level,source)==null){
                    groundPickup=false;
                    stage=4;
                    setCustomName(Component.literal("CREW "+(haulIndex+1)+"/4"));
                    return;
                }
                BlockPos near=approach(level,source);
                if(!level.hasChunkAt(near))return;
                if(!closeTo(near)){
                    if(tickCount%20==0)getNavigation().moveTo(
                            near.getX()+.5,near.getY(),near.getZ()+.5,.86);
                    return;
                }
                if(haulIndex==0){
                    List<StorageGolem> crew=level.getEntitiesOfClass(
                            StorageGolem.class,
                            new net.minecraft.world.phys.AABB(source).inflate(12),
                            other->other.playerHaul&&other.groundPickup
                                    &&other.stage==0&&other.source.equals(source)
                                    &&other.haulOwner.equals(haulOwner));
                    boolean ready=crew.size()==4;
                    int mask=0;
                    for(StorageGolem member:crew){
                        mask=WarehouseTeamProgress.arrive(mask,member.haulIndex);
                        BlockPos dock=member.approach(level,source);
                        if(!member.closeTo(dock))ready=false;
                    }
                    if(ready&&WarehouseTeamProgress.complete(mask)
                            &&StorageGroundPalletTransfer.lift(level,source,this)){
                        if(!StoragePalletCrew.formMovingPallet(level,crew,ownerId))
                            for(StorageGolem worker:crew)worker.startFollowing();
                    }
                }
                return;
            }
            PtpBlockEntity point=haulSource(level);
            if(point==null||!point.playerHaulValid(haulTicket,owner)){
                stage=4;return;
            }
            point.renewHaul(haulTicket);
            BlockPos near=approach(level,source);
            if(!level.hasChunkAt(near))return;
            if(!closeTo(near)){
                if(tickCount%20==0)getNavigation().moveTo(
                        near.getX()+.5,near.getY(),near.getZ()+.5,.85);
                return;
            }
            point.assembleHaul(haulTicket,haulIndex);
            if(point.haulStage(haulTicket)==1){
                List<StorageGolem> team=level.getEntitiesOfClass(
                        StorageGolem.class,new net.minecraft.world.phys.AABB(source).inflate(16),
                        other->other.playerHaul&&other.haulTicket==haulTicket
                                &&other.haulOwner.equals(haulOwner));
                if(team.size()==4){
                    StorageGolem leader=team.stream().filter(w->w.haulIndex==0)
                            .findFirst().orElse(null);
                    if(leader!=null&&point.giveToCarrier(level,leader,haulTicket,owner)){
                        if(!StoragePalletCrew.formMovingPallet(level,team,owner))
                            for(StorageGolem worker:team)worker.startFollowing();
                    }
                }
            }
            return;
        }
        var player=level.getPlayerByUUID(owner);
        if(player==null||player.isSpectator())return;
        // Both empty crew and laden team stay with player, not static rack.
        // A stable four-corner formation. The shared 2x2 pallet is drawn
        // only by the lead carrier between all four raised-arm Golems.
        // The squad walks a few blocks BEHIND the player and stays together.
        double behindX=-player.getDirection().getStepX()*2.8;
        double behindZ=-player.getDirection().getStepZ()*2.8;
        double x=player.getX()+behindX+(haulIndex%2==0?-.90:.90);
        double z=player.getZ()+behindZ+(haulIndex<2?-.90:.90);
        if(distanceToSqr(x,player.getY(),z)>5){
            if(tickCount%15==0)getNavigation().moveTo(x,player.getY(),z,.94);
        }else getNavigation().stop();
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
            setCustomNameVisible(false);
        }
    }
    /**
     * Unlike ordinary item jobs, the workers never withdraw cargo. The
     * wrapped source pallet stays sealed and locked until all four stand at
     * the destination, where the PTP commits one validated rack handoff.
     */
    private void tickHaul(ServerLevel level){
        if(stage==2){
            setCustomNameVisible(false);
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
            stage=2;
            setCustomNameVisible(false);
            return;
        }
        ptp.renewHaul(haulTicket);
        if(stage==0&&ptp.haulStage(haulTicket)==1){
            stage=1;
            setCustomName(Component.literal("PALLET "+(haulIndex+1)+"/4"));
        }
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
            if(ptp.haulStage(haulTicket)==1){
                stage=1;
                setCustomName(Component.literal("PALLET "+(haulIndex+1)+"/4"));
            }
        }else if(stage==1){
            // It is safe to call this repeatedly while waiting for the other
            // three. Bitmasks prevent duplicate arrival/commit operations.
            if(ptp.arriveHaul(haulTicket,haulIndex,level)){
                stage=2;
                setCustomNameVisible(false);
            }
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
    private boolean openWalkway(ServerLevel level,BlockPos p){
        return level.hasChunkAt(p)&&level.hasChunkAt(p.above())
                &&level.getBlockState(p).getCollisionShape(level,p).isEmpty()
                &&level.getBlockState(p.above()).getCollisionShape(level,p.above()).isEmpty();
    }
    private BlockPos approach(ServerLevel level,BlockPos place) {
        if(groundPickup&&level.hasChunkAt(place)){
            BlockState base=level.getBlockState(place);
            if(base.getBlock() instanceof PalletBlock){
                int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
                int minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
                for(int part=0;part<4;part++){
                    BlockPos at=PalletBlock.partPos(place,part,base);
                    minX=Math.min(minX,at.getX());maxX=Math.max(maxX,at.getX());
                    minZ=Math.min(minZ,at.getZ());maxZ=Math.max(maxZ,at.getZ());
                }
                BlockPos[] sides={
                    new BlockPos(minX,place.getY(),minZ-1),
                    new BlockPos(maxX+1,place.getY(),minZ),
                    new BlockPos(maxX,place.getY(),maxZ+1),
                    new BlockPos(minX-1,place.getY(),maxZ)
                };
                for(int attempt=0;attempt<4;attempt++){
                    BlockPos candidate=sides[(haulIndex+attempt)%4];
                    if(openWalkway(level,candidate))return candidate;
                }
                return sides[haulIndex];
            }
        }
        BlockEntity be=level.hasChunkAt(place)?level.getBlockEntity(place):null;
        if(be instanceof PtpBlockEntity || be instanceof ShulkerTankEntity){
            // A scanner, linked shelf or Hopper Adapter can occupy the old
            // hard-coded SOUTH entrance. Find a clear outer approach without
            // loading chunks; spread a four-Golem team across four sides.
            int distance=be instanceof PtpBlockEntity?2:1;
            net.minecraft.core.Direction[] faces={
                    net.minecraft.core.Direction.NORTH,net.minecraft.core.Direction.EAST,
                    net.minecraft.core.Direction.SOUTH,net.minecraft.core.Direction.WEST
            };
            BlockPos best=null;
            double score=Double.POSITIVE_INFINITY;
            for(int i=0;i<faces.length;i++){
                int side=(i+(haulJob?haulIndex:0))%faces.length;
                BlockPos p=place.relative(faces[side],distance);
                if(!openWalkway(level,p))continue;
                // The four workers take different available sides in index
                // order. Ordinary jobs take the closest free side to home.
                if(haulJob)return p;
                double candidate=p.distSqr(home);
                if(candidate<score){score=candidate;best=p;}
            }
            return best==null?place.south(distance):best;
        }
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
        if(haulJob){
            if(playerHaul)tickPlayerHaul(level);
            else tickHaul(level);
            return;
        }
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
        out.putBoolean("player_haul",playerHaul);
        out.putBoolean("ground_pickup",groundPickup);
        out.putString("haul_owner",haulOwner);
        if(!carryingPallet.isEmpty())out.store("carried_pallet",ItemStack.CODEC,carryingPallet);
        ContainerHelper.saveAllItems(out,palletContents);
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
        playerHaul=in.getBooleanOr("player_haul",false);
        groundPickup=in.getBooleanOr("ground_pickup",false);
        haulOwner=in.getStringOr("haul_owner","");
        carryingPallet=in.read("carried_pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        palletContents=NonNullList.withSize(216,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,palletContents);
        syncPalletVisual();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel server &&!deathDropsReleased){
            deathDropsReleased=true;
            releaseOutput(server);
            cancelHaul(server);
            dropCarriedPallet(server);
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
            dropCarriedPallet(server);
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
