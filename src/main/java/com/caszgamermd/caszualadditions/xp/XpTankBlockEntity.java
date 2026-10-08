package com.caszgamermd.caszualadditions.xp;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class XpTankBlockEntity extends BlockEntity {
    public static final int CAPACITY = 30970;

    private int xp;

    public XpTankBlockEntity(BlockPos pos, BlockState state) {
        super(XpBlockEntities.TANK, pos, state);
    }

    public int stored() {
        return xp;
    }

    public void setStored(int value) {
        xp = Math.max(0, Math.min(CAPACITY, value));
        setChanged();
    }

    void setVisualFill(int fill) {
        if (level == null || level.isClientSide()) return;

        BlockState state = getBlockState();
        int clamped = Math.max(0, Math.min(10, fill));
        if (state.hasProperty(XpTankBlock.FILL) && state.getValue(XpTankBlock.FILL) != clamped) {
            level.setBlock(worldPosition, state.setValue(XpTankBlock.FILL, clamped), 3);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("xp", xp);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        xp = Math.max(0, Math.min(CAPACITY, input.getIntOr("xp", 0)));
    }
}
