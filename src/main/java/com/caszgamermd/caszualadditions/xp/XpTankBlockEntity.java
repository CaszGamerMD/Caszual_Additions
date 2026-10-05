package com.caszgamermd.caszualadditions.xp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class XpTankBlockEntity extends BlockEntity {
 private int xp;
 public XpTankBlockEntity(BlockPos p,BlockState s){super(XpBlockEntities.TANK,p,s);}
 public int stored(){return xp;} public void setStored(int v){xp=Math.max(0,Math.min(100,v));setChanged();}
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.saveAdditional(tag,lookup);tag.putInt("xp",xp);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider lookup){super.loadAdditional(tag,lookup);xp=Math.max(0,Math.min(100,tag.getInt("xp").orElse(0)));}
}
