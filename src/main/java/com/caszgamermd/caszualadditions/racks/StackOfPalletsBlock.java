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
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Physically occupies 2x2 whole ground blocks. A single controller at part 0
 * owns all twelve original pallet ItemStacks; three satellite parts have no
 * block entities or drops. Render one accurate pallet per inventory slot.
 */
public final class StackOfPalletsBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,3);
    // Keep the old height property so existing one-block worlds load without
    // invalid blockstates. The actual 12-pallet artwork is now dynamic.
    public static final IntegerProperty HEIGHT=IntegerProperty.create("height",0,4);
    private final MapCodec<StackOfPalletsBlock> codec=MapCodec.unit(this);
    public StackOfPalletsBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART,0).setValue(HEIGHT,0));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){
        b.add(PART,HEIGHT);
    }
    public static BlockPos cell(BlockPos root,int part){
        return root.offset(part%2,0,part/2);
    }
    public static BlockPos root(BlockPos cell,BlockState state){
        int part=state.getValue(PART);
        return cell.offset(-(part%2),0,-part/2);
    }
    public static @Nullable StackOfPalletsBlockEntity controller(Level level,BlockPos cell){
        if(!level.hasChunkAt(cell))return null;
        BlockState state=level.getBlockState(cell);
        if(!(state.getBlock() instanceof StackOfPalletsBlock))return null;
        BlockPos root=root(cell,state);
        return level.hasChunkAt(root)
                &&level.getBlockEntity(root) instanceof StackOfPalletsBlockEntity be?be:null;
    }
    private static boolean fits(Level level,BlockPos origin){
        for(int part=0;part<4;part++){
            BlockPos cell=cell(origin,part);
            if(level.isOutsideBuildHeight(cell)
                    ||!level.getWorldBorder().isWithinBounds(cell)
                    ||!level.hasChunkAt(cell)
                    ||!level.getBlockState(cell).canBeReplaced()
                    ||RackBlock.reservedForAccess(level,cell))return false;
        }
        return true;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return fits(context.getLevel(),context.getClickedPos())
                ?defaultBlockState():null;
    }
    @Override protected RenderShape getRenderShape(BlockState state){
        return RenderShape.INVISIBLE; // All 12 distinct pallets drawn by one BER.
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return state.getValue(PART)==0?new StackOfPalletsBlockEntity(pos,state):null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(level,pos,state,placer,stack);
        if(level.isClientSide())return;
        // The whole structure is checked before item placement. No satellites
        // create a second inventory and no old pallet item data is discarded.
        for(int part=1;part<4;part++)
            level.setBlock(cell(pos,part),state.setValue(PART,part),3);
        if(level.getBlockEntity(pos) instanceof StackOfPalletsBlockEntity magazine)
            magazine.loadMagazine(stack);
    }
    @Override protected InteractionResult useItemOn(ItemStack held,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        if(!StackOfPalletsItem.isPallet(held))return InteractionResult.PASS;
        if(level.isClientSide())return InteractionResult.SUCCESS;
        StackOfPalletsBlockEntity magazine=controller(level,pos);
        if(magazine!=null){
            if(magazine.count()>=12)
                player.sendSystemMessage(Component.literal("Pallet stack full (12/12)."));
            else if(magazine.put(held.copyWithCount(1))){
                if(!player.getAbilities().instabuild)held.shrink(1);
                player.sendSystemMessage(Component.literal(
                        "Stored pallet ("+magazine.count()+"/12)."));
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        StackOfPalletsBlockEntity magazine=controller(level,pos);
        if(magazine!=null){
            if(player.isShiftKeyDown()){
                ItemStack next=magazine.take();
                if(!next.isEmpty())player.getInventory().placeItemBackInInventory(next);
                player.sendSystemMessage(Component.literal(
                        "Pallet stack: "+magazine.count()+"/12 remaining."));
            }else if(player instanceof ServerPlayer server)server.openMenu(magazine);
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,
            BlockState state,Player player){
        if(!level.isClientSide()){
            BlockPos origin=root(pos,state);
            StackOfPalletsBlockEntity magazine=controller(level,pos);
            // Breaking ANY one quarter breaks the whole magazine. Only the
            // root inventory can create the one fully data-bearing drop.
            if(magazine!=null&&!player.getAbilities().instabuild)
                Block.popResource(level,origin,magazine.asItem());
            for(int part=0;part<4;part++){
                BlockPos other=cell(origin,part);
                if(other.equals(pos)||!level.hasChunkAt(other))continue;
                BlockState found=level.getBlockState(other);
                if(found.is(this)&&found.getValue(PART)==part)
                    level.removeBlock(other,false);
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        return List.of(); // The controller drops one magazine in playerWillDestroy.
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext context){
        BlockPos origin=root(pos,state);
        return Block.box((origin.getX()-pos.getX())*16.0,0,
                (origin.getZ()-pos.getZ())*16.0,
                (origin.getX()+2-pos.getX())*16.0,15,
                (origin.getZ()+2-pos.getZ())*16.0);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter world,
            BlockPos pos,CollisionContext context){
        // The occupied 2x2 footprint blocks carts/robots as a single crate.
        return Block.box(0,0,0,16,15,16);
    }
}
