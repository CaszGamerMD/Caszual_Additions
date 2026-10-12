package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * One click on a rack SIDE chooses the nearest horizontal top corner and
 * runs exactly one slim beam along that corner. The beam is anchored at
 * rack-top height even when the player clicks a lower frame segment.
 *
 * One connector item builds the entire 1-4 block span if another rack is
 * reachable with an unobstructed horizontal line.
 */
public final class RackConnectorItem extends BlockItem {
    public RackConnectorItem(Block block,Properties properties){super(block,properties);}

    @Override public InteractionResult useOn(UseOnContext context){
        Level level=context.getLevel();
        RackBlockEntity source=level.hasChunkAt(context.getClickedPos())
                ?RackBlock.controller(level,context.getClickedPos()):null;
        if(source==null)return super.useOn(context);

        Direction direction=context.getClickedFace();
        Player player=context.getPlayer();
        if(!direction.getAxis().isHorizontal()){
            if(player!=null&&!level.isClientSide())
                player.sendSystemMessage(Component.literal(
                        "Click a side of the rack, close to the top corner you want to connect."));
            return InteractionResult.FAIL;
        }

        BlockPos root=source.getBlockPos();
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        int maxX=Integer.MIN_VALUE,maxZ=Integer.MIN_VALUE;
        for(int part=0;part<4;part++){
            BlockPos cell=RackBlock.cell(root,source.facing(),part);
            minX=Math.min(minX,cell.getX());
            maxX=Math.max(maxX,cell.getX());
            minZ=Math.min(minZ,cell.getZ());
            maxZ=Math.max(maxZ,cell.getZ());
        }
        // Choose ONE of the two corners on the clicked rack side.
        int x,z;
        boolean positive;
        if(direction.getAxis()==Direction.Axis.X){
            z=context.getClickLocation().z()<minZ+1?minZ:maxZ;
            x=direction==Direction.EAST?maxX+1:minX-1;
            positive=z==maxZ;
        }else{
            x=context.getClickLocation().x()<minX+1?minX:maxX;
            z=direction==Direction.SOUTH?maxZ+1:minZ-1;
            positive=x==maxX;
        }
        BlockPos first=new BlockPos(x,root.getY()+2,z);
        BlockState rod=RackContent.CONNECTOR_BLOCK.defaultBlockState()
                .setValue(RackConnectorBlock.FACING,direction)
                .setValue(RackConnectorBlock.CORNER_POSITIVE,positive);
        int span=1;
        boolean targetFound=false;
        for(int step=0;step<=RackNetwork.MAX_CONNECTOR_GAP;step++){
            BlockPos at=first.relative(direction,step);
            if(!level.hasChunkAt(at))return InteractionResult.FAIL;
            RackBlockEntity target=RackBlock.controller(level,at);
            if(target!=null){
                if(target.getBlockPos().equals(root))return InteractionResult.FAIL;
                if(step==0)return InteractionResult.FAIL; // touching rack: no beam necessary
                span=step;
                targetFound=true;
                break;
            }
            BlockState existing=level.getBlockState(at);
            if(existing.getBlock() instanceof RackConnectorBlock
                    &&existing.getValue(RackConnectorBlock.FACING).getAxis()
                            ==direction.getAxis()
                    &&existing.getValue(RackConnectorBlock.CORNER_POSITIVE)==positive)
                continue;
            if(!existing.canBeReplaced())break;
        }
        // If there is no rack at the far end, place one starter segment
        // at the chosen top corner; subsequent parts can be extended.
        if(!targetFound)span=1;
        boolean placed=false;
        for(int step=0;step<span;step++){
            BlockPos at=first.relative(direction,step);
            if(level.isOutsideBuildHeight(at)||!level.getWorldBorder().isWithinBounds(at)
                    ||!level.hasChunkAt(at))return InteractionResult.FAIL;
            BlockState current=level.getBlockState(at);
            if(current.is(rod.getBlock())
                    &&current.getValue(RackConnectorBlock.FACING).getAxis()
                            ==direction.getAxis()
                    &&current.getValue(RackConnectorBlock.CORNER_POSITIVE)==positive)
                continue;
            if(!current.canBeReplaced())return InteractionResult.FAIL;
            placed=true;
        }
        if(!placed)return InteractionResult.SUCCESS;
        if(!level.isClientSide()){
            for(int step=0;step<span;step++){
                BlockPos at=first.relative(direction,step);
                if(level.getBlockState(at).canBeReplaced())level.setBlock(at,rod,3);
            }
            ItemStack held=context.getItemInHand();
            if(player==null||!player.getAbilities().instabuild)held.shrink(1);
            if(player!=null)player.sendSystemMessage(Component.literal(
                    targetFound?"Connected racks with "+span+" top-corner rod section(s)."
                            :"Placed one top-corner rod. Connect another rack along this line."));
        }
        return InteractionResult.SUCCESS;
    }
}
