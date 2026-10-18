package com.caszgamermd.caszualadditions.painting;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class IndustrialPaintCanBlock extends Block implements EntityBlock {
    private final MapCodec<IndustrialPaintCanBlock> codec=MapCodec.unit(this);
    public IndustrialPaintCanBlock(BlockBehaviour.Properties props){super(props);}
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new IndustrialPaintCanBlockEntity(pos,state);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer server
                &&level.getBlockEntity(pos) instanceof IndustrialPaintCanBlockEntity can)
            ServerPlayNetworking.send(server,new IndustrialPaintNet.Open(pos,can.color()));
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(
            net.minecraft.world.item.ItemStack held,BlockState state,Level level,
            BlockPos pos,Player player,net.minecraft.world.InteractionHand hand,BlockHitResult hit){
        if(player instanceof ServerPlayer server
                &&level.getBlockEntity(pos) instanceof IndustrialPaintCanBlockEntity can)
            ServerPlayNetworking.send(server,new IndustrialPaintNet.Open(pos,can.color()));
        return InteractionResult.SUCCESS;
    }
}