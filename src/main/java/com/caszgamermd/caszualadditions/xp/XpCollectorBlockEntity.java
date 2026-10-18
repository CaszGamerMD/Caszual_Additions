package com.caszgamermd.caszualadditions.xp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

/** Placed ground XP vacuum. Capacity equals one 100-level XP tank.
 * The block entity is the single owner of every captured XP point.
 * Breaking it serializes the exact amount into the dropped collector.
 */
public final class XpCollectorBlockEntity extends BlockEntity {
    public static final int CAPACITY=30970;
    public static final int RADIUS=6;
    private int xp;
    public XpCollectorBlockEntity(BlockPos pos,BlockState state){
        super(XpBlockEntities.COLLECTOR,pos,state);
    }
    public int stored(){return xp;}
    public void setStored(int value){
        xp=Math.clamp(value,0,CAPACITY);
        setChanged();
    }
    public int withdraw(){
        int taken=xp;
        if(taken>0)setStored(0);
        return taken;
    }
    public static int itemXp(ItemStack item){
        CustomData data=item.get(DataComponents.CUSTOM_DATA);
        return data==null?0:Math.clamp(
                data.copyTag().getIntOr("stored_xp",0),0,CAPACITY);
    }
    public ItemStack asItem(){
        ItemStack result=new ItemStack(XpBlocks.XP_COLLECTOR);
        if(xp>0){
            CompoundTag data=new CompoundTag();
            data.putInt("stored_xp",xp);
            result.set(DataComponents.CUSTOM_DATA,CustomData.of(data));
        }
        return result;
    }
    public static void tick(ServerLevel level,BlockPos pos,XpCollectorBlockEntity collector){
        if(level.getGameTime()%5!=0||collector.xp>=CAPACITY)return;
        int stored=collector.xp;
        // XP orbs at ground level, within a small circular work area.
        // Never intercept orbs above/below separate floors more than 3
        // vertical blocks away. Skip an orb that cannot fit completely.
        AABB area=new AABB(pos).inflate(RADIUS,3,RADIUS);
        for(ExperienceOrb orb:level.getEntitiesOfClass(ExperienceOrb.class,
                area,entity->entity.isAlive())){
            double dx=orb.getX()-(pos.getX()+.5);
            double dz=orb.getZ()-(pos.getZ()+.5);
            if(dx*dx+dz*dz>RADIUS*RADIUS)continue;
            int value=orb.getValue();
            if(value<=0||value>CAPACITY-stored)continue;
            stored+=value;
            orb.discard();
            if(stored>=CAPACITY)break;
        }
        if(stored!=collector.xp)collector.setStored(stored);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.putInt("xp",xp);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        xp=Math.clamp(in.getIntOr("xp",0),0,CAPACITY);
    }
}
