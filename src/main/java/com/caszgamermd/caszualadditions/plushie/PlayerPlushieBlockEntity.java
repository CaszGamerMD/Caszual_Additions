package com.caszgamermd.caszualadditions.plushie;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PlayerPlushieBlockEntity extends BlockEntity {
    private @Nullable ResolvableProfile profile;

    public PlayerPlushieBlockEntity(BlockPos pos, BlockState state) {
        super(PlayerPlushies.PLAYER_PLUSHIE_ENTITY, pos, state);
    }

    public @Nullable ResolvableProfile profile() {
        return profile;
    }

    public void setProfile(@Nullable ResolvableProfile profile) {
        this.profile = profile;
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        profile = input.read("profile", ResolvableProfile.CODEC).orElse(null);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (profile != null) output.store("profile", ResolvableProfile.CODEC, profile);
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
