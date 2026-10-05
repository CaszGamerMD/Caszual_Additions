package dev.casz.utils.mixin;
import dev.casz.utils.cosmetics.SpecialCosmetics;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ArmorStandArmorModel.class)
public abstract class ArmorStandModelCosmeticsMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/ArmorStandRenderState;)V",at=@At("TAIL"))
    private void caszutils$specialParts(ArmorStandRenderState state,CallbackInfo ci){
        var model=(ArmorStandArmorModel)(Object)this;
        boolean customHead=SpecialCosmetics.isMobHead(state.headEquipment)||SpecialCosmetics.isEndRod(state.headEquipment)||SpecialCosmetics.isBone(state.headEquipment);
        if(customHead){model.head.visible=false;model.hat.visible=false;}
        if(SpecialCosmetics.isRodLike(state.chestEquipment)){model.leftArm.visible=false;model.rightArm.visible=false;model.body.visible=false;}
        if(SpecialCosmetics.rodLegs(state.legsEquipment,state.feetEquipment)){model.leftLeg.visible=false;model.rightLeg.visible=false;}
    }
}
