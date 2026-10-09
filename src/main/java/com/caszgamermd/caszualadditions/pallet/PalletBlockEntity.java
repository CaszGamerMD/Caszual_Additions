package com.caszgamermd.caszualadditions.pallet;

import java.util.List;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public final class PalletBlockEntity extends BlockEntity implements Container, ExtendedMenuProvider<BlockPos> {
    public static final int SLOTS = 216;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private BlockPos rootPos;
    private int part;
    private final Block[] wood = {
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS,
            Blocks.OAK_PLANKS
    };
    private int plasticColor = PalletData.DEFAULT_PLASTIC_COLOR;

    public PalletBlockEntity(BlockPos pos, BlockState state) {
        super(PalletContent.PALLET_BLOCK_ENTITY, pos, state);
        this.part = state.hasProperty(PalletBlock.PART) ? state.getValue(PalletBlock.PART) : 0;
        this.rootPos = PalletBlock.rootPos(pos, state);
    }

    public boolean isRoot() {
        return part == 0;
    }

    public int part() {
        return part;
    }

    public BlockPos rootPos() {
        return rootPos;
    }

    public int plasticColor() {
        return plasticColor;
    }

    public Block woodForBoard(int board) {
        return wood[Math.max(0, Math.min(3, board))];
    }

    public Block woodForPart() {
        return wood[Math.max(0, Math.min(3, part))];
    }

    public void initialize(BlockPos root, int part, ItemStack stack) {
        this.rootPos = root.immutable();
        this.part = part;

        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            if (pallet.kind() == PalletKind.WOOD) {
                List<Block> materials = PalletData.wood(stack);
                for (int i = 0; i < 4; i++) {
                    wood[i] = materials.get(i);
                }
            } else if (pallet.kind() == PalletKind.PLASTIC) {
                plasticColor = PalletData.plasticColor(stack);
            }
        }

        sync();
    }

    public ItemStack createDropStack() {
        ItemStack stack = new ItemStack(getBlockState().getBlock().asItem());
        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            if (pallet.kind() == PalletKind.WOOD) {
                stack = PalletData.applyWood(stack, List.of(wood));
            } else if (pallet.kind() == PalletKind.PLASTIC) {
                stack = PalletData.applyPlasticColor(stack, plasticColor);
            }
        }
        return stack;
    }

    private PalletBlockEntity controller() {
        if (level == null || isRoot()) return this;
        return level.getBlockEntity(rootPos) instanceof PalletBlockEntity be ? be : this;
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        PalletBlockEntity c = controller();
        for (ItemStack stack : c.items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        PalletBlockEntity c = controller();
        return slot >= 0 && slot < SLOTS ? c.items.get(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        PalletBlockEntity c = controller();
        ItemStack removed = ContainerHelper.removeItem(c.items, slot, count);
        if (!removed.isEmpty()) c.sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        PalletBlockEntity c = controller();
        ItemStack removed = ContainerHelper.takeItem(c.items, slot);
        if (!removed.isEmpty()) c.sync();
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        PalletBlockEntity c = controller();
        if (slot < 0 || slot >= SLOTS) return;

        c.items.set(slot, stack);
        if (stack.getCount() > c.getMaxStackSize(stack)) {
            stack.setCount(c.getMaxStackSize(stack));
        }
        c.sync();
    }

    @Override
    public void clearContent() {
        PalletBlockEntity c = controller();
        for (int i = 0; i < SLOTS; i++) {
            c.items.set(i, ItemStack.EMPTY);
        }
        c.sync();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(controller(), player, 6.0F);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (isRoot() && level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    private void sync() {
        super.setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("root", BlockPos.CODEC, rootPos);
        output.putInt("part", part);
        output.putInt("plastic_color", plasticColor);

        for (int i = 0; i < 4; i++) {
            output.putString(
                    "plank_" + i,
                    BuiltInRegistries.BLOCK.getKey(wood[i]).toString()
            );
        }

        if (isRoot()) {
            ContainerHelper.saveAllItems(output, items);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        rootPos = input.read("root", BlockPos.CODEC).orElse(worldPosition);
        part = input.getIntOr(
                "part",
                getBlockState().hasProperty(PalletBlock.PART)
                        ? getBlockState().getValue(PalletBlock.PART)
                        : 0
        );
        plasticColor = input.getIntOr("plastic_color", PalletData.DEFAULT_PLASTIC_COLOR);

        for (int i = 0; i < 4; i++) {
            Identifier id = Identifier.tryParse(
                    input.getStringOr("plank_" + i, "minecraft:oak_planks")
            );
            Block block = id == null
                    ? Blocks.OAK_PLANKS
                    : BuiltInRegistries.BLOCK.getValue(id);
            wood[i] = block == null ? Blocks.OAK_PLANKS : block;
        }

        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        if (isRoot()) {
            ContainerHelper.loadAllItems(input, items);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        if (getBlockState().getBlock() instanceof PalletBlock pallet) {
            return switch (pallet.kind()) {
                case WOOD -> Component.translatable("container.caszual_additions.wooden_pallet");
                case PLASTIC -> Component.translatable("container.caszual_additions.plastic_pallet");
                case IRON -> Component.translatable("container.caszual_additions.iron_pallet");
                case COPPER -> Component.translatable("container.caszual_additions.copper_pallet");
            };
        }
        return Component.translatable("container.caszual_additions.pallet");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(
            int containerId,
            Inventory inventory,
            Player player
    ) {
        PalletBlockEntity c = controller();
        return new PalletMenu(containerId, inventory, c);
    }

    @Override
    public BlockPos getScreenOpeningData(ServerPlayer player) {
        return controller().getBlockPos();
    }
}
