package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PalletBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 5, 16);

    private final PalletKind kind;
    private final int copperAge;
    private final MapCodec<PalletBlock> codec;

    public PalletBlock(PalletKind kind, int copperAge, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        this.copperAge = copperAge;
        this.codec = MapCodec.unit(this);
        registerDefaultState(stateDefinition.any().setValue(PART, 0));
    }

    public PalletKind kind() {
        return kind;
    }

    public int copperAge() {
        return copperAge;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    public boolean shouldChangedStateKeepBlockEntity(BlockState oldState) {
        return oldState.getBlock() instanceof PalletBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    public static BlockPos rootPos(BlockPos pos, BlockState state) {
        return switch (state.getValue(PART)) {
            case 1 -> pos.west();
            case 2 -> pos.north();
            case 3 -> pos.north().west();
            default -> pos;
        };
    }

    public static BlockPos partPos(BlockPos root, int part) {
        return switch (part) {
            case 1 -> root.east();
            case 2 -> root.south();
            case 3 -> root.south().east();
            default -> root;
        };
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos root = context.getClickedPos();
        for (int part = 0; part < 4; part++) {
            BlockPos target = partPos(root, part);
            if (!context.getLevel().getBlockState(target).canBeReplaced()) return null;
        }
        return defaultBlockState().setValue(PART, 0);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        BlockPos root = pos;
        for (int part = 1; part < 4; part++) {
            level.setBlock(
                    partPos(root, part),
                    defaultBlockState().setValue(PART, part),
                    3
            );
        }

        for (int part = 0; part < 4; part++) {
            BlockPos target = partPos(root, part);
            if (level.getBlockEntity(target) instanceof PalletBlockEntity be) {
                be.initialize(root, part, stack);
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PalletBlockEntity(pos, state);
    }

    private static @Nullable PalletBlockEntity rootEntity(Level level, BlockPos pos, BlockState state) {
        BlockPos root = rootPos(pos, state);
        return level.getBlockEntity(root) instanceof PalletBlockEntity be ? be : null;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit
    ) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            PalletBlockEntity root = rootEntity(level, pos, state);
            if (root != null) serverPlayer.openMenu(root);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos root = rootPos(pos, state);
            PalletBlockEntity controller = level.getBlockEntity(root) instanceof PalletBlockEntity be ? be : null;

            if (controller != null && !player.getAbilities().instabuild) {
                ItemStack palletDrop = controller.createDropStack();
                Containers.dropContents(level, root, controller);
                Block.popResource(level, root, palletDrop);
            }

            for (int part = 0; part < 4; part++) {
                BlockPos target = partPos(root, part);
                if (!target.equals(pos) && level.getBlockState(target).getBlock() instanceof PalletBlock) {
                    level.removeBlock(target, false);
                }
            }
        }

        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        return List.of();
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockPos root = rootPos(pos, state);
        if (level.getBlockEntity(root) instanceof PalletBlockEntity be) {
            return be.createDropStack();
        }
        return new ItemStack(this);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return kind == PalletKind.COPPER && copperAge < 3 && state.getValue(PART) == 0;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (kind != PalletKind.COPPER || copperAge >= 3 || state.getValue(PART) != 0) return;
        if (random.nextFloat() < 0.075F) {
            PalletContent.advanceCopper(level, pos, copperAge);
        }
    }
}
