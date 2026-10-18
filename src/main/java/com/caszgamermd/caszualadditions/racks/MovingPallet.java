package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import java.util.List;
import java.util.ArrayDeque;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * ONE physical moving pallet and FOUR attached, animated golem models.
 * Only this entity saves or moves the cargo. No separate golem entities
 * navigate while lifting or carrying; all four corner walkers use this
 * entity's exact position and leg phase.
 */
public final class MovingPallet extends PathfinderMob {
    private static final EntityDataAccessor<ItemStack> DISPLAY_PALLET=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DISPLAY_CARGO=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    /** True ONLY when the Golems physically have a deck in their hands.
     * This is independent of owning an undispatched replacement pallet
     * and remains correct on both clients after chunk/reload transitions. */
    private static final EntityDataAccessor<Boolean> HOLDING_DECK=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> LIFT_TICKS=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELEASE_TICKS=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    // Only the pallet/artwork translates into the rack during the reverse
    // extraction animation. All four robot corner models stay rigid.
    private static final EntityDataAccessor<Boolean> RACK_INSERT_ACTIVE=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> RACK_INSERT_X=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RACK_INSERT_Y=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> RACK_INSERT_Z=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.FLOAT);
    private String owner="";
    private final BlockPos[] shelves={BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO};
    private ItemStack pallet=ItemStack.EMPTY;
    private NonNullList<ItemStack> contents=NonNullList.withSize(216,ItemStack.EMPTY);
    private boolean delivered,deathDropsReleased,recallAfterDelivery;
    private int liftTicks;
    private int deliveryMode,releaseTicks;
    private BlockPos deliveryPosition=BlockPos.ZERO;
    // Cached service aisle for BOTH ground and PTP drop-offs. Never navigate
    // the wide cargo entity into the 2x2 load or 3x3 PTP floor itself.
    private BlockPos deliveryService=BlockPos.ZERO;
    private BlockPos remoteWarehouse=BlockPos.ZERO;
    private UUID remoteCourierId;
    private boolean remoteReplacementPlaced,remoteOldLegacy;
    private Direction remoteOldFacing=Direction.NORTH;
    private BlockPos remoteAisle=BlockPos.ZERO,remoteMoveAside=BlockPos.ZERO;
    private static final EntityDataAccessor<Integer> CREW_SIZE=SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    private Direction deliveryFacing=Direction.NORTH;
    private String deliveryFailure="";
    // A nearly two-block-wide mob can stop pathing before reaching a
    // precise point. Align locally after navigation and detect real stalls.
    private int deliveryStallTicks;
    private double lastDeliveryDistance=Double.POSITIVE_INFINITY;
    // Detect an obstructed or abandoned path instead of waiting forever on
    // the same unreachable target behind the player.
    private double lastFollowDistance=Double.POSITIVE_INFINITY;
    private int lastFollowProgressTick,obstacleSide;
    // The one physical load follows a checked footprint-wide path, not
    // unrestricted vanilla ground navigation that climbs nearby crates.
    private final ArrayDeque<BlockPos> floorFollowRoute=new ArrayDeque<>();
    private BlockPos lastFloorPlayer=BlockPos.ZERO;
    private int nextFloorFollowPlan;
    private BlockPos obstructedFloorCell=BlockPos.ZERO;
    private int avoidFloorCellUntil;
    private final ArrayDeque<BlockPos> rackFrontRoute=new ArrayDeque<>();
    private BlockPos lastRackFront=BlockPos.ZERO;
    private int nextRackFrontPlan,lastRackProgressTick;
    private double lastRackWaypointDistance=Double.POSITIVE_INFINITY;
    // The order and the route are different: these ephemeral waypoints may
    // be thrown away at any time without discarding deliveryMode/position.
    private final ArrayDeque<BlockPos> remoteFloorRoute=new ArrayDeque<>();
    private BlockPos remoteRouteGoal=BlockPos.ZERO,blockedRemoteCell=BlockPos.ZERO;
    private int nextRemotePlan,remoteAvoidUntil,remoteProgressTick;
    private double lastRemoteProgress=Double.POSITIVE_INFINITY;
    private int nextRemoteTerrainJump;

    public MovingPallet(EntityType<? extends MovingPallet> type,Level level){
        super(type,level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,40)
                .add(Attributes.MOVEMENT_SPEED,.144)
                .add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(DISPLAY_PALLET,ItemStack.EMPTY);
        builder.define(DISPLAY_CARGO,0);
        builder.define(HOLDING_DECK,false);
        builder.define(CREW_SIZE,4);
        builder.define(LIFT_TICKS,0);
        builder.define(RELEASE_TICKS,0);
        builder.define(RACK_INSERT_ACTIVE,false);
        builder.define(RACK_INSERT_X,0.0f);
        builder.define(RACK_INSERT_Y,0.0f);
        builder.define(RACK_INSERT_Z,0.0f);
    }
    public ItemStack displayPallet(){return entityData.get(DISPLAY_PALLET);}
    public int visibleCargo(){return entityData.get(DISPLAY_CARGO);}
    public boolean handsActuallyCarryingPallet(){return entityData.get(HOLDING_DECK);}
    public int displayedCrewSize(){return entityData.get(CREW_SIZE);}
    public float liftProgress(){return entityData.get(LIFT_TICKS)/12.0f;}
    public float releaseProgress(){return entityData.get(RELEASE_TICKS)/20.0f;}
    public boolean rackInserting(){return entityData.get(RACK_INSERT_ACTIVE);}
    public float rackInsertProgress(){
        return rackInserting()
                ?Math.clamp(entityData.get(RELEASE_TICKS)/40.0f,0,1):0;
    }
    public float rackInsertX(){return entityData.get(RACK_INSERT_X);}
    public float rackInsertY(){return entityData.get(RACK_INSERT_Y);}
    public float rackInsertZ(){return entityData.get(RACK_INSERT_Z);}
    public boolean isDelivering(){return deliveryMode!=0;}
    public boolean returningToWarehouse(){return deliveryMode==3;}
    public boolean remoteExchangeActive(){return deliveryMode>=4&&deliveryMode<=6;}
    public boolean freshPalletOnly(){
        if(pallet.isEmpty())return false;
        for(ItemStack content:contents)if(!content.isEmpty())return false;
        return true;
    }
    /** One real Golem takes exclusive ownership of the empty replacement. */
    public boolean splitRemoteCourier(StorageGolem worker){
        if(deliveryMode!=4||worker==null||!freshPalletOnly()||remoteCourierId!=null)return false;
        if(!worker.beginRemoteCourier(getUUID(),deliveryPosition,pallet.copyWithCount(1)))
            return false;
        remoteCourierId=worker.getUUID();
        pallet=ItemStack.EMPTY;syncDisplay();
        entityData.set(CREW_SIZE,3);
        deliveryFailure="THREE GOLEMS TO TARGET; ONE PUSHING EMPTY PALLET";
        return true;
    }
    public boolean acceptsRemoteLift(BlockPos target){
        return deliveryMode==4&&remoteCourierId!=null
                &&target.equals(deliveryPosition)&&pallet.isEmpty();
    }
    public boolean acceptRemoteLift(BlockPos target,BlockState oldState,
            ItemStack wrapped,NonNullList<ItemStack> recovered){
        if(!acceptsRemoteLift(target)||wrapped.isEmpty()||recovered.size()!=216)return false;
        pallet=wrapped.copyWithCount(1);
        for(int i=0;i<216;i++)contents.set(i,recovered.get(i).copy());
        remoteOldFacing=oldState.getValue(PalletBlock.FACING);
        remoteOldLegacy=oldState.getValue(PalletBlock.LEGACY);
        remoteMoveAside=BlockPos.ZERO;
        deliveryMode=5;remoteAisle=BlockPos.ZERO;
        // The OUTBOUND three-worker party travelled empty-handed. Only
        // NOW animate raising the REAL target pallet from the ground.
        liftTicks=0;
        entityData.set(LIFT_TICKS,0);
        syncDisplay();
        deliveryFailure="THREE GOLEMS LIFTED TARGET; MOVING ASIDE";
        return true;
    }
    /** The recovered pallet cannot occupy its former 2x2 ground cells.
     * Compare REAL footprint geometry, not a guessed root+1 midpoint:
     * EAST/WEST/NORTH/SOUTH orientations have different signed cells.
     * Existing loaded carriers are allowed to hand off as soon as their
     * entire collision box is clear, without waiting for a remote 7-block
     * staging waypoint that the AI cannot always reach. */
    public boolean remoteFootprintClear(){
        if(!loaded()||deliveryMode<5||deliveryMode>6)return false;
        if(!(pallet.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return false;
        BlockState state=block.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,remoteOldFacing)
                .setValue(PalletBlock.LEGACY,remoteOldLegacy);
        int minX=Integer.MAX_VALUE,maxX=Integer.MIN_VALUE;
        int minZ=Integer.MAX_VALUE,maxZ=Integer.MIN_VALUE;
        for(int i=0;i<4;i++){
            BlockPos cell=PalletBlock.partPos(deliveryPosition,i,state);
            minX=Math.min(minX,cell.getX());maxX=Math.max(maxX,cell.getX());
            minZ=Math.min(minZ,cell.getZ());maxZ=Math.max(maxZ,cell.getZ());
        }
        // A modest clearance beyond the pallet walls leaves room for
        // placement animation without routing the three bots too far away.
        AABB footprint=new AABB(minX-.5,deliveryPosition.getY()-.5,
                minZ-.5,maxX+1.5,deliveryPosition.getY()+2.6,maxZ+1.5);
        return !getBoundingBox().intersects(footprint);
    }
    public boolean readyForCourierPlacement(){
        return deliveryMode==6&&!remoteReplacementPlaced&&remoteFootprintClear();
    }
    public Direction remoteOldFacing(){return remoteOldFacing;}
    public boolean remoteOldLegacy(){return remoteOldLegacy;}
    public boolean confirmCourierPlacement(UUID courier){
        if(!readyForCourierPlacement()||!courier.equals(remoteCourierId))return false;
        remoteReplacementPlaced=true;
        deliveryFailure="NEW PALLET PLACED; WAITING FOR FOURTH GOLEM";
        return true;
    }
    public boolean joinRemoteCourier(UUID courier){
        if(deliveryMode!=6||!remoteReplacementPlaced
                ||!courier.equals(remoteCourierId))return false;
        remoteCourierId=null;
        entityData.set(CREW_SIZE,4);
        deliveryMode=3;
        recallAfterDelivery=true;
        deliveryPosition=remoteWarehouse;
        remoteFloorRoute.clear();nextRemotePlan=0;
        deliveryFailure="FOUR GOLEMS RETURNING RECOVERED PALLET";
        return true;
    }
    /** The 3/4 shared rig is created ONLY at the moment of lifting the
     * original wrapped pallet. Before this call the three identities exist
     * exclusively as three independent, individually walking Golems. */
    public boolean prepareRecoveredRemote(UUID player,BlockPos[] homes,
            UUID courierId,BlockPos target,BlockPos computer){
        if(player==null||courierId==null||homes.length!=4
                ||!pallet.isEmpty()||deliveryMode!=0)return false;
        owner=player.toString();
        for(int i=0;i<4;i++)shelves[i]=homes[i].immutable();
        remoteCourierId=courierId;
        remoteWarehouse=computer.immutable();
        deliveryPosition=target.immutable();
        deliveryMode=4;
        entityData.set(CREW_SIZE,3);
        liftTicks=0;entityData.set(LIFT_TICKS,0);
        syncDisplay();
        return true;
    }
    public boolean beginRemoteExchange(BlockPos target,BlockPos computer){
        if(!freshPalletOnly()||deliveryMode!=0)return false;
        remoteWarehouse=computer.immutable();
        deliveryPosition=target.immutable();
        deliveryMode=4;
        // An in-flight pre-dispatch replacement pallet must NEVER appear
        // in any of the three pickup Golems' raised hands.
        syncDisplay();
        deliveryFailure="DISPATCHING THREE GOLEMS PLUS ONE PALLET COURIER";
        return true;
    }
    public String deliveryStatus(){return deliveryFailure;}
    /** A durable order; the crew retains its cargo until it reaches home. */
    public String returnToWarehouse(ServerLevel level,BlockPos computer){
        // A RECALL ALL must not abandon the three-person carrier while the
        // fourth worker still owns the only replacement pallet. Completing
        // the swap already schedules a physical warehouse return.
        if(remoteCourierId!=null&&deliveryMode>=4&&deliveryMode<=6)
            return "Remote exchange still in progress; workers return automatically after the fourth Golem rejoins.";
        if(!loaded())return "The crew is not carrying a pallet.";
        if(level.hasChunkAt(computer)
                &&!level.getBlockState(computer).is(RackContent.RACK_COMPUTER))
            return "Linked Rack Computer no longer exists.";
        deliveryPosition=computer.immutable();
        deliveryMode=3;
        recallAfterDelivery=true;
        releaseTicks=0;
        entityData.set(RELEASE_TICKS,0);
        entityData.set(RACK_INSERT_ACTIVE,false);
        deliveryFailure="RETURNING PALLET TO WAREHOUSE";
        getNavigation().stop();
        return "Return assigned: INPUT PTP first, vacant PTP second, then free rack.";
    }
    public boolean owns(UUID player){return owner.equals(player.toString());}
    public boolean loaded(){return !pallet.isEmpty();}
    public boolean fromWarehouse(List<StorageGolemShelfEntity> group){
        for(BlockPos shelf:shelves){
            boolean found=false;
            for(StorageGolemShelfEntity member:group)
                if(member.getBlockPos().equals(shelf)){found=true;break;}
            if(!found)return false;
        }
        return true;
    }
    /** Populate data BEFORE spawning. The previous lead worker still
     * retains ownership until spawning is accepted by the server. */
    public boolean prepare(UUID player,List<StorageGolem> workers,
            ItemStack deck,NonNullList<ItemStack> items){
        if(!pallet.isEmpty()||deck.isEmpty()||items.size()!=216||workers.size()!=4)
            return false;
        boolean[] index=new boolean[4];
        for(StorageGolem golem:workers){
            int i=golem.crewIndex();
            if(i<0||i>=4||index[i]||!golem.ownedBy(player))return false;
            index[i]=true;
            shelves[i]=golem.shelfHome();
        }
        owner=player.toString();
        pallet=deck.copyWithCount(1);
        for(int i=0;i<216;i++)contents.set(i,items.get(i).copy());
        syncDisplay();
        return true;
    }
    private void syncDisplay(){
        boolean holding=!delivered&&!pallet.isEmpty()&&deliveryMode!=4;
        entityData.set(HOLDING_DECK,holding);
        entityData.set(DISPLAY_PALLET,holding?pallet.copy():ItemStack.EMPTY);
        int used=0;
        for(ItemStack item:contents)if(!item.isEmpty())used++;
        entityData.set(DISPLAY_CARGO,Math.min(27,used));
    }
    private boolean allFit(Container dest){
        for(int i=0;i<216;i++)
            if(!contents.get(i).isEmpty()&&!dest.canPlaceItem(i,contents.get(i)))
                return false;
        return true;
    }
    private boolean verify(Container dest){
        for(int i=0;i<216;i++){
            ItemStack expected=contents.get(i),real=dest.getItem(i);
            if(expected.isEmpty()!=real.isEmpty())return false;
            if(!expected.isEmpty()&&(!ItemStack.isSameItemSameComponents(expected,real)
                    ||expected.getCount()!=real.getCount()))return false;
        }
        return true;
    }
    private void clearCargo(){
        pallet=ItemStack.EMPTY;
        contents=NonNullList.withSize(216,ItemStack.EMPTY);
        delivered=true;
        // Once placed this carrier no longer represents physical cargo.
        // Keep it from colliding with the just-placed blocks while it
        // creates four safe individual workers on the next tick.
        setNoGravity(true);
        noPhysics=true;
        syncDisplay();
    }
    /** A scanner sets a destination; it NEVER places the pallet instantly. */
    public String requestRackDelivery(ServerLevel level,RackBlockEntity rack){
        if(!loaded()||delivered)return "No pallet ready to deliver.";
        // A new scanner click can retarget a blocked delivery.
        if(rack.hasPallet()||!rack.isEmpty())return "Destination rack must be empty.";
        if(RackPickupPlan.find(level,rack)==null)
            return "Rack needs a clear 2-wide, 3-deep access aisle with floor support.";
        deliveryPosition=rack.getBlockPos().immutable();
        deliveryFacing=rack.facing();
        deliveryMode=1;
        deliveryService=BlockPos.ZERO;
        rackFrontRoute.clear();
        lastRackFront=BlockPos.ZERO;
        nextRackFrontPlan=0;
        entityData.set(RACK_INSERT_ACTIVE,true);
        releaseTicks=0;
        deliveryFailure="";
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        entityData.set(RELEASE_TICKS,0);
        return "Golems carrying pallet to rack "+deliveryPosition.toShortString()
                +"; it will be unloaded only after they arrive.";
    }
    public String requestGroundDelivery(ServerLevel level,BlockPos location,Direction facing){
        if(!loaded()||delivered)return "No pallet ready to deliver.";
        // A new scanner click can retarget a blocked delivery.
        if(!groundClear(level,location,facing))
            return "Need a clear supported 2x2 destination floor.";
        deliveryPosition=location.immutable();
        deliveryFacing=facing;
        deliveryMode=2;
        deliveryService=BlockPos.ZERO;
        remoteFloorRoute.clear();
        rackFrontRoute.clear();
        nextRackFrontPlan=0;
        entityData.set(RACK_INSERT_ACTIVE,false);
        releaseTicks=0;
        deliveryFailure="";
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        entityData.set(RELEASE_TICKS,0);
        return "Golems carrying pallet to "+location.toShortString()
                +"; they will release it only after positioning.";
    }
    /** Treat a PTP like a rack, with an EXTERNAL three-wide service aisle.
     * The clicked location is the installed 3x3 station controller, never a
     * target tile for a 1.85-wide moving pallet rig to stand ON. */
    public String requestPtpDelivery(ServerLevel level,PtpBlockEntity point){
        if(!loaded()||delivered)return "No pallet ready to deliver.";
        if(!point.canInstallPallet())
            return "Destination PTP needs to be empty, not reserved and have clear space above it.";
        BlockPos center=point.getBlockPos();
        BlockPos service=serviceAisle(level,center,3,5);
        if(service.equals(BlockPos.ZERO))
            return "PTP needs a clear three-wide ground aisle outside its 3x3 platform.";
        deliveryPosition=center.immutable();
        deliveryService=service;
        deliveryMode=7;
        remoteFloorRoute.clear();
        nextRemotePlan=0;
        entityData.set(RACK_INSERT_ACTIVE,false);
        releaseTicks=0;entityData.set(RELEASE_TICKS,0);
        deliveryFailure="TRAVELLING TO PTP UNLOADING AISLE";
        return "Golems delivering pallet to PTP "+center.toShortString()
                +" from its clear access aisle. Cargo stays aboard until arrival.";
    }

    /** Identical rack/PTP service semantics: safe 3x3 support without
     * collision and Y-level preference, but never step into the pallet. */
    private BlockPos serviceAisle(ServerLevel level,BlockPos station,int min,int max){
        BlockPos best=BlockPos.ZERO;
        double nearest=Double.POSITIVE_INFINITY;
        for(Direction side:Direction.Plane.HORIZONTAL){
            for(int length=min;length<=max;length++){
                BlockPos trial=station.relative(side,length);
                if(!safePlatformFloor(level,trial))continue;
                double distance=trial.distSqr(blockPosition());
                if(distance<nearest){nearest=distance;best=trial;}
            }
        }
        return best;
    }
    /** Ground pallets are 2x2 and may rotate with the player. Find an aisle
     * outside the REAL oriented footprint, instead of targeting its center,
     * which previously made the carrier freeze trying to stand inside cargo. */
    private BlockPos groundDeliveryAisle(ServerLevel level,BlockPos root,Direction facing){
        if(!(pallet.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return BlockPos.ZERO;
        BlockState model=block.defaultBlockState()
                .setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing)
                .setValue(PalletBlock.LEGACY,false);
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(int part=0;part<4;part++){
            BlockPos cell=PalletBlock.partPos(root,part,model);
            minX=Math.min(minX,cell.getX());maxX=Math.max(maxX,cell.getX());
            minZ=Math.min(minZ,cell.getZ());maxZ=Math.max(maxZ,cell.getZ());
        }
        BlockPos best=BlockPos.ZERO;
        double nearest=Double.POSITIVE_INFINITY;
        for(int step=2;step<=4;step++){
            BlockPos[] candidates={
                    new BlockPos(minX,root.getY(),minZ-step),
                    new BlockPos(maxX+step,root.getY(),minZ),
                    new BlockPos(minX,root.getY(),maxZ+step),
                    new BlockPos(minX-step,root.getY(),minZ)
            };
            for(BlockPos corner:candidates){
                // The rig's waypoint is centered on its own 3x3 walkable
                // corridor and must not intersect the delivered pallet.
                if(!safePlatformFloor(level,corner))continue;
                double dist=corner.distSqr(blockPosition());
                if(dist<nearest){nearest=dist;best=corner;}
            }
        }
        return best;
    }
    private void tickAisleDelivery(ServerLevel level){
        BlockPos target=deliveryPosition;
        PtpBlockEntity point=deliveryMode==7
                &&level.getBlockEntity(target) instanceof PtpBlockEntity p?p:null;
        if(deliveryMode==7){
            if(point==null||!point.canInstallPallet()){
                waitForDelivery("WAITING: PTP OCCUPIED OR RESERVED; DELIVERY SAVED");return;
            }
        }else if(!groundClear(level,target,deliveryFacing)){
            waitForDelivery("WAITING: GROUND 2x2 FOOTPRINT BLOCKED; DELIVERY SAVED");
            return;
        }
        if(deliveryService.equals(BlockPos.ZERO)
                ||!safePlatformFloor(level,deliveryService)){
            deliveryService=deliveryMode==7?serviceAisle(level,target,3,5)
                    :groundDeliveryAisle(level,target,deliveryFacing);
            remoteFloorRoute.clear();nextRemotePlan=0;
        }
        if(deliveryService.equals(BlockPos.ZERO)){
            waitForDelivery("WAITING: NO ACCESSIBLE 3-WIDE UNLOADING AISLE; DELIVERY SAVED");
            return;
        }
        if(!nearRemoteGoal(deliveryService)){
            if(releaseTicks!=0){releaseTicks=0;entityData.set(RELEASE_TICKS,0);}
            navigateRemoteFloor(level,deliveryService);
            // A failed waypoint only retries the route; the original
            // destination and all 216 slots remain on this loaded rig.
            if(!deliveryFailure.startsWith("WAITING:")
                    &&!deliveryFailure.startsWith("REROUTING"))
                deliveryFailure="MOVING TO "+(deliveryMode==7?"PTP":"GROUND")
                        +" SERVICE AISLE";
            return;
        }
        getNavigation().stop();
        deliveryFailure="PLACING PALLET AT "+(deliveryMode==7?"PTP":"GROUND");
        if(releaseTicks<20){
            entityData.set(RELEASE_TICKS,++releaseTicks);
            return;
        }
        boolean placed=deliveryMode==7
                ?deliverToPtp(point)
                :deliverToGround(level,target,deliveryFacing);
        if(!placed){
            waitForDelivery("WAITING: DESTINATION REJECTED PALLET; ORDER SAVED");
            return;
        }
        deliveryMode=0;
        deliveryService=BlockPos.ZERO;
        deliveryFailure="";
        entityData.set(RACK_INSERT_ACTIVE,false);
        restoreWorkers(level);
    }
    /** Transfers the same pallet and exact 216 stacks to a vacant PTP as
     * a rack would. Only clear the rig AFTER item-by-item verification;
     * failures roll back the station, preserving the moving shipment. */
    public boolean deliverToPtp(PtpBlockEntity point){
        if(!loaded()||point==null||!point.canInstallPallet())return false;
        if(!point.installPallet(pallet.copyWithCount(1)))return false;
        boolean valid=allFit(point);
        if(valid){
            for(int i=0;i<216;i++)
                if(!contents.get(i).isEmpty())point.setItem(i,contents.get(i).copy());
            valid=verify(point);
        }
        if(!valid){
            point.clearContent();
            point.dropPallet();
            return false;
        }
        point.setSealed(com.caszgamermd.caszualadditions.pallet.GlassPalletWrap
                .wrapped(pallet));
        clearCargo();
        return true;
    }

    private boolean groundClear(ServerLevel level,BlockPos pos,Direction facing){
        if(!(pallet.getItem() instanceof BlockItem bi)
                ||!(bi.getBlock() instanceof PalletBlock block))return false;
        BlockState state=block.defaultBlockState().setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing).setValue(PalletBlock.LEGACY,false);
        for(int i=0;i<4;i++){
            BlockPos cell=PalletBlock.partPos(pos,i,state);
            if(level.isOutsideBuildHeight(cell)||!level.hasChunkAt(cell)
                    ||!level.getWorldBorder().isWithinBounds(cell)
                    ||!level.getBlockState(cell).canBeReplaced()
                    ||!StorageGolemReturnPath.walkable(level,cell))return false;
        }
        return true;
    }
    private void abortDelivery(String problem){
        deliveryFailure=problem;
        deliveryMode=0;
        entityData.set(RACK_INSERT_ACTIVE,false);
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        releaseTicks=0;
        entityData.set(RELEASE_TICKS,0);
        getNavigation().stop();
    }
    /** Path obstruction is a WAIT state, never a task cancellation. */
    private void waitForDelivery(String reason){
        deliveryFailure=reason;
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        releaseTicks=0;
        entityData.set(RELEASE_TICKS,0);
        rackFrontRoute.clear();
        nextRackFrontPlan=tickCount+20;
        getNavigation().stop();
    }
    /**
     * Drive the single loaded entity to the actual two-column FRONT aisle,
     * never to a shorter path against the rack's back or side wall.
     * A route failure does not discard the persisted scanner assignment.
     */
    private void navigateToRackFront(ServerLevel level,BlockPos goal,
            double exactX,double exactZ){
        BlockPos current=blockPosition();
        if(current.getY()!=goal.getY()){
            if(tickCount%12==0)
                getNavigation().moveTo(exactX,goal.getY(),exactZ,1.0);
            deliveryFailure="MOVING TO RACK-FRONT FLOOR";
            return;
        }
        if(!goal.equals(lastRackFront)){
            lastRackFront=goal.immutable();
            rackFrontRoute.clear();
            nextRackFrontPlan=0;
        }
        while(!rackFrontRoute.isEmpty()){
            BlockPos step=rackFrontRoute.peekFirst();
            if(!safePlatformFloor(level,step)){
                rackFrontRoute.clear();
                nextRackFrontPlan=0;
                break;
            }
            double dx=step.getX()+.5-getX(),dz=step.getZ()+.5-getZ();
            if(dx*dx+dz*dz>=.28)break;
            rackFrontRoute.removeFirst();
            lastRackWaypointDistance=Double.POSITIVE_INFINITY;
            lastRackProgressTick=tickCount;
        }
        if(rackFrontRoute.isEmpty()&&tickCount>=nextRackFrontPlan){
            List<BlockPos> steps=MovingPalletFloorPath.toRackFront(
                    current,goal,p->safePlatformFloor(level,p));
            nextRackFrontPlan=tickCount+20;
            if(steps==null){
                deliveryFailure="WAITING: NO CLEAR RACK FRONT ROUTE";
                getNavigation().stop();
                return;
            }
            rackFrontRoute.addAll(steps);
            lastRackWaypointDistance=Double.POSITIVE_INFINITY;
            lastRackProgressTick=tickCount;
        }
        if(rackFrontRoute.isEmpty())return;
        BlockPos next=rackFrontRoute.peekFirst();
        double dx=next.getX()+.5-getX(),dz=next.getZ()+.5-getZ();
        double distance=dx*dx+dz*dz;
        if(distance+.03<lastRackWaypointDistance){
            lastRackProgressTick=tickCount;
            lastRackWaypointDistance=distance;
        }else if(tickCount-lastRackProgressTick>100){
            rackFrontRoute.clear();
            nextRackFrontPlan=tickCount+20;
            lastRackWaypointDistance=Double.POSITIVE_INFINITY;
            deliveryFailure="WAITING: RACK-FRONT PATH BLOCKED, RETRYING";
            getNavigation().stop();
            return;
        }
        deliveryFailure="MOVING TO RACK FRONT";
        getNavigation().stop();
        getMoveControl().setWantedPosition(next.getX()+.5,
                WarehouseTerrain.standingY(level,next),next.getZ()+.5,1.0);
    }
    /**
     * Retain the same wrapped pallet and all 216 slots while the four-Golem
     * carrier returns. Prefer a vacant INPUT PTP, then any vacant PTP,
     * and only then a front-accessible rack slot. Unwrap AFTER successful
     * PTP commit. A blocked destination is a persistent WAIT, not deletion.
     */
    /** Maintain the same durable warehouse task while incrementally routing.
     * Unlike repeatedly targeting a point in a straight line with vanilla
     * pathfinding, every step was checked for the FULL 3x3 cargo corridor.
     * Routes use flat Y first and stairs only as a genuine fallback. */
    private boolean navigateRemoteFloor(ServerLevel level,BlockPos target){
        BlockPos here=blockPosition();
        if(!target.equals(remoteRouteGoal)){
            remoteRouteGoal=target.immutable();
            remoteFloorRoute.clear();
            nextRemotePlan=0;
            lastRemoteProgress=Double.POSITIVE_INFINITY;
            remoteProgressTick=tickCount;
        }
        while(!remoteFloorRoute.isEmpty()){
            BlockPos next=remoteFloorRoute.peekFirst();
            if(!safePlatformFloor(level,next)){
                remoteFloorRoute.clear();
                nextRemotePlan=tickCount+15;
                break;
            }
            double dx=next.getX()+.5-getX(),dz=next.getZ()+.5-getZ();
            // Allow a 1.85-wide rig to finish a waypoint without trying
            // to pin its center to within half a block indefinitely.
            if(dx*dx+dz*dz>.72
                    ||Math.abs(WarehouseTerrain.standingY(level,next)-getY())>.80)break;
            remoteFloorRoute.removeFirst();
            lastRemoteProgress=Double.POSITIVE_INFINITY;
            remoteProgressTick=tickCount;
        }
        if(remoteFloorRoute.isEmpty()&&tickCount>=nextRemotePlan){
            var path=WarehousePreferredPath.leg(here,target,
                    p->safePlatformFloor(level,p)
                            &&(tickCount>=remoteAvoidUntil
                                ||!p.equals(blockedRemoteCell)),
                    BlockPos.ZERO,p->WarehouseTerrain.standingY(level,p));
            nextRemotePlan=tickCount+30;
            if(path==null){
                deliveryFailure="WAITING: SAFE ROUTE BLOCKED, RETRYING";
                getNavigation().stop();
                return false;
            }
            remoteFloorRoute.addAll(path);
            lastRemoteProgress=Double.POSITIVE_INFINITY;
            remoteProgressTick=tickCount;
        }
        if(remoteFloorRoute.isEmpty())return false;
        BlockPos next=remoteFloorRoute.peekFirst();
        double dx=next.getX()+.5-getX(),dz=next.getZ()+.5-getZ();
        double distance=dx*dx+dz*dz;
        if(distance+.05<lastRemoteProgress){
            lastRemoteProgress=distance;
            remoteProgressTick=tickCount;
        }else if(tickCount-remoteProgressTick>60){
            // Three seconds of no physical progress: mark the bad waypoint,
            // take a different aisle and KEEP the original pickup/order.
            blockedRemoteCell=next.immutable();
            remoteAvoidUntil=tickCount+160;
            remoteFloorRoute.clear();
            nextRemotePlan=tickCount+10;
            lastRemoteProgress=Double.POSITIVE_INFINITY;
            remoteProgressTick=tickCount;
            deliveryFailure="REROUTING AROUND BLOCKED AISLE";
            getNavigation().stop();
            return false;
        }
        // The loaded pallet follows the SAME checked three-wide terrain
        // waypoints. Re-asking vanilla A* for a different path on a single
        // rise caused the entire carrying team to turn in place. Rise by
        // at most one block and descend under normal collision and gravity.
        int rise=next.getY()-here.getY();
        if(rise>1||rise< -1){
            deliveryFailure="WAITING: UNSAFE PALLET HEIGHT CHANGE";
            getNavigation().stop();
            return false;
        }
        getNavigation().stop();
        double waypointY=WarehouseTerrain.standingY(level,next);
        double riseHeight=waypointY-getY();
        getMoveControl().setWantedPosition(next.getX()+.5,
                waypointY,next.getZ()+.5,
                Math.abs(riseHeight)<=.501?1.0:.85);
        if(rise==1&&riseHeight>.60&&onGround()&&tickCount>=nextRemoteTerrainJump
                &&distance<2.8){
            getJumpControl().jump();
            nextRemoteTerrainJump=tickCount+10;
        }
        return true;
    }

    private boolean nearRemoteGoal(BlockPos target){
        double dx=target.getX()+.5-getX(),dz=target.getZ()+.5-getZ();
        return dx*dx+dz*dz<1.8&&Math.abs(target.getY()-getY())<.9;
    }
    private BlockPos remoteAisle(ServerLevel level,BlockPos target,int min,int max){
        BlockPos choice=BlockPos.ZERO;
        double closest=Double.POSITIVE_INFINITY;
        for(Direction side:Direction.Plane.HORIZONTAL)for(int n=min;n<=max;n++){
            BlockPos trial=target.relative(side,n);
            if(!safePlatformFloor(level,trial))continue;
            double distance=trial.distSqr(blockPosition());
            if(distance<closest){closest=distance;choice=trial;}
        }
        return choice;
    }
    /** Upgrade live saved four-person remote jobs from the previous JAR.
     * They still own one fresh empty pallet, so the fourth real courier can
     * be split off without requesting or consuming warehouse stock again. */
    private boolean ensureRemoteCourier(ServerLevel level){
        if(remoteCourierId!=null)return true;
        if(!freshPalletOnly()){
            deliveryFailure="WAITING: LOST REMOTE COURIER REFERENCE";
            return false;
        }
        UUID id;
        try{id=UUID.fromString(owner);}
        catch(IllegalArgumentException ex){return false;}
        StorageGolem courier=RackContent.STORAGE_GOLEM_TYPE.create(level,
                EntitySpawnReason.EVENT);
        if(courier==null)return false;
        courier.assignPlayerCrew(shelves[3],3,id);
        courier.setPos(getX()+1.4,getY(),getZ()+.3);
        ItemStack saved=pallet.copy();
        if(!splitRemoteCourier(courier))return false;
        if(level.addFreshEntity(courier))return true;
        // Failed spawn: remain an intact old-style assigned carrier. Never
        // lose or duplicate the empty replacement on a failed migration.
        remoteCourierId=null;
        pallet=saved;
        syncDisplay();
        entityData.set(CREW_SIZE,4);
        deliveryFailure="WAITING: RETRY COURIER SPAWN";
        return false;
    }

    private void tickRemoteExchange(ServerLevel level){
        if(!ensureRemoteCourier(level))return;
        // Old saves may contain a single rigid 3-model carrier already
        // walking OUTBOUND. Convert it to three real unlinked Golems on its
        // current floor. The rig is only valid AFTER lifting the pallet.
        if(pallet.isEmpty()&&remoteCourierId!=null){
            try{
                RemotePalletPickupCrew.splitLegacyRig(level,this,
                        UUID.fromString(owner),shelves.clone(),remoteCourierId,
                        deliveryPosition,remoteWarehouse);
            }catch(IllegalArgumentException ignored){
                deliveryFailure="WAITING: LEGACY REMOTE CREW OWNER INVALID";
            }
            return;
        }
        BlockPos target=deliveryPosition;
        if(StorageGroundPalletTransfer.wrappedController(level,target)==null){
            deliveryFailure="WAITING FOR WRAPPED TARGET PALLET";return;
        }
        // Fix the aisle target for the ENTIRE approach; changing to the
        // nearest side every tick previously made the team turn indefinitely.
        if(remoteAisle.equals(BlockPos.ZERO)
                ||!safePlatformFloor(level,remoteAisle))
            remoteAisle=remoteAisle(level,target,3,5);
        if(remoteAisle.equals(BlockPos.ZERO)){
            deliveryFailure="WAITING FOR CLEAR THREE-WIDE APPROACH";return;
        }
        if(!nearRemoteGoal(remoteAisle)){
            deliveryFailure="THREE GOLEMS APPROACHING WRAPPED TARGET";
            navigateRemoteFloor(level,remoteAisle);return;
        }
        getNavigation().stop();
        if(!StorageGroundPalletTransfer.liftForRemoteExchange(level,target,this))
            deliveryFailure="WAITING FOR TARGET PALLET LIFT";
    }
    private void tickRemoteStepAside(ServerLevel level){
        // Release the courier AS SOON AS the old footprint is physically
        // unobstructed. Previously the loaded three-person rig froze at a
        // cached 5-7 block waypoint despite already making room.
        if(remoteFootprintClear()){
            getNavigation().stop();
            remoteFloorRoute.clear();
            deliveryMode=6;
            deliveryFailure="OLD PALLET CLEAR; COURIER MAY PLACE REPLACEMENT";
            return;
        }
        if(remoteMoveAside.equals(BlockPos.ZERO)
                ||!safePlatformFloor(level,remoteMoveAside)){
            // No need to travel seven blocks from the arbitrary ROOT.
            // Select the closest wide, level ground that clears the
            // actual old footprint and frees the courier to place.
            remoteMoveAside=remoteAisle(level,deliveryPosition,4,7);
            remoteFloorRoute.clear();nextRemotePlan=0;
        }
        if(remoteMoveAside.equals(BlockPos.ZERO)){
            deliveryFailure="WAITING FOR CLEAR THREE-WIDE SIDE AISLE";return;
        }
        if(!nearRemoteGoal(remoteMoveAside)){
            deliveryFailure="THREE GOLEMS MOVING TARGET ASIDE";
            navigateRemoteFloor(level,remoteMoveAside);return;
        }
        // A planned aisle can be reached while the bounding box still
        // overlaps the old deck. Never claim placement-ready prematurely.
        getNavigation().stop();
        if(remoteFootprintClear()){
            deliveryMode=6;
            deliveryFailure="FOOTPRINT CLEARED; WAITING FOR EMPTY COURIER";
        }else{
            remoteMoveAside=BlockPos.ZERO;
            deliveryFailure="RESELECTING CLEAR LIFT-TEAM PARKING";
        }
    }
    private void tickRemoteRendezvous(ServerLevel level){
        getNavigation().stop();
        deliveryFailure=remoteReplacementPlaced
                ?"WAITING FOR FOURTH GOLEM TO JOIN"
                :"WAITING FOR COURIER TO PLACE EMPTY PALLET";
    }
    private void tickWarehouseReturn(ServerLevel level){
        BlockPos computer=deliveryPosition;
        if(!level.hasChunkAt(computer)){
            deliveryFailure="WAITING FOR WAREHOUSE CHUNK";
            getNavigation().stop();
            return;
        }
        if(!level.getBlockState(computer).is(RackContent.RACK_COMPUTER)){
            deliveryFailure="WAITING: HOME COMPUTER MISSING";
            getNavigation().stop();
            return;
        }
        var warehouse=WarehouseNetwork.fromComputer(level,computer);
        PtpBlockEntity point=null;
        for(PtpBlockEntity candidate:warehouse.points()){
            if(!candidate.canInstallPallet())continue;
            if(point==null||candidate.input()&&!point.input())point=candidate;
        }
        RackBlockEntity rack=null;
        if(point==null){
            for(RackBlockEntity candidate:warehouse.racks())
                if(!candidate.hasPallet()&&candidate.isEmpty()
                        &&RackPickupPlan.find(level,candidate)!=null){
                    rack=candidate;break;
                }
        }
        if(point==null&&rack==null){
            deliveryFailure="WAITING: NO VACANT INPUT PTP OR RACK";
            getNavigation().stop();return;
        }
        BlockPos target=point!=null?point.getBlockPos():rack.getBlockPos();
        BlockPos service=null;
        if(rack!=null){
            RackPickupPlan plan=RackPickupPlan.find(level,rack);
            if(plan!=null)service=plan.rigCenter();
        }else{
            for(Direction face:Direction.Plane.HORIZONTAL){
                BlockPos candidate=target.relative(face,3);
                if(safePlatformFloor(level,candidate)){
                    service=candidate;break;
                }
            }
        }
        if(service==null){
            deliveryFailure="WAITING: UNLOADING AISLE BLOCKED";
            getNavigation().stop();return;
        }
        double dx=service.getX()+.5-getX(),dz=service.getZ()+.5-getZ();
        if(dx*dx+dz*dz>2.25||Math.abs(service.getY()-getY())>.8){
            deliveryFailure="RETURNING TO "+(point!=null?"INPUT PTP":"RACK");
            navigateRemoteFloor(level,service);
            return;
        }
        getNavigation().stop();
        boolean done=false;
        if(point!=null){
            ItemStack unwrapped=com.caszgamermd.caszualadditions.pallet.GlassPalletWrap
                    .withWrap(pallet.copy(),false);
            if(point.installPallet(unwrapped)){
                boolean valid=allFit(point);
                if(valid){
                    for(int i=0;i<216;i++)
                        if(!contents.get(i).isEmpty())
                            point.setItem(i,contents.get(i).copy());
                    valid=verify(point);
                }
                if(valid){
                    point.setInput(true);
                    clearCargo();
                    done=true;
                }else{
                    point.clearContent();
                    point.dropPallet();
                }
            }
        }else if(rack!=null){
            done=deliverToRack(rack);
        }
        if(!done){
            deliveryFailure="WAITING: RETURN DESTINATION NOT READY";
            return;
        }
        deliveryMode=0;
        deliveryFailure="";
        restoreWorkers(level);
    }

    private void tickDelivery(ServerLevel level){
        if(deliveryMode==3){tickWarehouseReturn(level);return;}
        if(deliveryMode==2||deliveryMode==7){tickAisleDelivery(level);return;}
        if(deliveryMode==4){tickRemoteExchange(level);return;}
        if(deliveryMode==5){tickRemoteStepAside(level);return;}
        if(deliveryMode==6){tickRemoteRendezvous(level);return;}
        if(!level.hasChunkAt(deliveryPosition))return;
        BlockPos target=deliveryPosition;
        double targetX,targetZ;
        int targetY;
        RackBlockEntity rack=null;
        if(deliveryMode==1){
            rack=level.getBlockEntity(target) instanceof RackBlockEntity found?found:null;
            if(rack==null||rack.hasPallet()||!rack.isEmpty()){
                waitForDelivery("WAITING: RACK UNAVAILABLE (ORDER SAVED)");
                return;
            }
            RackPickupPlan plan=RackPickupPlan.find(level,rack);
            if(plan==null){
                waitForDelivery("WAITING: CLEAR RACK FRONT AISLE (ORDER SAVED)");
                return;
            }
            BlockPos left=RackBlock.cell(target,rack.facing(),0).relative(rack.facing(),2);
            BlockPos right=RackBlock.cell(target,rack.facing(),1).relative(rack.facing(),2);
            targetX=(left.getX()+right.getX())*.5+.5;
            targetZ=(left.getZ()+right.getZ())*.5+.5;
            targetY=plan.floorY();
        }else{
            if(!groundClear(level,target,deliveryFacing)){
                waitForDelivery("WAITING: DELIVERY FLOOR BLOCKED (ORDER SAVED)");
                return;
            }
            Direction right=deliveryFacing.getClockWise();
            targetX=target.getX()+.5+(right.getStepX()+deliveryFacing.getStepX())*.5;
            targetZ=target.getZ()+.5+(right.getStepZ()+deliveryFacing.getStepZ())*.5;
            targetY=target.getY();
        }
        double dx=targetX-getX(),dz=targetZ-getZ();
        double horizontal=Math.sqrt(dx*dx+dz*dz);
        double vertical=Math.abs(getY()-targetY);
        if(deliveryMode==1&&rack!=null
                &&(horizontal>1.6||vertical>.70)){
            if(releaseTicks!=0){
                releaseTicks=0;entityData.set(RELEASE_TICKS,0);
            }
            navigateToRackFront(level,
                    BlockPos.containing(targetX,targetY,targetZ),
                    targetX,targetZ);
            return;
        }
        // Pathfinding stops early for a 1.85-wide mob. Once close enough
        // to be in the clear delivery aisle, align using normal
        // collision-checked movement instead of requesting an impossible
        // sub-block-perfect navigation target every 8 ticks.
        if(horizontal>.40||vertical>.65){
            if(releaseTicks!=0){releaseTicks=0;entityData.set(RELEASE_TICKS,0);}
            if(horizontal>.01&&horizontal<2.1&&vertical<.70){
                getNavigation().stop();
                double step=Math.min(.13,horizontal);
                move(MoverType.SELF,new Vec3(dx/horizontal*step,0,dz/horizontal*step));
            }else if(tickCount%10==0){
                getNavigation().moveTo(targetX,targetY,targetZ,1.0);
            }
            double remaining=Math.hypot(targetX-getX(),targetZ-getZ());
            if(remaining+.025<lastDeliveryDistance){
                lastDeliveryDistance=remaining;
                deliveryStallTicks=0;
            }else if(++deliveryStallTicks>100){
                waitForDelivery("WAITING: DELIVERY APPROACH BLOCKED, RETRYING");
            }
            return;
        }
        getNavigation().stop();
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        deliveryStallTicks=0;
        if(deliveryMode==1&&rack!=null){
            // The final commit requires being physically in FRONT of the
            // rack's two open columns, not beside/behind the shelf frame.
            BlockPos a=RackBlock.cell(target,rack.facing(),0);
            BlockPos b=RackBlock.cell(target,rack.facing(),1);
            double cx=(a.getX()+b.getX())*.5+.5;
            double cz=(a.getZ()+b.getZ())*.5+.5;
            double front=(getX()-cx)*rack.facing().getStepX()
                    +(getZ()-cz)*rack.facing().getStepZ();
            if(front<1.25){
                waitForDelivery("WAITING: CREW MUST APPROACH RACK FRONT");
                return;
            }
            deliveryFailure="INSERTING PALLET";
            // Reverse pickup: first lift the pallet from the aisle to the
            // rack bay's elevation (22 ticks), then slide it inward through
            // the front opening (18 ticks). No cargo is transferred yet.
            BlockPos first=RackBlock.cell(target,rack.facing(),0);
            double minX=first.getX(),minZ=first.getZ();
            for(int i=1;i<4;i++){
                BlockPos cell=RackBlock.cell(target,rack.facing(),i);
                minX=Math.min(minX,cell.getX());
                minZ=Math.min(minZ,cell.getZ());
            }
            entityData.set(RACK_INSERT_X,(float)(minX+1-getX()));
            entityData.set(RACK_INSERT_Z,(float)(minZ+1-getZ()));
            entityData.set(RACK_INSERT_Y,(float)(
                    rack.getBlockPos().getY()+.06-(getY()+1.48)));
        }
        int releaseDuration=deliveryMode==1?RackPickupPlan.PULL_TICKS:20;
        if(releaseTicks<releaseDuration){
            entityData.set(RELEASE_TICKS,++releaseTicks);
            return;
        }
        // Only transfer inventory AFTER all four models turn inward and
        // lower their arms; a rejected placement keeps the whole shipment.
        boolean ok=deliveryMode==1?deliverToRack(rack)
                :deliverToGround(level,target,deliveryFacing);
        if(!ok){
            waitForDelivery("WAITING: PLACEMENT BLOCKED, RETRYING");
            return;
        }
        deliveryMode=0;
        deliveryFailure="";
        entityData.set(RACK_INSERT_ACTIVE,false);
        // Try separation immediately at the successful placement point.
        // If the aisle is crowded, keep the escrowed four-worker identity
        // and retry next ticks instead of spawning inside solid blocks.
        restoreWorkers(level);
    }

    public boolean deliverToRack(RackBlockEntity rack){
        if(!loaded()||rack.hasPallet()||!rack.isEmpty())return false;
        rack.install(pallet);
        if(!rack.hasPallet()||!allFit(rack)){
            rack.dropPallet();return false;
        }
        for(int i=0;i<216;i++)if(!contents.get(i).isEmpty())
            rack.setItem(i,contents.get(i).copy());
        if(!verify(rack)){
            rack.clearContent();rack.dropPallet();return false;
        }
        clearCargo();
        return true;
    }
    public boolean deliverToGround(ServerLevel level,BlockPos pos,Direction facing){
        return deliverToGround(level,pos,facing,false);
    }
    private boolean deliverToGround(ServerLevel level,BlockPos pos,Direction facing,
            boolean legacy){
        if(!loaded()||!(pallet.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return false;
        BlockState root=block.defaultBlockState().setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing).setValue(PalletBlock.LEGACY,legacy);
        for(int i=0;i<4;i++){
            BlockPos piece=PalletBlock.partPos(pos,i,root);
            if(level.isOutsideBuildHeight(piece)
                    ||!level.getWorldBorder().isWithinBounds(piece)
                    ||!level.hasChunkAt(piece)
                    ||!level.getBlockState(piece).canBeReplaced())return false;
        }
        if(!level.setBlock(pos,root,3))return false;
        boolean complete=true;
        for(int i=1;i<4;i++)
            if(!level.setBlock(PalletBlock.partPos(pos,i,root),
                    root.setValue(PalletBlock.PART,i),3)){
                complete=false;break;
            }
        PalletBlockEntity dest=level.getBlockEntity(pos)
                instanceof PalletBlockEntity be?be:null;
        if(dest==null)complete=false;
        if(complete){
            dest.initialize(pos,0,pallet);
            if(!allFit(dest))complete=false;
        }
        if(complete){
            for(int i=0;i<216;i++)if(!contents.get(i).isEmpty())
                dest.setItem(i,contents.get(i).copy());
            complete=verify(dest);
        }
        if(!complete){
            if(dest!=null)dest.clearContent();
            for(int i=0;i<4;i++){
                BlockPos where=PalletBlock.partPos(pos,i,root);
                BlockState placed=level.getBlockState(where);
                if(placed.getBlock()==block
                        &&placed.getValue(PalletBlock.PART)==i)
                    level.removeBlock(where,false);
            }
            return false;
        }
        clearCargo();
        return true;
    }
    /** Recreate precisely four *empty* walking crew members once the
     * pallet has been delivered. On partial spawn failure, roll them back
     * and retry; do not manufacture new shelf tokens. */
    private boolean restoreWorkers(ServerLevel level){
        UUID id;
        try{id=UUID.fromString(owner);}catch(IllegalArgumentException ex){return false;}
        java.util.ArrayList<StorageGolem> spawned=new java.util.ArrayList<>();
        for(int i=0;i<4;i++){
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null){for(StorageGolem worker:spawned)worker.discard();return false;}
            golem.assignPlayerCrew(shelves[i],i,id);
            // After the pallet is placed the former corner of the carried
            // model may be INSIDE the new pallet block. Stand each separate
            // worker safely OUTSIDE its footprint, oriented towards the
            // delivered load. Until this point all four corners were rigid.
            // Search progressively wider aisle rings, starting near
            // this worker's former corner. A rack or a placed pallet may
            // occupy one or more corners. Never put a detached Golem
            // inside the solid pallet/rack frame or another Golem's cell.
            boolean found=false;
            double yaw=Math.toRadians(getYRot());
            for(double radius:new double[]{1.55,2.25,3.05,3.85}){
                for(int step=0;step<8&&!found;step++){
                    int slot=(i*2+step)%8;
                    double angle=Math.PI*.25*slot;
                    double x=radius*Math.cos(angle),z=radius*Math.sin(angle);
                    double gx=getX()+x*Math.cos(yaw)-z*Math.sin(yaw);
                    double gz=getZ()+x*Math.sin(yaw)+z*Math.cos(yaw);
                    BlockPos feet=BlockPos.containing(gx,getY(),gz);
                    if(!StorageGolemReturnPath.walkable(level,feet))continue;
                    boolean occupied=false;
                    for(StorageGolem placed:spawned)
                        if(placed.blockPosition().equals(feet)
                                ||placed.distanceToSqr(gx,getY(),gz)<1.0){
                            occupied=true;break;
                        }
                    if(occupied)continue;
                    golem.setPos(gx,getY(),gz);
                    if(!level.noCollision(golem))continue;
                    double towardX=getX()-gx,towardZ=getZ()-gz;
                    float face=(float)Math.toDegrees(Math.atan2(-towardX,towardZ));
                    golem.setYRot(face);
                    golem.setYHeadRot(face);
                    golem.setYBodyRot(face);
                    found=true;
                }
                if(found)break;
            }
            if(!found){
                for(StorageGolem worker:spawned)worker.discard();
                return false; // No duplication: retry when a safe aisle opens.
            }
            if(!level.addFreshEntity(golem)){
                for(StorageGolem worker:spawned)worker.discard();
                return false;
            }
            spawned.add(golem);
        }
        if(recallAfterDelivery)
            for(StorageGolem worker:spawned)worker.recallToShelf(level);
        discard();
        return true;
    }
    /**
     * Check the actual loaded platform's ground support and footprint,
     * not just the central Golem's feet. A 1.85-wide pallet cannot fit
     * through a one-block opening or stand half over the edge of a rack.
     */
    private boolean safePlatformFloor(ServerLevel level,BlockPos center){
        return WarehouseTerrain.wideFloor(level,center);
    }

    private void followOnFloor(ServerLevel level,
            net.minecraft.world.entity.player.Player player){
        BlockPos playerFloor=BlockPos.containing(
                player.getX(),player.getY(),player.getZ());
        BlockPos current=blockPosition();
        boolean higher=player.getY()>getY()+.8;
        double dx=player.getX()-getX(),dz=player.getZ()-getZ();
        if(dx*dx+dz*dz<=1.6*1.6
                &&Math.abs(player.getY()-getY())<.7){
            getNavigation().stop();
            floorFollowRoute.clear();
            return;
        }
        // Only a real elevated player authorizes a one-block ascent.
        // Otherwise this entire navigation path contains level or LOWER
        // waypoints and the loaded platform does not climb cargo/racks.
        if(!playerFloor.equals(lastFloorPlayer)
                ||floorFollowRoute.isEmpty()&&tickCount>=nextFloorFollowPlan){
            lastFloorPlayer=playerFloor.immutable();
            floorFollowRoute.clear();
            var route=MovingPalletFloorPath.route(current,playerFloor,higher,
                    position->safePlatformFloor(level,position)
                            &&(tickCount>=avoidFloorCellUntil
                                ||!position.equals(obstructedFloorCell)),
                    (from,to)->WarehouseTerrain.smoothHalfStep(level,from,to));
            nextFloorFollowPlan=tickCount+24;
            if(route!=null)floorFollowRoute.addAll(route);
            lastFollowDistance=Double.POSITIVE_INFINITY;
            lastFollowProgressTick=tickCount;
        }
        while(!floorFollowRoute.isEmpty()){
            BlockPos waypoint=floorFollowRoute.peekFirst();
            if(!safePlatformFloor(level,waypoint)){
                floorFollowRoute.clear();
                nextFloorFollowPlan=0;
                return;
            }
            double sx=waypoint.getX()+.5-getX();
            double sz=waypoint.getZ()+.5-getZ();
            if(sx*sx+sz*sz<.28
                    &&Math.abs(getY()-WarehouseTerrain.standingY(level,waypoint))<.8){
                floorFollowRoute.removeFirst();
                lastFollowDistance=Double.POSITIVE_INFINITY;
                lastFollowProgressTick=tickCount;
            }else break;
        }
        if(floorFollowRoute.isEmpty())return;
        BlockPos next=floorFollowRoute.peekFirst();
        double vx=next.getX()+.5-getX();
        double vz=next.getZ()+.5-getZ();
        double distance=vx*vx+vz*vz;
        if(distance+.035<lastFollowDistance){
            lastFollowDistance=distance;
            lastFollowProgressTick=tickCount;
        }else if(tickCount-lastFollowProgressTick>60){
            obstructedFloorCell=next.immutable();
            avoidFloorCellUntil=tickCount+80;
            floorFollowRoute.clear();
            nextFloorFollowPlan=0;
            lastFollowDistance=Double.POSITIVE_INFINITY;
            lastFollowProgressTick=tickCount;
            getNavigation().stop();
            return;
        }
        getNavigation().stop();
        // Move control handles stepping off a supported ledge and falling
        // exactly one block down, instead of jumping back to level ground.
        getMoveControl().setWantedPosition(next.getX()+.5,
                WarehouseTerrain.standingY(level,next),next.getZ()+.5,1.0);
    }

    private void updateCrewLabel(){
        String text=delivered?"4 GOLEMS | UNLOADING"
                :isDelivering()?(deliveryMode==4
                    ?"3 GOLEMS | TARGET PICKUP"
                    :deliveryMode==3?"4 GOLEMS | RETURNING TO WAREHOUSE"
                    :deliveryMode==1?"4 GOLEMS | DELIVERING TO RACK"
                    :"4 GOLEMS | PLACING PALLET")
                :deliveryMode==5?"3 GOLEMS | MOVING ASIDE"
                :deliveryMode==6?"3 GOLEMS | COURIER HANDOFF"
                :liftTicks<12?"4 GOLEMS | LIFTING"
                :"4 GOLEMS | FOLLOWING PLAYER";
        if(getCustomName()==null||!getCustomName().getString().equals(text))
            setCustomName(net.minecraft.network.chat.Component.literal(text));
        if(!isCustomNameVisible())setCustomNameVisible(true);
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel server))return;
        if(tickCount%10==0)updateCrewLabel();
        if((deliveryMode>=3&&deliveryMode<=6)&&tickCount%40==0){
            // Keep a short, expiring simulation corridor around the actual
            // walking platform, plus its next destination. No teleportation.
            WarehouseRemoteTickets.keep(server,blockPosition());
            WarehouseRemoteTickets.keep(server,deliveryPosition);
            // Lease the NEXT portion of the walking corridor as well. A
            // distant endpoint ticket does not load the terrain BETWEEN.
            double vx=deliveryPosition.getX()+.5-getX();
            double vz=deliveryPosition.getZ()+.5-getZ();
            double length=Math.hypot(vx,vz);
            if(length>8)
                WarehouseRemoteTickets.keep(server,new BlockPos(
                        (int)Math.floor(getX()+vx/length*18),
                        blockPosition().getY(),
                        (int)Math.floor(getZ()+vz/length*18)));
        }
        if(delivered){
            if(tickCount%20==0)restoreWorkers(server);
            return;
        }
        // The first 12 ticks are a real lift animation; the one entity
        // does not begin pathfinding until all four corners rise together.
        if(liftTicks<12){
            liftTicks++;
            entityData.set(LIFT_TICKS,liftTicks);
            getNavigation().stop();
            return;
        }
        UUID id;
        try{id=UUID.fromString(owner);}catch(IllegalArgumentException ex){return;}
        // A committed scanner order is independent of the player's facing
        // or location, even if the player walks away after selecting it.
        if(isDelivering()){tickDelivery(server);return;}
        var player=server.getPlayerByUUID(id);
        if(player==null||player.isSpectator())return;
        // The four attached robots are one 1.85-wide load. Prefer level
        // warehouse aisles; allow one-block drops; only climb when the
        // followed PLAYER has actually moved up.
        followOnFloor(server,player);
    }
    private void dropCargo(ServerLevel level){
        if(pallet.isEmpty())return;
        // The one physical carrier holds ONE real pallet and all 216
        // indexed slots. Death places it intact on nearby safe ground,
        // or drops a single self-contained item if no 2x2 area is open.
        boolean preserveOldFacing=remoteExchangeActive()&&loaded();
        PalletDeathRecovery.release(level,blockPosition(),getX(),getY(),getZ(),
                pallet,contents,
                preserveOldFacing?remoteOldFacing:Direction.fromYRot(getYRot()),
                preserveOldFacing&&remoteOldLegacy);
        clearCargo();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource cause){
        if(level() instanceof ServerLevel level&&!deathDropsReleased){
            deathDropsReleased=true;
            // A split remote courier is still a real fourth Golem, not a
            // model owned twice. Recover its deck and send it to its shelf,
            // then drop ONLY the three workers bound to this carrier.
            int workerDrops=remoteCourierId==null?4:3;
            if(remoteCourierId!=null
                    &&level.getEntity(remoteCourierId) instanceof StorageGolem courier)
                courier.remoteCarrierDestroyed(level);
            dropCargo(level);
            for(int i=0;i<workerDrops;i++)
                Containers.dropItemStack(level,getX(),getY(),getZ(),
                        new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        }
        super.die(cause);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.putString("owner",owner);
        out.putBoolean("delivered",delivered);
        out.putBoolean("recall_after_delivery",recallAfterDelivery);
        out.putInt("lift_ticks",liftTicks);
        out.putInt("delivery_mode",deliveryMode);
        out.putInt("release_ticks",releaseTicks);
        out.store("delivery_position",BlockPos.CODEC,deliveryPosition);
        out.store("delivery_service",BlockPos.CODEC,deliveryService);
        out.store("remote_warehouse",BlockPos.CODEC,remoteWarehouse);
        out.putString("delivery_facing",deliveryFacing.getName());
        out.putString("delivery_failure",deliveryFailure);
        out.putBoolean("remote_replacement_placed",remoteReplacementPlaced);
        out.putBoolean("remote_old_legacy",remoteOldLegacy);
        out.putString("remote_old_facing",remoteOldFacing.getName());
        if(remoteCourierId!=null)out.putString("remote_courier_uuid",remoteCourierId.toString());
        out.store("remote_aisle",BlockPos.CODEC,remoteAisle);
        out.store("remote_move_aside",BlockPos.CODEC,remoteMoveAside);
        for(int i=0;i<4;i++)out.store("shelf_"+i,BlockPos.CODEC,shelves[i]);
        if(!pallet.isEmpty())out.store("pallet",ItemStack.CODEC,pallet);
        ContainerHelper.saveAllItems(out,contents);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        owner=in.getStringOr("owner","");
        delivered=in.getBooleanOr("delivered",false);
        recallAfterDelivery=in.getBooleanOr("recall_after_delivery",false);
        if(delivered){
            setNoGravity(true);
            noPhysics=true;
        }
        liftTicks=Math.clamp(in.getIntOr("lift_ticks",12),0,12);
        entityData.set(LIFT_TICKS,liftTicks);
        deliveryMode=Math.clamp(in.getIntOr("delivery_mode",0),0,7);
        releaseTicks=Math.clamp(in.getIntOr("release_ticks",0),0,20);
        deliveryPosition=in.read("delivery_position",BlockPos.CODEC).orElse(BlockPos.ZERO);
        deliveryService=in.read("delivery_service",BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteWarehouse=in.read("remote_warehouse",BlockPos.CODEC).orElse(BlockPos.ZERO);
        try{deliveryFacing=Direction.valueOf(
                in.getStringOr("delivery_facing","north")
                    .toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ex){deliveryFacing=Direction.NORTH;}
        deliveryFailure=in.getStringOr("delivery_failure","");
        remoteReplacementPlaced=in.getBooleanOr("remote_replacement_placed",false);
        remoteOldLegacy=in.getBooleanOr("remote_old_legacy",false);
        remoteAisle=in.read("remote_aisle",BlockPos.CODEC).orElse(BlockPos.ZERO);
        remoteMoveAside=in.read("remote_move_aside",BlockPos.CODEC).orElse(BlockPos.ZERO);
        try{remoteOldFacing=Direction.valueOf(in.getStringOr("remote_old_facing","north")
                .toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ex){remoteOldFacing=Direction.NORTH;}
        try{remoteCourierId=UUID.fromString(in.getStringOr("remote_courier_uuid",""));}
        catch(IllegalArgumentException ex){remoteCourierId=null;}
        entityData.set(CREW_SIZE,remoteCourierId==null?4:3);
        entityData.set(RELEASE_TICKS,releaseTicks);
        entityData.set(RACK_INSERT_ACTIVE,deliveryMode==1);
        for(int i=0;i<4;i++)
            shelves[i]=in.read("shelf_"+i,BlockPos.CODEC).orElse(BlockPos.ZERO);
        pallet=in.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        contents=NonNullList.withSize(216,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        syncDisplay();
    }
}
