package com.caszgamermd.caszualadditions.pallet;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible collision extenders for pallet cargo above the base.
 *
 * Minecraft only checks collision shapes in the block cells around an entity;
 * an upper-tier shape on the floor pallet alone cannot be stood on.
 * Proxy cells supply the actual collision at y=1..2, and stale proxies
 * above that height are removed on refresh (for old 7-tier pallets).
 *
 * The dynamic shape matches the same tightly packed 3x3x3 cubes used by the
 * renderer, including partially occupied upper rows and rotated footprints.
 */
public final class PalletCargoCollisionBlock extends Block {
    private final MapCodec<PalletCargoCollisionBlock> codec = MapCodec.unit(this);
    private static final int CAPACITY = 27;
    private static final int CELLS_PER_LAYER = 9;
    private static final double CELL = 2.0 / 3.0;
    private static final double FLOOR = 0.3125;

    public PalletCargoCollisionBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override protected MapCodec<? extends Block> codec() { return codec; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    // Match selection to the *visible* cargo cuboids, not a full cube
    // that would obscure gaps between boxes or an adjacent pallet.
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level,
                                            BlockPos pos, CollisionContext context) {
        return shapeFor(level,pos);
    }

    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level,
                                                     BlockPos pos, CollisionContext context) {
        return shapeFor(level, pos);
    }
    /** Cargo right-clicks behave like clicking the same underlying pallet.
     *  Inventory remains one 216-slot controller even for 3-level stacks.
     */
    @Override protected net.minecraft.world.InteractionResult useWithoutItem(
            BlockState state,net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.phys.BlockHitResult hit){
        BlockPos root=owner(level,pos);
        if(root==null)return net.minecraft.world.InteractionResult.PASS;
        return PalletBlock.openPallet(level,root,level.getBlockState(root),player);
    }
    @Override protected net.minecraft.world.InteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack,BlockState state,
            net.minecraft.world.level.Level level,BlockPos pos,
            net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
        // The wrapper, Scanner and Stack of Pallets must still receive their
        // own item.useOn call. Each now resolves this proxy to its root.
        if(player.isShiftKeyDown()
                ||stack.is(com.caszgamermd.caszualadditions.racks.RackContent.PORTABLE_WRAPPER)
                ||stack.is(com.caszgamermd.caszualadditions.racks.RackContent.STORAGE_SCANNER_ITEM)
                ||stack.is(com.caszgamermd.caszualadditions.racks.RackContent.STACK_OF_PALLETS))
            return net.minecraft.world.InteractionResult.PASS;
        BlockPos root=owner(level,pos);
        if(root==null)return net.minecraft.world.InteractionResult.PASS;
        return PalletBlock.openPallet(level,root,level.getBlockState(root),player);
    }

    /** Mining a cargo-facing hitbox breaks the ONE placed pallet, rather
     *  than silently deleting a temporary invisible collision cell.
     *  Preserve the existing single-drop and all-slots cargo-drop logic.
     */
    @Override public BlockState playerWillDestroy(net.minecraft.world.level.Level level,
            BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player){
        if(level instanceof ServerLevel server){
            BlockPos root=owner(level,pos);
            if(root!=null){
                BlockState home=level.getBlockState(root);
                if(home.getBlock() instanceof PalletBlock pallet){
                    pallet.playerWillDestroy(level,root,home,player);
                    level.removeBlock(root,false);
                }
            }
        }
        return super.playerWillDestroy(level,pos,state,player);
    }

    /**
     * Restrict visible items when a solid roof or other world block is directly
     * above the footprint, so display models never occupy a neighbor block.
     * Existing collision proxies do not count as obstructions.
     */
    public static int visibleCount(BlockGetter level, BlockPos root, BlockState home,
                                   PalletBlockEntity entity) {
        int count = 0;
        for (int i = 0; i < entity.getContainerSize() && count < CAPACITY; i++) {
            if (!entity.getItem(i).isEmpty()) count++;
        }
        int layers = 3;
        for (int height = 1; height <= 2; height++) {
            for (int part = 0; part < 4; part++) {
                BlockPos cell = PalletBlock.partPos(root, part, home).above(height);
                BlockState above = level.getBlockState(cell);
                if (!above.isAir() && !above.is(PalletContent.PALLET_CARGO_COLLISION)) {
                    layers = Math.min(layers, height);
                }
            }
        }
        return Math.min(count, layers * CELLS_PER_LAYER);
    }

    /** Resolve the real shared pallet controller behind an upper cargo
     *  selection cell. Never open an unrelated pallet beneath an air gap.
     */
    public static BlockPos owningPallet(BlockGetter level,BlockPos at){
        if(!(level.getBlockState(at).getBlock() instanceof PalletCargoCollisionBlock))
            return null;
        return owner(level,at);
    }

    private static BlockPos owner(BlockGetter level,BlockPos at){
        for(int down=1;down<=2;down++){
            BlockPos partPos=at.below(down);
            BlockState part=level.getBlockState(partPos);
            if(!(part.getBlock() instanceof PalletBlock))continue;
            BlockPos root=PalletBlock.rootPos(partPos,part);
            if(!(level.getBlockEntity(root) instanceof PalletBlockEntity pallet)
                    ||!pallet.isRoot())continue;
            BlockState home=level.getBlockState(root);
            if(!(home.getBlock() instanceof PalletBlock))continue;
            if(!cargoShape(level,at,root,home,pallet).isEmpty())return root;
        }
        return null;
    }

    /** Combine the real cargo cuboids of every part and upper proxy cell into
     *  the selection of ANY of the four pallet deck blocks. Shapes are
     *  transformed relative to the selected block, not the anchor corner.
     */
    public static VoxelShape selectionShape(BlockGetter level,BlockPos selected,
            BlockPos root,BlockState state){
        if(!(level.getBlockEntity(root) instanceof PalletBlockEntity pallet)
                ||!pallet.isRoot())return Shapes.empty();
        VoxelShape shape=Shapes.empty();
        for(int part=0;part<4;part++){
            BlockPos floor=PalletBlock.partPos(root,part,state);
            for(int height=0;height<=2;height++){
                BlockPos at=floor.above(height);
                VoxelShape section=cargoShape(level,at,root,state,pallet);
                if(!section.isEmpty())shape=Shapes.or(shape,section.move(
                        at.getX()-selected.getX(),at.getY()-selected.getY(),
                        at.getZ()-selected.getZ()));
            }
        }
        return shape;
    }

    public static VoxelShape shapeFor(BlockGetter level, BlockPos at) {
        for (int down = 0; down <= 4; down++) {
            BlockPos floorPos = at.below(down);
            BlockState floorState = level.getBlockState(floorPos);
            if (!(floorState.getBlock() instanceof PalletBlock)) continue;
            BlockPos root = PalletBlock.rootPos(floorPos, floorState);
            if (level.getBlockEntity(root) instanceof PalletBlockEntity entity
                    && entity.isRoot()) {
                BlockState home = level.getBlockState(root);
                return cargoShape(level, at, root, home, entity);
            }
        }
        return Shapes.empty();
    }

    private static VoxelShape cargoShape(BlockGetter level, BlockPos at, BlockPos root,
                                         BlockState home, PalletBlockEntity entity) {
        if (!(home.getBlock() instanceof PalletBlock)) return Shapes.empty();
        int count = visibleCount(level, root, home, entity);
        VoxelShape result = Shapes.empty();
        for (int i = 0; i < count; i++) {
            int col = i % 3;
            int row = (i / 3) % 3;
            int layer = i / CELLS_PER_LAYER;
            double u0 = col * CELL, u1 = (col + 1) * CELL;
            double v0 = row * CELL, v1 = (row + 1) * CELL;
            double x0, x1, z0, z1;

            if (home.getValue(PalletBlock.LEGACY)) {
                x0 = root.getX() + u0; x1 = root.getX() + u1;
                z0 = root.getZ() + v0; z1 = root.getZ() + v1;
            } else {
                Direction facing = home.getValue(PalletBlock.FACING);
                switch (facing) {
                    case NORTH -> {
                        x0 = root.getX() + u0; x1 = root.getX() + u1;
                        z0 = root.getZ() + 1 - v1; z1 = root.getZ() + 1 - v0;
                    }
                    case SOUTH -> {
                        x0 = root.getX() + 1 - u1; x1 = root.getX() + 1 - u0;
                        z0 = root.getZ() + v0; z1 = root.getZ() + v1;
                    }
                    case EAST -> {
                        x0 = root.getX() + v0; x1 = root.getX() + v1;
                        z0 = root.getZ() + u0; z1 = root.getZ() + u1;
                    }
                    case WEST -> {
                        x0 = root.getX() + 1 - v1; x1 = root.getX() + 1 - v0;
                        z0 = root.getZ() + 1 - u1; z1 = root.getZ() + 1 - u0;
                    }
                    default -> { continue; }
                }
            }
            double y0 = root.getY() + FLOOR + layer * CELL;
            double y1 = y0 + CELL;
            double left = Math.max(at.getX(), x0), right = Math.min(at.getX() + 1, x1);
            double bottom = Math.max(at.getY(), y0), top = Math.min(at.getY() + 1, y1);
            double front = Math.max(at.getZ(), z0), back = Math.min(at.getZ() + 1, z1);
            if (right - left <= 1e-5 || top - bottom <= 1e-5 || back - front <= 1e-5) continue;

            result = Shapes.or(result, Block.box(
                    (left - at.getX()) * 16, (bottom - at.getY()) * 16, (front - at.getZ()) * 16,
                    (right - at.getX()) * 16, (top - at.getY()) * 16, (back - at.getZ()) * 16));
        }
        return result;
    }

    /** Create/remove proxy cells whenever the shared pallet inventory changes. */
    public static void refresh(ServerLevel level, BlockPos root) {
        BlockState home = level.getBlockState(root);
        if (!(home.getBlock() instanceof PalletBlock)) return;
        for (int part = 0; part < 4; part++) {
            BlockPos floor = PalletBlock.partPos(root, part, home);
            // Clean up old 5-high cargo collision blocks at y=3/4 too.
            for (int height = 1; height <= 4; height++) {
                BlockPos at = floor.above(height);
                BlockState existing = level.getBlockState(at);
                boolean needed = height <= 2 && !shapeFor(level, at).isEmpty();
                if (needed && existing.isAir()) {
                    level.setBlock(at, PalletContent.PALLET_CARGO_COLLISION.defaultBlockState(), 3);
                } else if (!needed && existing.is(PalletContent.PALLET_CARGO_COLLISION)) {
                    level.removeBlock(at, false);
                }
            }
        }
    }

    /** Remove only our collision extenders; never modify other world blocks. */
    public static void clear(ServerLevel level, BlockPos root, BlockState home) {
        for (int part = 0; part < 4; part++) {
            BlockPos floor = PalletBlock.partPos(root, part, home);
            for (int height = 1; height <= 4; height++) {
                BlockPos at = floor.above(height);
                if (level.getBlockState(at).is(PalletContent.PALLET_CARGO_COLLISION)) {
                    level.removeBlock(at, false);
                }
            }
        }
    }
}
