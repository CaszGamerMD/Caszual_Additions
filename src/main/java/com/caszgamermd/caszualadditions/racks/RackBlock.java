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
        // Leave a full one-block-deep approach, across both columns and
        // all three heights, for rack labels, connectors and filter upgrades.
        for (int height = 0; height < 3; height++) {
            for (int column = 0; column < 2; column++) {
                BlockPos access = cell(root, facing, height * 4 + column).relative(facing);
                if (level.isOutsideBuildHeight(access)
                        || !level.getBlockState(access).canBeReplaced()) return false;
            }
        }
        // A newly placed rack must also not cover the upgrade corridor of
        // an existing rack. Only check loaded nearby positions.
        java.util.Set<BlockPos> proposed = new java.util.HashSet<>();
        for (int part = 0; part < 12; part++) proposed.add(cell(root, facing, part));
        for (int dx = -4; dx <= 4; dx++) for (int dy = -3; dy <= 3; dy++)
            for (int dz = -4; dz <= 4; dz++) {
                BlockPos oldRoot = root.offset(dx, dy, dz);
                if (!level.hasChunkAt(oldRoot)) continue;
                BlockState old = level.getBlockState(oldRoot);
                if (!(old.getBlock() instanceof RackBlock)
                        || old.getValue(PART) != 0) continue;
                Direction oldFacing = old.getValue(FACING);
                for (int height = 0; height < 3; height++)
                    for (int column = 0; column < 2; column++)
                        if (proposed.contains(
                                cell(oldRoot, oldFacing, height * 4 + column)
                                        .relative(oldFacing))) return false;
            }
        return true;
    }

    /**
     * Never put our warehouse fixtures in the mandatory one-block-deep
     * front approach of an already-built rack.
     */
    public static boolean reservedForAccess(Level level,BlockPos position) {
        for(int dx=-4;dx<=4;dx++)for(int dy=-3;dy<=3;dy++)for(int dz=-4;dz<=4;dz++){
            BlockPos root=position.offset(dx,dy,dz);
            if(!level.hasChunkAt(root))continue;
            BlockState old=level.getBlockState(root);
            if(!(old.getBlock() instanceof RackBlock)||old.getValue(PART)!=0)continue;
            Direction front=old.getValue(FACING);
            for(int h=0;h<3;h++)for(int col=0;col<2;col++){
                BlockPos free=cell(root,front,h*4+col).relative(front);
                if(free.equals(position))return true;
            }
        }
        return false;
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
        // Block placement must work identically on client and server, even
        // when a rack already contains a pallet.
        if(held.is(RackContent.CONNECTOR)
                ||held.is(RackContent.STORAGE_SCANNER_ITEM))
            return InteractionResult.PASS;
        if(held.is(net.minecraft.world.item.Items.SHEARS)&&rack.glassWrapped()){
            if(!level.isClientSide())rack.unwrapGlass();
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer server) {
            if (held.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() instanceof PalletBlock) {
                if (rack.hasPallet()) {
                    server.openMenu(rack);
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
                server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Label updated: "+RackNetwork.label(level,rack)));
                return InteractionResult.SUCCESS;
            }
            if (held.is(RackContent.CONNECTOR)) {
                // A Rack Connector is now a real block placed beyond the
                // clicked rack face. Leave normal BlockItem placement active.
                // Existing saved legacy connector bits remain readable.
                return InteractionResult.PASS;
            }
            if (held.is(RackContent.PALLET_FILTER)) {
                ItemStack sample = player.getOffhandItem();
                boolean alreadyInstalled=!rack.filterUpgrade().isEmpty();
                if(!rack.hasPallet()){
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Install a pallet before applying a filter."));
                    return InteractionResult.SUCCESS;
                }
                if (sample.isEmpty()) {
                    rack.clearFilter();
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Pallet filter cleared."));
                } else if (sample.is(RackContent.PALLET_FILTER)) {
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Hold an ordinary sample item in your offhand to set the filter."));
                } else {
                    rack.setFilter(sample);
                    server.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Pallet accepts only " + sample.getHoverName().getString()));
                    if (!alreadyInstalled && !player.getAbilities().instabuild)
                        held.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }
        // Normal use on any segment opens the installed pallet even while
        // holding ordinary inventory items. Leave rack upgrades above intact.
        if(rack.hasPallet()){
            if(!level.isClientSide() && player instanceof ServerPlayer server)
                server.openMenu(rack);
            return InteractionResult.SUCCESS;
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
                if (rack.hasPallet()) Block.popResource(level, root, rack.dropPallet());
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
            net.minecraft.world.level.BlockGetter level, BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        // The lower deck supports the pallet. All twelve rack parts have
        // collision on their OUTSIDE corner posts, not a fake floor in every
        // upper segment. Cargo collision is identical to the 3x3x3 display.
        BlockPos root=root(pos,state);
        Direction facing=state.getValue(FACING);
        int minX=Integer.MAX_VALUE,minZ=Integer.MAX_VALUE;
        for(int i=0;i<4;i++){
            BlockPos cell=cell(root,facing,i);
            minX=Math.min(minX,cell.getX());
            minZ=Math.min(minZ,cell.getZ());
        }
        double x=pos.getX()==minX?0:15;
        double z=pos.getZ()==minZ?0:15;
        var post=Block.box(x,0,z,x+1,16,z+1);
        var cargo=RackCargoCollision.shape(level,pos,state);
        // Keep a completely clear FRONT face at the two vertical level
        // boundaries. The sides and rear retain their rack framing.
        // A ground-level grill is low enough to support pallets and visually
        // matches the three narrow longitudinal support strips in the model.
        int partIndex=state.getValue(PART);
        int height=partIndex/4;
        boolean front=partIndex%4<2;
        var empty=net.minecraft.world.phys.shapes.Shapes.empty();
        var railX=(!front||height==0)?Block.box(0,0,z,16,1,z+1):empty;
        var railXTop=(!front||height==2)?Block.box(0,15,z,16,16,z+1):empty;
        var railZ=Block.box(x,0,0,x+1,1,16);
        var railZTop=Block.box(x,15,0,x+1,16,16);
        if(height==0){
            var grill=net.minecraft.world.phys.shapes.Shapes.or(
                    Block.box(3,1,2,4,2,14),
                    Block.box(8,1,2,9,2,14),
                    Block.box(13,1,2,14,2,14));
            return net.minecraft.world.phys.shapes.Shapes.or(
                    Block.box(0,0,0,16,1,16),grill,post,
                    railX,railXTop,railZ,railZTop,cargo);
        }
        return net.minecraft.world.phys.shapes.Shapes.or(
                post,railX,railXTop,railZ,railZTop,cargo);
    }
}
