package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.AppearanceRules;
import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticPreviewState;
import com.caszgamermd.caszualadditions.utils.cosmetics.Cosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public abstract class AvatarCosmeticsMixin {
    @Inject(
            method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL")
    )
    private void caszual_additions$appearance(
            Avatar entity,
            AvatarRenderState state,
            float partial,
            CallbackInfo ci
    ) {
        if (!(entity instanceof Player player)) return;

        boolean preview = CosmeticPreviewState.active(player);

        var head = CosmeticPreviewState.get(player, EquipmentSlot.HEAD);
        var chest = CosmeticPreviewState.get(player, EquipmentSlot.CHEST);
        var legs = CosmeticPreviewState.get(player, EquipmentSlot.LEGS);
        var feet = CosmeticPreviewState.get(player, EquipmentSlot.FEET);
        boolean wearableAquarium = SpecialCosmetics.isWearableAquarium(chest);
        boolean hide = preview || Cosmetics.hideArmor(player) || wearableAquarium;

        state.headEquipment = wearableAquarium ? net.minecraft.world.item.ItemStack.EMPTY : AppearanceRules.visible(head, state.headEquipment, hide);
        state.chestEquipment = AppearanceRules.visible(chest, state.chestEquipment, hide);
        state.legsEquipment = wearableAquarium ? net.minecraft.world.item.ItemStack.EMPTY : AppearanceRules.visible(legs, state.legsEquipment, hide);
        state.feetEquipment = wearableAquarium ? net.minecraft.world.item.ItemStack.EMPTY : AppearanceRules.visible(feet, state.feetEquipment, hide);

        if (wearableAquarium) {
            state.headItem.clear();
            state.wornHeadType = null;
            state.wornHeadProfile = null;
            return;
        }

        if (!head.isEmpty() || hide) {
            state.headItem.clear();
            state.wornHeadType = null;
            state.wornHeadProfile = null;

            // A cosmetic skull needs the same renderer as a normally equipped mob head.
            // Rendering it as a generic HEAD block item makes previews much too small.
            if (head.getItem() instanceof BlockItem blockItem
                    && blockItem.getBlock() instanceof AbstractSkullBlock skull) {
                state.wornHeadType = skull.getType();
                if (SpecialCosmetics.isPlayerHead(head)) {
                    state.wornHeadProfile = head.get(DataComponents.PROFILE);
                }
            } else if (SpecialCosmetics.isBone(head)) {
                state.wornHeadType = SkullBlock.Types.SKELETON;
            } else if (head.getItem() instanceof BlockItem
                    && !SpecialCosmetics.isEndRod(head)
                    && !SpecialCosmetics.isCrystalCluster(head)) {
                Minecraft.getInstance().getItemModelResolver()
                        .updateForLiving(state.headItem, head, ItemDisplayContext.HEAD, player);
            }
        }
    }
}
