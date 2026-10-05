package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
public final class XpTankBlockEntity extends BlockEntity {
 public static final int CAPACITY = 30970;
 private int xp;
 public XpTankBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.TANK,p,s);}
 public int stored(){return xp;} public void setStored(int v){xp=Math.max(0,Math.min(CAPACITY,v));setChanged();if(level!=null&&!level.isClientSide()){int fill=xp==0?0:Math.min(10,(xp*10+CAPACITY-1)/CAPACITY);var state=getBlockState();if(state.hasProperty(XpTankBlock.FILL)&&state.getValue(XpTankBlock.FILL)!=fill)level.setBlock(worldPosition,state.setValue(XpTankBlock.FILL,fill),3);}}
 @Override protected void saveAdditional(ValueOutput out){super.saveAdditional(out);out.putInt("xp",xp);}
 @Override protected void loadAdditional(ValueInput in){super.loadAdditional(in);xp=Math.max(0,Math.min(CAPACITY,in.getIntOr("xp",0)));}
}
