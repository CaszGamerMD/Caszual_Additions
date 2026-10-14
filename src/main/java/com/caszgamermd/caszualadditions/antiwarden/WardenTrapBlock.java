package com.caszgamermd.caszualadditions.antiwarden;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Low-profile spring trap. Nothing except a Warden can trigger it. */
public final class WardenTrapBlock extends BaseEntityBlock {
    public static final BooleanProperty TRIPPED=BooleanProperty.create("tripped");
    private static final VoxelShape OPEN_SHAPE=box(1,0,1,15,2,15);
    private static final VoxelShape CLOSED_SHAPE=box(0,0,0,16,4,16);
    private final MapCodec<WardenTrapBlock> codec=MapCodec.unit(this);

    public WardenTrapBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TRIPPED,false));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<
            net.minecraft.world.level.block.Block,BlockState> builder){
        builder.add(TRIPPED);
    }
    @Override protected RenderShape getRenderShape(BlockState state){
        return RenderShape.MODEL;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext context){
        return state.getValue(TRIPPED)?CLOSED_SHAPE:OPEN_SHAPE;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,
            BlockGetter world,BlockPos pos,CollisionContext context){
        return OPEN_SHAPE; // warden steps onto the plate, not a full block
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new WardenTrapBlockEntity(pos,state);
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide())return null;
        return (world,pos,block,entity)->{
            if(world instanceof ServerLevel server
                    &&entity instanceof WardenTrapBlockEntity trap)
                trap.serverTick(server);
        };
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!level.isClientSide()
                &&level.getBlockEntity(pos) instanceof WardenTrapBlockEntity trap){
            player.sendSystemMessage(Component.literal(
                    trap.isHolding()?"Warden trapped — harvesting experience."
                            :"Trap armed. Only Wardens trigger it."));
        }
        return InteractionResult.SUCCESS;
    }
}