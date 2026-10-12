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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import java.util.ArrayList;
import org.jspecify.annotations.Nullable;

/**
 * Physical 1-block section of an upper rack-frame corner rod. Runs horizontally along
 * its FACING axis; consecutive sections bridge up to four empty cells.
 * Each section has real collision, not just a rack-side render attachment.
 */
public final class RackConnectorBlock extends Block {
    public static final EnumProperty<Direction> FACING=BlockStateProperties.HORIZONTAL_FACING;
    /** Which of the rack's two top corners is used along this beam axis. */
    public static final BooleanProperty CORNER_POSITIVE=BooleanProperty.create("corner_positive");
    private final MapCodec<RackConnectorBlock> codec=MapCodec.unit(this);
    public RackConnectorBlock(BlockBehaviour.Properties props){
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH)
                .setValue(CORNER_POSITIVE,false));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(FACING,CORNER_POSITIVE);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        // Connectors intentionally span the rack approach lane, including
        // face-to-face aisle bridges. Other blocks still respect the lane.
        Direction side=context.getClickedFace();
        if(!side.getAxis().isHorizontal())side=context.getHorizontalDirection();
        double offset=side.getAxis()==Direction.Axis.X
                ?context.getClickLocation().z()-context.getClickedPos().getZ()
                :context.getClickLocation().x()-context.getClickedPos().getX();
        return defaultBlockState().setValue(FACING,side)
                .setValue(CORNER_POSITIVE,offset>=.5);
    }
    @Override public void setPlacedBy(net.minecraft.world.level.Level level,
            BlockPos pos,BlockState state,
            net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.isClientSide())return;
        Direction outward=state.getValue(FACING);
        RackBlockEntity source=RackBlock.controller(level,
                pos.relative(outward.getOpposite()));
        if(source==null)return;
        // Clicking a rack face places the first rod. If an opposite rack is
        // at most four clear sections away, fill the gap automatically.
        // One connector item is intentionally a complete bridging kit.
        for(int n=1;n<=RackNetwork.MAX_CONNECTOR_GAP;n++){
            BlockPos cell=pos.relative(outward,n);
            if(!level.hasChunkAt(cell))return;
            RackBlockEntity target=RackBlock.controller(level,cell);
            if(target!=null){
                if(target.getBlockPos().equals(source.getBlockPos()))return;
                for(int step=1;step<n;step++){
                    BlockPos mid=pos.relative(outward,step);
                    BlockState existing=level.getBlockState(mid);
                    if(existing.getBlock() instanceof RackConnectorBlock
                            &&existing.getValue(FACING).getAxis()==outward.getAxis()
                            &&existing.getValue(CORNER_POSITIVE)==state.getValue(CORNER_POSITIVE))
                        continue;
                    if(!existing.canBeReplaced())return;
                }
                for(int step=1;step<n;step++){
                    BlockPos mid=pos.relative(outward,step);
                    if(level.getBlockState(mid).canBeReplaced())
                        level.setBlock(mid,state,3);
                }
                return;
            }
            BlockState along=level.getBlockState(cell);
            if(!(along.getBlock() instanceof RackConnectorBlock
                    &&along.getValue(FACING).getAxis()==outward.getAxis()
                    &&along.getValue(CORNER_POSITIVE)==state.getValue(CORNER_POSITIVE))
                    &&!along.canBeReplaced())return;
        }
    }
    /**
     * A single connector kit places a continuous span. Every physical
     * segment belongs to that same logical connector; removing any segment
     * removes the entire directly contiguous beam and drops ONE kit.
     * Stop at a different corner/axis so unrelated rods are unaffected.
     */
    private static boolean sameBeam(BlockState original,BlockState adjacent){
        return adjacent.getBlock() instanceof RackConnectorBlock
            &&adjacent.getValue(FACING).getAxis()==original.getValue(FACING).getAxis()
            &&adjacent.getValue(CORNER_POSITIVE)==original.getValue(CORNER_POSITIVE);
    }
    private static List<BlockPos> beamParts(net.minecraft.world.level.Level level,
            BlockPos center,BlockState original){
        List<BlockPos> positions=new ArrayList<>();
        positions.add(center);
        Direction axis=original.getValue(FACING);
        for(Direction scan:new Direction[]{axis,axis.getOpposite()}){
            for(int step=1;step<=RackNetwork.MAX_CONNECTOR_GAP;step++){
                BlockPos candidate=center.relative(scan,step);
                if(!level.hasChunkAt(candidate)
                    ||!sameBeam(original,level.getBlockState(candidate)))break;
                positions.add(candidate);
            }
        }
        return positions;
    }
    // Avoid recursive cleanup while removing the other segments. This
    // block is a singleton, and all modification happens on the server tick.
    private boolean removingBeam;
    private void removeBeam(ServerLevel level,BlockPos pos,BlockState state,
            boolean drop){
        if(removingBeam)return;
        removingBeam=true;
        try{
            for(BlockPos other:beamParts(level,pos,state)){
                if(!other.equals(pos) && sameBeam(state,level.getBlockState(other)))
                    level.removeBlock(other,false);
            }
            if(drop)Block.popResource(level,pos,new ItemStack(this));
        }finally{
            removingBeam=false;
        }
    }
    @Override public BlockState playerWillDestroy(net.minecraft.world.level.Level level,
            BlockPos pos,BlockState state,Player player){
        if(level instanceof ServerLevel server)
            removeBeam(server,pos,state,!player.getAbilities().instabuild);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,
            ServerLevel level,BlockPos pos,boolean moved){
        if(!removingBeam)removeBeam(level,pos,state,false);
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder loot){
        return List.of(); // one drop from the beam owner, never per segment
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){
        // Single top-corner frame rod, not two center-mounted rods.
        // Collision is the same 2px cross-section as the rendered model.
        boolean positive=state.getValue(CORNER_POSITIVE);
        double start=positive?13.5:.5,end=positive?15.5:2.5;
        return state.getValue(FACING).getAxis()==Direction.Axis.X
                ?Block.box(0,14,start,16,16,end)
                :Block.box(start,14,0,end,16,16);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return getShape(state,level,pos,context);}
}
