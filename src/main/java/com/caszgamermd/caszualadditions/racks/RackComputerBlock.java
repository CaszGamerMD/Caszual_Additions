package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jspecify.annotations.Nullable;
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
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<RackComputerBlock> codec=MapCodec.unit(this);
    public RackComputerBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){return Block.box(2,0,2,14,16,14);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return Block.box(2,0,2,14,16,14);}
    @Override protected MapCodec<? extends Block> codec(){return codec;}

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return RackBlock.reservedForAccess(context.getLevel(),context.getClickedPos())
                ?null:defaultBlockState().setValue(FACING,
                        context.getHorizontalDirection().getOpposite());
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide() && player instanceof ServerPlayer server){
            ServerPlayNetworking.send(server,new RackComputerNet.Open(pos));
        }
        return InteractionResult.SUCCESS;
    }
}
