package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Mobile industrial three-step high-access stair. Nine parts represent a
 * single 3-deep, 3-high assembly (one column wide). Each ascending part
 * offers two half-block treads, so players and robots walk up smoothly
 * without jumping onto a tall block. Safety rails stay on both sides.
 *
 * The home position is reset by every MANUAL player move; automated moving
 * never overwrites it. All physical movement is server-side and atomic:
 * cannot move underneath riders, into racks or through occupied pallets.
 */
public final class RackStairsBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,8);
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty RAIL_LEFT=BooleanProperty.create("rail_left");
    public static final BooleanProperty RAIL_RIGHT=BooleanProperty.create("rail_right");
    private final MapCodec<RackStairsBlock> codec=MapCodec.unit(this);
    public RackStairsBlock(BlockBehaviour.Properties p){
        super(p);
        registerDefaultState(stateDefinition.any().setValue(PART,0)
                .setValue(FACING,Direction.NORTH)
                .setValue(RAIL_LEFT,true).setValue(RAIL_RIGHT,true));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block,BlockState> builder){
        builder.add(PART,FACING,RAIL_LEFT,RAIL_RIGHT);
    }
    /** Rails are individual per footprint cell, keeping all nine pieces a single stair.
     *  Connected rack/scaffold edges remain open, including after a push or rotation. */
    private static boolean hasSideConnection(BlockState neighbor,int part,Direction facing){
        if(neighbor.getBlock() instanceof RackScaffoldingBlock
                ||neighbor.getBlock() instanceof RackBlock)return true;
        return neighbor.getBlock() instanceof RackStairsBlock
                &&neighbor.getValue(PART)==part&&neighbor.getValue(FACING)==facing;
    }
    private static BlockState withRails(BlockState state,BlockGetter level,BlockPos at){
        Direction facing=state.getValue(FACING);
        int part=state.getValue(PART);
        return state.setValue(RAIL_LEFT,!hasSideConnection(
                        level.getBlockState(at.relative(facing.getCounterClockWise())),part,facing))
                .setValue(RAIL_RIGHT,!hasSideConnection(
                        level.getBlockState(at.relative(facing.getClockWise())),part,facing));
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,
            ScheduledTickAccess ticks,BlockPos at,Direction side,BlockPos neighborPos,
            BlockState neighbor,RandomSource random){
        Direction facing=state.getValue(FACING);
        if(side==facing.getCounterClockWise())
            return state.setValue(RAIL_LEFT,!hasSideConnection(neighbor,state.getValue(PART),facing));
        if(side==facing.getClockWise())
            return state.setValue(RAIL_RIGHT,!hasSideConnection(neighbor,state.getValue(PART),facing));
        return state;
    }
    public static BlockPos cell(BlockPos root,Direction facing,int part){
        return root.relative(facing,part%3).above(part/3);
    }
    public static BlockPos root(BlockPos pos,BlockState state){
        int part=state.getValue(PART);
        return pos.relative(state.getValue(FACING).getOpposite(),part%3)
                .below(part/3);
    }
    private static boolean fits(Level world,BlockPos root,Direction facing,
            java.util.Set<BlockPos> previous){
        for(int i=0;i<9;i++){
            BlockPos cell=cell(root,facing,i);
            if(world.isOutsideBuildHeight(cell)||!world.getWorldBorder().isWithinBounds(cell)
                    ||!world.hasChunkAt(cell))return false;
            if(!previous.contains(cell)
                    &&!world.getBlockState(cell).canBeReplaced())return false;
        }
        if(!(world instanceof ServerLevel server))return false;
        // The root itself is part of the stair; its ground beneath must
        // be solid (but cannot be another pallet or rack).
        BlockPos below=root.below();
        var support=server.getBlockState(below);
        return !support.getCollisionShape(server,below).isEmpty()
                &&!(support.getBlock() instanceof RackBlock)
                &&!(support.getBlock() instanceof com.caszgamermd.caszualadditions.pallet.PalletBlock);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx){
        Direction face=ctx.getHorizontalDirection().getOpposite();
        BlockState initial=defaultBlockState().setValue(FACING,face);
        if(!(ctx.getLevel() instanceof ServerLevel level))
            return withRails(initial,ctx.getLevel(),ctx.getClickedPos());
        return fits(level,ctx.getClickedPos(),face,java.util.Set.of())
                ?withRails(initial,level,ctx.getClickedPos()):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity who,ItemStack item){
        super.setPlacedBy(level,pos,state,who,item);
        if(level.isClientSide())return;
        for(int i=1;i<9;i++){
            BlockPos at=cell(pos,state.getValue(FACING),i);
            level.setBlock(at,withRails(state.setValue(PART,i),level,at),3);
        }
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return state.getValue(PART)==0?new RackStairsBlockEntity(pos,state):null;
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level,BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        if(!(level instanceof ServerLevel)||state.getValue(PART)!=0
                ||type!=RackContent.RACK_STAIRS_ENTITY)return null;
        return (world,pos,block,entity)->RackStairsBlockEntity.tick(
                (ServerLevel)world,pos,(RackStairsBlockEntity)entity);
    }
    public static @Nullable RackStairsBlockEntity controller(Level level,BlockPos pos){
        BlockState state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof RackStairsBlock))return null;
        return level.getBlockEntity(root(pos,state)) instanceof RackStairsBlockEntity be
                ?be:null;
    }
    public static boolean occupied(Level world,BlockPos root,Direction face){
        for(int part=0;part<9;part++){
            BlockPos pos=cell(root,face,part);
            // Check the whole staircase and the open head room above
            // every tread, so no entity can be moved/crushed by the rig.
            if(!world.getEntitiesOfClass(Entity.class,
                    new AABB(pos).inflate(.10,.08,.10),Entity::isAlive).isEmpty())
                return true;
        }
        return false;
    }
    /**
     * All nine occupied cells belong to ONE selectable stair unit.
     * Keep the per-cell tread collision below for movement, but make
     * highlighting and breaking any section target the entire assembly.
     */
    private static VoxelShape assemblyOutline(BlockPos pos,BlockState state){
        BlockPos origin=root(pos,state);
        Direction face=state.getValue(FACING);
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        for(int part=0;part<3;part++){
            BlockPos at=cell(origin,face,part);
            minX=Math.min(minX,at.getX());
            minZ=Math.min(minZ,at.getZ());
        }
        int width=face.getAxis()==Direction.Axis.X?3:1;
        int depth=face.getAxis()==Direction.Axis.Z?3:1;
        return box(16.0*(minX-pos.getX()),
                16.0*(origin.getY()-pos.getY()),
                16.0*(minZ-pos.getZ()),
                16.0*(minX+width-pos.getX()),
                16.0*(origin.getY()+3-pos.getY()),
                16.0*(minZ+depth-pos.getZ()));
    }

    private static boolean validAssembly(Level world,BlockPos origin,Direction face){
        for(int i=0;i<9;i++){
            BlockState part=world.getBlockState(cell(origin,face,i));
            if(!part.is(RackContent.RACK_STAIRS)
                    ||part.getValue(PART)!=i
                    ||part.getValue(FACING)!=face)return false;
        }
        return true;
    }

    /**
     * Rotate the entire single-item stair assembly clockwise on its
     * existing root. The new footprint must be clear and rider-free;
     * rotation preserves its controller, saved location and assignments.
     */
    public static boolean rotate(ServerLevel world,BlockPos origin){
        BlockState original=world.getBlockState(origin);
        if(!(original.getBlock() instanceof RackStairsBlock)
                ||original.getValue(PART)!=0)return false;
        Direction face=original.getValue(FACING);
        Direction turned=face.getClockWise();
        RackStairsBlockEntity before=controller(world,origin);
        if(before==null||before.reserved()||!validAssembly(world,origin,face)
                ||occupied(world,origin,face)
                ||occupied(world,origin,turned))return false;
        java.util.Set<BlockPos> previous=new java.util.HashSet<>();
        for(int i=0;i<9;i++)previous.add(cell(origin,face,i));
        if(!fits(world,origin,turned,previous))return false;
        var snapshot=before.snapshot();
        for(int i=0;i<9;i++)
            world.setBlock(cell(origin,face,i),Blocks.AIR.defaultBlockState(),2);
        for(int i=0;i<9;i++){
            BlockPos at=cell(origin,turned,i);
            world.setBlock(at,withRails(original.setValue(FACING,turned).setValue(PART,i),world,at),3);
        }
        if(world.getBlockEntity(origin) instanceof RackStairsBlockEntity moved)
            moved.restore(snapshot,origin);
        return true;
    }

    /** Called from player pushing and escorted Golem motion. */
    public static boolean move(ServerLevel world,BlockPos oldRoot,
            BlockPos to,boolean byPlayer){
        BlockState first=world.getBlockState(oldRoot);
        if(!(first.getBlock() instanceof RackStairsBlock)
                ||first.getValue(PART)!=0)return false;
        Direction face=first.getValue(FACING);
        RackStairsBlockEntity controller=controller(world,oldRoot);
        if(controller==null||!validAssembly(world,oldRoot,face)
                ||(byPlayer&&controller.reserved()))return false;
        // Never roll the stairs under an existing rider OR into an
        // entity standing in the intended destination footprint.
        if(occupied(world,oldRoot,face)||occupied(world,to,face))return false;
        java.util.Set<BlockPos> previous=new java.util.HashSet<>();
        for(int i=0;i<9;i++)previous.add(cell(oldRoot,face,i));
        if(!fits(world,to,face,previous))return false;
        RackStairsBlockEntity old=world.getBlockEntity(oldRoot)
                instanceof RackStairsBlockEntity be?be:null;
        if(old==null)return false;
        var snapshot=old.snapshot();
        // Clear old parts WITHOUT issuing nine independent drop events.
        for(int i=0;i<9;i++)world.setBlock(cell(oldRoot,face,i),
                Blocks.AIR.defaultBlockState(),2);
        for(int i=0;i<9;i++){
            BlockPos at=cell(to,face,i);
            world.setBlock(at,withRails(first.setValue(PART,i),world,at),3);
        }
        if(world.getBlockEntity(to) instanceof RackStairsBlockEntity moved)
            moved.restore(snapshot,byPlayer?to:snapshot.home());
        return true;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()){
            BlockPos root=root(pos,state);
            for(int i=0;i<9;i++){
                BlockPos part=cell(root,state.getValue(FACING),i);
                if(!part.equals(pos)&&level.getBlockState(part).is(this))
                    level.removeBlock(part,false);
            }
            if(!player.getAbilities().instabuild)
                Block.popResource(level,root,new ItemStack(this));
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        return List.of();
    }
    @Override protected RenderShape getRenderShape(BlockState state){
        return RenderShape.MODEL;
    }
    /** Collision rails exactly follow the rendered left/right steel barriers.
     *  Rotating the staircase rotates the open sides as well. */
    private static VoxelShape sideShape(Direction facing,boolean left,boolean support){
        Direction side=left?facing.getCounterClockWise():facing.getClockWise();
        return switch(side){
            case WEST -> support?box(0,0,1,1,16,2):box(0,3,0,1,16,16);
            case EAST -> support?box(15,0,1,16,16,2):box(15,3,0,16,16,16);
            case NORTH -> support?box(1,0,0,2,16,1):box(0,3,0,16,16,1);
            case SOUTH -> support?box(1,0,15,2,16,16):box(0,3,15,16,16,16);
            default -> Shapes.empty();
        };
    }
    private static VoxelShape shape(BlockState state){
        int part=state.getValue(PART),run=part%3,level=part/3;
        if(level>run)return Shapes.empty();
        Direction facing=state.getValue(FACING);
        boolean support=level<run;
        VoxelShape result=Shapes.empty();
        if(!support){
            // Two half-block treads, rising toward the facing direction.
            result=switch(facing){
                case NORTH -> Shapes.or(box(0,0,8,16,8,16),box(0,0,0,16,16,8));
                case SOUTH -> Shapes.or(box(0,0,0,16,8,8),box(0,0,8,16,16,16));
                case EAST -> Shapes.or(box(0,0,0,8,8,16),box(8,0,0,16,16,16));
                case WEST -> Shapes.or(box(8,0,0,16,8,16),box(0,0,0,8,16,16));
                default -> Shapes.empty();
            };
        }
        if(state.getValue(RAIL_LEFT))
            result=Shapes.or(result,sideShape(facing,true,support));
        if(state.getValue(RAIL_RIGHT))
            result=Shapes.or(result,sideShape(facing,false,support));
        return result;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext ctx){return assemblyOutline(pos,state);}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext ctx){return shape(state);}
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        RackStairsBlockEntity be=controller(server,pos);
        if(be==null)return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()){
            if(!rotate(server,be.getBlockPos()))
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Rack Stairs cannot rotate: someone is using them or the new footprint is blocked."));
            else player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Rack Stairs rotated clockwise. Home position saved."));
        }else{
            BlockPos current=be.getBlockPos();
            if(!move(server,current,current.relative(player.getDirection()),true))
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Rack Stairs cannot move: someone is using them, or the path is blocked."));
        }
        return InteractionResult.SUCCESS;
    }
}
