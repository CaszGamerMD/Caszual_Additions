package dev.casz.utils.mixin;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import dev.casz.utils.cosmetics.SpecialCosmetics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class PlayerCosmeticHeadMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void caszutils$blockHead(AvatarRenderState state,CallbackInfo ci){
        var model=(PlayerModel)(Object)this; boolean block=SpecialCosmetics.hasBlockHead(state.headEquipment);
        boolean mobHead=SpecialCosmetics.isMobHead(state.headEquipment);
        boolean boneHead=SpecialCosmetics.isBone(state.headEquipment);
        model.head.visible=!mobHead&&!boneHead; model.hat.visible=!mobHead&&!boneHead&&state.showHat; model.head.xScale=model.head.yScale=model.head.zScale=block&&!mobHead&&!boneHead?.99f:1f;
        boolean golem=SpecialCosmetics.isSnowGolem(state.headEquipment,state.chestEquipment,state.legsEquipment,state.feetEquipment);
        if(golem) model.head.y += 2.0f;
        boolean arms=golem||SpecialCosmetics.isRodLike(state.chestEquipment), legs=golem||SpecialCosmetics.specialLegs(state.legsEquipment,state.feetEquipment);
        if(arms){model.body.visible=model.jacket.visible=false;model.leftArm.visible=model.rightArm.visible=false;model.leftSleeve.visible=model.rightSleeve.visible=false;}
        if(legs){model.leftLeg.visible=model.rightLeg.visible=false;model.leftPants.visible=model.rightPants.visible=false;}
    }
}