package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
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
    /** 4 blocks per second at the standard 20 server ticks/second. */
    private static final double DRIVE_SPEED=0.20D;
    private static final double DRIVE_CONTROL=1.0D;
    private ItemStack jack=ItemStack.EMPTY;
    private BlockPos source=BlockPos.ZERO,target=BlockPos.ZERO,staging=BlockPos.ZERO;
    private BlockPos parking=BlockPos.ZERO;
    private boolean retrieving,groundDestination;
    private Direction groundFacing=Direction.NORTH;
    private int job,animation,stallTicks;
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
    }
    public ItemStack displayPallet(){return entityData.get(DECK);}
    public int displayedCargoCrates(){return entityData.get(CARGO_CRATES);}
    public float mastHeight(){return entityData.get(MAST);}
    public int action(){return entityData.get(ACTION);}
    public boolean busy(){return job!=0&&job!=5;}

    /** The cab seat is behind the steering column (positive model Z),
     * about 1.3 blocks above the tires, NOT on top of the roll cage.
     * Rotate the seat position with the forklift on both client and server.
     */
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){
        if(!(passenger instanceof StorageGolem))return super.getPassengerRidingPosition(passenger);
        double radians=Math.toRadians(getYRot());
        double rear=.59;
        return new Vec3(getX()-Math.sin(radians)*rear,
                getY()+.62,getZ()+Math.cos(radians)*rear);
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
        if(busy()||!world.hasChunkAt(rack.getBlockPos()))return false;
        RackPickupPlan plan=RackPickupPlan.forklift(world,rack);
        if(plan==null||plan.height()>RackPickupPlan.FORKLIFT_MAX_HEIGHT)return false;
        boolean pickup=rack.hasPallet();
        BlockPos src=pickup?rack.getBlockPos()
                :selectedFloorPallet!=null?selectedFloorPallet
                :nearestFloorPallet(world,plan.rigCenter());
        if(!pickup&&selectedFloorPallet!=null){
            PalletBlockEntity selected=StorageGroundPalletTransfer.wrappedController(
                    world,selectedFloorPallet);
            if(selected==null||!selected.getBlockPos().equals(selectedFloorPallet)
                    ||selectedFloorPallet.distSqr(rack.getBlockPos())>24*24)
                return false;
        }
        if(src==null)return false;
        if(pickup&&!rack.glassWrapped())return false;
        this.retrieving=pickup;
        this.groundDestination=pickup;
        this.source=src.immutable();
        this.target=rack.getBlockPos().immutable();
        this.staging=pickup?floorPlacement(world,rack,plan.floorY()):src.immutable();
        if(staging==null||staging.equals(BlockPos.ZERO))return false;
        if(!hire(world))return false;
        this.job=1;
        this.animation=0;
        this.status="DRIVER BOARDING";
        return true;
    }
    public boolean orderRackToRack(ServerLevel world,RackBlockEntity from,
            RackBlockEntity to){
        if(busy()||from==null||to==null||from.getBlockPos().equals(to.getBlockPos())
                ||!from.hasPallet()||!from.glassWrapped()||to.hasPallet()||!to.isEmpty()
                ||RackPickupPlan.forklift(world,from)==null
                ||RackPickupPlan.forklift(world,to)==null)return false;
        if(!hire(world))return false;
        source=from.getBlockPos().immutable();
        target=to.getBlockPos().immutable();
        staging=BlockPos.ZERO;
        retrieving=true;groundDestination=false;
        job=1;animation=0;status="DRIVER BOARDING - RACK TO RACK";
        return true;
    }
    public boolean orderGround(ServerLevel world,BlockPos from,BlockPos landing,
            Direction facing){
        if(busy()||landing==null||from==null||!ForkliftPalletIO.fits(
                world,landing,facing))return false;
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
    /** On flat unobstructed industrial flooring the forklift must not
     * freeze merely because the vanilla mob path search rejected a 1.2-wide
     * vehicle. This only enables the direct engine when the FULL bounding box
     * and every floor tile of the straight route are clear. Normal navigation
     * remains responsible for turns, hills and obstacle avoidance. */
    private boolean directLaneClear(ServerLevel world,double x,double y,double z){
        double dx=x-getX(),dz=z-getZ();
        double distance=Math.hypot(dx,dz);
        if(distance>24||Math.abs(y-getY())>.6)return false;
        int steps=Math.max(1,(int)Math.ceil(distance/.4));
        for(int i=1;i<=steps;i++){
            double amount=(double)i/steps;
            double tx=getX()+dx*amount,tz=getZ()+dz*amount;
            AABB test=getBoundingBox().move(tx-getX(),0,tz-getZ());
            if(!world.noCollision(this,test))return false;
            BlockPos floor=BlockPos.containing(tx,y,tz).below();
            if(!world.hasChunkAt(floor)
                    ||world.getBlockState(floor).getCollisionShape(world,floor).isEmpty())
                return false;
        }
        return true;
    }

    /** Progress-based driving: a path can report 'active' even while the
     * forklift is motionless, so detect real displacement toward the target.
     * Never teleport a pallet or bypass collisions to rescue a failed route. */
    private boolean drive(ServerLevel world,BlockPos goal){
        double x=goal.getX()+.5,z=goal.getZ()+.5;
        double dx=x-getX(),dz=z-getZ(),dist=dx*dx+dz*dz;
        double arriveDistance=job==5?.90*.90:3.0;
        if(dist<arriveDistance&&Math.abs(goal.getY()-getY())<1.2){
            getNavigation().stop();
            getMoveControl().setWantedPosition(getX(),getY(),getZ(),0);
            setDeltaMovement(getDeltaMovement().multiply(.7,1,.7));
            stallTicks=0;
            return true;
        }
        if(!goal.equals(lastDriveGoal)){
            lastDriveGoal=goal.immutable();
            lastDriveDistance=Double.POSITIVE_INFINITY;
            stallTicks=0;
            getNavigation().stop();
        }
        if(dist+.04<lastDriveDistance){
            lastDriveDistance=dist;
            stallTicks=0;
        }else stallTicks++;

        // Use a normal path when one exists, then power the wheels using
        // MoveControl rather than expecting the passenger Golem to walk.
        if(tickCount%20==0||getNavigation().isDone()||stallTicks==45)
            getNavigation().moveTo(x,goal.getY(),z,DRIVE_CONTROL);
        boolean direct=(getNavigation().isDone()||stallTicks>30)
                &&directLaneClear(world,x,goal.getY(),z);
        if(direct){
            getNavigation().stop();
            getMoveControl().setWantedPosition(x,goal.getY(),z,DRIVE_CONTROL);
        }
        status=stallTicks>100&&!direct
                ?"WAITING: FORKLIFT PATH BLOCKED; CLEAR THE DRIVE AISLE"
                :"DRIVING FORKLIFT TO SERVICE AISLE";
        return false;
    }
    private BlockPos service(ServerLevel world,RackBlockEntity rack){
        RackPickupPlan plan=RackPickupPlan.forklift(world,rack);
        return plan==null?null:plan.goal(3,false);
    }
    /** A ground pallet has no rack facing. Select a free vehicle-wide
     * approach next to that pallet instead of extrapolating the destination
     * rack direction from the source location. */
    private BlockPos groundService(ServerLevel world,BlockPos pallet){
        BlockPos best=null;
        double nearest=Double.POSITIVE_INFINITY;
        for(Direction face:Direction.Plane.HORIZONTAL){
            BlockPos candidate=pallet.relative(face,3);
            if(!world.hasChunkAt(candidate)
                    ||!StorageGolemReturnPath.walkable(world,candidate))continue;
            AABB spot=getBoundingBox().move(
                    candidate.getX()+.5-getX(),candidate.getY()-getY(),
                    candidate.getZ()+.5-getZ());
            if(!world.noCollision(this,spot))continue;
            double score=candidate.distSqr(blockPosition());
            if(score<nearest){nearest=score;best=candidate;}
        }
        return best;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel world))return;
        // Travel with lowered forks. Raise them only while picking up from
        // a rack, or after arriving at the destination rack and braking.
        boolean liftingAtSource=job==2&&retrieving;
        boolean liftingAtDestination=job==3&&!groundDestination&&animation>0;
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
            }else status="RETURNING TO PAINTED FORKLIFT PARKING BAY";
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
            BlockPos pickup=retrieving?service(world,origin)
                    :groundService(world,source);
            if(pickup==null){status="WAITING FOR CLEAR AISLE";return;}
            if(!drive(world,pickup))return;
            job=2;animation=0;
        }
        if(job==2){
            if(++animation<45){status="ALIGNING FORKS AND LIFTING";return;}
            boolean ok=retrieving?origin.liftToForklift(jack):
                    ForkliftPalletIO.liftGround(world,source,jack);
            if(!ok){status="WAITING: PALLET IN USE OR ACCESS BLOCKED";animation=0;return;}
            refreshDisplay();job=3;animation=0;
        }
        if(job==3){
            BlockPos landing=groundDestination
                    ?staging.relative(groundFacing,2):service(world,rack);
            if(landing==null){status="WAITING FOR SAFE FORKLIFT LANE";return;}
            // Do not drive out of a tall source rack with the mast extended.
            // With a load on the forks it must lower to travel height first.
            if(animation==0&&entityData.get(MAST)>.20f){
                getNavigation().stop();
                status="LOWERING LOADED PALLET FOR TRAVEL";
                return;
            }
            if(animation==0&&!drive(world,landing))return;
            // Animation starts ONLY once we've reached the destination aisle.
            if(++animation<55
                    ||(!groundDestination&&Math.abs(entityData.get(MAST)-goal)>.12f)){
                status=groundDestination?"LOWERING PALLET ONTO GROUND"
                        :"RAISING PALLET TO RACK HEIGHT";
                return;
            }
            boolean placed=groundDestination?ForkliftPalletIO.placeGround(
                    world,staging,groundFacing,jack):rack.installFromForklift(jack);
            if(!placed){status="WAITING FOR SAFE DROP LOCATION";animation=20;return;}
            refreshDisplay();job=4;animation=0;
        }
        if(job==4&&++animation>=25)endTask(world);
    }
    private void endTask(ServerLevel world){
        // A driver remains mounted and on duty after a pallet delivery.
        // Only a controller click on the idle forklift dismisses them.
        job=driver!=null&&!parking.equals(BlockPos.ZERO)?5:0;
        animation=0;retrieving=false;
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
    /** Dismissal is a queued physical job, even from an otherwise idle
     * vehicle. Never leave the operator behind in a rack aisle. */
    public String requestDriverDismissal(ServerLevel world){
        if(driver==null)return "FORKLIFT: no active driver.";
        if(parking.equals(BlockPos.ZERO))
            return "FORKLIFT: no saved painted parking bay; driver remains aboard.";
        if(atParkingBay()&&job==0&&!PalletJackItem.loaded(jack))
            return dismissDriver(world)
                    ?"FORKLIFT: parked; Golem returning to its original shelf."
                    :"FORKLIFT: unable to dismiss driver.";
        dismissAfterParking=true;
        if(job==0){job=5;animation=0;getNavigation().stop();}
        return "FORKLIFT: driver will return to the painted bay, park, then go home.";
    }
    public boolean dismissDriver(ServerLevel world){
        if(job!=0||PalletJackItem.loaded(jack)||!atParkingBay())return false;
        if(driver==null)return false;
        Entity passenger=world.getEntity(driver);
        if(passenger instanceof StorageGolem golem)
            golem.finishForkliftDriving(); // walks back to original shelf
        driver=null;
        getNavigation().stop();
        status="DRIVER DISMISSED - RETURNING TO SHELF";
        return true;
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource source){
        if(level() instanceof ServerLevel world){
            // One packed pallet must survive the vehicle's destruction.
            if(PalletJackItem.loaded(jack)){
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
        String uuid=in.getStringOr("driver_uuid","");
        try{driver=uuid.isBlank()?null:UUID.fromString(uuid);}
        catch(IllegalArgumentException e){driver=null;}
        jack=in.read("jack",ItemStack.CODEC).orElse(new ItemStack(RackContent.PALLET_JACK));
        refreshDisplay();
    }
}
