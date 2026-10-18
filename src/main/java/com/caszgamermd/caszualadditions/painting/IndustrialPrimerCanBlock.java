package com.caszgamermd.caszualadditions.painting;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
public final class IndustrialPrimerCanBlock extends Block {
    private final MapCodec<IndustrialPrimerCanBlock> codec=MapCodec.unit(this);
    public IndustrialPrimerCanBlock(BlockBehaviour.Properties props){super(props);}
    @Override protected MapCodec<? extends Block> codec(){return codec;}
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(player instanceof ServerPlayer server){
            boolean stored=server.getInventory().add(IndustrialBrushItem.dipped(true,0xb0b5b9));
            server.sendSystemMessage(Component.literal(stored
                    ?"Picked up a reusable Industrial Primer Brush."
                    :"Inventory full. Free a slot first."),true);
        }
        return InteractionResult.SUCCESS;
    }
}