package com.caszgamermd.caszualadditions.painting;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.registries.BuiltInRegistries;

/** A permanent, non-colliding cosmetic overlay attached to exactly one
 * physical block. It never changes the block's identity or its block entity.
 * Stored as an entity so vanilla and modded targets need no block subclasses.
 */
public final class IndustrialFinishEntity extends Entity {
    private static final EntityDataAccessor<Integer> COLOR=
            SynchedEntityData.defineId(IndustrialFinishEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> PRIMED=
            SynchedEntityData.defineId(IndustrialFinishEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> TARGET=
            SynchedEntityData.defineId(IndustrialFinishEntity.class,EntityDataSerializers.STRING);
    public IndustrialFinishEntity(EntityType<? extends IndustrialFinishEntity> type,Level level){
        super(type,level);
        noPhysics=true;
        setNoGravity(true);
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        builder.define(COLOR,0xffb0b5b9);
        builder.define(PRIMED,true);
        builder.define(TARGET,"");
    }
    public int color(){return entityData.get(COLOR);}
    public boolean primed(){return entityData.get(PRIMED);}
    public BlockPos anchor(){return blockPosition();}
    public void attach(ServerLevel level,BlockPos pos,boolean primer,int rgb){
        setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        entityData.set(COLOR,0xff000000|(rgb&0xffffff));
        entityData.set(PRIMED,primer);
        entityData.set(TARGET,BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString());
    }
    public void finish(boolean primer,int rgb){
        entityData.set(COLOR,0xff000000|(rgb&0xffffff));
        entityData.set(PRIMED,primer);
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel level)||tickCount%10!=0)return;
        BlockPos pos=anchor();
        if(!level.hasChunkAt(pos))return;
        String existing=BuiltInRegistries.BLOCK.getKey(
                level.getBlockState(pos).getBlock()).toString();
        if(level.getBlockState(pos).isAir()||!existing.equals(entityData.get(TARGET)))
            discard();  // Block removed or replaced: no lingering paint.
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        out.putInt("color",color());
        out.putBoolean("primed",primed());
        out.putString("target_block",entityData.get(TARGET));
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        entityData.set(COLOR,in.getIntOr("color",0xffb0b5b9));
        entityData.set(PRIMED,in.getBooleanOr("primed",true));
        entityData.set(TARGET,in.getStringOr("target_block",""));
    }
    @Override public boolean hurtServer(ServerLevel level,
            net.minecraft.world.damagesource.DamageSource damage,float amount){
        return false; // Cosmetic coating is not an attackable world object.
    }
    @Override public boolean isPickable(){return false;}
    @Override public boolean isPushable(){return false;}
}