package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import org.jspecify.annotations.Nullable;

/**
 * Physical 1-block section of an aisle crossbrace. Runs horizontally along
 * its FACING axis; consecutive sections bridge up to four empty cells.
 * Each section has real collision, not just a rack-side render attachment.
 */
public final class RackConnectorBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<RackConnectorBlock> codec=MapCodec.unit(this);
    public RackConnectorBlock(BlockBehaviour.Properties props){
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        // Connectors intentionally span the rack approach lane, including
        // face-to-face aisle bridges. Other blocks still respect the lane.
        Direction side=context.getClickedFace();
        if(!side.getAxis().isHorizontal())side=context.getHorizontalDirection();
        return defaultBlockState().setValue(FACING,side);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){
        return state.getValue(FACING).getAxis()==Direction.Axis.X
                ?net.minecraft.world.phys.shapes.Shapes.or(
                        Block.box(0,3,7,16,5,9),Block.box(0,11,7,16,13,9))
                :net.minecraft.world.phys.shapes.Shapes.or(
                        Block.box(7,3,0,9,5,16),Block.box(7,11,0,9,13,16));
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
