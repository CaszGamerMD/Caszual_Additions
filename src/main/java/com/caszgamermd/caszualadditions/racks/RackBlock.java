package com.caszgamermd.caszualadditions.racks;

import com.mojang.serialization.MapCodec;
import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import java.util.List;
import org.jspecify.annotations.Nullable;

/** A real 2-wide x 2-deep x 3-high pallet bay, owned by one controller. */
public final class RackBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 11);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final MapCodec<RackBlock> codec = MapCodec.unit(this);

    public RackBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PART, 0).setValue(FACING, Direction.NORTH));
    }
    @Override protected MapCodec<? extends Block> codec() { return codec; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PART, FACING);
    }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    public static BlockPos cell(BlockPos root, Direction facing, int part) {
        // The home block is the floor's front-left corner.
        return root.relative(facing.getCounterClockWise(), part % 2)
                .relative(facing.getOpposite(), (part % 4) / 2)
                .above(part / 4);
    }

    public static BlockPos root(BlockPos pos, BlockState state) {
        Direction face = state.getValue(FACING);
        int part = state.getValue(PART);
        return pos.relative(face.getClockWise(), part % 2)
                .relative(face, (part % 4) / 2)
                .below(part / 4);
    }

    private static boolean fits(Level level, BlockPos root, Direction facing) {
        for (int i = 0; i < 12; i++) {
            BlockPos p = cell(root, facing, i);
            if (level.isOutsideBuildHeight(p)
                    || !level.getWorldBorder().isWithinBounds(p)
                    || !level.getBlockState(p).canBeReplaced()) return false;
        }
        // Leave an actual approach space for rack upgrade cards and access.
        for (int i = 0; i < 2; i++) {
            BlockPos access = cell(root, facing, i).relative(facing);
            if (!level.getBlockState(access).canBeReplaced()) return false;
        }
        return true;
    }

    @Override public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        return fits(context.getLevel(), context.getClickedPos(), facing)
                ? defaultBlockState().setValue(FACING, facing) : null;
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        Direction face = state.getValue(FACING);
        for (int i = 1; i < 12; i++) {
            BlockPos at = cell(pos, face, i);
            if (!level.getBlockState(at).canBeReplaced()) {
                level.removeBlock(pos, false);
                return;
            }
        }
        for (int i = 1; i < 12; i++) {
            level.setBlock(cell(pos, face, i), state.setValue(PART, i), 3);
        }
    }

    @Override public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new RackBlockEntity(pos, state) : null;
    }

    public static @Nullable RackBlockEntity controller(Level level, BlockPos partPos) {
        BlockState state = level.getBlockState(partPos);
        if (!(state.getBlock() instanceof RackBlock)) return null;
        BlockPos root = root(partPos, state);
        return level.getBlockEntity(root) instanceof RackBlockEntity be ? be : null;
    }

    @Override protected InteractionResult useItemOn(ItemStack held, BlockState state,
                        Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        RackBlockEntity rack = controller(level, pos);
        if (rack == null) return InteractionResult.PASS;
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            if (held.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof PalletBlock) {
                if (rack.hasPallet()) {
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal("This rack already has a pallet."));
                    return InteractionResult.SUCCESS;
                }
                rack.install(held.copyWithCount(1));
                if (!player.getAbilities().instabuild) held.shrink(1);
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal("Pallet installed."));
                return InteractionResult.SUCCESS;
            }
            if (held.is(RackContent.RACK_TAG) || held.is(RackContent.AISLE_TAG)) {
                String name = held.getHoverName().getString();
                if (held.is(RackContent.RACK_TAG)) rack.setRackName(name.equals("Rack Tag") ? "Rack" : name);
                else rack.setAisleName(name.equals("Aisle Tag") ? "Aisle" : name);
                if (!player.getAbilities().instabuild) held.shrink(1);
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal("Label updated: " + rack.displayName()));
                return InteractionResult.SUCCESS;
            }
            if (held.is(RackContent.CONNECTOR)) {
                Direction side = hit.getDirection();
                rack.toggleConnector(side);
                if (!player.getAbilities().instabuild) held.shrink(1);
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Rack connector " + (rack.hasConnector(side) ? "enabled" : "disabled")
                        + " on " + side.getName()));
                return InteractionResult.SUCCESS;
            }
            if (held.is(RackContent.PALLET_FILTER)) {
                ItemStack sample = player.getOffhandItem();
                if (sample.isEmpty() || sample.is(RackContent.PALLET_FILTER)) {
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Hold a sample item in your offhand to set this pallet's filter."));
                } else {
                    rack.setFilter(sample);
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Pallet accepts only " + sample.getHoverName().getString()));
                    if (!player.getAbilities().instabuild) held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                          Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            RackBlockEntity rack = controller(level, pos);
            if (rack != null) {
                if (player.isShiftKeyDown()) {
                    if (rack.hasPallet() && rack.isEmpty()) {
                        player.getInventory().placeItemBackInInventory(rack.takePallet());
                    } else {
                        server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                "Empty this rack's pallet before removing it."));
                    }
                } else if (rack.hasPallet()) server.openMenu(rack);
                else server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Right-click with a pallet item to load this rack."));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos root = root(pos, state);
            RackBlockEntity rack = level.getBlockEntity(root) instanceof RackBlockEntity be ? be : null;
            if (rack != null && !player.getAbilities().instabuild) {
                Containers.dropContents(level, root, rack);
                if (rack.hasPallet()) Block.popResource(level, root, rack.takePallet());
                Block.popResource(level, root, new ItemStack(this));
            }
            for (int i=0;i<12;i++) {
                BlockPos at=cell(root,state.getValue(FACING),i);
                if (!at.equals(pos)) {
                    BlockState found=level.getBlockState(at);
                    if (found.is(this) && found.getValue(FACING)==state.getValue(FACING)
                            && found.getValue(PART)==i) level.removeBlock(at,false);
                }
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected List<ItemStack> getDrops(BlockState state, LootParams.Builder b) { return List.of(); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        // Frame rails are narrow enough to leave front access free.
        return Block.box(0, 0, 0, 16, 2, 16);
    }
}
