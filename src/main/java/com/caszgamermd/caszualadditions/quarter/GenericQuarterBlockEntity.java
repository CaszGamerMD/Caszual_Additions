package com.caszgamermd.caszualadditions.quarter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class GenericQuarterBlockEntity extends BlockEntity {
    private final BlockState[] materials = new BlockState[8];

    public GenericQuarterBlockEntity(BlockPos pos, BlockState state) {
        super(QuarterBlocks.QUARTER_BLOCK_ENTITY, pos, state);
    }

    public @Nullable BlockState material(int index) {
        return index >= 0 && index < 8 ? materials[index] : null;
    }

    public void setMaterial(int index, BlockState source) {
        if (index < 0 || index >= 8) return;
        materials[index] = source;
        sync();
    }

    public void clearMaterial(int index) {
        if (index < 0 || index >= 8) return;
        materials[index] = null;
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < 8; i++) {
            materials[i] = input.read("material_" + i, BlockState.CODEC).orElse(null);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < 8; i++) {
            if (materials[i] != null) {
                output.store("material_" + i, BlockState.CODEC, materials[i]);
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
