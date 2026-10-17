package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One 2x2 footprint, up to 12 FULL-WIDTH pallets vertically (5/16 blocks
 * per deck). Each of four height layers contains four linked footprint cells.
 * The bottom northwest cell is the ONLY 12-slot inventory and renderer.
 */
public final class StackOfPalletsBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
    // Height is retained for existing worlds; new collisions use the actual
    // ItemStacks in the shared controller, not a coarse 4-step approximation.
    public static final IntegerProperty HEIGHT=IntegerProperty.create("height",0,4);
    public static final IntegerProperty LAYER=IntegerProperty.create("layer",0,3);
    public static final int MAX_PALLETS=12;
    public static final int PALLET_THICKNESS_PIXELS=5; // actual pallet block is 5/16 high
    private final MapCodec<StackOfPalletsBlock> codec=MapCodec.unit(this);

    public StackOfPalletsBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(PART,0).setValue(LAYER,0).setValue(HEIGHT,0));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(PART,LAYER,HEIGHT);
    }
    public static BlockPos cell(BlockPos origin,int part,int layer){
        return origin.offset(part%2,layer,part/2);
    }
    public static BlockPos root(BlockPos cell,BlockState state){
        int part=state.getValue(PART);
        int layer=state.getValue(LAYER);
        return cell.offset(-(part%2),-layer,-part/2);
    }
    public static @Nullable StackOfPalletsBlockEntity controller(BlockGetter world,BlockPos cell){
        BlockState state=world.getBlockState(cell);
        if(!(state.getBlock() instanceof StackOfPalletsBlock))return null;
        BlockPos base=root(cell,state);
        if(world.getBlockState(base).getBlock()!=state.getBlock())return null;
        return world.getBlockEntity(base) instanceof StackOfPalletsBlockEntity be?be:null;
    }
    private static boolean fits(Level level,BlockPos base){
        // Reserve collision/interaction cells up to the twelfth deck. This
        // keeps a tall stack solid even if a worker walks beside its top.
        for(int layer=0;layer<4;layer++)for(int part=0;part<4;part++){
            BlockPos pos=cell(base,part,layer);
            if(level.isOutsideBuildHeight(pos)
                    ||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.hasChunkAt(pos)
                    ||!level.getBlockState(pos).canBeReplaced()
                    ||RackBlock.reservedForAccess(level,pos))return false;
        }
        return true;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return fits(context.getLevel(),context.getClickedPos())
                ?defaultBlockState():null;
    }
    @Override protected RenderShape getRenderShape(BlockState state){
        // 12 per-slot pallets, drawn full-size from the controller BER.
        return RenderShape.INVISIBLE;
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return state.getValue(PART)==0&&state.getValue(LAYER)==0
                ?new StackOfPalletsBlockEntity(pos,state):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.isClientSide())return;
        for(int layer=0;layer<4;layer++)for(int part=0;part<4;part++){
            if(layer==0&&part==0)continue;
            level.setBlock(cell(pos,part,layer),
                    state.setValue(PART,part).setValue(LAYER,layer),3);
        }
        if(level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity be)
            be.loadMagazine(stack);
    }
    /** Repair old 2x2 saved magazines when their previously unused
     * overhead is free. Never replace somebody else's actual blocks. */
    public static void repairUpperLayers(Level world,BlockPos root){
        if(world.isClientSide()||!world.hasChunkAt(root))return;
        BlockState state=world.getBlockState(root);
        if(!(state.getBlock() instanceof StackOfPalletsBlock)
                ||state.getValue(PART)!=0||state.getValue(LAYER)!=0)return;
        for(int layer=0;layer<4;layer++)for(int part=0;part<4;part++){
            if(layer==0&&part==0)continue;
            BlockPos pos=cell(root,part,layer);
            if(!world.hasChunkAt(pos))continue;
            BlockState found=world.getBlockState(pos);
            if(found.is(state.getBlock())){
                // Never hijack another inventory's tiles in a crowded area.
                if(!root(pos,found).equals(root))continue;
                continue;
            }
            if(found.canBeReplaced()&&!RackBlock.reservedForAccess(world,pos))
                world.setBlock(pos,state.setValue(PART,part)
                        .setValue(LAYER,layer),3);
        }
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!StackOfPalletsItem.isPallet(held))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        StackOfPalletsBlockEntity magazine=controller(level,pos);
        if(magazine!=null){
            if(magazine.count()>=MAX_PALLETS)
                player.sendSystemMessage(Component.literal("Pallet stack full (12/12)."));
            else if(magazine.put(held.copyWithCount(1))){
                if(!player.getAbilities().instabuild)held.shrink(1);
                player.sendSystemMessage(Component.literal(
                        "Added pallet ("+magazine.count()+"/12)."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        StackOfPalletsBlockEntity magazine=controller(level,pos);
        if(magazine==null)return InteractionResult.PASS;
        if(player.isShiftKeyDown()){
            // Inventory remains accessible without obstructing normal
            // empty-hand pickup. Any of the 2x2x4 pieces can open it.
            if(player instanceof ServerPlayer server)server.openMenu(magazine);
        }else{
            ItemStack taken=magazine.take();
            if(!taken.isEmpty()){
                // Vanilla places it in inventory, or safely drops it next
                // to the player when their inventory is full.
                player.getInventory().placeItemBackInInventory(taken);
                player.sendSystemMessage(Component.literal(
                        "Removed top pallet ("+magazine.count()+"/12 remain)."));
            }else player.sendSystemMessage(Component.literal("No pallets in this stack."));
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()){
            BlockPos origin=root(pos,state);
            StackOfPalletsBlockEntity magazine=controller(level,pos);
            if(magazine!=null&&!player.getAbilities().instabuild)
                Block.popResource(level,origin,magazine.asItem());
            // All 16 cells represent ONE item; never drop a second copy.
            for(int layer=0;layer<4;layer++)for(int part=0;part<4;part++){
                BlockPos other=cell(origin,part,layer);
                if(other.equals(pos)||!level.hasChunkAt(other))continue;
                BlockState found=level.getBlockState(other);
                if(found.is(this)&&found.getValue(PART)==part
                        &&found.getValue(LAYER)==layer)
                    level.removeBlock(other,false);
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        return List.of();
    }
    private static int heightAt(BlockGetter world,BlockPos pos){
        StackOfPalletsBlockEntity be=controller(world,pos);
        return be==null?0:be.count()*PALLET_THICKNESS_PIXELS;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext context){
        BlockPos origin=root(pos,state);
        int layer=state.getValue(LAYER);
        int remaining=heightAt(world,pos)-layer*16;
        if(remaining<=0)return layer==0
                ?Block.box((origin.getX()-pos.getX())*16,0,
                        (origin.getZ()-pos.getZ())*16,
                        (origin.getX()-pos.getX()+2)*16,1,
                        (origin.getZ()-pos.getZ()+2)*16)
                :Shapes.empty();
        return Block.box((origin.getX()-pos.getX())*16,0,
                (origin.getZ()-pos.getZ())*16,
                (origin.getX()-pos.getX()+2)*16,Math.min(16,remaining),
                (origin.getZ()-pos.getZ()+2)*16);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext context){
        int remaining=heightAt(world,pos)-state.getValue(LAYER)*16;
        return remaining<=0?Shapes.empty()
                :Block.box(0,0,0,16,Math.min(16,remaining),16);
    }
}
