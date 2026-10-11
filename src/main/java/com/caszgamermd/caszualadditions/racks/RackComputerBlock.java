package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Network access point; only physical face-adjacent racks grant access. */
public final class RackComputerBlock extends Block {
    private final MapCodec<RackComputerBlock> codec=MapCodec.unit(this);
    public RackComputerBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide() && player instanceof ServerPlayer server){
            ServerPlayNetworking.send(server,new RackComputerNet.Open(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
