package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A compact optical scanner mounted alongside any completed 3x3 PTP edge.
 * Scans real pallet contents, including sealed pallets, without extraction.
 */
public final class StorageScannerBlock extends Block {
    private final MapCodec<StorageScannerBlock> scannerCodec=MapCodec.unit(this);
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,14,16);
    public StorageScannerBlock(BlockBehaviour.Properties properties){super(properties);}
    @Override protected MapCodec<? extends Block> codec(){return scannerCodec;}
    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context){
        return RackBlock.reservedForAccess(context.getLevel(),context.getClickedPos())
                ?null:super.getStateForPlacement(context);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,
            BlockPos pos,Player player,BlockHitResult hit){
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        if(player.isShiftKeyDown()){
            player.sendSystemMessage(Component.literal(StoragePalletHaul.request(server,pos)));
            return InteractionResult.SUCCESS;
        }
        StorageScanner.Report result=StorageScanner.scan(server,pos);
        if(result==null){
            player.sendSystemMessage(Component.literal(
                    "Storage Scanner: place it touching a completed 3x3 Pallet Transfer Point."));
            return InteractionResult.SUCCESS;
        }
        player.sendSystemMessage(Component.literal(result.summary()));
        if(result.loaded()&&result.racks()==0)
            player.sendSystemMessage(Component.literal(
                    "No reachable Rack Computer network: inspection works, but capacity cannot be predicted."));
        return InteractionResult.SUCCESS;
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,
            CollisionContext context){return SHAPE;}
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){return SHAPE;}
}
