package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import java.util.List;
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
    private static final EntityDataAccessor<Integer> LIFT_TICKS=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> RELEASE_TICKS=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    private String owner="";
    private final BlockPos[] shelves={BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO};
    private ItemStack pallet=ItemStack.EMPTY;
    private NonNullList<ItemStack> contents=NonNullList.withSize(216,ItemStack.EMPTY);
    private boolean delivered,deathDropsReleased;
    private int liftTicks;
    private int deliveryMode,releaseTicks;
    private BlockPos deliveryPosition=BlockPos.ZERO;
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
        builder.define(LIFT_TICKS,0);
        builder.define(RELEASE_TICKS,0);
    }
    public ItemStack displayPallet(){return entityData.get(DISPLAY_PALLET);}
    public int visibleCargo(){return entityData.get(DISPLAY_CARGO);}
    public float liftProgress(){return entityData.get(LIFT_TICKS)/12.0f;}
    public float releaseProgress(){return entityData.get(RELEASE_TICKS)/20.0f;}
    public boolean isDelivering(){return deliveryMode!=0;}
    public String deliveryStatus(){return deliveryFailure;}
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
        entityData.set(DISPLAY_PALLET,pallet.copy());
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
        if(isDelivering())return "Crew already has a delivery destination.";
        if(rack.hasPallet()||!rack.isEmpty())return "Destination rack must be empty.";
        if(RackPickupPlan.find(level,rack)==null)
            return "Rack needs a clear two-wide access aisle with floor support.";
        deliveryPosition=rack.getBlockPos().immutable();
        deliveryFacing=rack.facing();
        deliveryMode=1;
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
        if(isDelivering())return "Crew already has a delivery destination.";
        if(!groundClear(level,location,facing))
            return "Need a clear supported 2x2 destination floor.";
        deliveryPosition=location.immutable();
        deliveryFacing=facing;
        deliveryMode=2;
        releaseTicks=0;
        deliveryFailure="";
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        entityData.set(RELEASE_TICKS,0);
        return "Golems carrying pallet to "+location.toShortString()
                +"; they will release it only after positioning.";
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
        deliveryStallTicks=0;
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        releaseTicks=0;
        entityData.set(RELEASE_TICKS,0);
        getNavigation().stop();
    }
    private void tickDelivery(ServerLevel level){
        if(!level.hasChunkAt(deliveryPosition))return;
        BlockPos target=deliveryPosition;
        double targetX,targetZ;
        int targetY;
        RackBlockEntity rack=null;
        if(deliveryMode==1){
            rack=level.getBlockEntity(target) instanceof RackBlockEntity found?found:null;
            if(rack==null||rack.hasPallet()||!rack.isEmpty()){
                abortDelivery("Target rack became unavailable; cargo remains onboard.");
                return;
            }
            RackPickupPlan plan=RackPickupPlan.find(level,rack);
            if(plan==null){
                abortDelivery("Rack access aisle is obstructed; cargo remains onboard.");
                return;
            }
            BlockPos left=RackBlock.cell(target,rack.facing(),0).relative(rack.facing(),2);
            BlockPos right=RackBlock.cell(target,rack.facing(),1).relative(rack.facing(),2);
            targetX=(left.getX()+right.getX())*.5+.5;
            targetZ=(left.getZ()+right.getZ())*.5+.5;
            targetY=plan.floorY();
        }else{
            if(!groundClear(level,target,deliveryFacing)){
                abortDelivery("Destination floor is blocked; cargo remains onboard.");
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
                abortDelivery("Pallet cannot reach the marked placement: "
                        +"check for a clear 2-block-wide access route. "
                        +"Cargo remains onboard; select another location.");
            }
            return;
        }
        getNavigation().stop();
        lastDeliveryDistance=Double.POSITIVE_INFINITY;
        deliveryStallTicks=0;
        if(releaseTicks<20){
            entityData.set(RELEASE_TICKS,++releaseTicks);
            return;
        }
        // Only transfer inventory AFTER all four models turn inward and
        // lower their arms; a rejected placement keeps the whole shipment.
        boolean ok=deliveryMode==1?deliverToRack(rack)
                :deliverToGround(level,target,deliveryFacing);
        if(!ok){
            abortDelivery("Placement failed; pallet and inventory remain onboard.");
            return;
        }
        deliveryMode=0;
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
        if(!loaded()||!(pallet.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return false;
        BlockState root=block.defaultBlockState().setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing).setValue(PalletBlock.LEGACY,false);
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
        discard();
        return true;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel server))return;
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
        // Exactly one navigation and collision body for all four corner
        // figures, with no teleporting through walls or individual drift.
        // Follow player POSITION, never their view direction. Turning
        // in place does not make the carried platform orbit.
        double x=player.getX(),z=player.getZ();
        double dx=x-getX(),dz=z-getZ();
        double distance=dx*dx+dz*dz;
        if(distance<1.40*1.40&&Math.abs(getY()-player.getY())<1.0){
            getNavigation().stop();
            lastFollowDistance=Double.POSITIVE_INFINITY;
            lastFollowProgressTick=tickCount;
            return;
        }
        if(distance+.15<lastFollowDistance){
            lastFollowProgressTick=tickCount;
            lastFollowDistance=distance;
        }else if(tickCount-lastFollowProgressTick>70){
            // If a pallet cannot pass directly through a narrow doorway,
            // approach the player's left/right flank, alternate after each
            // stall, and let vanilla find the actual collision-safe route.
            getNavigation().stop();
            obstacleSide=obstacleSide==1?-1:1;
            lastFollowDistance=Double.POSITIVE_INFINITY;
            lastFollowProgressTick=tickCount;
        }
        if((distance>1.90*1.90||Math.abs(getY()-player.getY())>1.6)
                &&tickCount%12==0){
            // Offset an approach only when physically stalled, not on turn.
            double len=Math.sqrt(Math.max(distance,.0001));
            double side=obstacleSide*.65;
            getNavigation().moveTo(x-dz/len*side,player.getY(),
                    z+dx/len*side,1.0);
        }else if(distance<1.55*1.55)getNavigation().stop();
    }
    private void dropCargo(ServerLevel level){
        if(pallet.isEmpty())return;
        Containers.dropItemStack(level,getX(),getY(),getZ(),pallet.copy());
        for(ItemStack contentsItem:contents)
            if(!contentsItem.isEmpty())
                Containers.dropItemStack(level,getX(),getY(),getZ(),contentsItem.copy());
        clearCargo();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource cause){
        if(level() instanceof ServerLevel level&&!deathDropsReleased){
            deathDropsReleased=true;
            dropCargo(level);
            for(int i=0;i<4;i++)
                Containers.dropItemStack(level,getX(),getY(),getZ(),
                        new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        }
        super.die(cause);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.putString("owner",owner);
        out.putBoolean("delivered",delivered);
        out.putInt("lift_ticks",liftTicks);
        out.putInt("delivery_mode",deliveryMode);
        out.putInt("release_ticks",releaseTicks);
        out.store("delivery_position",BlockPos.CODEC,deliveryPosition);
        out.putString("delivery_facing",deliveryFacing.getName());
        out.putString("delivery_failure",deliveryFailure);
        for(int i=0;i<4;i++)out.store("shelf_"+i,BlockPos.CODEC,shelves[i]);
        if(!pallet.isEmpty())out.store("pallet",ItemStack.CODEC,pallet);
        ContainerHelper.saveAllItems(out,contents);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        owner=in.getStringOr("owner","");
        delivered=in.getBooleanOr("delivered",false);
        if(delivered){
            setNoGravity(true);
            noPhysics=true;
        }
        liftTicks=Math.clamp(in.getIntOr("lift_ticks",12),0,12);
        entityData.set(LIFT_TICKS,liftTicks);
        deliveryMode=Math.clamp(in.getIntOr("delivery_mode",0),0,2);
        releaseTicks=Math.clamp(in.getIntOr("release_ticks",0),0,20);
        deliveryPosition=in.read("delivery_position",BlockPos.CODEC).orElse(BlockPos.ZERO);
        try{deliveryFacing=Direction.valueOf(
                in.getStringOr("delivery_facing","north")
                    .toUpperCase(java.util.Locale.ROOT));}
        catch(IllegalArgumentException ex){deliveryFacing=Direction.NORTH;}
        deliveryFailure=in.getStringOr("delivery_failure","");
        entityData.set(RELEASE_TICKS,releaseTicks);
        for(int i=0;i<4;i++)
            shelves[i]=in.read("shelf_"+i,BlockPos.CODEC).orElse(BlockPos.ZERO);
        pallet=in.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        contents=NonNullList.withSize(216,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        syncDisplay();
    }
}
