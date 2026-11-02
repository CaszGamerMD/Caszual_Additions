package com.caszgamermd.caszualadditions.enchantprinter;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

public final class EnchantmentPrinterBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<EnchantmentPrinterBlock> CODEC=simpleCodec(EnchantmentPrinterBlock::new);
    public EnchantmentPrinterBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<EnchantmentPrinterBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos,BlockState state){
        return new EnchantmentPrinterBlockEntity(pos,state);
    }
    private static InteractionResult open(Level level,BlockPos pos,Player player){
        if(level.isClientSide())return InteractionResult.SUCCESS;
        if(level.getBlockEntity(pos) instanceof EnchantmentPrinterBlockEntity machine
                && player instanceof ServerPlayer server)server.openMenu(machine);
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,Level level,
            BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return open(level,pos,player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,
            Player player,BlockHitResult hit){
        return open(level,pos,player);
    }
    /**
     * Vanilla loot machinery creates the ONE machine item. We attach only the
     * library/preset to that existing drop; the four stocked slots continue
     * to drop normally via playerWillDestroy, with no duplication.
     *
     * This also preserves the library for normal loot-producing destruction,
     * including non-player break paths that invoke the block's loot table.
     */
    @Override protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder){
        List<ItemStack> drops=super.getDrops(state,builder);
        BlockEntity blockEntity=builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if(blockEntity instanceof EnchantmentPrinterBlockEntity printer){
            long now=printer.getLevel()==null?0:printer.getLevel().getGameTime();
            for(ItemStack item:drops)
                if(item.is(EnchantmentPrinterContent.ITEM))
                    printer.saveLibraryToItem(item,now);
        }
        return drops;
    }
    /** Restore the saved library when this item is actually placed. */
    @Override public void setPlacedBy(Level world,BlockPos pos,BlockState state,
            @Nullable LivingEntity placer,ItemStack stack){
        super.setPlacedBy(world,pos,state,placer,stack);
        if(!world.isClientSide()
                && world.getBlockEntity(pos) instanceof EnchantmentPrinterBlockEntity printer)
            printer.restoreLibraryFromItem(stack,world.getGameTime());
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide()&&!player.getAbilities().instabuild
                && level.getBlockEntity(pos) instanceof EnchantmentPrinterBlockEntity machine)
            Containers.dropContents(level,pos,machine);
        return super.playerWillDestroy(level,pos,state,player);
    }
}
