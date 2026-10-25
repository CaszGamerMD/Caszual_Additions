package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Physically walk-driven jack, not a teleporting inventory tool.
 * This entity is the ONE owner of the packed jack and its complete 216 slots.
 * Sneaking uses Minecraft's native dismount. Never travel off the same floor.
 */
public final class PalletJackVehicle extends PathfinderMob {
    private static final EntityDataAccessor<ItemStack> DISPLAY_DECK =
            SynchedEntityData.defineId(PalletJackVehicle.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DISPLAY_CRATES =
            SynchedEntityData.defineId(PalletJackVehicle.class,EntityDataSerializers.INT);
    private ItemStack jack=ItemStack.EMPTY;
    private int floorY=Integer.MIN_VALUE;
    private boolean recovered;
    private static final double WALK_SPEED=.14;

    public PalletJackVehicle(EntityType<? extends PalletJackVehicle> type,Level level){
        super(type,level);
        setPersistenceRequired();
        setNoGravity(true);
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH,5)
                .add(Attributes.MOVEMENT_SPEED,.15).add(Attributes.FOLLOW_RANGE,8);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){
        super.defineSynchedData(b);
        b.define(DISPLAY_DECK,ItemStack.EMPTY);
        b.define(DISPLAY_CRATES,0);
    }
    public ItemStack displayPallet(){return entityData.get(DISPLAY_DECK);}
    public int displayedCargoCrates(){return entityData.get(DISPLAY_CRATES);}
    public boolean hasPallet(){return PalletJackItem.loaded(jack);}
    public int deckY(){return floorY;}
    public ItemStack storedJack(){return jack;}
    public void installJack(ItemStack stack,int y){
        jack=stack.copyWithCount(1);
        floorY=y;
        setNoGravity(true);
        updateDisplay();
    }
    public void updateDisplay(){
        if(!hasPallet()){
            entityData.set(DISPLAY_DECK,ItemStack.EMPTY);
            entityData.set(DISPLAY_CRATES,0);
            return;
        }
        var slots=PalletJackItem.unpack(jack);
        entityData.set(DISPLAY_DECK,slots.get(0).copyWithCount(1));
        int cargo=0;
        for(int i=1;i<slots.size();i++)if(!slots.get(i).isEmpty())cargo++;
        entityData.set(DISPLAY_CRATES,Math.min(cargo,27));
    }
    public boolean clickAllowed(Player player,BlockPos pos){
        return player.getVehicle()==this
                &&pos.getY()==floorY
                &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<64;
    }
    /** Safety applies to BOTH wheels and the front loaded 2x2 footprint. */
    public boolean safeFloor(Level level,double x,double z,float yaw){
        if(floorY==Integer.MIN_VALUE)return false;
        double rad=Math.toRadians(yaw);
        double frontX=-Math.sin(rad),frontZ=Math.cos(rad);
        double[] forward=hasPallet()?new double[]{-.35,.35,1.3,2.15}:new double[]{-.25,.25};
        for(double f:forward)for(double side:new double[]{-.40,.40}){
            double px=x+frontX*f+frontZ*side;
            double pz=z+frontZ*f-frontX*side;
            BlockPos foot=BlockPos.containing(px,floorY+.1,pz);
            if(!level.hasChunkAt(foot))return false;
            BlockState support=level.getBlockState(foot.below());
            // Full-height, sturdy support excludes stairs, slabs and holes.
            if(!support.isCollisionShapeFullBlock(level,foot.below())
                    ||!support.isFaceSturdy(level,foot.below(),Direction.UP)
                    ||!level.getFluidState(foot).isEmpty())return false;
            BlockState inside=level.getBlockState(foot);
            if(!inside.getCollisionShape(level,foot).isEmpty())return false;
        }
        return true;
    }
    @Override public void travel(Vec3 input){
        if(getFirstPassenger() instanceof Player controller){
            // Vanilla's ridden input is a walking input in mount-local X/Z.
            // Use player heading as forward; the jack follows walking input.
            float heading=controller.getYRot();
            if(level().isClientSide()){
                setYRot(heading);
                setDeltaMovement(Vec3.ZERO);
                return;
            }
            setYRot(heading);
            yBodyRot=heading;
            double radians=Math.toRadians(heading);
            double forward=Math.max(-1,Math.min(1,controller.zza));
            double strafe=Math.max(-1,Math.min(1,controller.xxa));
            double dx=(-Math.sin(radians)*forward+Math.cos(radians)*strafe)*WALK_SPEED;
            double dz=(Math.cos(radians)*forward+Math.sin(radians)*strafe)*WALK_SPEED;
            double length=Math.hypot(dx,dz);
            if(length>WALK_SPEED){dx*=WALK_SPEED/length;dz*=WALK_SPEED/length;}
            if(length>0.0001&&safeFloor(level(),getX()+dx,getZ()+dz,heading)){
                double oldX=getX(),oldZ=getZ();
                move(MoverType.SELF,new Vec3(dx,0,dz));
                // Collision resolution must never secretly step the vehicle.
                if(Math.abs(getY()-floorY)>.015||!safeFloor(level(),getX(),getZ(),heading))
                    setPos(oldX,floorY,oldZ);
            }
            setDeltaMovement(Vec3.ZERO);
            fallDistance=0;
            return;
        }
        // Park in place with no gravity when no player is controlling it.
        setDeltaMovement(Vec3.ZERO);
    }
    @Override protected Vec3 getRiddenInput(Player player,Vec3 input){
        return new Vec3(player.xxa,0,player.zza);
    }
    @Override protected float getRiddenSpeed(Player player){return (float)WALK_SPEED;}
    @Override protected void tickRidden(Player player,Vec3 input){
        super.tickRidden(player,input);
        setYRot(player.getYRot());
        yBodyRot=getYRot();
    }
    @Override public net.minecraft.world.entity.LivingEntity getControllingPassenger(){
        return getFirstPassenger() instanceof Player p?p:null;
    }
    @Override public boolean canJump(){return false;}
    @Override public boolean canSprint(){return false;}
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){
        return position().add(0,.12,0);
    }
    @Override public void tick(){
        super.tick();
        setNoGravity(true);
        if(!level().isClientSide()&&floorY!=Integer.MIN_VALUE){
            setDeltaMovement(Vec3.ZERO);
            if(Math.abs(getY()-floorY)>.015)setPos(getX(),floorY,getZ());
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        if(!level().isClientSide()){
            if(getFirstPassenger()!=null&&getFirstPassenger()!=player){
                player.sendSystemMessage(Component.literal("Pallet Jack: another player is driving."));
                return InteractionResult.SUCCESS;
            }
            player.startRiding(this);
            player.sendSystemMessage(Component.literal(
                "Walk to steer the Pallet Jack. Right-click a same-level pallet, rack or floor. Sneak to leave."));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public void die(DamageSource damage){
        recover();
        super.die(damage);
    }
    /** Exactly one packed jack item. A loaded jack never drops its 216 slots separately. */
    private void recover(){
        if(recovered||!(level() instanceof ServerLevel server))return;
        recovered=true;
        ejectPassengers();
        if(jack.isEmpty())jack=new ItemStack(RackContent.PALLET_JACK);
        Containers.dropItemStack(server,getX(),getY(),getZ(),jack.copyWithCount(1));
        jack=ItemStack.EMPTY;
        updateDisplay();
    }
    @Override public void remove(RemovalReason reason){
        if(reason.shouldDestroy())recover();
        super.remove(reason);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.putInt("jack_floor_y",floorY);
        if(!jack.isEmpty())out.store("jack_contents",ItemStack.CODEC,jack);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        floorY=in.getIntOr("jack_floor_y",blockPosition().getY());
        jack=in.read("jack_contents",ItemStack.CODEC).orElse(new ItemStack(RackContent.PALLET_JACK));
        updateDisplay();
        setNoGravity(true);
    }
}
