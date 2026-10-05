package dev.casz.utils.cosmetics;

import java.util.EnumMap;
import java.util.Map;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public final class Cosmetics {
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final Map<EquipmentSlot, AttachmentType<ItemStack>> DATA = new EnumMap<>(EquipmentSlot.class);
    public static final AttachmentType<Boolean> HIDE_ARMOR = AttachmentRegistry.create(
        Identifier.fromNamespaceAndPath("caszutils", "hide_armor"),
        builder -> builder.initializer(() -> false).persistent(com.mojang.serialization.Codec.BOOL)
            .copyOnDeath().syncWith(net.minecraft.network.codec.ByteBufCodecs.BOOL, AttachmentSyncPredicate.all()));
    public static final ExtendedMenuType<CosmeticsMenu, Integer> MENU = Registry.register(BuiltInRegistries.MENU,
        Identifier.fromNamespaceAndPath("caszutils", "cosmetics"),
        new ExtendedMenuType<>((id, inventory, ignored) -> new CosmeticsMenu(id, inventory), StreamCodec.unit(0)));
    static {
        for (var slot : SLOTS) DATA.put(slot, AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath("caszutils", "cosmetic_" + slot.getName()),
            builder -> builder.initializer(() -> ItemStack.EMPTY).persistent(ItemStack.OPTIONAL_CODEC)
                .copyOnDeath().syncWith(ItemStack.OPTIONAL_STREAM_CODEC, AttachmentSyncPredicate.all())));
    }
    private Cosmetics() {}
    public static ItemStack get(Player player, EquipmentSlot slot) { return player.getAttachedOrElse(DATA.get(slot), ItemStack.EMPTY).copy(); }
    public static void set(Player player, EquipmentSlot slot, ItemStack stack) {
        if (!ItemStack.matches(get(player, slot), stack)) player.setAttached(DATA.get(slot), stack.copy());
    }
    public static boolean hideArmor(Player player) { return player.getAttachedOrElse(HIDE_ARMOR, false); }
    public static void setHideArmor(Player player, boolean hide) { player.setAttached(HIDE_ARMOR, hide); }
    public static boolean accepts(ItemStack stack, EquipmentSlot slot) {
        if (stack.getItem() instanceof BlockItem) return true;
        if (stack.is(Items.STICK)) return slot == EquipmentSlot.CHEST;
        if (stack.is(Items.BONE)) return true;
        if (stack.is(Items.BLAZE_POWDER)) return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.FEET;
        if (stack.is(Items.ECHO_SHARD) || stack.is(Items.SLIME_BALL) || stack.is(Items.GUNPOWDER)) return slot == EquipmentSlot.FEET;
        if (stack.is(Items.LIGHTNING_ROD)) return slot == EquipmentSlot.HEAD;
        if (stack.is(Items.NAUTILUS_SHELL)) return true;
        var equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == slot;
    }
    public static void initialize() {
        FlowerTrail.initialize();
        BlazePowderEffects.initialize();
        FootstepEffects.initialize();
        PayloadTypeRegistry.serverboundPlay().register(OpenCosmetics.TYPE, OpenCosmetics.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ToggleArmorVisibility.TYPE, ToggleArmorVisibility.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ToggleArmorVisibility.TYPE, (payload, context) -> setHideArmor(context.player(), payload.hidden()));
        ServerPlayNetworking.registerGlobalReceiver(OpenCosmetics.TYPE, (payload, context) -> {
            var player = context.player();
            if (player.isAlive() && !player.isSpectator()) player.openMenu(new Provider());
        });
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player && !player.level().getGameRules().get(GameRules.KEEP_INVENTORY)) {
                for (var slot : SLOTS) {
                    var stack = get(player, slot);
                    if (!stack.isEmpty()) player.drop(stack, true);
                    set(player, slot, ItemStack.EMPTY);
                }
            }
        });
    }
    private record Provider() implements ExtendedMenuProvider<Integer> {
        public Integer getScreenOpeningData(ServerPlayer player) { return 0; }
        public Component getDisplayName() { return Component.translatable("gui.caszutils.cosmetics"); }
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new CosmeticsMenu(id, inventory); }
    }
}