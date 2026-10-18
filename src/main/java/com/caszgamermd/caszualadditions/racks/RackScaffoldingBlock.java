package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Warehouse rack walkway with guardrails on exposed edges and a clear
 * rack-loading face. Connected platforms form a continuous open deck.
 * Folding retracts BOTH platform and safety rails for pallet insertion/
 * removal. Occupied walkways never fold out from under entities. */
public final class RackScaffoldingBlock extends Block {
    public static final BooleanProperty FOLDED=BooleanProperty.create("folded");
    public static final DirectionProperty FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<RackScaffoldingBlock> codec=MapCodec.unit(this);
    private static final VoxelShape DECK=box(0,0,0,16,3,16);
    private static final VoxelShape STOWED=box(0,0,0,16,2,2);
    private static final VoxelShape NORTH=box(0,2,0,16,16,2);
    private static final VoxelShape SOUTH=box(0,2,14,16,16,16);
    private static final VoxelShape WEST=box(0,2,0,2,16,16);
    private static final VoxelShape EAST=box(14,2,0,16,16,16);
    public RackScaffoldingBlock(BlockBehaviour.Properties p){
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FOLDED,false)
                .setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING,FOLDED);
    }
    private static boolean exposed(BlockGetter level,BlockPos pos,Direction side){
        BlockState other=level.getBlockState(pos.relative(side));
        return !(other.getBlock() instanceof RackScaffoldingBlock)
                ||other.getValue(FOLDED);
    }
    private static VoxelShape shape(BlockGetter level,BlockPos pos,BlockState state){
        if(state.getValue(FOLDED))return STOWED;
        VoxelShape result=DECK;
        // The pallet-loading side is deliberately open. Add rails on
        // every other EXPOSED side, not between adjacent walkway tiles.
        Direction facing=state.getValue(FACING);
        if(facing!=Direction.NORTH&&exposed(level,pos,Direction.NORTH))
            result=Shapes.or(result,NORTH);
        if(facing!=Direction.SOUTH&&exposed(level,pos,Direction.SOUTH))
            result=Shapes.or(result,SOUTH);
        if(facing!=Direction.WEST&&exposed(level,pos,Direction.WEST))
            result=Shapes.or(result,WEST);
        if(facing!=Direction.EAST&&exposed(level,pos,Direction.EAST))
            result=Shapes.or(result,EAST);
        return result;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext ctx){return shape(world,pos,state);}
    @Override protected VoxelShape getCollisionShape(BlockState state,
            BlockGetter world,BlockPos pos,CollisionContext ctx){
        // Folded deck and rail are inside a wall cassette. There is NO
        // protruding collision in the pallet-lifting aisle.
        return state.getValue(FOLDED)?Shapes.empty():shape(world,pos,state);
    }
    public static boolean occupied(Level world,BlockPos pos){
        // Never retract under feet, nor close the railing through people.
        return !world.getEntitiesOfClass(Entity.class,
                new AABB(pos.getX(),pos.getY(),pos.getZ(),
                        pos.getX()+1,pos.getY()+2,pos.getZ()+1),
                Entity::isAlive).isEmpty();
    }
    public static boolean setFolded(Level world,BlockPos pos,boolean folded){
        BlockState state=world.getBlockState(pos);
        if(!(state.getBlock() instanceof RackScaffoldingBlock))return false;
        if(state.getValue(FOLDED)==folded)return true;
        if(occupied(world,pos))return false;
        world.setBlock(pos,state.setValue(FOLDED,folded),3);
        return true;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level world,BlockPos pos,Player player,BlockHitResult hit){
        if(!world.isClientSide()){
            if(!setFolded(world,pos,!state.getValue(FOLDED)))
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Scaffolding occupied — wait until the walkway is clear."));
        }
        return InteractionResult.SUCCESS;
    }
}
