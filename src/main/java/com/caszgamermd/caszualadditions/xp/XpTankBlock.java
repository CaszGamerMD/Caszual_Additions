package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
public final class XpTankBlock extends Block implements EntityBlock {
 public static final MapCodec<XpTankBlock> CODEC=simpleCodec(XpTankBlock::new);
 public static final IntegerProperty FILL=IntegerProperty.create("fill",0,10);
 public static final BooleanProperty NORTH=BooleanProperty.create("north"),SOUTH=BooleanProperty.create("south"),WEST=BooleanProperty.create("west"),EAST=BooleanProperty.create("east");
 public XpTankBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FILL,0).setValue(NORTH,false).setValue(SOUTH,false).setValue(WEST,false).setValue(EAST,false));}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FILL,NORTH,SOUTH,WEST,EAST);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){return connections(defaultBlockState(),ctx.getLevel(),ctx.getClickedPos());}
 @Override protected BlockState updateShape(BlockState state,net.minecraft.world.level.LevelReader level,net.minecraft.world.level.ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,BlockState neighborState,net.minecraft.util.RandomSource random){return horizontal(direction)?connections(state,level,pos):state;}
 private static boolean horizontal(Direction d){return d==Direction.NORTH||d==Direction.SOUTH||d==Direction.WEST||d==Direction.EAST;}
 private static BlockState connections(BlockState s,net.minecraft.world.level.BlockGetter level,BlockPos p){return s.setValue(NORTH,connects(level,p.north())).setValue(SOUTH,connects(level,p.south())).setValue(WEST,connects(level,p.west())).setValue(EAST,connects(level,p.east()));}
 private static boolean connects(net.minecraft.world.level.BlockGetter level,BlockPos p){return level.getBlockState(p).getBlock() instanceof XpTankBlock;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new XpTankBlockEntity(pos,state);}
}
