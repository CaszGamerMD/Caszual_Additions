package com.caszgamermd.caszualadditions.curtains;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
    private static final VoxelShape NORTH_SOUTH=box(0,0,7,16,16,9);
    private static final VoxelShape EAST_WEST=box(7,0,0,9,16,16);
    public PlasticCurtainsBlock(BlockSetType type,BlockBehaviour.Properties props){
        super(type,props);
    }
    private static VoxelShape sheet(BlockState state){
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
    private static boolean hostile(Entity entity){
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
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        // A curtain never swings open like a hinged wooden door.
        return InteractionResult.PASS;
    }
}
