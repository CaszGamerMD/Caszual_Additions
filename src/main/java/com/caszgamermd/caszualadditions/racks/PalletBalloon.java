package com.caszgamermd.caszualadditions.racks;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/**
 * Player-driven pallet hot-air balloon. The first passenger is the pilot;
 * the basket accepts four players and four complete pallet item snapshots.
 * Horizontal speed is linearly scaled from 2 m/s at Y=64 to 20 m/s at Y=300.
 */
public final class PalletBalloon extends PathfinderMob {
    private static final EntityDataAccessor<Integer> CARGO_COUNT =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> ENVELOPE_HEAT =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<ItemStack> CARGO_0 =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> CARGO_1 =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> CARGO_2 =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> CARGO_3 =
            SynchedEntityData.defineId(PalletBalloon.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<?>[] CARGO_DATA =
            {CARGO_0,CARGO_1,CARGO_2,CARGO_3};
    private final List<ItemStack> pallets=new ArrayList<>(4);
    private float turnInput,forwardInput,liftInput;
    private float serverTurn,serverForward,serverLift;
    private int damage;
    private boolean dropped;

    public PalletBalloon(EntityType<? extends PalletBalloon> type,Level level){
        super(type,level);
        setPersistenceRequired();
        setNoGravity(true);
        for(int i=0;i<4;i++)pallets.add(ItemStack.EMPTY);
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,20)
                .add(Attributes.MOVEMENT_SPEED,.2)
                .add(Attributes.FOLLOW_RANGE,8);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(CARGO_COUNT,0);
        builder.define(ENVELOPE_HEAT,0f);
        builder.define(CARGO_0,ItemStack.EMPTY);
        builder.define(CARGO_1,ItemStack.EMPTY);
        builder.define(CARGO_2,ItemStack.EMPTY);
        builder.define(CARGO_3,ItemStack.EMPTY);
    }
    public int palletCount(){return entityData.get(CARGO_COUNT);}
    @SuppressWarnings("unchecked")
    public ItemStack palletAt(int slot){
        if(slot<0||slot>=4)return ItemStack.EMPTY;
        return entityData.get((EntityDataAccessor<ItemStack>)CARGO_DATA[slot]).copy();
    }
    public float heat(){return entityData.get(ENVELOPE_HEAT);}
    public void setPilotInput(float forward,float turn,float lift){
        turnInput=Math.clamp(turn,-1,1);
        forwardInput=Math.clamp(forward,-1,1);
        liftInput=Math.clamp(lift,-1,1);
    }
    public double horizontalSpeed(){
        double t=Math.clamp((getY()-64.0)/236.0,0.0,1.0);
        return (2.0+18.0*t)/20.0;
    }
    public boolean addPallet(ItemStack pallet){
        if(level().isClientSide()||pallet.isEmpty()||pallets.size()!=4)return false;
        for(int i=0;i<4;i++)if(pallets.get(i).isEmpty()){
            pallets.set(i,pallet.copyWithCount(1));
            syncCargo();return true;
        }
        return false;
    }
    public ItemStack removePallet(){
        if(level().isClientSide())return ItemStack.EMPTY;
        for(int i=3;i>=0;i--)if(!pallets.get(i).isEmpty()){
            ItemStack result=pallets.get(i);
            pallets.set(i,ItemStack.EMPTY);
            syncCargo();return result;
        }
        return ItemStack.EMPTY;
    }
    @SuppressWarnings("unchecked")
    private void syncCargo(){
        entityData.set(CARGO_COUNT,(int)pallets.stream().filter(s->!s.isEmpty()).count());
        for(int i=0;i<4;i++)
            entityData.set((EntityDataAccessor<ItemStack>)CARGO_DATA[i],pallets.get(i).copy());
    }
    @Override public boolean canAddPassenger(Entity passenger){
        return getPassengers().size()<4&&super.canAddPassenger(passenger);
    }
    @Override public net.minecraft.world.entity.LivingEntity getControllingPassenger(){
        return getFirstPassenger() instanceof Player p?p:null;
    }
    @Override public Vec3 getPassengerRidingPosition(Entity passenger){
        int index=Math.max(0,getPassengers().indexOf(passenger));
        // Seats are at the four basket corners, above the pallet deck pads.
        double[][] seats={{-1.72,1.35,-1.72},{1.72,1.35,-1.72},
                {-1.72,1.35,1.72},{1.72,1.35,1.72}};
        return position().add(seats[Math.min(3,index)][0],seats[Math.min(3,index)][1],
                seats[Math.min(3,index)][2]);
    }
    @Override public void travel(Vec3 input){
        if(level().isClientSide()){
            // The client samples the pilot's real key bindings. The server
            // independently receives normal movement intent from the pilot.
            if(getFirstPassenger() instanceof Player pilot){
                Vec3 intent=input;
                if(intent.lengthSqr()<1.0e-5)intent=new Vec3(input.x,0,
                        input.z);
                serverForward=(float)Math.clamp(intent.z,-1,1);
                serverTurn=(float)Math.clamp(intent.x,-1,1);
                serverLift=pilot.isSprinting()?-.55f:(pilot.isJumping()?.55f:0f);
            }
        }else if(getFirstPassenger() instanceof ServerPlayer pilot){
            Vec3 intent=pilot.getLastClientMoveIntent();
            serverForward=(float)Math.clamp(intent.z,-1,1);
            serverTurn=(float)Math.clamp(intent.x,-1,1);
            serverLift=pilot.isSprinting()?-.55f:(pilot.isJumping()?.55f:0f);
        }
        if(getFirstPassenger() instanceof Player pilot){
            float turn=level().isClientSide()?turnInput:serverTurn;
            float forward=level().isClientSide()?forwardInput:serverForward;
            float lift=level().isClientSide()?liftInput:serverLift;
            float targetYaw=pilot.getYRot();
            // Smooth rudder input and pilot look both influence heading.
            setYRot(getYRot()+turn*1.8f);
            if(Math.abs(turn)<.01f)setYRot(targetYaw);
            float yaw=(float)Math.toRadians(getYRot());
            double dx=-Math.sin(yaw)*forward*horizontalSpeed();
            double dz=Math.cos(yaw)*forward*horizontalSpeed();
            double dy=lift*.11;
            Vec3 movement=new Vec3(dx,dy,dz);
            if(!level().isClientSide())setDeltaMovement(Vec3.ZERO);
            move(MoverType.SELF,movement);
            if(horizontalCollision||verticalCollision)setDeltaMovement(Vec3.ZERO);
            fallDistance=0;
            entityData.set(ENVELOPE_HEAT,(float)Math.clamp((getY()-64)/236.0,0,1));
        }else{
            // A balloon without a pilot gently loses altitude, never falls.
            if(!level().isClientSide())move(MoverType.SELF,new Vec3(0,-.015,0));
        }
    }
    @Override public void tick(){
        super.tick();
        setNoGravity(true);
        if(!level().isClientSide()&&getY()<64)setPos(getX(),64,getZ());
        if(!level().isClientSide()&&getY()>320)setPos(getX(),320,getZ());
        if(!level().isClientSide()&&getPassengers().isEmpty()&&getY()<=64.05){
            // Empty balloons settle gently on the ground and remain mountable.
            setDeltaMovement(Vec3.ZERO);
        }
    }
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(player.isShiftKeyDown())return InteractionResult.PASS;
        ItemStack held=player.getItemInHand(hand);
        if(held.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                &&blockItem.getBlock() instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock){
            if(!level().isClientSide()){
                if(palletCount()>=4||!addPallet(held)){
                    player.sendSystemMessage(Component.literal("Pallet Balloon basket is full (4 pallets)."));
                }else{
                    if(!player.getAbilities().instabuild)held.shrink(1);
                    player.sendSystemMessage(Component.literal("Pallet loaded into the balloon basket."));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if(getPassengers().contains(player)){
            if(!level().isClientSide()&&held.isEmpty()){
                ItemStack pallet=removePallet();
                if(!pallet.isEmpty()){
                    net.minecraft.world.Containers.dropItemStack(level(),getX(),getY()+.5,getZ(),pallet);
                    player.sendSystemMessage(Component.literal("Pallet unloaded from the balloon basket."));
                }
            }
            return InteractionResult.SUCCESS;
        }
        if(getPassengers().size()>=4){
            if(!level().isClientSide())player.sendSystemMessage(
                    Component.literal("Pallet Balloon basket is full (4 players)."));
            return InteractionResult.SUCCESS;
        }
        if(!level().isClientSide())player.startRiding(this);
        return InteractionResult.SUCCESS;
    }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
            DamageSource source,float amount){
        if(isRemoved())return false;
        damage+=Math.max(1,Math.round(amount));
        if(damage>=20)breakBalloon(level);
        return true;
    }
    private void breakBalloon(net.minecraft.server.level.ServerLevel level){
        if(dropped)return;dropped=true;
        for(Entity passenger:getPassengers())passenger.stopRiding();
        for(ItemStack pallet:pallets)if(!pallet.isEmpty())
            net.minecraft.world.Containers.dropItemStack(level,getX(),getY(),getZ(),pallet);
        net.minecraft.world.Containers.dropItemStack(level,getX(),getY(),getZ(),
                new ItemStack(RackContent.PALLET_BALLOON_ITEM));
        discard();
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.store("damage",com.mojang.serialization.Codec.INT,damage);
        for(int i=0;i<4;i++)if(!pallets.get(i).isEmpty())
            out.store("pallet_"+i,ItemStack.CODEC,pallets.get(i));
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        damage=in.getIntOr("damage",0);
        for(int i=0;i<4;i++)pallets.set(i,in.read("pallet_"+i,ItemStack.CODEC).orElse(ItemStack.EMPTY));
        syncCargo();
    }
}
