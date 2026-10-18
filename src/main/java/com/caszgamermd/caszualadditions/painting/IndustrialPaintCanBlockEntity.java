package com.caszgamermd.caszualadditions.painting;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
public final class IndustrialPaintCanBlockEntity extends BlockEntity {
    private int color=0x31a6ed;
    public IndustrialPaintCanBlockEntity(BlockPos pos,BlockState state){
        super(IndustrialPaintContent.PAINT_CAN_ENTITY,pos,state);
    }
    public int color(){return color;}
    public void setColor(int rgb){color=rgb&0xffffff;setChanged();}
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.putInt("paint_rgb",color);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        color=in.getIntOr("paint_rgb",0x31a6ed)&0xffffff;
    }
}