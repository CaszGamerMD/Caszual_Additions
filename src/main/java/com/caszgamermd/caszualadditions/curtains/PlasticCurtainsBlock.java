package com.caszgamermd.caszualadditions.curtains;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Soft two-high strip-curtain panels. Unlike an ordinary door these don't
 * open or hinge out: players and peaceful creatures pass straight through
 * them, while hostile mobs physically collide with the strips.
 *
 * For generic ray traces and navigation (empty collision context), expose
 * the actual hanging strip surface as an opaque LOS/pathfinding barrier:
 * monsters cannot see a player directly across the curtain. A real
 * peaceful entity has no physical collision; passing through is seamless.
 *
 * Models have NO left/right frames so adjacent panels of the same facing
 * visually join into a single continuous warehouse curtain and top rail.
 */
public final class PlasticCurtainsBlock extends DoorBlock {
    public static final BooleanProperty TOP_RAIL=BooleanProperty.create("top_rail");
    private static final VoxelShape NORTH_SOUTH=box(0,0,7,16,16,9);
    private static final VoxelShape EAST_WEST=box(7,0,0,9,16,16);
    public PlasticCurtainsBlock(BlockSetType type,BlockBehaviour.Properties props){
        super(type,props);
        registerDefaultState(defaultBlockState().setValue(TOP_RAIL,true));
    }
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block,BlockState> builder){
        super.createBlockStateDefinition(builder);
        builder.add(TOP_RAIL);
    }
    @Override protected BlockState updateShape(BlockState state,LevelReader level,
            ScheduledTickAccess ticks,BlockPos pos,Direction direction,BlockPos neighborPos,
            BlockState neighbor,RandomSource random){
        BlockState result=super.updateShape(state,level,ticks,pos,direction,neighborPos,neighbor,random);
        if(result.is(this)&&result.getValue(HALF)==DoubleBlockHalf.UPPER&&direction==Direction.UP)
            result=result.setValue(TOP_RAIL,!(neighbor.getBlock() instanceof PlasticCurtainsExtensionBlock
                    &&neighbor.getValue(PlasticCurtainsExtensionBlock.FACING)==result.getValue(FACING)));
        return result;
    }
    static VoxelShape sheet(BlockState state){
        Direction facing=state.getValue(FACING);
        return facing.getAxis()==Direction.Axis.X?EAST_WEST:NORTH_SOUTH;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return sheet(state);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,
            BlockGetter level,BlockPos pos,CollisionContext context){
        if(context instanceof EntityCollisionContext entityContext){
            Entity entity=entityContext.getEntity();
            // Non-hostiles: players, passive mobs, pets, vehicles, and
            // loose items brush straight through the flexible strips.
            if(entity!=null&&!hostile(entity))return Shapes.empty();
        }
        // For monsters and path/LOS contexts the hanging panel acts as
        // a solid wall. A two-pixel plane is sufficient to stop traversal.
        return sheet(state);
    }
    static boolean hostile(Entity entity){
        return entity instanceof Enemy
                ||entity instanceof Mob mob
                    &&mob.getType().getCategory()==MobCategory.MONSTER;
    }
    @Override protected VoxelShape getVisualShape(BlockState state,
            BlockGetter level,BlockPos pos,CollisionContext context){
        // Vision rays should not pass through it even though it looks
        // translucent to the player. Sculk/ambient lighting is untouched.
        return sheet(state);
    }
    /**
     * Existing two-high curtain is unchanged. Right-click any of its panels
     * with a curtain item to add exactly ONE taller section. Each extension
     * costs one item and moves the header rail to the new uppermost tile.
     * Horizontal curtain strips still join seamlessly as before.
     */
    static InteractionResult addHeight(Level world,BlockPos clicked,Player player,ItemStack held){
        if(world.isClientSide())return InteractionResult.SUCCESS;
        BlockState start=world.getBlockState(clicked);
        if(!(start.getBlock() instanceof PlasticCurtainsBlock)
                &&!(start.getBlock() instanceof PlasticCurtainsExtensionBlock))
            return InteractionResult.FAIL;
        Direction facing=start.getValue(FACING);
        BlockPos top=clicked;
        if(start.getBlock() instanceof PlasticCurtainsBlock
                &&start.getValue(HALF)==DoubleBlockHalf.LOWER){
            top=top.above();
            BlockState upper=world.getBlockState(top);
            if(!(upper.getBlock() instanceof PlasticCurtainsBlock)
                    ||upper.getValue(HALF)!=DoubleBlockHalf.UPPER
                    ||upper.getValue(FACING)!=facing)return InteractionResult.FAIL;
        }
        // Find the cap of THIS column only. Its adjacent columns can have
        // different heights without affecting one another.
        while(!world.isOutsideBuildHeight(top.above())){
            BlockPos above=top.above();
            BlockState next=world.getBlockState(above);
            if(!(next.getBlock() instanceof PlasticCurtainsExtensionBlock)
                    ||next.getValue(PlasticCurtainsExtensionBlock.FACING)!=facing)break;
            top=above;
        }
        BlockPos target=top.above();
        if(world.isOutsideBuildHeight(target)
                ||!world.getWorldBorder().isWithinBounds(target)
                ||!world.hasChunkAt(target)
                ||!world.getBlockState(target).canBeReplaced()){
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Curtain cannot extend: there is no free space above."));
            return InteractionResult.SUCCESS;
        }
        BlockState added=PlasticCurtainsContent.EXTENSION.defaultBlockState()
                .setValue(PlasticCurtainsExtensionBlock.FACING,facing)
                .setValue(PlasticCurtainsExtensionBlock.TOP_RAIL,true);
        if(world.setBlock(target,added,3)){
            BlockState oldTop=world.getBlockState(top);
            if(oldTop.getBlock() instanceof PlasticCurtainsBlock)
                world.setBlock(top,oldTop.setValue(TOP_RAIL,false),3);
            else if(oldTop.getBlock() instanceof PlasticCurtainsExtensionBlock)
                world.setBlock(top,oldTop.setValue(PlasticCurtainsExtensionBlock.TOP_RAIL,false),3);
            if(!player.getAbilities().instabuild)held.shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level world,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(held.is(PlasticCurtainsContent.ITEM))
            return addHeight(world,pos,player,held);
        return InteractionResult.PASS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        // A curtain never swings open like a hinged wooden door.
        return InteractionResult.PASS;
    }
}
