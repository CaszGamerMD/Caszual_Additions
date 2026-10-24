package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import com.caszgamermd.caszualadditions.pallet.PalletContent;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

/**
 * A physical, Golem-driven warehouse forklift. One actual pallet with its
 * original 216 slots is held by the vehicle between pickup and delivery.
 * Jobs survive chunk unloads. No instant long-range inventory transfer.
 */
public final class WarehouseForklift extends PathfinderMob {
    private static final EntityDataAccessor<ItemStack> DECK=
            SynchedEntityData.defineId(WarehouseForklift.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Float> MAST=
            SynchedEntityData.defineId(WarehouseForklift.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> ACTION=
            SynchedEntityData.defineId(WarehouseForklift.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CARGO_CRATES=
            SynchedEntityData.defineId(WarehouseForklift.class,EntityDataSerializers.INT);
    /** Small signed front wheel steering angle, synchronized for client model. */
    private static final EntityDataAccessor<Float> STEERING=
            SynchedEntityData.defineId(WarehouseForklift.class,EntityDataSerializers.FLOAT);
    /** Actual travel distance, not an AI speed multiplier:
     * Minecraft runs 20 server ticks per second, so 0.20 block/tick = 4 m/s. */
    private static final double DRIVE_BLOCKS_PER_TICK=0.20D;
    private static final double DRIVE_SPEED=0.20D;
    private static final double DRIVE_CONTROL=1.0D;
    /** Only used to cap physical motor displacement after vanilla AI movement. */
    private double driveTickStartX,driveTickStartZ;
    private ItemStack jack=ItemStack.EMPTY;
    private BlockPos source=BlockPos.ZERO,target=BlockPos.ZERO,staging=BlockPos.ZERO;
    private BlockPos parking=BlockPos.ZERO;
    private boolean retrieving,groundDestination;
    private Direction groundFacing=Direction.NORTH;
    private int job,animation,stallTicks;
    /** Transient aisle route: only the current job and the cargo Jack persist.
     * After chunk reload the route is recomputed against current obstacles. */
    private List<Vec3> aisleRoute=List.of();
    private int routeStep,lastRouteTick=-100;
    /** Acceleration is per game tick; the true top speed is 0.2
     * blocks/tick (4 blocks/sec at 20 TPS) regardless of path length. */
    private double rollingSpeed;
    private static final double MOTOR_ACCELERATION=.05D;
    private static final double ROUTE_SEGMENT_LENGTH=3.5D;
    /** Actual server displacement telemetry, not an AI speed estimate. */
    private double telemetryX=Double.NaN,telemetryZ=Double.NaN;
    private double telemetryDistance,currentBlocksPerSecond;
    private int telemetryTicks;
    private double lastHeadingError;
    private Vec3 aisleTarget;
    private Dock cachedGroundDock;
    private BlockPos cachedDockRoot=BlockPos.ZERO;
    private int cachedDockJob=-1,lastDockSearch=-100;
    private record Dock(Vec3 position,Direction forksFace) {}
    /** Rack stages: 0 approach safe exterior, 1 lift at exterior,
     * 2 turn gradually into rack, 3 transfer, 4 back out with mast high,
     * 5 forks completely clear. Ground jobs keep the original flow. */
    private int sourceRackStage,destinationRackStage;
    private int sourceArcCursor=-1,destinationArcCursor=-1;
    private int sourceArcTurn,destinationArcTurn;
    private double sourceReverseDepth,sourceAisleShift,
            destinationReverseDepth,destinationAisleShift;
    private ForkliftRackManeuver.Plan sourceArc,destinationArc;
    // No-path searches sample hundreds of fork/cargo cells. Retry at most
    // once per second if the aisle is blocked, not on every server tick.
    private int lastSourceArcSearch=-100,lastDestinationArcSearch=-100;
    private void resetRackManeuver(){
        sourceRackStage=0;destinationRackStage=0;
        sourceArcCursor=-1;destinationArcCursor=-1;
        sourceArcTurn=0;destinationArcTurn=0;
        sourceReverseDepth=0;sourceAisleShift=0;
        destinationReverseDepth=0;destinationAisleShift=0;
        sourceArc=null;destinationArc=null;
        lastSourceArcSearch=-100;lastDestinationArcSearch=-100;
    }
    private BlockPos lastDriveGoal=BlockPos.ZERO;
    private double lastDriveDistance=Double.POSITIVE_INFINITY;
    private UUID driver;
    private boolean dismissAfterParking;
    private String status="PARKED";
    public WarehouseForklift(EntityType<? extends WarehouseForklift> type,Level level){
        super(type,level);
        setPersistenceRequired();
        jack=new ItemStack(RackContent.PALLET_JACK);
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,55)
                .add(Attributes.MOVEMENT_SPEED,DRIVE_SPEED)
                .add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(DECK,ItemStack.EMPTY);
        builder.define(MAST,0f);
        builder.define(ACTION,0);
        builder.define(CARGO_CRATES,0);
        builder.define(STEERING,0f);
    }
    public ItemStack displayPallet(){return entityData.get(DECK);}
    public int displayedCargoCrates(){return entityData.get(CARGO_CRATES);}
    public float mastHeight(){return entityData.get(MAST);}
    public float steeringAngle(){return entityData.get(STEERING);}
    public int action(){return entityData.get(ACTION);}
    public boolean busy(){return job!=0&&job!=5;}
    /** A forklift can be visually idle but still own the only packed pallet.
     * Never assign a second pickup until the first cargo is unloaded or
     * explicitly recovered by its owner using emergency recovery. */
    public boolean hasOnboardPallet(){return PalletJackItem.loaded(jack);}
    /** An old or interrupted transfer might have stored item stacks but
     * lost its slot-zero deck. It renders as an EMPTY forklift, yet still
     * owns inventory. Treat those stacks as cargo, never as an empty Jack. */
    public boolean hasPackedCargo(){
        if(!jack.is(RackContent.PALLET_JACK))return false;
        for(ItemStack item:PalletJackItem.unpack(jack))
            if(!item.isEmpty())return true;
        return false;
    }
    public boolean readyForPickup(){return !busy()&&!hasPackedCargo();}
    private boolean beginWithoutCargo(){
        if(busy()){
            status="CURRENT FORKLIFT JOB IN PROGRESS";
            return false;
        }
        if(hasPackedCargo()){
            refreshDisplay();
            status=hasOnboardPallet()
                    ?"FORKLIFT HAS A PACKED PALLET IN ITS JACK; FINISH DELIVERY "
                     +"OR USE SAFE CARGO RECOVERY BEFORE PICKING UP ANOTHER"
                    :"FORKLIFT JACK CONTAINS UNDISPLAYED SAVED CARGO; "
                     +"USE SAFE RECOVERY, NEVER OVERWRITE IT";
            return false;
        }
        return true;
    }

    /** Position the operator at the exact cushion center, not a guessed
     * lateral correction. Vanilla rider attachment calls this on BOTH
     * server and client, so the seat follows all turns and driving ticks. */
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){
        if(!(passenger instanceof StorageGolem))
            return super.getPassengerRidingPosition(passenger);
        return WarehouseForkliftSeat.riderPosition(position(),getYRot());
    }
    public void setParking(BlockPos at){parking=at.immutable();}
    public BlockPos parking(){return parking;}
    public boolean hasDriver(ServerLevel world){
        if(driver==null)return false;
        return world.getEntity(driver) instanceof StorageGolem golem
                &&golem.isAlive();
    }
    public boolean hasWarehouseDriver(ServerLevel world,java.util.Set<BlockPos> shelves){
        return hasDriver(world) &&world.getEntity(driver) instanceof StorageGolem golem
                &&shelves.contains(golem.shelfHome());
    }
    public String boardEscort(ServerLevel world,java.util.UUID owner,BlockPos warehouse){
        if(job!=0&&job!=5)return "FORKLIFT: busy carrying a pallet.";
        if(hasDriver(world))return "FORKLIFT: operator already aboard.";
        java.util.Set<BlockPos> connected=new java.util.HashSet<>();
        for(StorageGolemShelfEntity shelf:WarehouseNetwork.fromComputer(world,warehouse).shelves())
            connected.add(shelf.getBlockPos());
        StorageGolem golem=StorageGolemForkliftEscort.escort(world,owner,blockPosition());
        if(golem==null||!connected.contains(golem.shelfHome()))
            return "FORKLIFT: summon one operator from a linked shelf first.";
        if(driver!=null&&world.getEntity(driver) instanceof StorageGolem previous)
            return "FORKLIFT: previously assigned operator still active.";
        golem.boardAssignedForklift(getUUID());
        driver=golem.getUUID();
        status="GOLEM ASSIGNED - BOARDING FORKLIFT";
        return "FORKLIFT: operator "+golem.golemName()+" assigned; boarding now.";
    }
    public String status(){return status;}
    /** Measured over 20 server ticks, including all stoppages. Helps tell
     * a blocked route from a client animation/rendering problem. */
    public String driveDiagnostics(){
        double speed=Math.round(currentBlocksPerSecond*10.0)/10.0;
        return "measured "+speed+" blocks/s, segment "
                +(aisleRoute.isEmpty()?0:Math.min(routeStep+1,aisleRoute.size()))
                +"/"+aisleRoute.size()+", yaw error "
                +Math.round(lastHeadingError)+" deg";
    }
    private void measureActualMotion(){
        if(Double.isFinite(telemetryX)){
            telemetryDistance+=Math.hypot(getX()-telemetryX,getZ()-telemetryZ);
        }
        telemetryX=getX();
        telemetryZ=getZ();
        if(++telemetryTicks>=20){
            currentBlocksPerSecond=telemetryDistance;
            telemetryDistance=0;
            telemetryTicks=0;
        }
    }
    private void refreshDisplay(){
        if(jack.isEmpty()||!PalletJackItem.loaded(jack)){
            entityData.set(DECK,ItemStack.EMPTY);
            entityData.set(CARGO_CRATES,0);
            return;
        }
        var stored=PalletJackItem.unpack(jack);
        entityData.set(DECK,stored.get(0).copyWithCount(1));
        int visible=0;
        // The complete payload remains in the jack. Only a count of occupied
        // slots is synced to draw the same 3x3x3 crate stack as Golem carriers.
        for(int i=1;i<stored.size();i++)
            if(!stored.get(i).isEmpty())visible++;
        entityData.set(CARGO_CRATES,Math.min(27,visible));
    }
    /** Dispatch requires one docked, named Golem driver from a real shelf. */
    private boolean hire(ServerLevel world){
        // Keep the SAME driver between separate pallet transfers.
        if(driver!=null){
            Entity assigned=world.getEntity(driver);
            if(assigned instanceof StorageGolem golem&&golem.isAlive())return true;
            status="ASSIGNED GOLEM NOT LOADED - dismiss and request a new driver";
            return false;
        }
        for(int dx=-20;dx<=20;dx++)for(int dz=-20;dz<=20;dz++)
            for(int dy=-2;dy<=2;dy++){
                BlockPos at=blockPosition().offset(dx,dy,dz);
                if(!world.hasChunkAt(at)
                        ||!(world.getBlockEntity(at) instanceof StorageGolemShelfEntity shelf)
                        ||shelf.docked()<1)continue;
                BlockPos exit=StorageGolemShelfDoor.exit(world,shelf);
                if(exit==null)continue;
                StorageGolem worker=RackContent.STORAGE_GOLEM_TYPE.create(
                        world,EntitySpawnReason.EVENT);
                if(worker==null)return false;
                String name=shelf.takeNamed();
                if(name==null)return false;
                worker.setGolemName(name);
                worker.assignForkliftDriver(shelf.getBlockPos(),getUUID());
                StorageGolemShelfDoor.emerge(worker,exit,StorageGolemShelfDoor.front(shelf));
                if(!world.addFreshEntity(worker)){
                    shelf.addNamed(name);
                    return false;
                }
                driver=worker.getUUID();
                return true;
            }
        status="NO DOCKED GOLEM WITH A CLEAR SHELF EXIT WITHIN 20 BLOCKS";
        return false;
    }
    /** Source or destination is selected by a player using the forklift item
     * on the desired rack. No warehouse racks are rearranged without orders. */
    public boolean order(ServerLevel world,RackBlockEntity rack){
        return order(world,rack,null);
    }
    public boolean order(ServerLevel world,RackBlockEntity rack,BlockPos selectedFloorPallet){
        if(!beginWithoutCargo())return false;
        if(!world.hasChunkAt(rack.getBlockPos())){
            status="RACK CHUNK NOT LOADED";
            return false;
        }
        RackPickupPlan plan=RackPickupPlan.forklift(world,rack);
        if(plan==null||plan.height()>RackPickupPlan.FORKLIFT_MAX_HEIGHT){
            status="NO CLEAR TWO-ROW FORKLIFT APPROACH OR MAST HEIGHT EXCEEDED";
            return false;
        }
        boolean pickup=rack.hasPallet();
        BlockPos src=pickup?rack.getBlockPos()
                :selectedFloorPallet!=null?selectedFloorPallet
                :nearestFloorPallet(world,plan.rigCenter());
        if(!pickup&&selectedFloorPallet!=null){
            PalletBlockEntity selected=StorageGroundPalletTransfer.wrappedController(
                    world,selectedFloorPallet);
            if(selected==null||!selected.getBlockPos().equals(selectedFloorPallet)
                    ||selectedFloorPallet.distSqr(rack.getBlockPos())>24*24){
                status="SELECTED GROUND PALLET IS NOT WRAPPED, LOADED OR WITHIN 24 BLOCKS";
                return false;
            }
        }
        if(src==null){
            status="NO WRAPPED SOURCE PALLET FOUND NEAR DESTINATION";
            return false;
        }
        if(pickup&&!rack.glassWrapped()){
            status="SOURCE RACK PALLET NEEDS GLASS WRAPPING";
            return false;
        }
        if(!pickup&&(rack.hasPallet()||!rack.isEmpty())){
            status=rack.hasPallet()?"DESTINATION RACK ALREADY CONTAINS A PALLET"
                    :"DESTINATION RACK HAS STORED ITEMS WITHOUT A PALLET; "
                    +"EMPTY INVENTORY SAFELY BEFORE USE";
            return false;
        }
        BlockPos proposedStaging=pickup?floorPlacement(world,rack,plan.floorY())
                :src.immutable();
        if(proposedStaging==null||proposedStaging.equals(BlockPos.ZERO)){
            status="NO EMPTY 2x2 STAGING FOOTPRINT; CHECK ACTUAL BLOCKS AND FLOOR";
            return false;
        }
        if(!hire(world))return false;
        this.retrieving=pickup;
        this.groundDestination=pickup;
        this.source=src.immutable();
        this.target=rack.getBlockPos().immutable();
        this.staging=proposedStaging;
        if(pickup)this.groundFacing=rack.facing();
        resetRackManeuver();
        this.job=1;
        this.animation=0;
        this.status="DRIVER BOARDING";
        return true;
    }
    public boolean orderRackToRack(ServerLevel world,RackBlockEntity from,
            RackBlockEntity to){
        if(!beginWithoutCargo())return false;
        if(from==null||to==null){
            status="SOURCE OR DESTINATION IS NOT A VALID RACK";
            return false;
        }
        if(from.getBlockPos().equals(to.getBlockPos())){
            status="SOURCE AND DESTINATION ARE THE SAME 2x2x3 RACK";
            return false;
        }
        if(!from.hasPallet()||!from.glassWrapped()){
            status="SOURCE RACK NEEDS A REAL GLASS-WRAPPED PALLET";
            return false;
        }
        if(to.hasPallet()){
            status="DESTINATION RACK CONTAINS A PALLET: "+to.getBlockPos().toShortString();
            return false;
        }
        if(!to.isEmpty()){
            status="DESTINATION RACK HAS "+to.occupiedSlots()
                    +" STORED ITEM SLOTS BUT NO PALLET; ITEMS NOT OVERWRITTEN";
            return false;
        }
        if(RackPickupPlan.forklift(world,from)==null){
            status="SOURCE RACK CANNOT EXTRACT: AISLE BLOCKED OR MAST ABOVE 12 BLOCKS";
            return false;
        }
        if(RackPickupPlan.forklift(world,to)==null){
            status="DESTINATION RACK CANNOT ACCEPT: AISLE BLOCKED OR MAST ABOVE 12 BLOCKS";
            return false;
        }
        if(!hire(world))return false;
        source=from.getBlockPos().immutable();
        target=to.getBlockPos().immutable();
        staging=BlockPos.ZERO;
        retrieving=true;groundDestination=false;
        resetRackManeuver();
        job=1;animation=0;status="DRIVER BOARDING - RACK TO RACK";
        return true;
    }
    public boolean orderGround(ServerLevel world,BlockPos from,BlockPos landing,
            Direction facing){
        if(!beginWithoutCargo())return false;
        if(landing==null||from==null||!ForkliftPalletIO.fits(
                world,landing,facing)){
            status="GROUND DESTINATION NEEDS FOUR CLEAR PALLET CELLS ON SOLID FLOOR";
            return false;
        }
        RackBlockEntity rack=world.getBlockEntity(from) instanceof RackBlockEntity r?r:null;
        boolean fromRack=rack!=null&&rack.hasPallet()&&rack.glassWrapped();
        if(fromRack&&RackPickupPlan.forklift(world,rack)==null)return false;
        if(!fromRack){
            var ground=StorageGroundPalletTransfer.wrappedController(world,from);
            if(ground==null||!ground.getBlockPos().equals(from))return false;
        }
        if(!hire(world))return false;
        source=from.immutable();target=from.immutable();staging=landing.immutable();
        retrieving=fromRack;groundDestination=true;groundFacing=facing;
        resetRackManeuver();
        job=1;animation=0;status="DRIVER BOARDING - PALLET TO GROUND";
        return true;
    }

    private BlockPos nearestFloorPallet(ServerLevel level,BlockPos around){
        BlockPos best=null;double distance=Double.POSITIVE_INFINITY;
        for(int dx=-12;dx<=12;dx++)for(int dz=-12;dz<=12;dz++)
            for(int dy=-2;dy<=2;dy++){
                BlockPos at=around.offset(dx,dy,dz);
                if(!level.hasChunkAt(at))continue;
                PalletBlockEntity found=StorageGroundPalletTransfer.wrappedController(level,at);
                if(found==null)continue;
                double score=found.getBlockPos().distSqr(blockPosition());
                if(score<distance){best=found.getBlockPos();distance=score;}
            }
        return best;
    }
    private BlockPos floorPlacement(ServerLevel world,RackBlockEntity rack,int floorY){
        BlockPos root=rack.getBlockPos();Direction front=rack.facing();
        // Prefer the pallet-sized apron in front of the rack. Keep all
        // four blocks clear; don't overwrite a neighboring rack.
        for(int depth=2;depth<=7;depth++)
            for(int offset=-2;offset<=2;offset++){
                BlockPos at=root.relative(front,depth)
                        .relative(front.getClockWise(),offset);
                at=new BlockPos(at.getX(),floorY,at.getZ());
                if(ForkliftPalletIO.fits(world,at,front))return at;
            }
        return null;
    }
    private boolean seatedDriver(ServerLevel world){
        if(driver==null)return false;
        Entity occupant=world.getEntity(driver);
        return occupant instanceof StorageGolem golem && golem.getVehicle()==this;
    }
    /** Explicit heading matters at the load face: pointing at a corner
     * makes the forks clip one side of the pallet. Hold the truck square. */
    private boolean turnForksToward(double x,double z){
        double dx=x-getX(),dz=z-getZ();
        if(dx*dx+dz*dz<.01)return true;
        return turnForksToYaw((float)Math.toDegrees(Math.atan2(-dx,dz)));
    }
    private boolean turnForksToYaw(float wanted){
        float error=Mth.wrapDegrees(wanted-getYRot());
        float steering=Mth.clamp(error/45f,-1f,1f);
        if(Math.abs(entityData.get(STEERING)-steering)>.035f)
            entityData.set(STEERING,steering);
        float yaw=getYRot()+Mth.clamp(error,-14f,14f);
        setYRot(yaw);
        setYBodyRot(yaw);
        setYHeadRot(yaw);
        return Math.abs(Mth.wrapDegrees(wanted-yaw))<3f;
    }
    private boolean alignForTransfer(Dock dock){
        // The carriage and BOTH fork tines must point straight along
        // the rack's normal, not diagonally toward its controller corner.
        Direction face=dock.forksFace();
        boolean aligned=turnForksToYaw((float)Math.toDegrees(
                Math.atan2(-face.getStepX(),face.getStepZ())));
        getNavigation().stop();
        setDeltaMovement(0,getDeltaMovement().y,0);
        return aligned;
    }

    /** Move up to 0.20 blocks per tick while keeping the 1.2-wide vehicle
     * entirely inside the clearance checked by ForkliftAislePath. */
    private boolean motorStep(ServerLevel world,Vec3 waypoint){
        double dx=waypoint.x-getX(),dz=waypoint.z-getZ();
        double distance=Math.hypot(dx,dz);
        if(distance<.06)return true;
        // Steering and rolling are concurrent when the wheels already
        // point approximately down the lane. Only a SHARP aisle corner
        // requires turning in place; the old <3-degree requirement at
        // EVERY half-block A* waypoint was what caused start-stop driving.
        boolean facing=turnForksToward(waypoint.x,waypoint.z);
        float desired=(float)Math.toDegrees(Math.atan2(-dx,dz));
        double error=Math.abs(Mth.wrapDegrees(desired-getYRot()));
        lastHeadingError=error;
        if(!facing&&error>52){
            rollingSpeed=Math.max(0,rollingSpeed-MOTOR_ACCELERATION*1.5);
            setDeltaMovement(0,getDeltaMovement().y,0);
            return false;
        }
        double alreadyMoved=Math.hypot(
                getX()-driveTickStartX,getZ()-driveTickStartZ);
        double cornerLimit=error>35?DRIVE_BLOCKS_PER_TICK*.65:
                error>18?DRIVE_BLOCKS_PER_TICK*.83:DRIVE_BLOCKS_PER_TICK;
        rollingSpeed=Math.min(DRIVE_BLOCKS_PER_TICK,
                rollingSpeed+MOTOR_ACCELERATION);
        double step=Math.min(distance,
                Math.max(0,Math.min(rollingSpeed,cornerLimit)-alreadyMoved));
        if(step<.005)return false;
        Vec3 delta=new Vec3(dx/distance*step,0,dz/distance*step);
        Vec3 next=position().add(delta);
        if(!ForkliftAislePath.canStand(world,this,next)){
            rollingSpeed=0;
            return false;
        }
        double beforeX=getX(),beforeZ=getZ();
        move(MoverType.SELF,delta);
        setDeltaMovement(0,getDeltaMovement().y,0);
        return Math.hypot(getX()-beforeX,getZ()-beforeZ)>.01;
    }

    private boolean drive(ServerLevel world,BlockPos goal){
        return drive(world,Vec3.atCenterOf(goal));
    }
    /** Follow a routed series of aisle waypoints, never the direct bearing
     * to a blocked pallet. Replan if a path is obstructed by a newly placed
     * block or moving vehicle. Refuse to go through a wall if none exists. */
    private boolean drive(ServerLevel world,Vec3 goal){
        // The custom forklift motor is the sole horizontal driver. Never
        // issue a dummy vanilla MoveControl instruction at our own current
        // position: it can overwrite speed/heading in the following AI tick.
        // Navigation is already stopped before super.tick() while on a job.
        double dx=goal.x-getX(),dz=goal.z-getZ();
        double distance=Math.hypot(dx,dz);
        double tolerance=job==5?.82:.16;
        if(distance<tolerance&&Math.abs(goal.y-getY())<.7){
            aisleRoute=List.of();
            routeStep=0;
            rollingSpeed=0;
            setDeltaMovement(0,getDeltaMovement().y,0);
            stallTicks=0;
            return true;
        }
        boolean changed=aisleTarget==null||aisleTarget.distanceToSqr(goal)>.01;
        if(changed){
            aisleTarget=goal;
            aisleRoute=List.of();
            routeStep=0;
            lastRouteTick=-100;
        }
        if(Math.abs(goal.y-getY())>.7){
            status="WAITING: FORKLIFT REQUIRES A LEVEL AISLE TO THE DOCK";
            return false;
        }
        if(aisleRoute.isEmpty()||routeStep>=aisleRoute.size()){
            if(tickCount-lastRouteTick<20)return false;
            lastRouteTick=tickCount;
            List<Vec3> raw=ForkliftAislePath.route(world,this,goal);
            aisleRoute=ForkliftAislePath.smooth(world,this,raw,
                    ROUTE_SEGMENT_LENGTH);
            routeStep=0;
            if(aisleRoute.isEmpty()){
                rollingSpeed=0;
                status="WAITING: NO SAFE FORKLIFT AISLE ROUTE TO DOCK";
                return false;
            }
        }
        // Hold a CONSTANT steering point until reaching that segment.
        // Changing it every tick was causing hesitation and false corner
        // braking even after the acceleration patch.
        while(routeStep<aisleRoute.size()
                &&Math.hypot(getX()-aisleRoute.get(routeStep).x,
                        getZ()-aisleRoute.get(routeStep).z)<.08)
            routeStep++;
        if(routeStep>=aisleRoute.size())return false;
        Vec3 waypoint=aisleRoute.get(routeStep);
        boolean moved=motorStep(world,waypoint);
        if(moved){
            stallTicks=0;
            status="DRIVING FORKLIFT VIA CLEAR WAREHOUSE AISLE";
        }else if(++stallTicks>30){
            // The next segment has been blocked. Replan, but NEVER switch
            // to a straight-to-pallet fallback that intersects rack frames.
            aisleRoute=List.of();
            routeStep=0;
            rollingSpeed=0;
            stallTicks=0;
            status="REPLANNING FORKLIFT AISLE AROUND OBSTRUCTION";
        }
        return false;
    }
    private static Vec3 rackPalletCenter(RackBlockEntity rack){
        BlockPos root=rack.getBlockPos();
        Direction face=rack.facing(),side=face.getCounterClockWise();
        // The controller is the front-left cube of the 2x2 rack. The
        // center lies between BOTH columns and half a block into the bay.
        return new Vec3(root.getX()+.5+side.getStepX()*.5
                        -face.getStepX()*.5,root.getY(),
                root.getZ()+.5+side.getStepZ()*.5-face.getStepZ()*.5);
    }
    private Dock service(ServerLevel world,RackBlockEntity rack){
        RackPickupPlan plan=RackPickupPlan.forklift(world,rack);
        if(plan==null)return null;
        Direction outward=rack.facing();
        Vec3 palletCenter=rackPalletCenter(rack);
        // The rendered pallet sits 2.82 blocks forward of the chassis,
        // halfway between the fork tines. Aim the chassis directly behind
        // this center, in row two of the three-block working aisle.
        Vec3 position=new Vec3(
                palletCenter.x+outward.getStepX()*2.82,
                plan.floorY(),
                palletCenter.z+outward.getStepZ()*2.82);
        return ForkliftAislePath.canStand(world,this,position)
                ?new Dock(position,outward.getOpposite()):null;
    }
    private Vec3 groundPalletCenter(ServerLevel world,BlockPos root,Direction facing){
        var state=world.getBlockState(root);
        if(!(state.getBlock() instanceof PalletBlock))
            state=PalletContent.WOODEN_PALLET.defaultBlockState()
                    .setValue(PalletBlock.FACING,facing).setValue(PalletBlock.PART,0);
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        for(int i=0;i<4;i++){
            BlockPos at=PalletBlock.partPos(root,i,state);
            minX=Math.min(minX,at.getX());
            minZ=Math.min(minZ,at.getZ());
        }
        return new Vec3(minX+1.0,root.getY(),minZ+1.0);
    }
    /** Try all four sides and select the SHORTEST REACHABLE aisle route,
     * not merely the closest location separated by a glass rack bank. */
    private Dock groundService(ServerLevel world,BlockPos root,Direction facing){
        if(cachedDockJob==job&&root.equals(cachedDockRoot)
                &&cachedGroundDock!=null)return cachedGroundDock;
        if(cachedDockJob==job&&root.equals(cachedDockRoot)
                &&tickCount-lastDockSearch<30)return null;
        lastDockSearch=tickCount;
        cachedDockJob=job;
        cachedDockRoot=root.immutable();
        cachedGroundDock=null;
        Vec3 center=groundPalletCenter(world,root,facing);
        int fewest=Integer.MAX_VALUE;
        for(Direction side:Direction.Plane.HORIZONTAL){
            Vec3 position=new Vec3(center.x+side.getStepX()*2.82,
                    center.y,center.z+side.getStepZ()*2.82);
            if(!ForkliftAislePath.canStand(world,this,position))continue;
            List<Vec3> path=ForkliftAislePath.route(world,this,position);
            if(!path.isEmpty()&&path.size()<fewest){
                fewest=path.size();
                cachedGroundDock=new Dock(position,side.getOpposite());
            }
        }
        return cachedGroundDock;
    }
    private ForkliftRackManeuver.Plan rackArc(ServerLevel world,
            RackBlockEntity rack,boolean sourceRack){
        if(rack==null)return null;
        ForkliftRackManeuver.Plan cached=sourceRack?sourceArc:destinationArc;
        if(cached!=null)return cached;
        int last=sourceRack?lastSourceArcSearch:lastDestinationArcSearch;
        if(tickCount-last<20)return null;
        if(sourceRack)lastSourceArcSearch=tickCount;
        else lastDestinationArcSearch=tickCount;
        Dock dock=service(world,rack);
        if(dock==null)return null;
        int preferred=sourceRack?sourceArcTurn:destinationArcTurn;
        double depth=sourceRack?sourceReverseDepth:destinationReverseDepth;
        double shift=sourceRack?sourceAisleShift:destinationAisleShift;
        ForkliftRackManeuver.Plan planned=ForkliftRackManeuver.plan(
                world,this,rack,dock.position(),preferred,depth,shift);
        if(planned!=null){
            if(sourceRack){
                sourceArc=planned;sourceArcTurn=planned.turn();
                sourceReverseDepth=planned.reverseDepth();
                sourceAisleShift=planned.aisleShift();
            }else{
                destinationArc=planned;destinationArcTurn=planned.turn();
                destinationReverseDepth=planned.reverseDepth();
                destinationAisleShift=planned.aisleShift();
            }
        }
        return planned;
    }

    /** Keep BOTH tines at rack height while tracing the reverse arc.
     * Entering follows the same checked path in reverse, instead of turning
     * instantly at a rack corner. No movement is allowed to skip collision
     * checks or exceed the normal per-tick motor displacement budget. */
    private boolean followRackArc(ServerLevel world,
            ForkliftRackManeuver.Plan plan,RackBlockEntity rack,
            boolean entering,boolean atSource){
        if(plan==null)return false;
        int cursor=atSource?sourceArcCursor:destinationArcCursor;
        if(cursor<0)cursor=entering?plan.poses().size()-1:0;
        int next=cursor+(entering?-1:1);
        if(next<0||next>=plan.poses().size()){
            if(atSource)sourceArcCursor=cursor;
            else destinationArcCursor=cursor;
            return true;
        }
        ForkliftRackManeuver.Pose nextPose=plan.poses().get(next);
        Vec3 goal=nextPose.position();
        double dx=goal.x-getX(),dz=goal.z-getZ();
        double length=Math.hypot(dx,dz);
        double remaining=Math.max(0,DRIVE_BLOCKS_PER_TICK-
                Math.hypot(getX()-driveTickStartX,getZ()-driveTickStartZ));
        double step=Math.min(Math.min(.12,remaining),length);
        Vec3 delta=length<.0001?Vec3.ZERO:new Vec3(dx/length*step,0,dz/length*step);
        Vec3 position=position().add(delta);
        float yaw=length<.0001?nextPose.yaw():
                ForkliftRackManeuver.angleTo(getYRot(),nextPose.yaw(),
                        (float)Math.min(1,step/length));
        int floor=(int)Math.floor(getY());
        if(!ForkliftAislePath.canStand(world,this,position)
                ||!ForkliftRackManeuver.clearForkEnvelope(world,position,yaw,
                    floor,rack.getBlockPos().getY(),rack,true)){
            status="WAITING: RACK MANEUVER BLOCKED; FORKS HELD AT SAFE HEIGHT";
            getNavigation().stop();
            return false;
        }
        if(step>.001){
            double beforeX=getX(),beforeZ=getZ();
            move(MoverType.SELF,delta);
            if(Math.hypot(getX()-beforeX,getZ()-beforeZ)<step*.6){
                status="WAITING: RACK AISLE OBSTRUCTION DURING BACKING TURN";
                return false;
            }
        }
        setYRot(yaw);setYBodyRot(yaw);setYHeadRot(yaw);
        entityData.set(STEERING,Mth.clamp(
                Mth.wrapDegrees(nextPose.yaw()-getYRot())/45f,-1f,1f));
        setDeltaMovement(0,getDeltaMovement().y,0);
        if(Math.hypot(goal.x-getX(),goal.z-getZ())<.045
                &&Math.abs(Mth.wrapDegrees(nextPose.yaw()-getYRot()))<2.5f){
            cursor=next;
            if(atSource)sourceArcCursor=cursor;
            else destinationArcCursor=cursor;
        }else{
            if(atSource)sourceArcCursor=cursor;
            else destinationArcCursor=cursor;
        }
        status=entering?"CURVING INTO RACK WITH FORKS AT PALLET HEIGHT":
                "BACKING CLEAR OF RACK WHILE GRADUALLY TURNING 90 DEGREES";
        return false;
    }
    /** Approach exterior with LOW forks, raise fully clear of the racks,
     * then steer the truck and fork tips into the exact square dock. */
    private boolean enterRackSafely(ServerLevel world,RackBlockEntity rack,
            boolean atSource){
        ForkliftRackManeuver.Plan plan=rackArc(world,rack,atSource);
        if(plan==null){
            status="WAITING: NO SAFE STRAIGHT OR 90-DEGREE RACK APPROACH";
            return false;
        }
        int stage=atSource?sourceRackStage:destinationRackStage;
        if(stage==0){
            if(!drive(world,plan.outside().position()))return false;
            // Match the tangent of the entry curve WHILE the tines are
            // lowered and fully outside, never snap the chassis 90 degrees
            // once the raised tines are inside the adjacent rack row.
            if(!turnForksToYaw(plan.outside().yaw())){
                status="ALIGNING FORKLIFT WITH OUTER RACK TURN";return false;
            }
            stage=1;
            if(atSource)sourceRackStage=1;else destinationRackStage=1;
        }
        if(stage==1){
            if(Math.abs(entityData.get(MAST)
                    -Math.clamp(rack.getBlockPos().getY()-getY(),
                            0,RackPickupPlan.FORKLIFT_MAX_HEIGHT))>.10f){
                status="RAISING FORKS OUTSIDE RACK BEFORE ENTRY";return false;
            }
            stage=2;
            if(atSource)sourceRackStage=2;else destinationRackStage=2;
        }
        if(stage==2){
            if(!followRackArc(world,plan,rack,true,atSource))return false;
            if(atSource)sourceRackStage=3;else destinationRackStage=3;
        }
        return stage==3 ||(atSource?sourceRackStage:destinationRackStage)==3;
    }
    private boolean exitRackSafely(ServerLevel world,RackBlockEntity rack,
            boolean atSource){
        ForkliftRackManeuver.Plan plan=rackArc(world,rack,atSource);
        if(plan==null){
            status="WAITING: SAFE RACK WITHDRAWAL UNAVAILABLE; MAST HELD";
            return false;
        }
        if(!followRackArc(world,plan,rack,false,atSource))return false;
        if(!ForkliftRackManeuver.fullyOutside(world,plan,
                (int)Math.floor(getY()),rack.getBlockPos().getY(),rack)){
            status="WAITING: FORKS STILL INSIDE RACK; REFUSING TO LOWER";
            return false;
        }
        if(atSource)sourceRackStage=5;else destinationRackStage=5;
        return true;
    }

    @Override public void tick(){
        // Include any navigation displacement in the physical 0.20-block
        // movement budget so the forklift cannot exceed the requested speed.
        driveTickStartX=getX();
        driveTickStartZ=getZ();
        // The custom aisle motor owns ALL horizontal driving. Allowing
        // vanilla navigation to tick here previously pulled the vehicle
        // directly toward the pallet before the collision-aware route.
        if(job!=0){
            getNavigation().stop();
            setDeltaMovement(0,getDeltaMovement().y,0);
        }
        super.tick();
        if(!level().isClientSide())measureActualMotion();
        // Rider coordinates are owned by the forklift on both logical sides.
        // Keep rider rotation tied to the cab, not independent golem look AI:
        // the passenger may otherwise visually turn away while steering.
        for(Entity passenger:getPassengers()){
            if(passenger instanceof StorageGolem golem){
                golem.setYRot(getYRot());
                golem.setYBodyRot(getYRot());
                golem.setYHeadRot(getYRot());
            }
        }
        if(!(level() instanceof ServerLevel world))return;
        // Rack mast only changes at the SAFE EXTERIOR, never during
        // the curved 90-degree withdrawal or while forks are in a shelf.
        boolean liftingAtSource=retrieving&&sourceRackStage>=1
                &&sourceRackStage<5&&(job==1||job==2||job==3);
        boolean liftingAtDestination=!groundDestination
                &&destinationRackStage>=1&&destinationRackStage<5
                &&(job==3||job==4);
        float goal=liftingAtSource
                ?Math.clamp(source.getY()-blockPosition().getY(),0,
                        RackPickupPlan.FORKLIFT_MAX_HEIGHT)
                :liftingAtDestination
                    ?Math.clamp(target.getY()-blockPosition().getY(),0,
                            RackPickupPlan.FORKLIFT_MAX_HEIGHT)
                    :0;
        float current=entityData.get(MAST);
        float eased=current+(goal-current)*.12f;
        entityData.set(MAST,Math.abs(goal-eased)<.012f?goal:eased);
        entityData.set(ACTION,job);
        if(job==0)return;
        if(job==5){
            // Keep vehicle movement contingent on an actual seated operator.
            if(driver!=null&&!seatedDriver(world)){
                getNavigation().stop();
                status="WAITING FOR FORKLIFT OPERATOR TO BOARD";
                return;
            }
            if(parking.equals(BlockPos.ZERO)){
                status="WAITING: FORKLIFT PARKING BAY NOT SET";
                return;
            }
            if(drive(world,parking)){
                job=0;status="PARKED - DRIVER ON DUTY";
                if(dismissAfterParking){
                    dismissAfterParking=false;
                    dismissDriver(world);
                }
            }else if(!status.startsWith("WAITING: FORKLIFT PATH BLOCKED"))
                status="RETURNING TO PAINTED FORKLIFT PARKING BAY";
            return;
        }
        if(!seatedDriver(world)){
            getNavigation().stop();
            status=driver!=null?"GOLEM LEAVING SHELF / BOARDING FORKLIFT"
                    :"WAITING FOR AN AVAILABLE GOLEM";
            return;
        }
        if(!world.hasChunkAt(source)||!world.hasChunkAt(target)){
            getNavigation().stop();status="WAITING FOR LOADED CHUNKS";return;
        }
        RackBlockEntity rack=world.getBlockEntity(target)
                instanceof RackBlockEntity r?r:null;
        RackBlockEntity origin=world.getBlockEntity(source)
                instanceof RackBlockEntity r?r:null;
        if(!groundDestination&&rack==null){
            status="WAITING FOR TARGET RACK";return;
        }
        if(retrieving&&origin==null){status="WAITING FOR SOURCE RACK";return;}
        if(job==1){
            if(retrieving){
                if(!enterRackSafely(world,origin,true))return;
            }else{
                Dock pickup=groundService(world,source,groundFacing);
                if(pickup==null){status="WAITING FOR REACHABLE PALLET-SIDE AISLE";return;}
                if(!drive(world,pickup.position()))return;
                if(!alignForTransfer(pickup)){
                    status="SQUARING FORKS WITH CENTER OF SOURCE PALLET";return;
                }
            }
            job=2;animation=0;
        }
        if(job==2){
            Dock pickup=retrieving?service(world,origin)
                    :groundService(world,source,groundFacing);
            if(pickup==null||(retrieving&&sourceRackStage!=3)
                    ||!alignForTransfer(pickup)){
                status="SQUARING FORKS WITH CENTER OF SOURCE PALLET";
                return;
            }
            if(++animation<45){status="ALIGNING FORKS AND LIFTING";return;}
            boolean ok=retrieving?origin.liftToForklift(jack):
                    ForkliftPalletIO.liftGround(world,source,jack);
            if(!ok){status="WAITING: PALLET IN USE OR ACCESS BLOCKED";animation=0;return;}
            refreshDisplay();
            if(retrieving){sourceRackStage=4;sourceArcCursor=0;}
            job=3;animation=0;
        }
        if(job==3){
            // The source rack stays at its original lift height until the
            // *entire* fork envelope has moved into the aisle. Never lower
            // the carriage or start another job while still in the rack.
            if(retrieving&&sourceRackStage==4){
                if(!exitRackSafely(world,origin,true))return;
                sourceArc=null;sourceArcCursor=-1;
            }
            // A carried pallet travels LOW to its next destination. When
            // that destination is a rack, the mast is raised again only
            // at the outside end of its verified curved entrance route.
            if(destinationRackStage==0&&entityData.get(MAST)>.20f){
                status="LOWERING PALLET AFTER COMPLETE RACK WITHDRAWAL";
                return;
            }
            if(!groundDestination){
                if(!enterRackSafely(world,rack,false))return;
                Dock finalDock=service(world,rack);
                if(finalDock==null||!alignForTransfer(finalDock)){
                    status="SQUARING FORKS WITH CENTER OF DESTINATION";
                    return;
                }
            }else{
                Dock landing=groundService(world,staging,groundFacing);
                if(landing==null){status="WAITING FOR REACHABLE DROP-OFF AISLE";return;}
                if(!drive(world,landing.position()))return;
                if(!alignForTransfer(landing)){
                    status="SQUARING FORKS WITH CENTER OF DESTINATION";return;
                }
            }
            if(++animation<55
                    ||(!groundDestination&&Math.abs(entityData.get(MAST)-goal)>.12f)){
                status=groundDestination?"LOWERING PALLET ONTO GROUND"
                        :"PLACING PALLET INTO RACK AT PRESET MAST HEIGHT";
                return;
            }
            boolean placed=groundDestination?ForkliftPalletIO.placeGround(
                    world,staging,groundFacing,jack):rack.installFromForklift(jack);
            if(!placed){status="WAITING FOR SAFE DROP LOCATION";animation=20;return;}
            refreshDisplay();
            if(!groundDestination){destinationRackStage=4;destinationArcCursor=0;}
            job=4;animation=0;
        }
        if(job==4){
            // Empty tines retract from the shelf still fully raised. The
            // lowering animation starts only after the exterior check.
            if(!groundDestination&&destinationRackStage==4){
                if(!exitRackSafely(world,rack,false))return;
                destinationArc=null;destinationArcCursor=-1;
            }
            if(entityData.get(MAST)>.20f){
                status="LOWERING EMPTY FORKS CLEAR OF RACKS";return;
            }
            if(++animation>=25)endTask(world);
        }
    }
    private void endTask(ServerLevel world){
        // A driver remains mounted and on duty after a pallet delivery.
        // Only a controller click on the idle forklift dismisses them.
        job=driver!=null&&!parking.equals(BlockPos.ZERO)?5:0;
        animation=0;retrieving=false;
        resetRackManeuver();
        status=job==5?"RETURNING TO PAINTED FORKLIFT PARKING BAY"
                :driver==null?"PARKED":"PARKED - DRIVER ON DUTY";
    }
    /** Used when clicking the actual seated Golem, not the forklift body. */
    private boolean atParkingBay(){
        if(parking.equals(BlockPos.ZERO))return false;
        double dx=getX()-(parking.getX()+.5),dz=getZ()-(parking.getZ()+.5);
        return dx*dx+dz*dz<.90*.90
                &&Math.abs(getY()-parking.getY())<.8;
    }
    /** Cancel the stalled order and drive home with the original packed
     * cargo. This runs even when the current task cannot be completed. */
    public String requestDriverDismissal(ServerLevel world){
        if(driver==null)return "FORKLIFT: no active driver.";
        if(parking.equals(BlockPos.ZERO))
            return "FORKLIFT: no saved parking bay. Sneak-click the forklift "
                    +"with empty hand for an emergency driver release.";
        if(atParkingBay()){
            job=0;animation=0;dismissAfterParking=false;
            return dismissDriver(world)
                    ?"FORKLIFT: parked, driver heading to shelf."
                    :"FORKLIFT: driver not loaded; retry when nearby.";
        }
        // A failed loading job must not block recall indefinitely.
        // Cargo already in the Pallet Jack stays in the forklift.
        dismissAfterParking=true;
        job=5;animation=0;stallTicks=0;
        lastDriveGoal=BlockPos.ZERO;
        getNavigation().stop();
        status="RETURNING TO PARKING BAY FOR DRIVER DISMISSAL";
        return "FORKLIFT: task canceled; returning to parking bay, then "
                +"driver dismounts. Any sealed pallet stays in the forklift.";
    }
    public boolean dismissDriver(ServerLevel world){
        if(job!=0||!atParkingBay()||driver==null)return false;
        Entity passenger=world.getEntity(driver);
        if(!(passenger instanceof StorageGolem golem))return false;
        golem.finishForkliftDriving();
        driver=null;dismissAfterParking=false;
        getNavigation().stop();
        status=hasPackedCargo()
                ?"DRIVER HOME; PACKED JACK CARGO SECURE IN FORKLIFT"
                :"DRIVER DISMISSED - RETURNING TO SHELF";
        return true;
    }
    /** A separate deliberate override if the vehicle cannot physically
     * reach its bay. Never force-teleport the truck or delete loaded cargo. */
    public String emergencyDriverRelease(ServerLevel world){
        if(driver==null)return "FORKLIFT: no driver aboard.";
        Entity passenger=world.getEntity(driver);
        if(!(passenger instanceof StorageGolem golem))
            return "FORKLIFT: driver not loaded; cannot release safely.";
        getNavigation().stop();
        setDeltaMovement(0,getDeltaMovement().y,0);
        job=0;animation=0;stallTicks=0;dismissAfterParking=false;
        lastDriveGoal=BlockPos.ZERO;
        golem.finishForkliftDriving();driver=null;
        status=PalletJackItem.loaded(jack)
                ?"EMERGENCY STOP; PACKED PALLET SECURE IN FORKLIFT"
                :"EMERGENCY STOP; DRIVER RETURNING TO SHELF";
        return "FORKLIFT: emergency stop; driver released. "
                +(PalletJackItem.loaded(jack)
                    ?"Cargo stays safely packed. Sneak-click again with "
                     +"empty hand to retrieve the complete Pallet Jack."
                    :"No cargo was aboard.");
    }
    /** Recover a stranded packed pallet with every slot intact. After
     * transfer, clear the vehicle's copy exactly once to prevent duplication.
     * Vanilla inventory return drops overflow near the player if necessary. */
    public String recoverStuckCargo(Player player){
        if(driver!=null||job!=0)
            return "FORKLIFT: stop and release the driver first.";
        if(!hasPackedCargo())
            return "FORKLIFT: no packed pallet cargo to recover.";
        ItemStack packed=jack.copy();
        jack=new ItemStack(RackContent.PALLET_JACK);
        refreshDisplay();
        player.getInventory().placeItemBackInInventory(packed);
        status="PACKED PALLET JACK RECOVERED BY PLAYER";
        return "FORKLIFT: original packed Pallet Jack returned with every item.";
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel world){
            // One packed pallet must survive the vehicle's destruction.
            if(hasPackedCargo()){
                net.minecraft.world.Containers.dropItemStack(world,
                        getX(),getY(),getZ(),jack.copy());
                jack.remove(net.minecraft.core.component.DataComponents.CONTAINER);
            }
            net.minecraft.world.Containers.dropItemStack(world,
                    getX(),getY(),getZ(),new ItemStack(RackContent.FORKLIFT_ITEM));
            // Destruction is not an ordinary driver dismissal. The vehicle
            // no longer exists to drive back to its parking bay.
            Entity rider=driver==null?null:world.getEntity(driver);
            if(rider instanceof StorageGolem golem)golem.finishForkliftDriving();
            driver=null;
        }
        super.die(source);
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(level().isClientSide())return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()&&(player.getItemInHand(hand).isEmpty()
                ||player.getItemInHand(hand).is(RackContent.FORKLIFT_CONTROLLER))){
            player.sendSystemMessage(Component.literal(
                    driver==null?recoverStuckCargo(player)
                        :emergencyDriverRelease((ServerLevel)level())));
            return InteractionResult.SUCCESS;
        }
        if(player.getItemInHand(hand).is(RackContent.STORAGE_SCANNER_ITEM)){
            var scanner=player.getItemInHand(hand);
            if(StorageScannerItem.mode(scanner)!=StorageScannerItem.Mode.FORKLIFT)
                return InteractionResult.SUCCESS;
            StorageScannerItem.onForkliftClick((ServerLevel)level(),
                    (net.minecraft.server.level.ServerPlayer)player,scanner,this);
            return InteractionResult.SUCCESS;
        }
        if(player.getItemInHand(hand).is(RackContent.FORKLIFT_CONTROLLER)){
            player.sendSystemMessage(Component.literal(
                    requestDriverDismissal((ServerLevel)level())));
        }else{
            player.sendSystemMessage(Component.literal("Forklift: "+status
                    +(PalletJackItem.loaded(jack)?" | carrying wrapped pallet":"")
                    +" | "+driveDiagnostics()
                    +". Dispatch by selecting a rack with the black Forklift Clipboard."));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.store("source",BlockPos.CODEC,source);
        out.store("target",BlockPos.CODEC,target);
        out.store("staging",BlockPos.CODEC,staging);
        out.store("parking",BlockPos.CODEC,parking);
        out.putBoolean("retrieving",retrieving);
        out.putBoolean("ground_destination",groundDestination);
        out.putString("ground_facing",groundFacing.getName());
        out.putBoolean("dismiss_after_parking",dismissAfterParking);
        out.putInt("job",job);
        out.putInt("animation",animation);
        out.putInt("source_rack_stage",sourceRackStage);
        out.putInt("destination_rack_stage",destinationRackStage);
        out.putInt("source_arc_cursor",sourceArcCursor);
        out.putInt("destination_arc_cursor",destinationArcCursor);
        out.putInt("source_arc_turn",sourceArcTurn);
        out.putInt("destination_arc_turn",destinationArcTurn);
        out.putDouble("source_reverse_depth",sourceReverseDepth);
        out.putDouble("source_aisle_shift",sourceAisleShift);
        out.putDouble("destination_reverse_depth",destinationReverseDepth);
        out.putDouble("destination_aisle_shift",destinationAisleShift);
        if(driver!=null)out.putString("driver_uuid",driver.toString());
        if(!jack.isEmpty())out.store("jack",ItemStack.CODEC,jack);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        source=in.read("source",BlockPos.CODEC).orElse(BlockPos.ZERO);
        target=in.read("target",BlockPos.CODEC).orElse(BlockPos.ZERO);
        staging=in.read("staging",BlockPos.CODEC).orElse(BlockPos.ZERO);
        parking=in.read("parking",BlockPos.CODEC).orElse(BlockPos.ZERO);
        retrieving=in.getBooleanOr("retrieving",false);
        groundDestination=in.getBooleanOr("ground_destination",false);
        try{groundFacing=Direction.valueOf(in.getStringOr("ground_facing","NORTH")
                .toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException e){groundFacing=Direction.NORTH;}
        dismissAfterParking=in.getBooleanOr("dismiss_after_parking",false);
        job=in.getIntOr("job",0);
        animation=in.getIntOr("animation",0);
        // Older in-flight transfers begin from their safest equivalent
        // phase: loaded shelf pickup withdraws before mast lowering.
        sourceRackStage=in.getIntOr("source_rack_stage",
                retrieving&&job==2?3:retrieving&&job>=3?4:0);
        destinationRackStage=in.getIntOr("destination_rack_stage",
                job==4&&!groundDestination?4:0);
        sourceArcCursor=in.getIntOr("source_arc_cursor",-1);
        destinationArcCursor=in.getIntOr("destination_arc_cursor",-1);
        sourceArcTurn=in.getIntOr("source_arc_turn",0);
        destinationArcTurn=in.getIntOr("destination_arc_turn",0);
        sourceReverseDepth=in.getDoubleOr("source_reverse_depth",0);
        sourceAisleShift=in.getDoubleOr("source_aisle_shift",0);
        destinationReverseDepth=in.getDoubleOr("destination_reverse_depth",0);
        destinationAisleShift=in.getDoubleOr("destination_aisle_shift",0);
        sourceArc=null;destinationArc=null;
        String uuid=in.getStringOr("driver_uuid","");
        try{driver=uuid.isBlank()?null:UUID.fromString(uuid);}
        catch(IllegalArgumentException e){driver=null;}
        jack=in.read("jack",ItemStack.CODEC).orElse(new ItemStack(RackContent.PALLET_JACK));
        refreshDisplay();
    }
}
