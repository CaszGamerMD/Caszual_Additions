package com.caszgamermd.caszualadditions.mail;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The physical blue collection mailbox or personal shared PO Box access.
 * No items live in the block: PO Boxes are per-player world SavedData and
 * outgoing mail stays in the sending player's private open menu.
 */
public final class MailBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape MAILBOX_SHAPE=Shapes.or(
            Block.box(5,0,5,11,7,11),Block.box(2,7,2,14,24,14));
    private static final VoxelShape PO_BOX_SHAPE=Block.box(1,0,1,15,16,15);
    private static final MapCodec<MailBlock> MAILBOX_CODEC=
            simpleCodec(props->new MailBlock(true,props));
    private static final MapCodec<MailBlock> BOX_CODEC=
            simpleCodec(props->new MailBlock(false,props));
    private final boolean outgoing;
    public MailBlock(boolean outgoing,BlockBehaviour.Properties props){
        super(props);
        this.outgoing=outgoing;
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH));
    }
    @Override protected MapCodec<? extends HorizontalDirectionalBlock> codec(){
        return outgoing?MAILBOX_CODEC:BOX_CODEC;
    }
    @Override protected void createBlockStateDefinition(
            StateDefinition.Builder<Block,BlockState> builder){
        builder.add(FACING);
    }
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return defaultBlockState().setValue(FACING,context.getHorizontalDirection().getOpposite());
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return outgoing?MAILBOX_SHAPE:PO_BOX_SHAPE;
    }
    private InteractionResult open(Level level,BlockPos pos,Player player){
        if(!(player instanceof ServerPlayer serverPlayer))return InteractionResult.SUCCESS;
        MailSavedData.get(serverPlayer.level().getServer()).remember(serverPlayer);
        serverPlayer.openMenu(new ExtendedMenuProvider<BlockPos>(){
            @Override public Component getDisplayName(){
                return Component.literal(outgoing?"USPS-style Mailbox":"Personal PO Box");
            }
            @Override public @Nullable AbstractContainerMenu createMenu(
                    int id,Inventory inventory,Player user){
                return new MailMenu(id,inventory,pos,outgoing);
            }
            @Override public BlockPos getScreenOpeningData(ServerPlayer user){return pos;}
        });
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack,BlockState state,
            Level level,BlockPos pos,Player player,InteractionHand hand,BlockHitResult hit){
        return open(level,pos,player);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,
            Level level,BlockPos pos,Player player,BlockHitResult hit){
        return open(level,pos,player);
    }
}
