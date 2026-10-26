package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** Two-high Shulker Station: 64 empty-box magazine, three simultaneous
 * packing positions, and adapter-only export of completed boxes to a pallet. */
public final class ShulkerTankBlock extends Block implements EntityBlock {
    public static final EnumProperty<DoubleBlockHalf> HALF=
            EnumProperty.create("half",DoubleBlockHalf.class);
    private final MapCodec<ShulkerTankBlock> codec=MapCodec.unit(this);
    public ShulkerTankBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(HALF,DoubleBlockHalf.LOWER));
    }
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(HALF);
    }
    public static BlockPos root(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos;
    }
    /** Both faces of the tall station resolve to ONE lower inventory.
     * Repair older top-half block entities without duplicating or deleting
     * their cargo, including saves containing two LOWER halves stacked. */
    public static @Nullable ShulkerTankEntity controller(Level level,BlockPos pos) {
        BlockState state=level.getBlockState(pos);
        if(!(state.getBlock() instanceof ShulkerTankBlock))return null;
        BlockPos base=root(pos,state);
        if(state.getValue(HALF)==DoubleBlockHalf.LOWER
                &&level.getBlockState(pos.below()).getBlock() instanceof ShulkerTankBlock
                &&level.getBlockState(pos.below()).getValue(HALF)==DoubleBlockHalf.LOWER){
            base=pos.below();
            // Do not rewrite a legacy upper block before its old BE has
            // been transferred, or its second saved inventory could vanish.
        }
        if(!(level.getBlockEntity(base) instanceof ShulkerTankEntity station))
            return null;
        // A top BE is never a valid second container. Move any real saved
        // contents into the lower BE and safely drop overflow (not overwrite).
        BlockPos top=base.above();
        if(level instanceof ServerLevel server
                &&server.getBlockState(top).getBlock() instanceof ShulkerTankBlock
                &&server.getBlockEntity(top) instanceof ShulkerTankEntity extra
                &&extra!=station){
            for(ItemStack overflow:station.absorbLegacyTop(extra))
                Block.popResource(server,base,overflow);
            server.removeBlockEntity(top);
            BlockState upper=server.getBlockState(top);
            if(upper.getValue(HALF)!=DoubleBlockHalf.UPPER)
                server.setBlock(top,upper.setValue(HALF,DoubleBlockHalf.UPPER),3);
        }
        if(level instanceof ServerLevel server&&!base.equals(pos)
                &&server.getBlockState(pos).getBlock() instanceof ShulkerTankBlock
                &&server.getBlockState(pos).getValue(HALF)!=DoubleBlockHalf.UPPER)
            server.setBlock(pos,server.getBlockState(pos)
                    .setValue(HALF,DoubleBlockHalf.UPPER),3);
        return station;
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos upper=context.getClickedPos().above();
        return context.getLevel().isOutsideBuildHeight(upper)
                || !context.getLevel().getBlockState(upper).canBeReplaced()
                || RackBlock.reservedForAccess(context.getLevel(),context.getClickedPos())
                || RackBlock.reservedForAccess(context.getLevel(),upper)
                ?null:defaultBlockState();
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        level.setBlock(pos.above(),state.setValue(HALF,DoubleBlockHalf.UPPER),3);
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER
                ?new ShulkerTankEntity(pos,state):null;
    }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,BlockState state,BlockEntityType<T> type){
        if(level.isClientSide()||state.getValue(HALF)!=DoubleBlockHalf.LOWER)
            return null;
        return (world,pos,block,entity)->{
            if(world instanceof ServerLevel server
                    &&entity instanceof ShulkerTankEntity station
                    &&server.getGameTime()%20==0)station.flushFinished(server);
        };
    }
    @Override protected InteractionResult useItemOn(ItemStack item, BlockState state,Level level,
            BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit) {
        if(!ShulkerTankEntity.isEmptyShulker(item)){
            if(level instanceof ServerLevel &&player instanceof ServerPlayer server){
                ShulkerTankEntity station=controller(level,pos);
                if(station!=null)server.openMenu(station);
            }
            return InteractionResult.SUCCESS;
        }
        if(level.isClientSide())return InteractionResult.SUCCESS;
        ShulkerTankEntity tank=controller(level,pos);
        if(tank!=null && tank.add(item.copyWithCount(1))) {
            if(!player.getAbilities().instabuild)item.shrink(1);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "Shulker Station: "+tank.count()+"/64 empty boxes, "
                    +tank.activeCount()+"/3 packing"));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit) {
        if(!level.isClientSide()){
            ShulkerTankEntity tank=controller(level,pos);
            if(tank!=null) {
                if(player.isShiftKeyDown()) {
                    ItemStack result=tank.take();
                    if(!result.isEmpty())player.getInventory().placeItemBackInInventory(result);
                }
                if(player instanceof ServerPlayer serverPlayer)
                    serverPlayer.openMenu(tank);
            }
        }
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide()){
            BlockPos root=root(pos,state);
            ShulkerTankEntity tank=level.getBlockEntity(root) instanceof ShulkerTankEntity t?t:null;
            if(tank!=null&&!player.getAbilities().instabuild){
                Containers.dropContents(level,root,tank);
                for(ItemStack box:tank.takePackedOnBreak())
                    Block.popResource(level,root,box);
                Block.popResource(level,root,new ItemStack(this));
            }
            BlockPos other=state.getValue(HALF)==DoubleBlockHalf.UPPER?pos.below():pos.above();
            if(level.getBlockState(other).is(this))level.removeBlock(other,false);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){return List.of();}
}
