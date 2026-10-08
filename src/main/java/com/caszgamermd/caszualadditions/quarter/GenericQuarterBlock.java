package com.caszgamermd.caszualadditions.quarter;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class GenericQuarterBlock extends Block implements EntityBlock {
    public static final MapCodec<GenericQuarterBlock> CODEC = simpleCodec(GenericQuarterBlock::new);

    public static final BooleanProperty NWD = BooleanProperty.create("nwd");
    public static final BooleanProperty NED = BooleanProperty.create("ned");
    public static final BooleanProperty SWD = BooleanProperty.create("swd");
    public static final BooleanProperty SED = BooleanProperty.create("sed");
    public static final BooleanProperty NWU = BooleanProperty.create("nwu");
    public static final BooleanProperty NEU = BooleanProperty.create("neu");
    public static final BooleanProperty SWU = BooleanProperty.create("swu");
    public static final BooleanProperty SEU = BooleanProperty.create("seu");
    public static final BooleanProperty[] CORNERS = {NWD, NED, SWD, SED, NWU, NEU, SWU, SEU};

    public GenericQuarterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CORNERS) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NWD, NED, SWD, SED, NWU, NEU, SWU, SEU);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    private static Vec3 relative(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Vec3 hit = context.getClickLocation();
        return new Vec3(hit.x - pos.getX(), hit.y - pos.getY(), hit.z - pos.getZ());
    }

    public static int cornerIndex(Vec3 relative) {
        boolean east = relative.x >= 0.5;
        boolean up = relative.y >= 0.5;
        boolean south = relative.z >= 0.5;
        if (!up && !south) return east ? 1 : 0;
        if (!up) return east ? 3 : 2;
        if (!south) return east ? 5 : 4;
        return east ? 7 : 6;
    }

    private static boolean allCorners(BlockState state) {
        for (BooleanProperty property : CORNERS) {
            if (!state.getValue(property)) return false;
        }
        return true;
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!QuarterBlocks.isQuarterPiece(context.getItemInHand()) || allCorners(state)) return false;
        return !state.getValue(CORNERS[cornerIndex(relative(context))]);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        int index = cornerIndex(relative(context));
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            return existing.getValue(CORNERS[index]) ? null : existing.setValue(CORNERS[index], true);
        }
        return defaultBlockState().setValue(CORNERS[index], true);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity)) return;

        BlockState source = QuarterBlocks.source(stack);
        for (int i = 0; i < 8; i++) {
            if (state.getValue(CORNERS[i]) && blockEntity.material(i) == null) {
                blockEntity.setMaterial(i, source);
                break;
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GenericQuarterBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context
    ) {
        VoxelShape shape = Shapes.empty();
        if (state.getValue(NWD)) shape = Shapes.or(shape, box(0, 0, 0, 8, 8, 8));
        if (state.getValue(NED)) shape = Shapes.or(shape, box(8, 0, 0, 16, 8, 8));
        if (state.getValue(SWD)) shape = Shapes.or(shape, box(0, 0, 8, 8, 8, 16));
        if (state.getValue(SED)) shape = Shapes.or(shape, box(8, 0, 8, 16, 8, 16));
        if (state.getValue(NWU)) shape = Shapes.or(shape, box(0, 8, 0, 8, 16, 8));
        if (state.getValue(NEU)) shape = Shapes.or(shape, box(8, 8, 0, 16, 16, 8));
        if (state.getValue(SWU)) shape = Shapes.or(shape, box(0, 8, 8, 8, 16, 16));
        if (state.getValue(SEU)) shape = Shapes.or(shape, box(8, 8, 8, 16, 16, 16));
        return shape;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        List<ItemStack> drops = new ArrayList<>();

        if (builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof GenericQuarterBlockEntity blockEntity) {
            for (int i = 0; i < 8; i++) {
                if (!state.getValue(CORNERS[i])) continue;
                BlockState source = blockEntity.material(i);
                if (source != null) drops.add(QuarterBlocks.pieceFor(source, 1));
            }
        }

        return drops;
    }

    @Override
    protected ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state,
            boolean includeData
    ) {
        if (level.getBlockEntity(pos) instanceof GenericQuarterBlockEntity blockEntity) {
            for (int i = 0; i < 8; i++) {
                BlockState source = blockEntity.material(i);
                if (state.getValue(CORNERS[i]) && source != null) {
                    return QuarterBlocks.pieceFor(source, 1);
                }
            }
        }

        return QuarterBlocks.textured(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 1);
    }
}
