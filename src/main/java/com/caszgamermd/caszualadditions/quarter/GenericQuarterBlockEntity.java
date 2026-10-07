package com.caszgamermd.caszualadditions.quarter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class GenericQuarterBlockEntity extends BlockEntity {
    private final BlockState[] materials=new BlockState[8];

    public GenericQuarterBlockEntity(BlockPos pos,BlockState state){
        super(QuarterBlocks.QUARTER_BLOCK_ENTITY,pos,state);
    }

    public BlockState material(int index){return index>=0&&index<8?materials[index]:null;}

    public void setMaterial(int index,BlockState source){
        if(index<0||index>=8)return;
        materials[index]=source;
        setChanged();
        if(level!=null){
            var state=getBlockState();
            level.sendBlockUpdated(worldPosition,state,state,3);
        }
    }

    @Override public void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        for(int i=0;i<8;i++)materials[i]=in.read("material_"+i,BlockState.CODEC).orElse(null);
    }

    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        for(int i=0;i<8;i++)if(materials[i]!=null)out.store("material_"+i,BlockState.CODEC,materials[i]);
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){
        return saveWithoutMetadata(provider);
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
