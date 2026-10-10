package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public final class PalletBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    /** Direction the placing player was looking; determines the 2x2 footprint. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /**
     * The old pallet always extended east and south from part 0. Saved world
     * states without these new properties inherit legacy=true, preserving
     * existing placed pallets and their shared inventory.
     */
    public static final BooleanProperty LEGACY = BooleanProperty.create("legacy");
    public static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 5, 16);

    private final PalletKind kind;
    private final int copperAge;
    private final MapCodec<PalletBlock> codec;

    public PalletBlock(PalletKind kind, int copperAge, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        this.copperAge = copperAge;
        this.codec = MapCodec.unit(this);
        registerDefaultState(stateDefinition.any()
                .setValue(PART, 0)
                .setValue(FACING, Direction.NORTH)
                .setValue(LEGACY, true));
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
        builder.add(PART, FACING, LEGACY);
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

    /** "Home" is always part 0: the actual block the player clicked. */
    public static BlockPos rootPos(BlockPos pos, BlockState state) {
        int part = state.getValue(PART);
        if (state.getValue(LEGACY)) {
            return switch (part) {
                case 1 -> pos.west();
                case 2 -> pos.north();
                case 3 -> pos.north().west();
                default -> pos;
            };
        }

        Direction forward = state.getValue(FACING);
        Direction right = forward.getClockWise();
        return switch (part) {
            case 1 -> pos.relative(right.getOpposite());
            case 2 -> pos.relative(forward.getOpposite());
            case 3 -> pos.relative(right.getOpposite()).relative(forward.getOpposite());
            default -> pos;
        };
    }

    /** Offset from the home corner, with right and forward relative to player. */
    public static BlockPos partPos(BlockPos home, int part, BlockState homeState) {
        if (homeState.getValue(LEGACY)) {
            return switch (part) {
                case 1 -> home.east();
                case 2 -> home.south();
                case 3 -> home.south().east();
                default -> home;
            };
        }
        Direction forward = homeState.getValue(FACING);
        Direction right = forward.getClockWise();
        return switch (part) {
            case 1 -> home.relative(right);
            case 2 -> home.relative(forward);
            case 3 -> home.relative(right).relative(forward);
            default -> home;
        };
    }

    /** Pick the preferred orientation first, then the three rotations. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos home = context.getClickedPos();
        Direction preferred = context.getHorizontalDirection();
        for (int turn = 0; turn < 4; turn++) {
            Direction candidate = preferred;
            for (int i = 0; i < turn; i++) candidate = candidate.getClockWise();
            BlockState proposed = defaultBlockState().setValue(PART, 0)
                    .setValue(FACING, candidate).setValue(LEGACY, false);
            if (fitsAt(context.getLevel(), home, proposed)) return proposed;
        }
        // Do not place even a partial pallet when no 2x2 footprint is open.
        return null;
    }

    private static boolean fitsAt(Level level, BlockPos home, BlockState proposed) {
        for (int part = 0; part < 4; part++) {
            BlockPos target = partPos(home, part, proposed);
            if (level.isOutsideBuildHeight(target)
                    || !level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).canBeReplaced()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;

        // The home block has already been placed by Minecraft. Re-check all
        // other positions before writing anything to avoid partial pallets.
        for (int part = 1; part < 4; part++) {
            BlockPos target = partPos(pos, part, state);
            if (level.isOutsideBuildHeight(target)
                    || !level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).canBeReplaced()) {
                level.removeBlock(pos, false);
                return;
            }
        }
        for (int part = 1; part < 4; part++) {
            BlockPos target = partPos(pos, part, state);
            level.setBlock(target, state.setValue(PART, part), 3);
        }
        for (int part = 0; part < 4; part++) {
            BlockPos target = partPos(pos, part, state);
            if (level.getBlockEntity(target) instanceof PalletBlockEntity be) {
                be.initialize(pos, part, stack);
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
                BlockPos target = partPos(root, part, state);
                BlockState other = level.getBlockState(target);
                if (!target.equals(pos) && other.is(state.getBlock())
                        && other.hasProperty(PART) && other.getValue(PART) == part
                        && other.getValue(FACING) == state.getValue(FACING)
                        && other.getValue(LEGACY) == state.getValue(LEGACY)) {
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
