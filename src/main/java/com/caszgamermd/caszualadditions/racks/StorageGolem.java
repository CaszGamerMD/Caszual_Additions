package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.ArrayDeque;
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
import net.minecraft.tags.FluidTags;
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
    // The client cannot see remoteCourier / remoteFresh / playerHaul / stage
    // NBT task fields. Those are server-only. Sync a tiny visual-only state
    // rather than attempting to read private mission ownership on client:
    // 0 = hands free, 1 = supporting loaded pallet, 2 = pushing empty deck.
    private static final EntityDataAccessor<Integer> DISPLAY_PALLET_ACTION=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.INT);
    // These are three SAMPLES of actual in-transit items. The complete
    // nine-slot order inventory stays server-owned and persisted in NBT.
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_A=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_B=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_C=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_D=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_E=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_F=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_G=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_H=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DISPLAY_HELD_I=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DISPLAY_HELD_SLOTS=
            SynchedEntityData.defineId(StorageGolem.class,EntityDataSerializers.INT);
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(DISPLAY_PALLET,ItemStack.EMPTY);
        builder.define(DISPLAY_CARGO,0);
        builder.define(DISPLAY_PALLET_ACTION,0);
        builder.define(DISPLAY_HELD_A,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_B,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_C,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_D,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_E,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_F,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_G,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_H,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_I,ItemStack.EMPTY);
        builder.define(DISPLAY_HELD_SLOTS,0);
    }
    public int displayedLoadSlots(){return entityData.get(DISPLAY_HELD_SLOTS);}
    public ItemStack displayedLoad(int sample){
        return switch(sample){
            case 0->entityData.get(DISPLAY_HELD_A);
            case 1->entityData.get(DISPLAY_HELD_B);
            case 2->entityData.get(DISPLAY_HELD_C);
            case 3->entityData.get(DISPLAY_HELD_D);
            case 4->entityData.get(DISPLAY_HELD_E);
            case 5->entityData.get(DISPLAY_HELD_F);
            case 6->entityData.get(DISPLAY_HELD_G);
            case 7->entityData.get(DISPLAY_HELD_H);
            case 8->entityData.get(DISPLAY_HELD_I);
            default->ItemStack.EMPTY;
        };
    }
    private void setHeldVisual(EntityDataAccessor<ItemStack> accessor,
            ItemStack stack){
        ItemStack sample=stack.isEmpty()?ItemStack.EMPTY:stack.copyWithCount(1);
        if(!ItemStack.matches(entityData.get(accessor),sample))
            entityData.set(accessor,sample);
    }
    private void syncHeldVisual(){
        if(!carryingPallet.isEmpty()){
            clearHeldVisual();return;
        }
        java.util.ArrayList<ItemStack> real=new java.util.ArrayList<>(9);
        if(stationOrder){
            for(ItemStack stack:orderCargo)
                if(!stack.isEmpty())real.add(stack);
        }else{
            if(!cargo.isEmpty())real.add(cargo);
            if(!emptyBox.isEmpty())real.add(emptyBox);
        }
        int slots=real.size();
        if(entityData.get(DISPLAY_HELD_SLOTS)!=slots)
            entityData.set(DISPLAY_HELD_SLOTS,slots);
        setHeldVisual(DISPLAY_HELD_A,slots>0?real.get(0):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_B,slots>1?real.get(1):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_C,slots>2?real.get(2):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_D,slots>3?real.get(3):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_E,slots>4?real.get(4):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_F,slots>5?real.get(5):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_G,slots>6?real.get(6):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_H,slots>7?real.get(7):ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_I,slots>8?real.get(8):ItemStack.EMPTY);
    }
    private void clearHeldVisual(){
        if(entityData.get(DISPLAY_HELD_SLOTS)!=0)
            entityData.set(DISPLAY_HELD_SLOTS,0);
        setHeldVisual(DISPLAY_HELD_A,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_B,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_C,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_D,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_E,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_F,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_G,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_H,ItemStack.EMPTY);
        setHeldVisual(DISPLAY_HELD_I,ItemStack.EMPTY);
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
    private long observedRecallSerial;
    private boolean remoteCourier;
    private UUID remoteCarrierId;
    // Before pickup, each of the THREE Golems is a real, separately
    // navigating and saved mob. They have no shared carrier or pallet.
    private boolean remotePickup;
    /** Two real walking pushers move pallets between transfer node and train
     * or conveyor. Separate from the four-Golem REMOTE and INPUT crews. */
    private boolean nodePush;
    private UUID nodePushMission,nodePushVehicle;
    private BlockPos nodePushNode=BlockPos.ZERO,nodePushGoal=BlockPos.ZERO;
    private boolean nodePushLoading;
    private UUID remotePickupMission,remotePickupCourier;
    private BlockPos remotePickupTarget=BlockPos.ZERO;
    private BlockPos remotePickupWarehouse=BlockPos.ZERO,remotePickupRally=BlockPos.ZERO;
    private final BlockPos[] remotePickupHomes={
            BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO};
    private BlockPos remoteCourierTarget=BlockPos.ZERO,remoteCourierAisle=BlockPos.ZERO,remoteRigLastKnown=BlockPos.ZERO;
    private BlockPos remoteCourierJoinAisle=BlockPos.ZERO,remoteCourierRejectedAisle=BlockPos.ZERO;
    // A single, persistent choice of service side and a checked cached
    // cardinal route. Replanning it on every tick made the pushed 2x2
    // pallet circle between competing "nearest" aisles.
    private final ArrayDeque<BlockPos> courierRoute=new ArrayDeque<>();
    private BlockPos courierRouteGoal=BlockPos.ZERO,courierBlockedCell=BlockPos.ZERO;
    private int nextCourierReplan,lastCourierProgress,courierAvoidUntil;
    private int courierStalls,courierRetargetCooldown;
    private double courierProgressDistance=Double.POSITIVE_INFINITY;
    private ItemStack remoteFresh=ItemStack.EMPTY;
    private int remoteCourierPhase;
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
    // An order runner has nine REAL cargo slots. Its shulker is packed at
    // the station, not invisibly inside the worker.
    private boolean stationOrder;
    private boolean palletStackJob;
    private boolean rackStairMover;
    private boolean rackStairRequestChecked,rackStairRequestActive;
    private long rackStairDeadline;
    private BlockPos rackStairLast=BlockPos.ZERO;
    private BlockPos orderPoint=BlockPos.ZERO;
    private NonNullList<ItemStack> orderCargo=NonNullList.withSize(9,ItemStack.EMPTY);
    // Four workers are independently saved and physically navigate both
    // checkpoints; their sealed pallet remains escrowed on the source PTP.
    private boolean haulJob;
    private int haulTicket,haulIndex;
    private boolean playerHaul;
    // Stable identity assigned by the shelf on insertion. Job labels are
    // presentation only and never overwrite this saved identity.
    private String golemName="";
    private boolean groundPickup;
    private boolean forkliftDriver;
    private boolean forkliftEscort;
    private UUID forkliftEscortOwner;
    private java.util.UUID forkliftId;
    private boolean rackPickup;
    // Fixed physical service face for Direct pickup; DO NOT reselect the
    // nearest side each tick, which previously made workers spin/freeze.
    private Direction playerPtpFace=Direction.NORTH;
    private int rackFloorY;
    private String haulOwner="";
    /** Set after the followed player dies/leaves the world; no long-range
     * pursuit after respawn. The worker waits where it stopped until the
     * same owner walks back within 16 blocks. Persist across chunk reloads. */
    private boolean waitingForPlayer;
    private ItemStack carryingPallet=ItemStack.EMPTY;
    private NonNullList<ItemStack> palletContents=NonNullList.withSize(216,ItemStack.EMPTY);
    private boolean deathDropsReleased;
    private int waitTicks;
    // Return trips follow SAME-LEVEL, checked walkways and turn corners,
    // rather than vanilla paths that climb over boxes to reach shelf tops.
    private final ArrayDeque<BlockPos> flatHomeRoute=new ArrayDeque<>();
    private int nextHomeReplanTick,lastHomeProgressTick;
    private double lastHomeWaypointDistance=Double.POSITIVE_INFINITY;
    private BlockPos lastHomeGoal=BlockPos.ZERO;
    private BlockPos blockedHomeCell=BlockPos.ZERO;
    private int avoidHomeUntil;
    private String routeIssue="";
    private int routeFailures;
    // A remote mission does not wander forever when a water barrier or
    // blocked dry corridor makes the destination unreachable. The scanner
    // deliberately releases this persisted safety pause.
    private boolean remotePathPaused;
    private String remotePauseReason="";
    private int remoteUnreachableAttempts;
    public String golemName(){return golemName;}
    public void setGolemName(String name){
        golemName=name==null||name.isBlank()
                ?WarehouseGolemNames.random(level().getRandom()):name;
        updateTaskLabel();
    }
    private boolean showDebugStatuses(){
        return level() instanceof ServerLevel world&&world.hasChunkAt(home)
                &&world.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                &&shelf.debugStatuses();
    }
    public StorageGolem(EntityType<? extends StorageGolem> type,Level level){
        super(type,level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,20)
                .add(Attributes.MOVEMENT_SPEED,0.2625)
                .add(Attributes.FOLLOW_RANGE,28);
    }
    @Override protected void registerGoals() {
        // Jobs choose destinations server-side. Random strolling would
        // interfere with deliberate pallet-to-rack navigation.
    }
    /** New workers inherit their shelf's current persisted recall epoch,
     * so yesterday's RECALL ALL never cancels brand-new sorting jobs. */
    private void markCurrentRecall(){
        if(level() instanceof ServerLevel server&&server.hasChunkAt(home)
                &&server.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf)
            observedRecallSerial=shelf.recallSerial();
    }
    private void checkPendingRecall(ServerLevel server){
        if(tickCount%20!=0||!server.hasChunkAt(home))return;
        if(server.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                &&shelf.recallSerial()>observedRecallSerial){
            observedRecallSerial=shelf.recallSerial();
            recallToShelf(server);
        }
    }
    /** One independently pathfinding Golem pushes ONE unwrapped empty deck
     * while the three-worker MovingPallet rig heads for the wrapped target.
     * Both reference one UUID, never the player's current position. */
    public boolean beginRemoteCourier(UUID rig,BlockPos target,ItemStack empty){
        if(rig==null||target==null||!StackOfPalletsItem.isPallet(empty)
                ||empty.getCount()!=1||remoteCourier)return false;
        remoteCourier=true;
        remotePathPaused=false;
        remotePauseReason="";
        remoteUnreachableAttempts=0;
        remoteCarrierId=rig;
        remoteCourierTarget=target.immutable();
        remoteCourierAisle=BlockPos.ZERO;
        remoteCourierJoinAisle=BlockPos.ZERO;
        remoteCourierRejectedAisle=BlockPos.ZERO;
        resetCourierRoute();
        remoteRigLastKnown=blockPosition().immutable();
        remoteFresh=com.caszgamermd.caszualadditions.pallet.GlassPalletWrap
                .withWrap(empty.copyWithCount(1),false);
        remoteCourierPhase=0;
        stage=0;haulJob=false;playerHaul=false;
        syncRemoteFreshVisual();
        syncPalletAction();
        setCustomName(Component.literal("EMPTY PALLET | 1 GOLEM PUSHING"));
        setCustomNameVisible(true);
        return true;
    }
    public boolean remoteCourierActive(){return remoteCourier;}
    /** Real carrying stage, not a lingering client-visible name.
     * Stage 4 is idle/awaiting orders and MUST have arms at the sides. */
    public boolean carryingPalletPose(){
        return entityData.get(DISPLAY_PALLET_ACTION)==1;
    }
    public boolean pushingPalletPose(){
        return entityData.get(DISPLAY_PALLET_ACTION)==2;
    }
    private void syncPalletAction(){
        int action=remoteCourier&&remoteCourierPhase==0
                &&!remoteFresh.isEmpty()?2:playerHaul&&stage==1?1:0;
        if(entityData.get(DISPLAY_PALLET_ACTION)!=action)
            entityData.set(DISPLAY_PALLET_ACTION,action);
    }

    public void beginRemotePickup(UUID mission,UUID courier,BlockPos target,
            BlockPos warehouse,BlockPos rally,BlockPos[] fourHomes){
        if(mission==null||courier==null||fourHomes.length!=4)
            throw new IllegalArgumentException("Remote pickup needs 4 shelf identities");
        remotePickup=true;
        remotePathPaused=false;
        remotePauseReason="";
        remoteUnreachableAttempts=0;
        remotePickupMission=mission;
        remotePickupCourier=courier;
        remotePickupTarget=target.immutable();
        remotePickupWarehouse=warehouse.immutable();
        remotePickupRally=rally.immutable();
        for(int i=0;i<4;i++)remotePickupHomes[i]=fourHomes[i].immutable();
        stage=0;waitTicks=0;
        markCurrentRecall();
        boolean bayOnly=DroneBayNoReplacementDispatch.NO_COURIER.equals(courier);
        setCustomName(Component.literal((bayOnly?"BAY PICKUP ":"REMOTE PICKUP ")+(haulIndex+1)
                +(bayOnly?"/4":"/3")+" | EMPTY HANDED"));
        setCustomNameVisible(true);
    }
    public void beginNodePush(UUID mission,BlockPos node,UUID vehicle,
            BlockPos goal,boolean loading){
        nodePush=true;nodePushMission=mission;nodePushVehicle=vehicle;
        nodePushNode=node.immutable();nodePushGoal=goal.immutable();
        nodePushLoading=loading;haulJob=false;playerHaul=false;stage=0;
        setCustomName(Component.literal("TRANSFER PUSH "+(haulIndex+1)+"/2"));
        setCustomNameVisible(true);
    }
    public boolean nodePushActive(){return nodePush;}
    public UUID nodePushMission(){return nodePushMission;}
    public UUID nodePushVehicle(){return nodePushVehicle;}
    public BlockPos nodePushNode(){return nodePushNode;}
    public int nodePushIndex(){return haulIndex;}
    public boolean nodePushLoading(){return nodePushLoading;}
    public boolean nodePushReady(ServerLevel level){
        return nodePush&&routeDistanceSq(level,nodePushGoal)<1.30;
    }
    public void finishNodePush(){
        nodePush=false;nodePushMission=null;nodePushVehicle=null;
        stage=2;haulJob=false;playerHaul=false;
        routeIssue="PUSH COMPLETE — RETURNING TO SHELF";
    }
    private void tickNodePush(ServerLevel level){
        if(nodePushVehicle==null||nodePushMission==null){finishNodePush();return;}
        if(tickCount%40==0){
            WarehouseRemoteTickets.keep(level,nodePushNode);
            WarehouseRemoteTickets.keep(level,nodePushGoal);
            WarehouseRemoteTickets.keep(level,blockPosition());
        }
        if(!level.hasChunkAt(nodePushNode)
                ||level.getEntity(nodePushVehicle)==null){
            routeIssue="WAITING FOR TRAIN OR CONVEYOR TO RECONNECT";
            return;
        }
        if(!StorageGolemReturnPath.walkable(level,nodePushGoal)){
            routeIssue="WAITING FOR CLEAR PUSH AISLE";
            return;
        }
        if(!nodePushReady(level)){
            routeIssue="WALKING TO PALLET TRANSFER NODE";
            walkTo(level,nodePushGoal,1.02);
            return;
        }
        getNavigation().stop();
        setDeltaMovement(0,getDeltaMovement().y,0);
        routeIssue="PUSHING PALLET TO TRANSPORT WITH PARTNER";
        if(haulIndex==0)PalletNodePushCrew.tryComplete(level,this);
    }
    public boolean remotePickupActive(){return remotePickup;}
    public boolean remotePathPaused(){return remotePathPaused;}
    private void pauseRemotePath(String reason){
        remotePathPaused=true;
        remotePauseReason=reason;
        routeIssue="WAITING FOR HELP: "+reason+" | RIGHT-CLICK SCANNER";
        getNavigation().stop();
        flatWorkRoute.clear();
        courierRoute.clear();
        // Ice retains momentum when navigation stops. A genuinely paused
        // worker must not slide sideways into the other workers or pallet.
        setDeltaMovement(0,getDeltaMovement().y,0);
    }
    /** Scanner interaction: only a deliberate player action retries an
     * unreachable remote route. Never teleport the worker or reset cargo. */
    public boolean resumeRemotePath(){
        if(!(remotePickup||remoteCourier)||!remotePathPaused)return false;
        remotePathPaused=false;
        remotePauseReason="";
        remoteUnreachableAttempts=0;
        routeIssue="RETRYING DRY-LAND ROUTE";
        flatWorkRoute.clear();
        workGoal=BlockPos.ZERO;
        nextWorkPlan=0;
        blockedWorkCell=BlockPos.ZERO;
        avoidWorkUntil=0;
        remoteCourierAisle=BlockPos.ZERO;
        remoteCourierRejectedAisle=BlockPos.ZERO;
        courierRetargetCooldown=0;
        resetCourierRoute();
        getNavigation().stop();
        return true;
    }
    public int remotePickupIndex(){return haulIndex;}
    public UUID remotePickupMission(){return remotePickupMission;}
    public UUID remotePickupCourier(){return remotePickupCourier;}
    public BlockPos remotePickupTarget(){return remotePickupTarget;}
    public BlockPos remotePickupWarehouse(){return remotePickupWarehouse;}
    public BlockPos remotePickupRally(){return remotePickupRally;}
    public BlockPos[] remotePickupHomes(){return remotePickupHomes.clone();}
    /** Physically reached the assigned dry rally tile (ice is solid floor).
     * A paused worker never counts as a successfully assembled lift slot. */
    public boolean atRemotePickupGoal(ServerLevel level,BlockPos goal){
        return !remotePathPaused&&routeDistanceSq(level,goal)<1.30;
    }
    public UUID remotePickupOwner(){
        try{return UUID.fromString(haulOwner);}
        catch(IllegalArgumentException ex){return null;}
    }
    private void tickRemotePickup(ServerLevel level){
        if(remotePickupMission==null)return;
        if(tickCount%40==0){
            WarehouseRemoteTickets.keep(level,blockPosition());
            WarehouseRemoteTickets.keep(level,remotePickupTarget);
            WarehouseRemoteTickets.keep(level,remotePickupRally);
            double dx=remotePickupRally.getX()+.5-getX();
            double dz=remotePickupRally.getZ()+.5-getZ();
            double length=Math.hypot(dx,dz);
            if(length>8)WarehouseRemoteTickets.keep(level,new BlockPos(
                    (int)Math.floor(getX()+dx/length*16),
                    blockPosition().getY(),
                    (int)Math.floor(getZ()+dz/length*16)));
        }
        if(remotePathPaused){
            routeIssue="WAITING FOR HELP: "+remotePauseReason+" | RIGHT-CLICK SCANNER";
            getNavigation().stop();
            return;
        }
        if(isInWater()||level.getFluidState(blockPosition()).is(FluidTags.WATER)){
            pauseRemotePath("GOLEM IS IN WATER; BUILD A DRY ROUTE");
            return;
        }
        // No rigid carrier exists on the outbound journey. Each mob has
        // its own saved target, waypoint, AI, walking animation, and route.
        if(!level.hasChunkAt(remotePickupTarget)
                ||StorageGroundPalletTransfer.wrappedController(
                        level,remotePickupTarget)==null){
            routeIssue="WAITING FOR WRAPPED TARGET PALLET";return;
        }
        BlockPos goal=RemotePalletPickupCrew.individualGoal(
                remotePickupRally,remotePickupTarget,haulIndex);
        if(!StorageGolemReturnPath.walkable(level,goal)){
            routeIssue="WAITING FOR CLEAR THREE-GOLEM SERVICE AISLE";
            getNavigation().stop();return;
        }
        if(routeDistanceSq(level,goal)>1.30){
            walkTo(level,goal,1.05);
            // Preserve real route diagnostics (blocked chunks, no path)
            // instead of hiding every failure under a "walking" label.
            if(routeIssue.isBlank())
                routeIssue="WALKING EMPTY HANDED TO REMOTE TARGET";
            return;
        }
        getNavigation().stop();
        // Prevent slippery ice from drifting an already arrived worker
        // back across its partner's assigned rally cell.
        setDeltaMovement(0,getDeltaMovement().y,0);
        flatWorkRoute.clear();
        remoteUnreachableAttempts=0;
        routeIssue="READY TO LIFT; WAITING FOR TEAM";
        if(haulIndex==0)RemotePalletPickupCrew.tryLift(level,this);
    }

    /** If the three-worker cargo rig was destroyed, reclaim the actual
     * fourth Golem as an independently walking shelf worker. Its original
     * unwrapped replacement pallet is dropped once for player recovery. */
    public void remoteCarrierDestroyed(ServerLevel level){
        if(!remoteCourier)return;
        if(!remoteFresh.isEmpty()){
            Containers.dropItemStack(level,getX(),getY(),getZ(),remoteFresh);
            remoteFresh=ItemStack.EMPTY;
        }
        remoteCourier=false;
        remoteCarrierId=null;
        entityData.set(DISPLAY_PALLET,ItemStack.EMPTY);
        stage=2;
        haulJob=false;playerHaul=false;
        remoteCourierPhase=0;
        syncPalletAction();
        routeIssue="CARRIER LOST; RETURNING TO SHELF";
        getNavigation().stop();
    }

    private void syncRemoteFreshVisual(){
        if(remoteCourier)entityData.set(DISPLAY_PALLET,remoteFresh.copy());
    }
    private void resetCourierRoute(){
        courierRoute.clear();
        courierRouteGoal=BlockPos.ZERO;
        courierBlockedCell=BlockPos.ZERO;
        nextCourierReplan=0;
        lastCourierProgress=tickCount;
        courierAvoidUntil=0;
        courierProgressDistance=Double.POSITIVE_INFINITY;
    }
    /** The physical replacement deck is two blocks wide; only its push
     * route needs a three-wide aisle. AFTER placement the worker is empty
     * and can walk through regular one-wide worker corridors to rejoin. */
    private boolean followCourierRoute(ServerLevel level,BlockPos goal,
            boolean pushing){
        if(remotePathPaused||goal.equals(BlockPos.ZERO))return false;
        if(!goal.equals(courierRouteGoal)){
            resetCourierRoute();
            courierRouteGoal=goal.immutable();
        }
        // Require arrival at the actual reserved tile. The previous
        // distance-only test accepted adjacent tiles and left the courier
        // waiting for a placement that could never be committed.
        double finalDistance=distanceToSqr(goal.getX()+.5,
                WarehouseTerrain.standingY(level,goal),goal.getZ()+.5);
        if(blockPosition().getX()==goal.getX()
                &&blockPosition().getZ()==goal.getZ()
                &&finalDistance<.85){
            getNavigation().stop();
            courierRoute.clear();
            courierStalls=0;
            return true;
        }
        while(!courierRoute.isEmpty()){
            BlockPos cell=courierRoute.peekFirst();
            if(!(pushing?RemotePalletCourierRoute.clear(level,cell)
                    :StorageGolemReturnPath.walkable(level,cell))){
                // A pallet may have been placed beside the route after the
                // courier planned it. Reject the ACTUAL blocked waypoint;
                // retrying an identical route causes a Golem to spin into
                // that pallet forever.
                courierBlockedCell=cell.immutable();
                courierAvoidUntil=tickCount+140;
                courierRoute.clear();
                nextCourierReplan=tickCount+10;
                courierProgressDistance=Double.POSITIVE_INFINITY;
                lastCourierProgress=tickCount;
                courierStalls++;
                routeIssue="REROUTING AROUND NEARBY PALLET / BLOCKED AISLE";
                if(courierStalls>=3)
                    pauseRemotePath("NEARBY PALLET BLOCKS COURIER ROUTE");
                getNavigation().stop();
                return false;
            }
            if(distanceToSqr(cell.getX()+.5,
                    WarehouseTerrain.standingY(level,cell),
                    cell.getZ()+.5)<.38){
                courierRoute.removeFirst();
                courierProgressDistance=Double.POSITIVE_INFINITY;
                lastCourierProgress=tickCount;
                courierStalls=0;
            }else break;
        }
        if(courierRoute.isEmpty()&&tickCount>=nextCourierReplan){
            BlockPos avoid=tickCount<courierAvoidUntil
                    ?courierBlockedCell:BlockPos.ZERO;
            BlockPos crewRally=BlockPos.ZERO;
            if(pushing)for(var entity:level.getAllEntities()){
                if(entity instanceof StorageGolem worker
                        &&worker.remotePickupActive()
                        &&remoteCarrierId!=null
                        &&remoteCarrierId.equals(worker.remotePickupMission())){
                    crewRally=worker.remotePickupRally();
                    break;
                }
            }
            var route=RemotePalletCourierRoute.route(level,blockPosition(),
                    goal,avoid,pushing,crewRally);
            nextCourierReplan=tickCount+25;
            if(route==null||route.isEmpty()){
                courierStalls++;
                if(courierStalls>=3){
                    pauseRemotePath("NO SAFE DRY CORRIDOR FOR COURIER");
                    return false;
                }
                // An unreachable final destination is itself a blocked
                // service side. Don't wait forever replaying partial routes
                // against the same wall or spinning toward its corners.
                routeIssue="WAITING FOR "+(pushing?"WIDE PALLET":"GOLEM")
                        +" CORRIDOR; RETRYING";
                getNavigation().stop();
                return false;
            }
            courierRoute.addAll(route);
            remoteUnreachableAttempts=0;
            courierProgressDistance=Double.POSITIVE_INFINITY;
            lastCourierProgress=tickCount;
        }
        if(courierRoute.isEmpty()){
            getNavigation().stop();
            return false;
        }
        BlockPos next=courierRoute.peekFirst();
        double distance=distanceToSqr(next.getX()+.5,next.getY(),
                next.getZ()+.5);
        if(distance+.10<courierProgressDistance){
            courierProgressDistance=distance;
            lastCourierProgress=tickCount;
        }else if(tickCount-lastCourierProgress>75){
            // Never return to a guessed opposite side or orbit the pallet.
            // Save the job/goal, temporarily avoid the blocked aisle cell
            // and search for a DIFFERENT checked cardinal corridor.
            courierBlockedCell=next.immutable();
            courierAvoidUntil=tickCount+140;
            courierRoute.clear();
            nextCourierReplan=tickCount+12;
            lastCourierProgress=tickCount;
            courierProgressDistance=Double.POSITIVE_INFINITY;
            courierStalls++;
            routeIssue="REROUTING AROUND BLOCKED PALLET AISLE";
            getNavigation().stop();
            if(courierStalls>=3){
                pauseRemotePath("COURIER STALLED AT BLOCKED AISLE");
            }
            return false;
        }
        // Don't send a new vanilla A* destination on every tick. Steer
        // toward the stable next cell like other warehouse Golems.
        followTerrainStep(next,1.06);
        routeIssue=pushing?"PUSHING PALLET THROUGH CLEAR AISLE"
                :"WALKING TO THREE-GOLEM LOADED TEAM";
        return false;
    }
    private void courierRejectedAisleFromStall(){
        remoteCourierRejectedAisle=remoteCourierAisle;
        remoteCourierAisle=BlockPos.ZERO;
        courierRoute.clear();
        courierRouteGoal=BlockPos.ZERO;
        courierStalls=0;
        courierRetargetCooldown=tickCount+50;
    }
    /** Flat-first single-worker path; never tries to squeeze the 2x2
     * carrier through one-wide gaps or path into the old target blocks. */
    private void tickRemoteCourier(ServerLevel level){
        if(remoteCarrierId==null)return;
        if(remotePathPaused){
            routeIssue="WAITING FOR HELP: "+remotePauseReason+" | RIGHT-CLICK SCANNER";
            getNavigation().stop();
            return;
        }
        if(isInWater()||level.getFluidState(blockPosition()).is(FluidTags.WATER)){
            pauseRemotePath("COURIER IS IN WATER; BUILD A DRY ROUTE");
            return;
        }
        // The courier cannot rely on the pallet team being loaded to
        // renew its own tickets. Both independent travellers need their own
        // rolling simulation corridor, including a point ahead of travel.
        if(tickCount%40==0){
            WarehouseRemoteTickets.keep(level,blockPosition());
            WarehouseRemoteTickets.keep(level,remoteCourierTarget);
            if(!remoteRigLastKnown.equals(BlockPos.ZERO))
                WarehouseRemoteTickets.keep(level,remoteRigLastKnown);
            // Load the ACTUAL planned waypoint, not the target's solid
            // pallet footprint. Direct-line guesses stalled through racks.
            BlockPos goal=!courierRoute.isEmpty()?courierRoute.peekFirst()
                    :remoteCourierPhase==0&&!remoteCourierAisle.equals(BlockPos.ZERO)
                        ?remoteCourierAisle
                        :remoteCourierPhase==1&&!remoteCourierJoinAisle.equals(BlockPos.ZERO)
                            ?remoteCourierJoinAisle:remoteCourierTarget;
            WarehouseRemoteTickets.keep(level,goal);
            double dx=goal.getX()+.5-getX(),dz=goal.getZ()+.5-getZ();
            double length=Math.hypot(dx,dz);
            if(length>8)WarehouseRemoteTickets.keep(level,new BlockPos(
                    (int)Math.floor(getX()+dx/length*16),
                    blockPosition().getY(),
                    (int)Math.floor(getZ()+dz/length*16)));
        }
        // The rig deliberately DOES NOT EXIST while the three workers are
        // still walking independently to the target. The courier must
        // travel at the same time and wait ONLY at its destination.
        var other=level.getEntity(remoteCarrierId);
        MovingPallet rig=other instanceof MovingPallet carrier?carrier:null;
        if(rig!=null)remoteRigLastKnown=rig.blockPosition().immutable();
        // Restart between fourth-worker handoff and courier discard: the
        // recovered carrier already owns the worker identity. Don't run an
        // orphaned fifth Golem or endlessly retry an already-committed join.
        if(remoteCourierPhase==1&&remoteFresh.isEmpty()
                &&rig!=null&&rig.returningToWarehouse()){
            remoteCourier=false;
            discard();
            return;
        }
        if(remoteCourierPhase==0){
            BlockPos trio=BlockPos.ZERO;
            // Independent lifters retain their reserved rally until pickup.
            // Don't mistake their work area for the courier's dropoff lane.
            for(var entity:level.getAllEntities()){
                if(entity instanceof StorageGolem worker
                        &&worker.remotePickupActive()
                        &&remoteCarrierId.equals(worker.remotePickupMission())){
                    trio=worker.remotePickupRally();
                    break;
                }
            }
            BlockPos[] assignedFootprint=RemotePalletCourierRoute.targetFootprint(
                    level,remoteCourierTarget,rig,remoteFresh);
            if(assignedFootprint==null){
                routeIssue="WAITING FOR SAVED TARGET PALLET FOOTPRINT";
                getNavigation().stop();
                return;
            }
            // A golem already next to the correct pallet must NEVER run
            // 4-7 blocks away toward an obsolete chosen service aisle.
            // This also repairs persisted pre-fix courier missions without
            // changing their target UUID or moving the carried pallet item.
            if(RemotePalletCourierRoute.validServiceAisle(
                    level,blockPosition(),assignedFootprint,trio)
                    &&!blockPosition().equals(remoteCourierAisle)){
                remoteCourierAisle=blockPosition().immutable();
                remoteCourierRejectedAisle=BlockPos.ZERO;
                resetCourierRoute();
            }
            if(!remoteCourierAisle.equals(BlockPos.ZERO)
                    &&!RemotePalletCourierRoute.validServiceAisle(
                            level,remoteCourierAisle,assignedFootprint,trio)){
                // Old builds cached a point 4-7 blocks from the TARGET
                // CORNER, sometimes beside a completely different pallet.
                // Retire any such obsolete or newly obstructed aisle.
                remoteCourierRejectedAisle=remoteCourierAisle;
                remoteCourierAisle=BlockPos.ZERO;
                resetCourierRoute();
            }
            if(remoteCourierAisle.equals(BlockPos.ZERO)){
                if(tickCount<courierRetargetCooldown){
                    routeIssue="WAITING FOR TARGET-PERIMETER RETRY";
                    getNavigation().stop();
                    return;
                }
                remoteCourierAisle=RemotePalletCourierRoute.chooseServiceAisle(
                        level,blockPosition(),remoteCourierTarget,trio,
                        remoteCourierRejectedAisle,rig,remoteFresh);
                if(remoteCourierAisle.equals(BlockPos.ZERO)){
                    remoteCourierRejectedAisle=BlockPos.ZERO;
                    courierRetargetCooldown=tickCount+60;
                    if(++remoteUnreachableAttempts>=3)
                        pauseRemotePath("NO CLEAR SERVICE AISLE AT ASSIGNED PALLET");
                    else routeIssue="WAITING FOR CLEAR AISLE AT ASSIGNED PALLET";
                    getNavigation().stop();return;
                }
                remoteUnreachableAttempts=0;
                resetCourierRoute();
            }
            if(!followCourierRoute(level,remoteCourierAisle,true)){
                if(!remotePathPaused&&!routeIssue.startsWith("WAITING")
                        &&!routeIssue.startsWith("REROUTING"))
                    routeIssue="PUSHING FULL-SIZE PALLET TO ASSIGNED TARGET AISLE";
                return;
            }
            getNavigation().stop();
            if(rig==null||!rig.readyForCourierPlacement()){
                routeIssue="WAITING FOR THREE INDIVIDUAL GOLEMS TO LIFT AND MOVE ASIDE";
                return;
            }
            if(StorageGroundPalletTransfer.placeRemoteReplacement(level,
                    remoteCourierTarget,rig,this,remoteFresh)){
                remoteFresh=ItemStack.EMPTY;
                remoteCourierPhase=1;
                syncPalletAction();
                remoteCourierAisle=BlockPos.ZERO;
                remoteCourierJoinAisle=BlockPos.ZERO;
                resetCourierRoute();
                syncRemoteFreshVisual();
                routeIssue="EMPTY PALLET INSTALLED; JOINING CARRY PARTY";
            }else routeIssue="WAITING FOR CLEAR 2x2 PLACEMENT FOOTPRINT";
            return;
        }
        if(remoteCourierPhase==1){
            if(rig==null){
                routeIssue="WAITING FOR LOADED PALLET TEAM TO RELOAD";
                return;
            }
            // The carrier's CENTER is physically occupied by the loaded
            // 1.85-wide platform. Join from its first clear side aisle, not
            // a goal INSIDE the collision box (which caused 20s stalls).
            if(distanceToSqr(rig)<9.61
                    &&Math.abs(getY()-rig.getY())<1.8){
                if(rig.joinRemoteCourier(getUUID())){
                    remoteCourier=false;remoteFresh=ItemStack.EMPTY;
                    entityData.set(DISPLAY_PALLET,ItemStack.EMPTY);
                    discard(); // The rig already owns all FOUR shelf tokens.
                }else routeIssue="WAITING FOR THREE-GOLEM CARRIER TO ACCEPT FOURTH";
                return;
            }
            if(remoteCourierJoinAisle.equals(BlockPos.ZERO)
                    ||!StorageGolemReturnPath.walkable(level,remoteCourierJoinAisle)){
                remoteCourierJoinAisle=RemotePalletCourierRoute.chooseJoinAisle(
                        level,blockPosition(),rig.blockPosition());
                resetCourierRoute();
            }
            if(remoteCourierJoinAisle.equals(BlockPos.ZERO)){
                routeIssue="WAITING FOR CLEAR APPROACH TO LOADED TEAM";
                getNavigation().stop();
                return;
            }
            if(!followCourierRoute(level,remoteCourierJoinAisle,false)){
                if(!routeIssue.startsWith("WAITING")
                        &&!routeIssue.startsWith("REROUTING"))
                    routeIssue="REJOINING THREE-GOLEM LOADED CARRIER";
                return;
            }
            // If the old join point is too far from an off-center rig,
            // throw away the incorrect stale aim instead of standing
            // forever at a waypoint which cannot finish the handoff.
            remoteCourierJoinAisle=BlockPos.ZERO;
            resetCourierRoute();
            routeIssue="ADJUSTING TO LOADED CARRIER'S ACTUAL POSITION";
        }
    }

    /** A second Golem is dispatched specifically to shepherd a mobile
     * stair unit when an elevated rack needs worker access. No cargo. */
    public void assignForkliftEscort(BlockPos shelf,UUID owner){
        home=shelf.immutable();markCurrentRecall();
        forkliftEscort=true;forkliftEscortOwner=owner;
        forkliftDriver=false;forkliftId=null;
        stage=7;cargo=ItemStack.EMPTY;emptyBox=ItemStack.EMPTY;
    }
    public boolean escortFor(UUID owner){
        return forkliftEscort&&owner!=null&&owner.equals(forkliftEscortOwner);
    }
    public void boardAssignedForklift(UUID vehicle){
        if(!forkliftEscort)return;
        forkliftEscort=false;forkliftEscortOwner=null;
        assignForkliftDriver(home,vehicle);
    }
    public boolean isDrivingForklift(){return forkliftDriver;}
    public void dismissForkliftEscort(){
        if(!forkliftEscort)return;
        forkliftEscort=false;forkliftEscortOwner=null;
        stage=2;getNavigation().stop();
    }
    private void tickForkliftEscort(ServerLevel level){
        if(forkliftEscortOwner==null){dismissForkliftEscort();return;}
        var owner=level.getServer().getPlayerList().getPlayer(forkliftEscortOwner);
        if(owner==null||owner.level()!=level){dismissForkliftEscort();return;}
        BlockPos goal=owner.blockPosition();
        if(distanceToSqr(owner)>6.25)walkTo(level,goal,1.10);
        else getNavigation().stop();
    }
    public void assignForkliftDriver(BlockPos shelf,java.util.UUID vehicle){
        home=shelf.immutable();markCurrentRecall();
        forkliftDriver=true;forkliftId=vehicle;
        forkliftEscort=false;forkliftEscortOwner=null;
        stage=7;cargo=ItemStack.EMPTY;
        emptyBox=ItemStack.EMPTY;
    }
    public void finishForkliftDriving(){
        if(!forkliftDriver)return;
        stopRiding();forkliftDriver=false;forkliftId=null;
        stage=2;getNavigation().stop();
    }
    private void tickForkliftDriver(ServerLevel level){
        if(forkliftId==null
                ||!(level.getEntity(forkliftId) instanceof WarehouseForklift lift)){
            finishForkliftDriving();
            return;
        }
        if(getVehicle()==lift){getNavigation().stop();return;}
        if(getVehicle()!=null)stopRiding();
        double distance=distanceToSqr(lift);
        if(distance<3.24){
            getNavigation().stop();
            startRiding(lift);
            return;
        }
        walkTo(level,lift.blockPosition(),1.08);
    }

    public void assignRackStairMover(BlockPos shelf,BlockPos stairs){
        home=shelf.immutable();markCurrentRecall();
        rackStairLast=stairs.immutable();rackStairMover=true;
        stage=6;cargo=ItemStack.EMPTY;emptyBox=ItemStack.EMPTY;
    }
    private void tickRackStairs(ServerLevel level){
        RackStairsBlockEntity stairs=WarehouseRackAccess.findAssignedStairs(
                level,getUUID(),rackStairLast);
        if(stairs==null){
            rackStairMover=false;stage=2;getNavigation().stop();
            return;
        }
        BlockPos current=stairs.getBlockPos();
        rackStairLast=current.immutable();
        if(!stairs.reserved()){
            rackStairMover=false;stage=2;getNavigation().stop();
            return;
        }
        // Work from alongside the equipment, NEVER stand on its steps:
        // standing on it would activate the occupant safety interlock.
        Direction face=stairs.getBlockState().getValue(RackStairsBlock.FACING);
        BlockPos aside=current.relative(face.getClockWise(),1);
        if(!StorageGolemReturnPath.walkable(level,aside))
            aside=current.relative(face.getCounterClockWise(),1);
        if(StorageGolemReturnPath.walkable(level,aside)
                &&distanceToSqr(aside.getX()+.5,aside.getY(),
                        aside.getZ()+.5)>2.25)walkTo(level,aside,1.10);
        else getNavigation().stop();
    }
    public void assign(BlockPos home,BlockPos source,BlockPos dest,ItemStack sample){
        rackStairRequestChecked=false;
        rackStairRequestActive=false;
        this.home=home.immutable();markCurrentRecall();
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
    public void assignPalletStack(BlockPos shelf,BlockPos sourcePtp,BlockPos rack){
        assign(shelf,sourcePtp,rack,new ItemStack(RackContent.STACK_OF_PALLETS));
        palletStackJob=true;
        itemId="caszual_additions:stack_of_pallets";
    }
    public boolean isPalletStackJobFrom(BlockPos ptp){
        return palletStackJob&&stage!=2&&source.equals(ptp);
    }
    public void assignOutput(BlockPos home,BlockPos source,BlockPos destination,
            ItemStack sample,int ticket,int quantity){
        assign(home,source,destination,sample);
        outputJob=true;outputTicket=ticket;
        pickupLimit=Math.clamp(quantity,1,64);
    }
    public void assignStationOrder(BlockPos station,BlockPos ptp,int amount){
        stationOrder=true;
        destination=station.immutable();
        orderPoint=ptp.immutable();
        pickupLimit=Math.clamp(amount,1,9*64);
        orderCargo=NonNullList.withSize(9,ItemStack.EMPTY);
        stage=0;
    }
    private List<ItemStack> orderItems(){
        List<ItemStack> stacks=new ArrayList<>();
        for(ItemStack stack:orderCargo)if(!stack.isEmpty())stacks.add(stack);
        return stacks;
    }
    private void dropOrderCargo(ServerLevel level){
        for(int i=0;i<9;i++){
            ItemStack item=orderCargo.get(i);
            if(!item.isEmpty())
                Containers.dropItemStack(level,getX(),getY(),getZ(),item.copy());
            orderCargo.set(i,ItemStack.EMPTY);
        }
    }
    /** A packed worker physically visits its shelf-connected magazine. */
    public void assignPackedOutput(BlockPos tank,int amount){
        packedJob=true;magazine=tank.immutable();
        stage=4;
        pickupLimit=Math.clamp(amount,1,27*64);
    }
    public void assignHaul(BlockPos shelf,BlockPos transfer,BlockPos rack,
            int ticket,int index){
        home=shelf.immutable();markCurrentRecall();source=transfer.immutable();destination=rack.immutable();
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
        waitingForPlayer=false;
        // RECALL ALL persists as a serial number on the shelf. Workers
        // deployed AFTER that command must inherit the current serial or
        // they immediately interpret the OLD recall as a NEW instruction
        // and turn around on their very first task tick.
        markCurrentRecall();
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
            rackPickup=false;
            haulTicket=0;
            stage=0;
            setCustomName(Component.literal("RALLY "+(haulIndex+1)+"/4"));
        }
    }
    /** A rack-mounted pallet is stored in the rack controller, not in
     * the rack frame blocks the player's crosshair happens to hit. */
    public void beginRackPickup(BlockPos rackRoot,int floorY){
        if(playerHaul&&stage==4&&!holdsPallet()){
            source=rackRoot.immutable();
            destination=rackRoot.immutable();
            groundPickup=false;
            rackPickup=true;
            rackFloorY=floorY;
            haulTicket=0;
            stage=0;
            setCustomName(Component.literal("RALLY "+(haulIndex+1)+"/4"));
        }
    }
    public void cancelPendingPickup(ServerLevel level){
        if(playerHaul&&stage==0){
            cancelHaul(level);
            if(rackPickup&&level.hasChunkAt(source)
                    &&level.getBlockEntity(source) instanceof RackBlockEntity rack){
                UUID owner=ownerId();
                if(owner!=null)rack.cancelPull(owner);
            }
            groundPickup=false;
            rackPickup=false;
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
        if(remotePickup)return "WALKING EMPTY HANDED TO WRAPPED REMOTE PALLET";
        if(remoteCourier)return remoteCourierPhase==0?"PUSHING UNWRAPPED EMPTY PALLET":"JOINING LOADED THREE-GOLEM TEAM";
        if(palletStackJob)return stage==0?"COLLECTING EMPTY PALLET":stage==1?"STACKING EMPTY PALLET":"RETURNING TO SHELF";
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
            case 0->stationOrder?"PICKING 9-SLOT ORDER":"PICKING OUTPUT ORDER";
            case 1->stationOrder?"DELIVERING TO SHULKER STATION":"DELIVERING OUTPUT ORDER";
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
        if(stationOrder&&!orderItems().isEmpty())
            return "ORDER: "+orderItems().size()+"/9 slots";
        if(!cargo.isEmpty())return cargo.getHoverName().getString()
                +" x"+cargo.getCount();
        return emptyBox.isEmpty()?"EMPTY":"EMPTY SHULKER";
    }
    /** Every independently walking robot advertises its actual task.
     * The PALLET prefix is preserved for the existing pallet renderer. */
    private void updateTaskLabel(){
        String label;
        if(forkliftEscort){
            label="FORKLIFT OPERATOR | FOLLOWING PLAYER";
        }else if(forkliftDriver){
            label=getVehicle() instanceof WarehouseForklift
                    ?"DRIVER | FORKLIFT":"DRIVER | BOARDING FORKLIFT";
        }else if(rackStairMover){
            label="ESCORT | RACK STAIRS";
        }else if(nodePush){
            label="TRANSFER PUSH "+(haulIndex+1)+"/2 | "
                +(routeIssue.isBlank()?"WALKING TO NODE":routeIssue);
        }else if(remotePickup){
            boolean bayOnly=DroneBayNoReplacementDispatch.NO_COURIER.equals(remotePickupCourier);
            label=(bayOnly?"BAY PICKUP ":"REMOTE PICKUP ")+(haulIndex+1)
                    +(bayOnly?"/4":"/3")+" | EMPTY HANDED"
                    +(routeIssue.isBlank()?"":" | "+routeIssue);
        }else if(remoteCourier){
            label="EMPTY PALLET COURIER | "
                    +(remoteCourierPhase==0?"PUSHING":"JOINING")
                    +(routeIssue.isBlank()?"":" | "+routeIssue);
        }else if(playerHaul&&stage==1){
            label=waitingForPlayer?"PALLET "+(haulIndex+1)+"/4 | WAITING FOR PLAYER"
                    :"PALLET "+(haulIndex+1)+"/4 | WITH PLAYER";
        }else if(playerHaul&&stage==0){
            label="RALLY "+(haulIndex+1)+"/4 | COLLECT";
        }else if(playerHaul&&stage==4){
            label="CREW "+(haulIndex+1)+"/4 | IDLE";
        }else if(stage==2){
            label=routeIssue.isBlank()?"RETURN | SHELF":
                    "RETURN | "+routeIssue;
        }else{
            label=switch(stage){
                case 0 -> (stationOrder?"ORDER":"PICK")+" | "+shortItem();
                case 1 -> (stationOrder?"TO SHULKER":"DELIVER")+" | "+shortItem();
                case 3 -> "RETURN STOCK | "+shortItem();
                case 4 -> "GET EMPTY BOX";
                case 5 -> "RETURN EMPTY BOX";
                default -> currentWork();
            };
            if(haulJob)label="HAUL "+(haulIndex+1)+"/4 | "+(stage==0?"COLLECT":"DELIVER");
            if(!routeIssue.isBlank())label+=" | "+routeIssue;
        }
        if(golemName.isBlank()){
            // Old-world workers gain identities once without changing the
            // physical shelf token or the associated inventory.
            golemName=WarehouseGolemNames.random(level().getRandom());
        }
        String displayed=showDebugStatuses()?golemName+" | "+label:golemName;
        if(getCustomName()==null||!getCustomName().getString().equals(displayed))
            setCustomName(Component.literal(displayed));
        if(!isCustomNameVisible())setCustomNameVisible(true);
    }
    private String shortItem(){
        String id=itemId;
        int colon=id.indexOf(':');
        if(colon>=0)id=id.substring(colon+1);
        return id.isBlank()?"STOCK":id.length()>15?id.substring(0,15):id;
    }
    public int crewIndex(){return haulIndex;}
    public void beginPlayerPickup(BlockPos ptp,int ticket,Direction accessFace){
        if(playerHaul&&stage==4){
            source=ptp.immutable();destination=ptp.immutable();
            haulTicket=ticket;groundPickup=false;rackPickup=false;stage=0;
            playerPtpFace=accessFace;
            setCustomName(Component.literal("RALLY "+(haulIndex+1)+"/4"));
        }
    }
    public void startFollowing(){
        if(playerHaul&&stage==0){
            stage=1;
            groundPickup=false;
            rackPickup=false;
            setCustomName(Component.literal("PALLET "+(haulIndex+1)+"/4"));
        }
    }
    public void clearDelivery(){
        if(playerHaul&&stage==1){stage=4;haulTicket=0;
            rackPickup=false;groundPickup=false;
            setCustomName(Component.literal("CREW "+(haulIndex+1)+"/4"));}
    }
    public void dismissCrew(){
        if(playerHaul&&stage==4){stage=2;setCustomNameVisible(false);}
    }
    /** Warehouse console recall applies to ALL unshelved workers, including
     * active sorting and order runners. Live cargo remains on its worker
     * until normal shelf-return recovery, never teleports to inventory. */
    public boolean recallToShelf(ServerLevel level){
        if(remoteCourier||stage==2||holdsPallet())return false;
        if(playerHaul)cancelPendingPickup(level);
        if(haulJob&&!playerHaul)cancelHaul(level);
        if(outputJob)releaseOutput(level);
        stage=2;
        routeIssue="RECALLED TO SHELF";
        getNavigation().stop();
        flatHomeRoute.clear();
        nextHomeReplanTick=0;
        return true;
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
    /** Fallback while the team is still four separate physical workers:
     * an empty PTP receives the same full pallet/216 slots as a rack.
     * The old single leader does not relinquish cargo until verification. */
    public boolean placeIntoPtp(PtpBlockEntity point){
        if(!playerHaul||stage!=1||carryingPallet.isEmpty()
                ||point==null||!point.canInstallPallet())return false;
        if(!point.installPallet(carryingPallet.copyWithCount(1)))return false;
        boolean valid=allCargoFits(point);
        if(valid){
            for(int i=0;i<216;i++)
                if(!palletContents.get(i).isEmpty())
                    point.setItem(i,palletContents.get(i).copy());
            valid=verifyCargo(point);
        }
        if(!valid){
            point.clearContent();
            point.dropPallet();
            return false;
        }
        point.setSealed(com.caszgamermd.caszualadditions.pallet.GlassPalletWrap
                .wrapped(carryingPallet));
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
        // The former four-independent-worker carrier is a legacy path
        // with the same 216-slot pallet ownership. Recover intact here too.
        PalletDeathRecovery.release(level,blockPosition(),getX(),getY(),getZ(),
                carryingPallet,palletContents,Direction.fromYRot(getYRot()),false);
        releaseCarriedPallet();
    }
    private UUID ownerId(){
        try{return UUID.fromString(haulOwner);}catch(IllegalArgumentException ex){return null;}
    }
    private void tickPlayerHaul(ServerLevel level){
        if(stage==2){
            setCustomNameVisible(false);
            returnToShelf(level);
            return;
        }
        UUID owner=ownerId();
        if(owner==null){stage=2;return;}
        if(stage==0){
            if(!level.hasChunkAt(source))return;
            if(rackPickup){
                RackBlockEntity rack=level.getBlockEntity(source)
                        instanceof RackBlockEntity be?be:null;
                if(rack==null||!rack.hasPallet()||!rack.glassWrapped()){
                    rackPickup=false;
                    stage=4;
                    setCustomName(Component.literal("CREW "+(haulIndex+1)+"/4"));
                    return;
                }
                // Two pullers (0/1) work at the shelf front; 2/3 wait
                // farther into the aisle until the load has cleared the rack.
                // All four stand on the computed floor, even for tall racks.
                BlockPos near=approach(level,source);
                if(!level.hasChunkAt(near))return;
                if(!closeTo(near)){
                    walkTo(level,near,1.20);
                    return;
                }
                getNavigation().stop();
                if(haulIndex==0){
                    List<StorageGolem> crew=level.getEntitiesOfClass(
                            StorageGolem.class,
                            new net.minecraft.world.phys.AABB(source).inflate(24),
                            other->other.playerHaul&&other.rackPickup
                                    &&other.stage==0&&other.source.equals(source)
                                    &&other.haulOwner.equals(haulOwner));
                    StorageGolem second=crew.stream().filter(w->w.haulIndex==1)
                            .findFirst().orElse(null);
                    boolean twoReady=second!=null
                            &&second.closeTo(second.approach(level,source));
                    if(twoReady && rack.startPull(owner,rackFloorY)
                            &&rack.pullProgress()>=RackPickupPlan.PULL_TICKS){
                        boolean ready=crew.size()==4;
                        int mask=0;
                        for(StorageGolem member:crew){
                            mask=WarehouseTeamProgress.arrive(mask,member.haulIndex);
                            if(!member.closeTo(member.approach(level,source)))
                                ready=false;
                        }
                        if(ready&&WarehouseTeamProgress.complete(mask)
                                &&rack.liftToCarrier(this)){
                            if(!StoragePalletCrew.formMovingPallet(level,crew,owner))
                                for(StorageGolem worker:crew)worker.startFollowing();
                        }
                    }
                }
                return;
            }
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
                    walkTo(level,near,1.20);
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
                walkTo(level,near,1.20);
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
        if(player==null||!player.isAlive()||player.isSpectator()){
            waitingForPlayer=true;
            getNavigation().stop();
            setDeltaMovement(0,getDeltaMovement().y,0);
            return;
        }
        if(waitingForPlayer){
            if(player.distanceToSqr(getX(),getY(),getZ())>16.0*16.0){
                getNavigation().stop();
                setDeltaMovement(0,getDeltaMovement().y,0);
                return;
            }
            waitingForPlayer=false;
        }
        // An empty crew stays at the position where it last caught up.
        // No relative orientation, no forced follow-behind formation:
        // spinning the player in place must not cause any path changes.
        // Approx. one block of horizontal personal space with a small
        // start/stop hysteresis to avoid constant short movement bursts.
        double dx=getX()-player.getX(), dz=getZ()-player.getZ();
        double horizontal=dx*dx+dz*dz;
        double yDifference=Math.abs(getY()-player.getY());
        if(horizontal<1.35*1.35&&yDifference<1.0){
            getNavigation().stop();
            return;
        }
        if(horizontal>1.85*1.85||yDifference>1.5){
            if(tickCount%12==0)
                getNavigation().moveTo(player.getX(),player.getY(),player.getZ(),1.0);
        }else if(!getNavigation().isDone()&&horizontal<1.55*1.55){
            getNavigation().stop();
        }
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
            returnToShelf(level);
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
            if(tickCount%20==0)getNavigation().moveTo(walkway.getX()+.5,
                    walkway.getY(),walkway.getZ()+.5,1.10);
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
        BlockPos ptpPos=stationOrder?orderPoint:destination;
        return level.hasChunkAt(ptpPos)
                &&level.getBlockEntity(ptpPos) instanceof PtpBlockEntity ptp?ptp:null;
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
        return StorageGolemReturnPath.walkable(level,p);
    }
    /** Workstations occupy solid blocks. Choose standable aisle squares
     * at the robot's foot level, instead of asking vanilla pathfinding to
     * climb onto the target machine or upper rack frame. */
    private boolean occupiedByWorker(ServerLevel level,BlockPos pos){
        return !level.getEntitiesOfClass(StorageGolem.class,
                new net.minecraft.world.phys.AABB(pos).inflate(.1,.05,.1),
                golem->golem!=this&&!golem.isRemoved()).isEmpty();
    }
    private BlockPos floorApproach(ServerLevel level,BlockPos desired){
        BlockPos onFloor=new BlockPos(desired.getX(),blockPosition().getY(),desired.getZ());
        if(openWalkway(level,onFloor)&&!occupiedByWorker(level,onFloor))
            return onFloor;
        // A rack/packing-station approach is a ZONE, not one magic cell.
        // If another golem occupies the best square, use a neighboring
        // service tile instead of both workers walking into each other.
        int maxShift=openWalkway(level,onFloor)?1:3;
        for(int range=1;range<=maxShift;range++)
            for(Direction direction:Direction.Plane.HORIZONTAL){
                BlockPos nearby=onFloor.relative(direction,range);
                if(openWalkway(level,nearby)
                        &&!occupiedByWorker(level,nearby))return nearby;
            }
        return openWalkway(level,onFloor)?onFloor:desired;
    }
    /**
     * Orders use a persistent accessible SERVICE CELL, not the center of a
     * full rack block. Probe both front columns and the nearby station aisle,
     * then pick an actual traversable route rather than a geometrically
     * close wall. Keep the choice long enough for another worker to pass.
     */
    private BlockPos orderApproach(ServerLevel level,BlockPos place){
        if(place.equals(orderAccessTarget)&&stage==orderAccessStage
                &&tickCount<orderAccessExpires
                &&openWalkway(level,orderAccessGoal)
                &&(!occupiedByWorker(level,orderAccessGoal)
                    ||blockPosition().equals(orderAccessGoal)))
            return orderAccessGoal;
        orderAccessTarget=place.immutable();
        orderAccessStage=stage;
        orderAccessExpires=tickCount+60;
        BlockEntity machine=level.hasChunkAt(place)?level.getBlockEntity(place):null;
        java.util.LinkedHashSet<BlockPos> choices=new java.util.LinkedHashSet<>();
        if(machine instanceof RackBlockEntity rack){
            // The rack is opaque to Golems: approach ONLY from its FRONT,
            // never through a side/back frame or its upper cargo bays.
            for(int depth=1;depth<=3;depth++)
                for(int col=0;col<2;col++){
                    BlockPos cell=RackBlock.cell(place,rack.facing(),col)
                            .relative(rack.facing(),depth);
                    choices.add(new BlockPos(cell.getX(),blockPosition().getY(),
                            cell.getZ()));
                }
        }else{
            for(int depth=1;depth<=2;depth++)
                for(Direction side:Direction.Plane.HORIZONTAL){
                    BlockPos cell=place.relative(side,depth);
                    choices.add(new BlockPos(cell.getX(),blockPosition().getY(),
                            cell.getZ()));
                }
        }
        var occupied=occupiedAisle(level,place);
        BlockPos feet=blockPosition(),best=null;
        int bestLength=Integer.MAX_VALUE;
        for(BlockPos cell:choices){
            if(!openWalkway(level,cell)
                    ||occupied.contains(cell)&&!cell.equals(feet))continue;
            List<BlockPos> route=StorageGolemReturnPath.steps(feet,cell,
                    p->openWalkway(level,p)&&!occupied.contains(p));
            if(route!=null&&route.size()<bestLength){
                best=cell;
                bestLength=route.size();
            }
        }
        if(best==null){
            // Keep the task pending until an aisle opens; never silently
            // choose a route through the solid rack as a fallback.
            routeIssue="WAITING FOR ORDER ACCESS";
            // Wait at the FIRST front/service position, never select an
            // unverified rear rack face or accidentally complete a pickup
            // from behind a solid frame.
            return choices.isEmpty()?place:choices.iterator().next();
        }
        orderAccessGoal=best.immutable();
        routeIssue="";
        return best;
    }

    private BlockPos approach(ServerLevel level,BlockPos place) {
        if(stationOrder&&(stage==0||stage==1||stage==3))
            return orderApproach(level,place);
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
        if(playerHaul&&stage==0&&be instanceof PtpBlockEntity){
            // Same aisle throughout the job, just like rack pickup.
            // Each worker gets one standable cell OUTSIDE the 3x3 platform.
            // Never path to the actual PTP controller or cut across modules.
            return place.relative(playerPtpFace,2+haulIndex/2)
                    .relative(playerPtpFace.getClockWise(),haulIndex%2);
        }
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
                BlockPos p=floorApproach(level,place.relative(faces[side],distance));
                if(!openWalkway(level,p))continue;
                // The four workers take different available sides in index
                // order. Ordinary jobs take the closest free side to home.
                if(haulJob)return p;
                double candidate=p.distSqr(home);
                if(candidate<score){score=candidate;best=p;}
            }
            return best==null?floorApproach(level,place.south(distance)):best;
        }
        if(be instanceof RackBlockEntity rack){
            if(!rackPickup){
                BlockPos service=WarehouseSortingReach.access(level,rack);
                if(service!=null)return service;
            }
            if(rackPickup){
                RackPickupPlan plan=RackPickupPlan.find(level,rack);
                // The original target floor is saved per worker; if shelves
                // change midway, let the job fail safely rather than climb.
                if(plan!=null&&plan.floorY()==rackFloorY)
                    return plan.goal(haulIndex,
                            rack.pullProgress()>=RackPickupPlan.PULL_TICKS);
            }
            BlockPos front=RackBlock.cell(place,rack.facing(),haulIndex%2)
                    .relative(rack.facing(),1+haulIndex/2);
            BlockPos aisle=floorApproach(level,front);
            if(openWalkway(level,aisle))return aisle;
            BlockPos fallback=floorApproach(level,RackBlock.cell(place,
                    rack.facing(),haulIndex%2).relative(rack.facing(),2));
            return openWalkway(level,fallback)?fallback:aisle;
        }
        return floorApproach(level,place);
    }
    // Stable order pick/drop approach. Recomputing the nearest free cell
    // every tick makes two workers switch sides indefinitely at one shelf.
    private BlockPos orderAccessGoal=BlockPos.ZERO;
    private BlockPos orderAccessTarget=BlockPos.ZERO;
    private int orderAccessStage=-1,orderAccessExpires;
    private BlockPos workGoal=BlockPos.ZERO;
    private int lastWorkProgress, nextWorkPlan;
    private BlockPos blockedWorkCell=BlockPos.ZERO;
    private int avoidWorkUntil;
    private double lastWorkDistance=Double.POSITIVE_INFINITY;
    private int nextTerrainJumpTick;

    /**
     * Prefer a checked level corridor when destination and feet have the
     * same Y. Cache the waypoint so recalculating every AI tick doesn't
     * make the Golem stop and restart. Stalls force replanning; true level
     * changes still use vanilla ramp/stair navigation.
     */
    /** Cheap occupied-aisle snapshot, captured ONCE per A* search, never
     * queried thousands of times inside the routing predicate. */
    private java.util.Set<BlockPos> occupiedAisle(ServerLevel level,BlockPos goal){
        java.util.Set<BlockPos> positions=new java.util.HashSet<>();
        double midX=(getX()+goal.getX())*.5,midZ=(getZ()+goal.getZ())*.5;
        double range=Math.min(48,Math.max(12,Math.abs(goal.getX()-getX())*.5+7
                +Math.abs(goal.getZ()-getZ())*.5));
        for(StorageGolem other:level.getEntitiesOfClass(StorageGolem.class,
                new net.minecraft.world.phys.AABB(
                        midX-range,getY()-1,midZ-range,
                        midX+range,getY()+2,midZ+range),
                g->g!=this&&!g.isRemoved())){
            // Each member of the same 3-Golem remote squad walks to its
            // OWN fixed slot. Marking each other as dynamically BLOCKED
            // caused them to plan around each other's current square,
            // exchange planned sides every 20 ticks and dance indefinitely.
            // Entity collision still handles temporary close passing.
            if(remotePickup&&other.remotePickup
                    &&remotePickupMission!=null
                    &&remotePickupMission.equals(other.remotePickupMission))
                continue;
            // Likewise the full-width courier is moving to a separate
            // destination: don't replan a route around its moving feet
            // on every step of the same shared remote mission.
            if(remotePickup&&other.remoteCourier
                    &&remotePickupMission!=null
                    &&remotePickupMission.equals(other.remoteCarrierId))
                continue;
            positions.add(other.blockPosition());
        }
        positions.remove(blockPosition());
        return positions;
    }
    private double routeDistanceSq(ServerLevel level,BlockPos position){
        double dx=position.getX()+.5-getX(),dz=position.getZ()+.5-getZ();
        double y=WarehouseTerrain.standingY(level,position)-getY();
        return dx*dx+dz*dz+y*y;
    }
    private void steerOnFloor(BlockPos next,double speed){
        // Every warehouse waypoint requires a collision-bearing dry floor.
        // Never apply an artificial swimming impulse over open water.
        if(!getNavigation().isDone())getNavigation().stop();
        // On ice vanilla friction is tiny: full-speed steering overshoots
        // adjacent waypoint centers and leaves golems dancing in circles.
        // Ease down during final approach without teleporting or flying.
        BlockState floor=level().getBlockState(blockPosition().below());
        boolean ice=floor.is(net.minecraft.world.level.block.Blocks.ICE)
                ||floor.is(net.minecraft.world.level.block.Blocks.PACKED_ICE)
                ||floor.is(net.minecraft.world.level.block.Blocks.BLUE_ICE)
                ||floor.is(net.minecraft.world.level.block.Blocks.FROSTED_ICE);
        if(ice){
            double dx=next.getX()+.5-getX(),dz=next.getZ()+.5-getZ();
            if(dx*dx+dz*dz<1.1){
                var motion=getDeltaMovement();
                setDeltaMovement(motion.x*.58,motion.y,motion.z*.58);
            }
            speed=Math.min(speed,.72);
        }
        double walkY=level() instanceof ServerLevel server
                ?WarehouseTerrain.standingY(server,next):next.getY();
        getMoveControl().setWantedPosition(next.getX()+.5,walkY,
                next.getZ()+.5,speed);
    }
    /** Follow a CHECKED adjacent terrain step without asking vanilla A*
     * to invent a second route for every tiny one-block ledge. The weighted
     * planner always prefers level ground and never permits a two-block leap.
     * Jump only for a genuine single-block ascent; descending uses gravity. */
    private void followTerrainStep(BlockPos next,double speed){
        BlockPos feet=blockPosition();
        int delta=next.getY()-feet.getY();
        if(delta>1||delta< -1)return;
        steerOnFloor(next,speed);
        // Bottom slab to full floor is a gentle 0.5-block step, not a
        // full one-block jump (even when integer block Y changed by 1).
        boolean realRise=level() instanceof ServerLevel server
                &&WarehouseTerrain.standingY(server,next)-getY()>.60;
        if(delta==1&&realRise&&onGround()
                &&tickCount>=nextTerrainJumpTick){
            double dx=next.getX()+.5-getX(),dz=next.getZ()+.5-getZ();
            if(dx*dx+dz*dz<2.6){
                getJumpControl().jump();
                nextTerrainJumpTick=tickCount+10;
            }
        }
    }
    private void walkTo(ServerLevel level,BlockPos target,double speed){
        BlockPos feet=blockPosition();
        if(!target.equals(workGoal)){
            workGoal=target.immutable();
            flatWorkRoute.clear();
            nextWorkPlan=0;
            lastWorkDistance=Double.POSITIVE_INFINITY;
            lastWorkProgress=tickCount;
            routeIssue="";
        }
        while(!flatWorkRoute.isEmpty()
                &&routeDistanceSq(level,flatWorkRoute.peekFirst())<.50){
            flatWorkRoute.removeFirst();
            lastWorkDistance=Double.POSITIVE_INFINITY;
            lastWorkProgress=tickCount;
            remoteUnreachableAttempts=0;
        }
        if(flatWorkRoute.isEmpty()&&tickCount>=nextWorkPlan){
            var occupied=occupiedAisle(level,target);
            java.util.function.Predicate<BlockPos> available=p->
                    StorageGolemReturnPath.walkable(level,p)
                    &&!occupied.contains(p)
                    &&(tickCount>=avoidWorkUntil||!p.equals(blockedWorkCell));
            // The shared weighted router considers level routes FIRST but
            // permits a short stepped path when going around the hill would
            // be excessively long or impossible. Nearby goals still demand
            // a COMPLETE checked dry route, never a partial loop.
            List<BlockPos> route=WarehousePreferredPath.leg(feet,target,
                    available,tickCount<avoidWorkUntil
                            ?blockedWorkCell:BlockPos.ZERO,
                    p->WarehouseTerrain.standingY(level,p));
            nextWorkPlan=tickCount+20;
            if(route!=null){
                flatWorkRoute.addAll(route);
                routeIssue="";
            }else if(remotePickup&&++remoteUnreachableAttempts>=3){
                pauseRemotePath("NO SAFE DRY PATH (WATER OR OBSTRUCTION)");
            }else{
                routeIssue="WAITING FOR SAFE WALKABLE ROUTE";
            }
        }
        if(flatWorkRoute.isEmpty())return;
        BlockPos next=flatWorkRoute.peekFirst();
        double distance=routeDistanceSq(level,next);
        if(distance+.02<lastWorkDistance){
            lastWorkDistance=distance;
            lastWorkProgress=tickCount;
        }else if(tickCount-lastWorkProgress>70){
            blockedWorkCell=next.immutable();
            avoidWorkUntil=tickCount+60;
            routeIssue="REROUTING";
            flatWorkRoute.clear();
            nextWorkPlan=0;
            getNavigation().stop();
            lastWorkProgress=tickCount;
            lastWorkDistance=Double.POSITIVE_INFINITY;
            if(remotePickup&&++remoteUnreachableAttempts>=3)
                pauseRemotePath("WORKER STALLED AT DRY-LAND WAYPOINT");
            return;
        }
        // No second vanilla path search on a terrain transition: it used
        // to ignore the chosen one-block step and spin along the hillside.
        followTerrainStep(next,speed);
    }
    private final ArrayDeque<BlockPos> flatWorkRoute=new ArrayDeque<>();

    private boolean closeTo(BlockPos p){
        // An order must actually reach the selected service square.
        // The old 1.87-block radius could treat a rack wall as arrival.
        return distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)
                <(stationOrder?.94:3.5);
    }
    /**
     * Dock at the same floor the Golem is actually walking on. Shelves
     * occupy a full solid block, so their Y coordinate can differ by one
     * from the Golem's foot position. A floor-level or raised-front shelf
     * must not make a returning Golem wait forever outside its home.
     */
    private boolean atShelfEntrance(ServerLevel level){
        return atShelfEntrance(level,home);
    }
    private boolean atShelfEntrance(ServerLevel level,BlockPos shelfPos){
        BlockPos feet=blockPosition();
        if(feet.getY()!=shelfPos.getY())return false;
        if(!level.hasChunkAt(shelfPos)
                ||!level.getBlockState(shelfPos).is(RackContent.STORAGE_SHELF))
            return false;
        Direction facing=level.getBlockState(shelfPos)
                .getValue(StorageGolemShelfBlock.FACING);
        return StorageGolemReturnPath.canDockFrontNearby(feet,shelfPos,facing,
                p->StorageGolemReturnPath.walkable(level,p));
    }
    /**
     * A shelf bank is shared capacity. If the assigned two-slot shelf was
     * refilled while the worker was away, dock into another physically
     * reachable, connected shelf with space. Never teleport a robot to a
     * remote member of a long shelf network or allocate a third token.
     */
    private StorageGolemShelfEntity availableDock(ServerLevel level,
            StorageGolemShelfEntity assigned){
        if(assigned.docked()<2)return assigned;
        StorageGolemShelfEntity nearest=null;
        double best=Double.POSITIVE_INFINITY;
        for(StorageGolemShelfEntity member:StorageGolemShelfEntity.linked(
                level,home)){
            if(member.docked()>=2
                    ||!atShelfEntrance(level,member.getBlockPos()))continue;
            BlockPos pos=member.getBlockPos();
            double distance=distanceToSqr(pos.getX()+.5,getY(),pos.getZ()+.5);
            if(distance<best){
                nearest=member;
                best=distance;
            }
        }
        return nearest;
    }
    /** Returning to a shelf never targets its top or walks through it.
     * Four-direction same-floor detours avoid the normal AI temptation to
     * hop straight over a one-block obstruction. Paths are rechecked when
     * the warehouse changes; nothing loads chunks out of range. */
    private void returnToShelf(ServerLevel level){
        // A personal DIRECT recall is useful precisely when the player is
        // NOT at the Rack Computer. Keep the worker, its home shelf, and
        // the next forward leg simulated while the Golem WALKS home. Never
        // teleport or redock at distance; each worker must physically
        // reach its original shelf. These bounded tickets automatically
        // expire once the Golem docks and stops refreshing them.
        if(playerHaul&&stage==2&&tickCount%20==0){
            WarehouseRemoteTickets.keep(level,blockPosition());
            WarehouseRemoteTickets.keep(level,home);
            BlockPos feet=blockPosition();
            int dx=home.getX()-feet.getX(),dz=home.getZ()-feet.getZ();
            double length=Math.sqrt((double)dx*dx+(double)dz*dz);
            if(length>8)WarehouseRemoteTickets.keep(level,new BlockPos(
                    feet.getX()+(int)Math.round(dx/length*16),
                    feet.getY(),feet.getZ()+(int)Math.round(dz/length*16)));
        }
        if(!level.hasChunkAt(home)
                ||!(level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf))
            return;
        if(atShelfEntrance(level)){
            getNavigation().stop();
            flatHomeRoute.clear();
            StorageGolemShelfEntity receiving=availableDock(level,shelf);
            if(receiving!=null&&receiving.addNamed(golemName)){
                routeIssue="";
                setCustomNameVisible(false);
                // A recalled/aborted worker can still own unsorted items,
                // an empty shulker or even a loaded pallet. The previous
                // return branch dropped these before redocking; keep that
                // no-item-loss guarantee when routing is changed.
                releaseOutput(level);
                if(!emptyBox.isEmpty()){
                    Containers.dropItemStack(level,getX(),getY(),getZ(),emptyBox);
                    emptyBox=ItemStack.EMPTY;
                }
                if(!cargo.isEmpty()){
                    Containers.dropItemStack(level,getX(),getY(),getZ(),cargo);
                    cargo=ItemStack.EMPTY;
                }
                dropCarriedPallet(level);
                discard();
            }else{
                // Keep the real worker until a usable slot opens. Make the
                // reason persist through periodic task-label updates.
                routeIssue="SHELF FULL 2/2";
                setCustomName(Component.literal("RETURN | "+routeIssue));
                setCustomNameVisible(true);
            }
            return;
        }
        BlockPos from=blockPosition();
        if(from.getY()!=home.getY()){
            // HOME is still the assigned shelf across reloads. Avoid a
            // straight-line vanilla climb onto nearby blocks: use the same
            // checked flat-first corridor planner for changing floors.
            BlockPos entrance=home.relative(StorageGolemShelfDoor.front(shelf));
            if(StorageGolemReturnPath.walkable(level,entrance))
                walkTo(level,entrance,1.10);
            else routeIssue="WAITING FOR SAFE SHELF ENTRANCE";
            return;
        }
        // Plan around the shelf's X/Z on the GOLEM'S current floor, not the
        // shelf block Y. This fixes a deadlock when the shelf is at ground
        // block height and the Golem walks beside it one block higher.
        BlockPos shelfPlane=home;
        if(!lastHomeGoal.equals(shelfPlane)){
            lastHomeGoal=shelfPlane.immutable();
            flatHomeRoute.clear();
            nextHomeReplanTick=0;
        }
        while(!flatHomeRoute.isEmpty()){
            BlockPos waypoint=flatHomeRoute.peekFirst();
            if(!StorageGolemReturnPath.walkable(level,waypoint)){
                flatHomeRoute.clear();
                nextHomeReplanTick=0;
                break;
            }
            if(distanceToSqr(waypoint.getX()+.5,waypoint.getY(),
                    waypoint.getZ()+.5)<.32){
                flatHomeRoute.removeFirst();
                lastHomeWaypointDistance=Double.POSITIVE_INFINITY;
                lastHomeProgressTick=tickCount;
            }else break;
        }
        if(flatHomeRoute.isEmpty()&&tickCount>=nextHomeReplanTick){
            var occupied=occupiedAisle(level,shelfPlane);
            var planned=StorageGolemReturnPath.stepsHome(from,shelfPlane,
                    StorageGolemShelfDoor.front(shelf),
                    p->StorageGolemReturnPath.walkable(level,p)
                            &&!occupied.contains(p)
                            &&(tickCount>=avoidHomeUntil
                                ||!p.equals(blockedHomeCell)));
            nextHomeReplanTick=tickCount+20;
            if(planned!=null){
                flatHomeRoute.addAll(planned);
                lastHomeWaypointDistance=Double.POSITIVE_INFINITY;
                lastHomeProgressTick=tickCount;
                routeIssue="";
            }else{
                // A same-level corridor is preferable, not mandatory.
                // Go to the dry front-door tile using the shared weighted
                // single-block-step planner instead of freezing at a hill.
                BlockPos entrance=home.relative(StorageGolemShelfDoor.front(shelf));
                if(StorageGolemReturnPath.walkable(level,entrance)){
                    walkTo(level,entrance,1.08);
                }else{
                    routeIssue="WAITING FOR SAFE SHELF ENTRANCE";
                    getNavigation().stop();
                }
                return;
            }
        }
        if(flatHomeRoute.isEmpty()){
            // A completed or zero-length route means we have reached an
            // accessible entrance even if path node coordinates are rounded
            // differently from the mob's actual standing block.
            if(tickCount>=nextHomeReplanTick)nextHomeReplanTick=0;
            return;
        }
        BlockPos next=flatHomeRoute.peekFirst();
        double distance=distanceToSqr(next.getX()+.5,next.getY(),next.getZ()+.5);
        if(distance+.12<lastHomeWaypointDistance){
            lastHomeWaypointDistance=distance;
            lastHomeProgressTick=tickCount;
        }else if(tickCount-lastHomeProgressTick>50){
            // Dynamic blockers (e.g. a new rack/pallet) may invalidate a
            // corridor even when its terminal waypoint is clear.
            // Another Golem may occupy this same aisle waypoint. Mark
            // that ONE cell as temporarily impassable for the next search,
            // so repeated retries explore a different aisle instead of
            // restarting the identical blocked path forever.
            blockedHomeCell=next.immutable();
            avoidHomeUntil=tickCount+60;
            routeIssue="REROUTING";
            getNavigation().stop();
            flatHomeRoute.clear();
            nextHomeReplanTick=0;
            return;
        }
        // Never redirect a worker which is already within the docking
        // aisle just because the other worker reached a nearby waypoint.
        if(atShelfEntrance(level))returnToShelf(level);
        else followTerrainStep(next,1.08);
    }
    private Container loaded(ServerLevel level,BlockPos pos) {
        return level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof Container c
                ?c:null;
    }
    /**
     * One physical Golem carries one EMPTY pallet from an INPUT transfer
     * station to a Stack of Pallets sitting inside a rack pallet. Items are
     * transferred only at the actual source/target, never by remote teleport.
     */
    private void tickPalletStackJob(ServerLevel level){
        BlockPos machine=stage==0?source:destination;
        if(!level.hasChunkAt(machine))return;
        BlockPos access=approach(level,machine);
        if(!level.hasChunkAt(access))return;
        if(!closeTo(access)){
            walkTo(level,access,1.10);
            return;
        }
        getNavigation().stop();
        if(stage==0){
            PtpBlockEntity ptp=level.getBlockEntity(source)
                    instanceof PtpBlockEntity p?p:null;
            if(ptp==null||!ptp.input()||!ptp.hasPallet()||ptp.sealed()
                    ||!ptp.emptyCargo()){
                routeIssue="WAITING FOR EMPTY INPUT PALLET";
                return;
            }
            // Atomic ownership change: remove only after the worker actually
            // arrives, and keep the resulting stack in the saved cargo field.
            ItemStack taken=ptp.removePallet();
            if(taken.isEmpty())return;
            cargo=taken;
            stage=1;
            routeIssue="";
            return;
        }
        if(cargo.isEmpty()){
            stage=2;return;
        }
        if(level.getBlockEntity(destination) instanceof StackOfPalletsBlockEntity magazine){
            if(magazine.put(cargo.copyWithCount(1))){
                cargo=ItemStack.EMPTY;
                stage=2;
                routeIssue="";
            }else routeIssue="WAITING FOR FREE PALLET STACK SLOT";
            return;
        }
        RackBlockEntity rack=level.getBlockEntity(destination)
                instanceof RackBlockEntity r?r:null;
        if(rack==null||!rack.hasPallet()){
            routeIssue="WAITING FOR PALLET STACK STORAGE";
            return;
        }
        for(int i=0;i<rack.getContainerSize();i++){
            ItemStack magazine=rack.getItem(i);
            if(!StackOfPalletsItem.canStore(magazine,cargo))continue;
            ItemStack updated=magazine.copy();
            if(!StackOfPalletsItem.put(updated,cargo)
                    ||!rack.canPlaceItem(i,updated))continue;
            rack.setItem(i,updated);
            if(!ItemStack.matches(rack.getItem(i),updated))continue;
            cargo=ItemStack.EMPTY;
            stage=2;
            routeIssue="";
            return;
        }
        routeIssue="WAITING FOR AVAILABLE STACK OF PALLETS";
    }

    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel level))return;
        // An assigned forklift operator ignores ordinary warehouse recall:
        // only the controller's explicit dismiss command sends it home.
        if(!remoteCourier&&!remotePickup&&!nodePush&&!forkliftDriver&&!forkliftEscort)
            checkPendingRecall(level);
        // Status strings from task transitions should not leak into
        // player-visible nameplates for 10 ticks when debug is disabled.
        updateTaskLabel();
        syncHeldVisual();
        syncPalletAction();
        if(forkliftDriver){tickForkliftDriver(level);return;}
        if(forkliftEscort){tickForkliftEscort(level);return;}
        if(remotePickup){tickRemotePickup(level);return;}
        if(nodePush){tickNodePush(level);return;}
        if(remoteCourier){tickRemoteCourier(level);return;}
        if(rackStairMover){tickRackStairs(level);return;}
        if(stage==0&&level.getBlockEntity(source) instanceof RackBlockEntity elevated
                &&source.getY()>=home.getY()+3){
            if(!rackStairRequestChecked){
                rackStairRequestChecked=true;
                rackStairRequestActive=WarehouseRackAccess.requestStairs(level,elevated);
                rackStairDeadline=level.getGameTime()+650;
            }
            if(rackStairRequestActive&&level.getGameTime()<rackStairDeadline
                    &&!WarehouseRackAccess.stairsReady(level,elevated)){
                routeIssue="WAITING FOR RACK STAIRS AND HELPER GOLEM";
                getNavigation().stop();
                return;
            }
        }else if(rackStairRequestActive&&stage!=0){
            if(level.getBlockEntity(source) instanceof RackBlockEntity finished)
                WarehouseRackAccess.finishStairAccess(level,finished.getBlockPos(),
                        finished.facing());
            rackStairRequestActive=false;
        }
        // Movement controls expire after each mob AI tick. Drive the next
        // checked aisle cell EVERY server tick; previously the one-shot
        // commands were sent only once per ten ticks, causing stop/go
        // stuttering and making route recovery appear frozen.
        // Return-home jobs MUST use the flat same-Y grid router, never the
        // generic approach() navigation that can select a jump over a block.
        if(stage==2){
            releaseOutput(level);
            if(stationOrder&&!orderItems().isEmpty())dropOrderCargo(level);
            returnToShelf(level);
            return;
        }
        if(palletStackJob){tickPalletStackJob(level);return;}
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
            walkTo(level,walkway,1.10);
            return;
        }
        getNavigation().stop();
        // Never sort from a floor more than two blocks below a rack, even
        // if a path recalculation or another mod redirected the worker.
        BlockPos handling=stage==1?destination:source;
        if((stage==0||stage==1||stage==3)
                &&level.getBlockEntity(handling) instanceof RackBlockEntity rack
                &&!WarehouseSortingReach.withinReach(blockPosition(),rack)){
            routeIssue="WAITING FOR LADDER, STAIRS OR SCAFFOLD ACCESS";
            return;
        }
        if(stage==4){
            PtpBlockEntity output=outputPoint(level);
            if(output==null||!output.readyForOutput()
                    ||!output.hasReservation(outputTicket)){
                stage=2;return;
            }
            ShulkerTankEntity tank=level.getBlockEntity(magazine)
                    instanceof ShulkerTankEntity t?t:null;
            if(tank==null||tank.isEmpty()){
                routeIssue="WAITING FOR EMPTY SHULKERS";
                return;
            }
            emptyBox=tank.take();
            if(emptyBox.isEmpty()){
                routeIssue="WAITING FOR EMPTY SHULKERS";
                return;
            }
            routeIssue="";
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
            if(stationOrder){
                PtpBlockEntity ptp=outputPoint(level);
                ShulkerTankEntity station=level.getBlockEntity(destination)
                        instanceof ShulkerTankEntity t?t:null;
                Container stock=loaded(level,source);
                if(ptp==null||!ptp.hasReservation(outputTicket)
                        ||station==null||stock==null){
                    // Missing/temporarily unloaded stock, a congested
                    // station or a slow reservation must not erase a
                    // player's submitted order after one tick.
                    routeIssue="WAITING FOR ORDER SOURCE / STATION";
                    return;
                }
                List<ItemStack> preview=new ArrayList<>(9);
                List<Integer> slots=new ArrayList<>(9);
                int collected=0;
                for(int i=0;i<stock.getContainerSize()&&preview.size()<9
                        &&collected<pickupLimit;i++){
                    ItemStack item=stock.getItem(i);
                    if(item.isEmpty()||!net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(item.getItem()).toString().equals(itemId))continue;
                    int take=Math.min(item.getCount(),pickupLimit-collected);
                    preview.add(item.copyWithCount(take));slots.add(i);
                    collected+=take;
                }
                if(preview.isEmpty()){
                    routeIssue="WAITING FOR ORDER STOCK";
                    return; // Reservation and task remain active.
                }
                if(!station.canAccept(preview,level,ptp)){
                    routeIssue="WAITING FOR SHULKER SPACE";
                    return; // A station may finish/flush a box next tick.
                }
                routeIssue="";
                List<ItemStack> taken=new ArrayList<>(9);
                for(int i=0;i<slots.size();i++){
                    ItemStack part=stock.removeItem(slots.get(i),preview.get(i).getCount());
                    if(!part.isEmpty())taken.add(part);
                }
                if(taken.isEmpty()){
                    routeIssue="WAITING FOR RACK STOCK";
                    return;
                }
                orderCargo=NonNullList.withSize(9,ItemStack.EMPTY);
                for(int i=0;i<taken.size();i++)orderCargo.set(i,taken.get(i));
                stage=1;waitTicks=0;return;
            }
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
            if(stationOrder){
                PtpBlockEntity ptp=outputPoint(level);
                ShulkerTankEntity station=level.getBlockEntity(destination)
                        instanceof ShulkerTankEntity t?t:null;
                List<ItemStack> delivery=orderItems();
                if(delivery.isEmpty()){
                    routeIssue="ORDER CARGO MISSING";
                    stage=2;return;
                }
                if(ptp==null||!ptp.hasReservation(outputTicket)||station==null){
                    routeIssue="WAITING FOR ORDER DESTINATION";
                    return;
                }
                if(station.accept(delivery,level,ptp)){
                    int count=delivery.stream().mapToInt(ItemStack::getCount).sum();
                    for(int i=0;i<9;i++)orderCargo.set(i,ItemStack.EMPTY);
                    ptp.outputDelivered(outputTicket,count);
                    station.finishOrder(level,ptp);
                    stage=2;waitTicks=0;return;
                }
                // A busy 3-shulker station is NOT a rejected order.
                // Retain all nine cargo slots until there is room instead
                // of abandoning the delivery after just four seconds.
                if(tickCount%20==0){
                    waitTicks++;
                    routeIssue="WAITING FOR STATION SPACE";
                }
                return;
            }
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
                if(outputJob){
                    // Keep this reservation and loaded stack in transit.
                    // An output PTP may temporarily be full while its
                    // hopper/attached Shulker Station is still working.
                    routeIssue="WAITING FOR OUTPUT SPACE";
                }else{
                    // Input sorting may reroute to another compatible rack.
                    BlockPos alternate=StorageGolemDispatch.findRackFor(level,home,cargo);
                    if(alternate!=null){
                        destination=alternate;
                        routeIssue="REROUTING TO AVAILABLE RACK";
                    }else{
                        routeIssue="WAITING FOR RACK SPACE";
                    }
                }
                waitTicks=0;
            }
            return;
        }
        if(stage==3){
            if(stationOrder){
                for(int i=0;i<9;i++){
                    ItemStack item=orderCargo.get(i);
                    if(item.isEmpty())continue;
                    StorageGolemDispatch.insert(level,source,item);
                    if(!item.isEmpty())
                        Containers.dropItemStack(level,getX(),getY(),getZ(),item);
                    orderCargo.set(i,ItemStack.EMPTY);
                }
                releaseOutput(level);
                stage=2;return;
            }
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
        if(level.getBlockEntity(home) instanceof StorageGolemShelfEntity shelf
                &&shelf.addNamed(golemName)){
            discard(); // one entity becomes exactly one docked golem token
        }
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.store("home",BlockPos.CODEC,home);
        out.putLong("observed_recall_serial",observedRecallSerial);
        out.store("source",BlockPos.CODEC,source);
        out.store("destination",BlockPos.CODEC,destination);
        if(!cargo.isEmpty())out.store("cargo",ItemStack.CODEC,cargo);
        out.putInt("stage",stage);out.putString("item",itemId);out.putInt("wait",waitTicks);
        out.putBoolean("output_job",outputJob);
        out.putBoolean("pallet_stack_job",palletStackJob);
        out.putBoolean("forklift_escort",forkliftEscort);
        if(forkliftEscortOwner!=null)
            out.putString("forklift_escort_owner",forkliftEscortOwner.toString());
        out.putBoolean("forklift_driver",forkliftDriver);
        if(forkliftId!=null)out.putString("forklift_id",forkliftId.toString());
        out.putBoolean("rack_stair_mover",rackStairMover);
        out.putBoolean("rack_stair_request_checked",rackStairRequestChecked);
        out.putBoolean("rack_stair_request_active",rackStairRequestActive);
        out.putLong("rack_stair_deadline",rackStairDeadline);
        out.store("rack_stair_last",BlockPos.CODEC,rackStairLast);
        out.putInt("output_ticket",outputTicket);
        out.putInt("pickup_limit",pickupLimit);
        if(!requestedSample.isEmpty())out.store("requested_sample",ItemStack.CODEC,requestedSample);
        out.putBoolean("packed_job",packedJob);
        out.putBoolean("station_order",stationOrder);
        out.store("order_ptp",BlockPos.CODEC,orderPoint);
        for(int i=0;i<9;i++)if(!orderCargo.get(i).isEmpty())
            out.store("order_slot_"+i,ItemStack.CODEC,orderCargo.get(i));
        out.store("magazine",BlockPos.CODEC,magazine);
        out.putInt("packed_item_count",packedItemCount);
        if(!emptyBox.isEmpty())out.store("empty_box",ItemStack.CODEC,emptyBox);
        out.putBoolean("haul_job",haulJob);
        out.putInt("haul_ticket",haulTicket);
        out.putInt("haul_worker_index",haulIndex);
        out.putBoolean("node_push",nodePush);
        if(nodePushMission!=null)out.putString("node_push_mission",nodePushMission.toString());
        if(nodePushVehicle!=null)out.putString("node_push_vehicle",nodePushVehicle.toString());
        out.store("node_push_node",BlockPos.CODEC,nodePushNode);
        out.store("node_push_goal",BlockPos.CODEC,nodePushGoal);
        out.putBoolean("node_push_loading",nodePushLoading);
        out.putBoolean("remote_pickup_worker",remotePickup);
        out.putBoolean("remote_path_paused",remotePathPaused);
        out.putString("remote_pause_reason",remotePauseReason);
        if(remotePickupMission!=null)out.putString(
                "remote_pickup_mission",remotePickupMission.toString());
        if(remotePickupCourier!=null)out.putString(
                "remote_pickup_courier",remotePickupCourier.toString());
        out.store("remote_pickup_target",BlockPos.CODEC,remotePickupTarget);
        out.store("remote_pickup_warehouse",BlockPos.CODEC,remotePickupWarehouse);
        out.store("remote_pickup_rally",BlockPos.CODEC,remotePickupRally);
        for(int i=0;i<4;i++)out.store(
                "remote_pickup_shelf_"+i,BlockPos.CODEC,remotePickupHomes[i]);
        out.putBoolean("remote_courier",remoteCourier);
        out.putInt("remote_courier_phase",remoteCourierPhase);
        if(remoteCarrierId!=null)out.putString("remote_courier_rig",remoteCarrierId.toString());
        out.store("remote_courier_target",BlockPos.CODEC,remoteCourierTarget);
        out.store("remote_courier_aisle",BlockPos.CODEC,remoteCourierAisle);
        out.store("remote_courier_join_aisle",BlockPos.CODEC,remoteCourierJoinAisle);
        out.store("remote_courier_rejected_aisle",BlockPos.CODEC,remoteCourierRejectedAisle);
        out.store("remote_rig_last_known",BlockPos.CODEC,remoteRigLastKnown);
        if(!remoteFresh.isEmpty())out.store("remote_empty_pallet",ItemStack.CODEC,remoteFresh);
        out.putBoolean("player_haul",playerHaul);
        out.putBoolean("waiting_for_player",waitingForPlayer);
        out.putString("golem_name",golemName);
        out.putBoolean("ground_pickup",groundPickup);
        out.putBoolean("rack_pickup",rackPickup);
        out.putString("player_ptp_face",playerPtpFace.getName());
        out.putInt("rack_floor_y",rackFloorY);
        out.putString("haul_owner",haulOwner);
        if(!carryingPallet.isEmpty())out.store("carried_pallet",ItemStack.CODEC,carryingPallet);
        ContainerHelper.saveAllItems(out,palletContents);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        home=in.read("home",BlockPos.CODEC).orElse(BlockPos.ZERO);
        observedRecallSerial=Math.max(0,in.getLongOr("observed_recall_serial",0L));
        source=in.read("source",BlockPos.CODEC).orElse(BlockPos.ZERO);
        destination=in.read("destination",BlockPos.CODEC).orElse(BlockPos.ZERO);
        cargo=in.read("cargo",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        stage=Math.clamp(in.getIntOr("stage",2),0,5);
        itemId=in.getStringOr("item","");
        waitTicks=Math.max(0,in.getIntOr("wait",0));
        outputJob=in.getBooleanOr("output_job",false);
        palletStackJob=in.getBooleanOr("pallet_stack_job",false);
        forkliftEscort=in.getBooleanOr("forklift_escort",false);
        try{
            String owner=in.getStringOr("forklift_escort_owner","");
            forkliftEscortOwner=owner.isBlank()?null:UUID.fromString(owner);
        }catch(IllegalArgumentException ex){forkliftEscortOwner=null;}
        forkliftDriver=in.getBooleanOr("forklift_driver",false);
        try{
            String forklift=in.getStringOr("forklift_id","");
            forkliftId=forklift.isBlank()?null:java.util.UUID.fromString(forklift);
        }catch(IllegalArgumentException ex){forkliftId=null;}
        rackStairMover=in.getBooleanOr("rack_stair_mover",false);
        rackStairRequestChecked=in.getBooleanOr("rack_stair_request_checked",false);
        rackStairRequestActive=in.getBooleanOr("rack_stair_request_active",false);
        rackStairDeadline=in.getLongOr("rack_stair_deadline",0);
        rackStairLast=in.read("rack_stair_last",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        outputTicket=Math.max(0,in.getIntOr("output_ticket",0));
        pickupLimit=Math.clamp(in.getIntOr("pickup_limit",64),1,27*64);
        requestedSample=in.read("requested_sample",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        packedJob=in.getBooleanOr("packed_job",false);
        stationOrder=in.getBooleanOr("station_order",false);
        orderPoint=in.read("order_ptp",BlockPos.CODEC).orElse(BlockPos.ZERO);
        orderCargo=NonNullList.withSize(9,ItemStack.EMPTY);
        for(int i=0;i<9;i++)orderCargo.set(i,in.read("order_slot_"+i,
                ItemStack.CODEC).orElse(ItemStack.EMPTY));
        magazine=in.read("magazine",BlockPos.CODEC).orElse(BlockPos.ZERO);
        packedItemCount=Math.max(0,in.getIntOr("packed_item_count",0));
        emptyBox=in.read("empty_box",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        haulJob=in.getBooleanOr("haul_job",false);
        haulTicket=Math.max(0,in.getIntOr("haul_ticket",0));
        haulIndex=Math.clamp(in.getIntOr("haul_worker_index",0),0,3);
        nodePush=in.getBooleanOr("node_push",false);
        try{nodePushMission=UUID.fromString(in.getStringOr("node_push_mission",""));}
        catch(IllegalArgumentException ex){nodePushMission=null;}
        try{nodePushVehicle=UUID.fromString(in.getStringOr("node_push_vehicle",""));}
        catch(IllegalArgumentException ex){nodePushVehicle=null;}
        nodePushNode=in.read("node_push_node",BlockPos.CODEC).orElse(BlockPos.ZERO);
        nodePushGoal=in.read("node_push_goal",BlockPos.CODEC).orElse(BlockPos.ZERO);
        nodePushLoading=in.getBooleanOr("node_push_loading",false);
        remotePickup=in.getBooleanOr("remote_pickup_worker",false);
        remotePathPaused=in.getBooleanOr("remote_path_paused",false);
        remotePauseReason=in.getStringOr("remote_pause_reason","PATH BLOCKED");
        try{remotePickupMission=UUID.fromString(in.getStringOr("remote_pickup_mission",""));}
        catch(IllegalArgumentException ex){remotePickupMission=null;}
        try{remotePickupCourier=UUID.fromString(in.getStringOr("remote_pickup_courier",""));}
        catch(IllegalArgumentException ex){remotePickupCourier=null;}
        remotePickupTarget=in.read("remote_pickup_target",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        remotePickupWarehouse=in.read("remote_pickup_warehouse",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        remotePickupRally=in.read("remote_pickup_rally",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        for(int i=0;i<4;i++)remotePickupHomes[i]=in.read(
                "remote_pickup_shelf_"+i,BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteCourier=in.getBooleanOr("remote_courier",false);
        remoteCourierPhase=Math.clamp(in.getIntOr("remote_courier_phase",0),0,1);
        remoteCourierTarget=in.read("remote_courier_target",BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteCourierAisle=in.read("remote_courier_aisle",BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteCourierJoinAisle=in.read("remote_courier_join_aisle",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        remoteCourierRejectedAisle=in.read("remote_courier_rejected_aisle",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
        remoteRigLastKnown=in.read("remote_rig_last_known",BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteFresh=in.read("remote_empty_pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        try{remoteCarrierId=UUID.fromString(in.getStringOr("remote_courier_rig",""));}
        catch(IllegalArgumentException ex){remoteCarrierId=null;}
        playerHaul=in.getBooleanOr("player_haul",false);
        waitingForPlayer=in.getBooleanOr("waiting_for_player",false);
        golemName=in.getStringOr("golem_name","");
        groundPickup=in.getBooleanOr("ground_pickup",false);
        rackPickup=in.getBooleanOr("rack_pickup",false);
        try{playerPtpFace=Direction.valueOf(in.getStringOr("player_ptp_face","north")
                .toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ex){playerPtpFace=Direction.NORTH;}
        rackFloorY=in.getIntOr("rack_floor_y",source.getY());
        haulOwner=in.getStringOr("haul_owner","");
        carryingPallet=in.read("carried_pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        palletContents=NonNullList.withSize(216,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,palletContents);
        syncPalletVisual();
        syncHeldVisual();
        syncRemoteFreshVisual();
        syncPalletAction();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel server &&!deathDropsReleased){
            deathDropsReleased=true;
            releaseOutput(server);
            cancelHaul(server);
            dropCarriedPallet(server);
            if(remoteCourier&&!remoteFresh.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),remoteFresh);
                remoteFresh=ItemStack.EMPTY;
            }
            dropOrderCargo(server);
            if(!cargo.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),cargo);
                cargo=ItemStack.EMPTY;
            }
            if(!emptyBox.isEmpty()){
                Containers.dropItemStack(server,getX(),getY(),getZ(),emptyBox);
                emptyBox=ItemStack.EMPTY;
            }
            // A remote courier is already one of the four shelf tokens
            // owned by the moving rig. Never drop a fifth worker.
            if(!remoteCourier)Containers.dropItemStack(server,getX(),getY(),getZ(),
                    WarehouseGolemNames.namedToken(golemName));
        }
        super.die(source);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(level().isClientSide())return InteractionResult.SUCCESS;
        if(player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)
                &&StorageScannerItem.mode(player.getItemInHand(hand))
                        ==StorageScannerItem.Mode.FORKLIFT
                &&player instanceof net.minecraft.server.level.ServerPlayer user){
            if(getVehicle() instanceof WarehouseForklift lift){
                WarehouseGolemDebug.actionBar(user,Component.literal(
                        lift.requestDriverDismissal((ServerLevel)level())));
                return InteractionResult.SUCCESS;
            }
            if(escortFor(user.getUUID())){
                dismissForkliftEscort();
                WarehouseGolemDebug.actionBar(user,Component.literal(
                        "FORKLIFT: unassigned operator returning to shelf."));
                return InteractionResult.SUCCESS;
            }
        }
        if(player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)
                &&StorageScannerItem.mode(player.getItemInHand(hand))
                        ==StorageScannerItem.Mode.RECALL
                &&player instanceof net.minecraft.server.level.ServerPlayer user){
            if(StorageScannerItem.recallDoubleClick((ServerLevel)level(),user,getUUID())){
                if(holdsPallet()){
                    WarehouseGolemDebug.actionBar(user,Component.literal(
                            "RECALL: this Golem holds a pallet; use the complete "
                            +"four-Golem rig to release it safely."));
                }else{
                    recallToShelf((ServerLevel)level());
                    WarehouseGolemDebug.actionBar(user,Component.literal(
                            "RECALL: Golem returning to original shelf."));
                }
            }else WarehouseGolemDebug.actionBar(user,Component.literal(
                    "RECALL: right-click the same Golem again quickly to confirm."));
            return InteractionResult.SUCCESS;
        }
        if(player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)
                &&(remotePickup||remoteCourier)){
            if(resumeRemotePath())
                WarehouseGolemDebug.message(player,home,Component.literal(
                        "Scanner: Golem resumed dry-land pathfinding. "
                        +"If the obstruction remains, it will pause again."));
            else if(remoteCourier)
                WarehouseGolemDebug.message(player,home,Component.literal(
                        "Scanner: courier is locked to pallet at "
                        +remoteCourierTarget.toShortString()
                        +" | Service aisle: "+remoteCourierAisle.toShortString()
                        +" | Waypoint: "+(courierRoute.isEmpty()
                                ?"NOT PLANNED":courierRoute.peekFirst().toShortString())
                        +" | "+routeIssue));
            else WarehouseGolemDebug.message(player,home,Component.literal(
                    "Scanner: pickup worker's target "
                    +remotePickupTarget.toShortString()
                    +" | Rally: "+remotePickupRally.toShortString()
                    +" | "+routeIssue));
            return InteractionResult.SUCCESS;
        }
        if(player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)
                &&StorageScannerItem.mode(player.getItemInHand(hand))
                        ==StorageScannerItem.Mode.DIRECT
                &&player instanceof net.minecraft.server.level.ServerPlayer user
                &&ownedBy(user.getUUID())){
            // Clicking ANY personally assigned follower sends the WHOLE
            // independent crew home. No Rack Computer interaction or nearby
            // shelf is needed. The coordinator first checks the entire
            // group to avoid splitting four Golems around live cargo.
            WarehouseGolemDebug.message(player,home,Component.literal(
                    StoragePalletCrew.recallFollowing((ServerLevel)level(),user)));
            return InteractionResult.SUCCESS;
        }
        if(!player.isShiftKeyDown())return InteractionResult.PASS;
        if(remotePickup||remoteCourier){
            WarehouseGolemDebug.message(player,home,Component.literal(
                    "This Golem is completing an assigned remote pallet exchange."));
            return InteractionResult.SUCCESS;
        }
        if(level() instanceof ServerLevel server){
            releaseOutput(server);
            cancelHaul(server);
            dropCarriedPallet(server);
        }
        for(int i=0;i<9;i++){
            ItemStack box=orderCargo.get(i);
            if(!box.isEmpty()){
                player.getInventory().placeItemBackInInventory(box.copy());
                orderCargo.set(i,ItemStack.EMPTY);
            }
        }
        if(!cargo.isEmpty()){
            player.getInventory().placeItemBackInInventory(cargo.copy());
            cargo=ItemStack.EMPTY;
        }
        if(!emptyBox.isEmpty()){
            player.getInventory().placeItemBackInInventory(emptyBox.copy());
            emptyBox=ItemStack.EMPTY;
        }
        player.getInventory().placeItemBackInInventory(WarehouseGolemNames.namedToken(golemName));
        discard();
        return InteractionResult.SUCCESS;
    }
}
