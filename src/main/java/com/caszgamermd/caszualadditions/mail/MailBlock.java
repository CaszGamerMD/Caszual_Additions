package com.caszgamermd.caszualadditions.mail;

import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** The physical blue collection mailbox or personal shared PO Box access.
 * No items live in the block: PO Boxes are per-player world SavedData and
 * outgoing mail stays in the sending player's private open menu.
 */
public final class MailBlock extends Block {
    private static final MapCodec<MailBlock> MAILBOX_CODEC=
            simpleCodec(props->new MailBlock(true,props));
    private static final MapCodec<MailBlock> BOX_CODEC=
            simpleCodec(props->new MailBlock(false,props));
    private final boolean outgoing;
    public MailBlock(boolean outgoing,BlockBehaviour.Properties props){
        super(props);
        this.outgoing=outgoing;
    }
    @Override protected MapCodec<? extends Block> codec(){
        return outgoing?MAILBOX_CODEC:BOX_CODEC;
    }
    private InteractionResult open(Level level,BlockPos pos,Player player){
        if(!(player instanceof ServerPlayer serverPlayer))return InteractionResult.SUCCESS;
        MailSavedData.get(serverPlayer.getServer()).remember(serverPlayer);
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
