package com.caszgamermd.caszualadditions.xp;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.BlockHitResult;
public final class XpShowerBlock extends Block implements EntityBlock {
 public static final EnumProperty<Direction> FACING=EnumProperty.create("facing",Direction.class,d->d.getAxis().isHorizontal());
 private static final net.minecraft.world.phys.shapes.VoxelShape NS=net.minecraft.world.phys.shapes.Shapes.or(Block.box(6.5,11,0,9.5,14,12),Block.box(6.5,8,9,9.5,12,12),Block.box(4.5,6,7.5,11.5,8,14.5));
 private static final net.minecraft.world.phys.shapes.VoxelShape EW=net.minecraft.world.phys.shapes.Shapes.or(Block.box(0,11,6.5,12,14,9.5),Block.box(9,8,6.5,12,12,9.5),Block.box(7.5,6,4.5,14.5,8,11.5));
 public static final MapCodec<XpShowerBlock> CODEC=simpleCodec(XpShowerBlock::new);
 public XpShowerBlock(BlockBehaviour.Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));}
 @Override protected MapCodec<? extends Block> codec(){return CODEC;}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return (state.getValue(FACING).getAxis()==Direction.Axis.Z)?NS:EW;}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return (state.getValue(FACING).getAxis()==Direction.Axis.Z)?NS:EW;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING);}
 @Override public BlockState getStateForPlacement(BlockPlaceContext ctx){Direction face=ctx.getClickedFace();Direction support=face.getAxis().isHorizontal()?face.getOpposite():ctx.getHorizontalDirection();return defaultBlockState().setValue(FACING,support);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new XpShowerBlockEntity(p,s);}
 @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
  if(!level.isClientSide()&&player instanceof ServerPlayer sp&&level.getBlockEntity(pos) instanceof XpShowerBlockEntity shower)shower.toggle(sp);
  return InteractionResult.SUCCESS;
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
  if(level.isClientSide())return null;return type==XpBlockEntities.SHOWER?(l,p,s,be)->XpShowerBlockEntity.tick((net.minecraft.server.level.ServerLevel)l,p,(XpShowerBlockEntity)be):null;
 }
}
