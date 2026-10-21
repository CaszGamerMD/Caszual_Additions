package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.storage.loot.LootParams;
import java.util.List;
import net.minecraft.world.item.ItemStack;

/** Painted 2-dimensional industrial no-parking obstruction marker. Physical
 * collision is empty so forklift can rest here. Not replaceable by blocks. */
public final class ForkliftParkingBlock extends Block {
    public static final MapCodec<ForkliftParkingBlock> CODEC=
            simpleCodec(ForkliftParkingBlock::new);
    public ForkliftParkingBlock(BlockBehaviour.Properties props){super(props);}
    @Override protected MapCodec<? extends Block> codec(){return CODEC;}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return Block.box(0,0,0,16,0.5,16);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,
            BlockPos pos,CollisionContext context){
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
    @Override protected List<ItemStack> getDrops(BlockState s,LootParams.Builder b){
        return List.of();
    }
}
