package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Warehouse walkway with visible AND collidable safety rails only on open
 * edges. Rack and scaffold connections are seamless; a ladder directly
 * underneath replaces the solid deck with a climb-through hatch.
 */
public final class RackScaffoldingBlock extends Block {
    public static final BooleanProperty FOLDED = BooleanProperty.create("folded");
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RAIL_NORTH = BooleanProperty.create("rail_north");
    public static final BooleanProperty RAIL_SOUTH = BooleanProperty.create("rail_south");
    public static final BooleanProperty RAIL_WEST = BooleanProperty.create("rail_west");
    public static final BooleanProperty RAIL_EAST = BooleanProperty.create("rail_east");
    public static final BooleanProperty LADDER_HATCH = BooleanProperty.create("ladder_hatch");

    private final MapCodec<RackScaffoldingBlock> codec = MapCodec.unit(this);
    private static final VoxelShape DECK = box(0, 0, 0, 16, 3, 16);
    private static final VoxelShape HATCH_DECK = Shapes.or(
            box(0, 0, 0, 2, 3, 16), box(14, 0, 0, 16, 3, 16),
            box(2, 0, 0, 14, 3, 2), box(2, 0, 14, 14, 3, 16));
    private static final VoxelShape STOWED = box(0, 0, 0, 16, 4, 2);
    private static final VoxelShape NORTH = box(0, 3, 0, 16, 16, 2);
    private static final VoxelShape SOUTH = box(0, 3, 14, 16, 16, 16);
    private static final VoxelShape WEST = box(0, 3, 0, 2, 16, 16);
    private static final VoxelShape EAST = box(14, 3, 0, 16, 16, 16);

    public RackScaffoldingBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FOLDED, false).setValue(FACING, Direction.NORTH)
                .setValue(RAIL_NORTH, true).setValue(RAIL_SOUTH, true)
                .setValue(RAIL_WEST, true).setValue(RAIL_EAST, true)
                .setValue(LADDER_HATCH, false));
    }

    @Override protected MapCodec<? extends Block> codec() { return codec; }

    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FOLDED, RAIL_NORTH, RAIL_SOUTH,
                RAIL_WEST, RAIL_EAST, LADDER_HATCH);
    }

    private static boolean isWalkwayConnection(BlockState neighbor) {
        if (neighbor.getBlock() instanceof RackScaffoldingBlock
                || neighbor.getBlock() instanceof RackBlock) return true;
        // A staircase is three steps high, but only the diagonal (0, 4, 8)
        // cells have actual treads; below-step support cells are NOT exits.
        return neighbor.getBlock() instanceof RackStairsBlock
                && neighbor.getValue(RackStairsBlock.PART) % 3
                        == neighbor.getValue(RackStairsBlock.PART) / 3;
    }

    private static BooleanProperty sideProperty(Direction direction) {
        return switch (direction) {
            case NORTH -> RAIL_NORTH;
            case SOUTH -> RAIL_SOUTH;
            case WEST -> RAIL_WEST;
            case EAST -> RAIL_EAST;
            default -> throw new IllegalArgumentException("Not a horizontal edge: " + direction);
        };
    }

    private static boolean isElevatedStairLanding(BlockGetter level,BlockPos pos,Direction side) {
        // The stair's top tread (part 8) finishes at Y+3. The scaffold
        // placed one block above that tread is its connected upper walkway.
        BlockState below=level.getBlockState(pos.relative(side).below());
        return below.getBlock() instanceof RackStairsBlock
                && below.getValue(RackStairsBlock.PART)==8;
    }

    private static BlockState connectionState(BlockState state, BlockGetter level, BlockPos pos) {
        for (Direction side : Direction.Plane.HORIZONTAL) {
            state = state.setValue(sideProperty(side),
                    !(isWalkwayConnection(level.getBlockState(pos.relative(side)))
                       || isElevatedStairLanding(level,pos,side)));
        }
        return state.setValue(LADDER_HATCH,
                level.getBlockState(pos.below()).getBlock() instanceof LadderBlock);
    }

    private static void scheduleElevatedStairRefresh(Level level,BlockPos at) {
        if(!(level instanceof ServerLevel server))return;
        for(Direction side:Direction.Plane.HORIZONTAL){
            BlockPos candidate=at.relative(side).below();
            BlockState stair=server.getBlockState(candidate);
            if(stair.getBlock() instanceof RackStairsBlock&&stair.getValue(RackStairsBlock.PART)==8)
                server.scheduleTick(candidate,stair.getBlock(),1);
        }
    }

    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,
            BlockState oldState,boolean moved){
        super.onPlace(state,level,pos,oldState,moved);
        if(!level.isClientSide())scheduleElevatedStairRefresh(level,pos);
    }

    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,
            BlockState nextState,boolean moved){
        super.onRemove(state,level,pos,nextState,moved);
        if(!level.isClientSide())scheduleElevatedStairRefresh(level,pos);
    }

    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,
            RandomSource random){
        BlockState corrected=connectionState(state,level,pos);
        if(!corrected.equals(state))level.setBlock(pos,corrected,3);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState().setValue(FACING,
                context.getHorizontalDirection().getOpposite());
        return connectionState(state, context.getLevel(), pos);
    }

    @Override protected BlockState updateShape(
            BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN || direction.getAxis().isHorizontal())
            return connectionState(state,level,pos);
        return state;
    }

    private static VoxelShape shape(BlockState state) {
        if (state.getValue(FOLDED)) return STOWED;
        VoxelShape result = state.getValue(LADDER_HATCH) ? HATCH_DECK : DECK;
        if (state.getValue(RAIL_NORTH)) result = Shapes.or(result, NORTH);
        if (state.getValue(RAIL_SOUTH)) result = Shapes.or(result, SOUTH);
        if (state.getValue(RAIL_WEST)) result = Shapes.or(result, WEST);
        if (state.getValue(RAIL_EAST)) result = Shapes.or(result, EAST);
        return result;
    }

    @Override protected VoxelShape getShape(BlockState state, BlockGetter world,
            BlockPos pos, CollisionContext context) {
        return shape(state);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state,
            BlockGetter world, BlockPos pos, CollisionContext context) {
        // Pallets pass through folded scaffolding; its tiny stowed cassette
        // remains selectable but has no collision in the loading aisle.
        return state.getValue(FOLDED) ? Shapes.empty() : shape(state);
    }

    public static boolean occupied(Level world, BlockPos pos) {
        return !world.getEntitiesOfClass(Entity.class,
                new AABB(pos.getX(), pos.getY(), pos.getZ(),
                        pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1),
                Entity::isAlive).isEmpty();
    }

    public static boolean setFolded(Level world, BlockPos pos, boolean folded) {
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof RackScaffoldingBlock)) return false;
        if (state.getValue(FOLDED) == folded) return true;
        if (occupied(world, pos)) return false;
        world.setBlock(pos, state.setValue(FOLDED, folded), 3);
        scheduleElevatedStairRefresh(world,pos);
        return true;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            if (!setFolded(world, pos, !state.getValue(FOLDED)))
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Scaffolding occupied — wait until the walkway is clear."));
        }
        return InteractionResult.SUCCESS;
    }
}
