package com.caszgamermd.caszualadditions.antiwarden;

import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Single-Warden holding cell. Server owns capture, damage and experience. */
public final class WardenTrapBlockEntity extends BlockEntity {
    private static final int DAMAGE_INTERVAL=20;
    private static final float DAMAGE_PER_TICK=2.0F;
    private UUID captive;
    private int harmTicks;

    public WardenTrapBlockEntity(BlockPos pos,BlockState state){
        super(AntiWardenContent.TRAP_ENTITY,pos,state);
    }
    public boolean isHolding(){return captive!=null;}

    public void serverTick(ServerLevel level){
        if(level.getGameTime()%2!=0)return;
        Warden warden=captive==null?null
                :level.getEntity(captive) instanceof Warden w ?w:null;
        if(warden==null||!warden.isAlive()){
            if(captive!=null){
                captive=null;
                harmTicks=0;
                setChanged();
                setTripped(level,false);
            }
            if(level.getGameTime()%10==0)tryCapture(level);
            return;
        }
        // Reassert the restraint before the Warden's next AI movement.
        Vec3 center=new Vec3(worldPosition.getX()+.5,
                worldPosition.getY()+.125,worldPosition.getZ()+.5);
        warden.getNavigation().stop();
        warden.setDeltaMovement(Vec3.ZERO);
        warden.setPos(center.x,center.y,center.z);
        if(++harmTicks>=DAMAGE_INTERVAL/2){
            harmTicks=0;
            // Experience is earned from actual damage, not timer ticks.
            if(warden.hurtServer(level,level.damageSources().magic(),
                    DAMAGE_PER_TICK)){
                ExperienceOrb.award(level,center.add(0,.8,0),2);
            }
        }
    }
    private void tryCapture(ServerLevel level){
        BlockPos pos=worldPosition;
        List<Warden> targets=level.getEntitiesOfClass(Warden.class,
                new AABB(pos).inflate(.65,1.7,.65),
                w->w.isAlive()&&Math.abs(w.getX()-pos.getX()-.5)<.82
                        &&Math.abs(w.getZ()-pos.getZ()-.5)<.82
                        &&Math.abs(w.getY()-(pos.getY()+.125))<1.0);
        if(targets.isEmpty())return;
        Warden caught=targets.getFirst();
        captive=caught.getUUID();
        harmTicks=0;
        caught.getNavigation().stop();
        caught.setDeltaMovement(Vec3.ZERO);
        setTripped(level,true);
        level.playSound(null,pos,SoundEvents.IRON_TRAPDOOR_CLOSE,
                SoundSource.BLOCKS,.9f,.7f);
        setChanged();
    }
    private void setTripped(ServerLevel level,boolean value){
        BlockState state=getBlockState();
        if(state.hasProperty(WardenTrapBlock.TRIPPED)
                &&state.getValue(WardenTrapBlock.TRIPPED)!=value)
            level.setBlock(worldPosition,
                    state.setValue(WardenTrapBlock.TRIPPED,value),3);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        if(captive!=null)out.putString("warden",captive.toString());
        out.putInt("damage_ticks",harmTicks);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        try{
            String id=in.getStringOr("warden","");
            captive=id.isBlank()?null:UUID.fromString(id);
        }catch(IllegalArgumentException ex){captive=null;}
        harmTicks=Math.max(0,in.getIntOr("damage_ticks",0));
    }
}
