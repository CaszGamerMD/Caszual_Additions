package dev.casz.utils.mixin;
import dev.casz.utils.cosmetics.AppearanceSettings;
import dev.casz.utils.cosmetics.Cosmetics;
import dev.casz.utils.cosmetics.AppearanceRules;
import dev.casz.utils.cosmetics.SpecialCosmetics;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class AvatarCosmeticsMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",at=@At("TAIL"))
    private void caszutils$appearance(Avatar entity,AvatarRenderState state,float partial,CallbackInfo ci){
        if(!(entity instanceof Player player)) return;
        boolean hide=AppearanceSettings.hideArmor();
        var head=Cosmetics.get(player,EquipmentSlot.HEAD); var chest=Cosmetics.get(player,EquipmentSlot.CHEST);
        var legs=Cosmetics.get(player,EquipmentSlot.LEGS); var feet=Cosmetics.get(player,EquipmentSlot.FEET);
        state.headEquipment=AppearanceRules.visible(head,state.headEquipment,hide); state.chestEquipment=AppearanceRules.visible(chest,state.chestEquipment,hide);
        state.legsEquipment=AppearanceRules.visible(legs,state.legsEquipment,hide); state.feetEquipment=AppearanceRules.visible(feet,state.feetEquipment,hide);
        if(!head.isEmpty()||hide){
            state.headItem.clear(); state.wornHeadType=null; state.wornHeadProfile=null;
            if(SpecialCosmetics.isPlayerHead(head)){state.wornHeadType=SkullBlock.Types.PLAYER; state.wornHeadProfile=head.get(DataComponents.PROFILE);}
            else if(head.getItem() instanceof BlockItem&&!SpecialCosmetics.isEndRod(head)&&!SpecialCosmetics.isCrystalCluster(head))
                Minecraft.getInstance().getItemModelResolver().updateForLiving(state.headItem,head,ItemDisplayContext.HEAD,player);
        }
    }
}