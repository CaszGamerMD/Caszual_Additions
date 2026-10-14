package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Reusable, handheld clear plastic stretch-wrap dispenser. It never extracts contents. */
public final class PortablePalletWrapperItem extends Item {
    public PortablePalletWrapperItem(Properties properties){super(properties);}

    @Override public InteractionResult useOn(UseOnContext context){
        Level level=context.getLevel();
        Player player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        BlockPos clicked=context.getClickedPos();
        if(level.getBlockState(clicked).getBlock() instanceof PalletBlock){
            var state=level.getBlockState(clicked);
            BlockPos root=PalletBlock.rootPos(clicked,state);
            if(!level.hasChunkAt(root))return InteractionResult.FAIL;
            if(!(level.getBlockEntity(root) instanceof PalletBlockEntity pallet))
                return InteractionResult.FAIL;
            if(level instanceof ServerLevel){
                pallet.setGlassWrapped(!pallet.glassWrapped());
                player.sendSystemMessage(Component.literal(pallet.glassWrapped()
                        ?"Pallet secured in clear plastic stretch wrap. Four Golems can now lift it."
                        :"Plastic stretch wrap removed."));
            }
            return InteractionResult.SUCCESS;
        }
        if(level.getBlockState(clicked).getBlock() instanceof RackBlock){
            RackBlockEntity rack=RackBlock.controller(level,clicked);
            if(rack==null||!rack.hasPallet())return InteractionResult.FAIL;
            if(level instanceof ServerLevel){
                rack.setGlassWrapped(!rack.glassWrapped());
                player.sendSystemMessage(Component.literal(
                        rack.glassWrapped()?"Rack pallet plastic-wrapped.":"Rack pallet unwrapped."));
            }
            return InteractionResult.SUCCESS;
        }
        if(level.getBlockState(clicked).getBlock() instanceof PtpBlock){
            PtpBlockEntity ptp=PtpBlock.controller(level,clicked);
            if(ptp==null||!ptp.hasPallet())return InteractionResult.FAIL;
            if(level instanceof ServerLevel){
                if(ptp.hauling()){
                    player.sendSystemMessage(Component.literal(
                            "Cannot change wrapping while a four-Golem haul is active."));
                    return InteractionResult.FAIL;
                }
                ptp.setSealed(!ptp.sealed());
                player.sendSystemMessage(Component.literal(
                        ptp.sealed()?"PTP pallet plastic-wrapped.":"PTP pallet unwrapped."));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
