package com.caszgamermd.caszualadditions.utils.mixin;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerCosmeticHeadMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void caszual_additions$blockHead(AvatarRenderState state,CallbackInfo ci){
        var model=(PlayerModel)(Object)this;
        if (SpecialCosmetics.isWearableAquarium(state.chestEquipment)) {
            model.head.visible=model.hat.visible=model.body.visible=model.jacket.visible=false;
            model.leftArm.visible=model.rightArm.visible=model.leftSleeve.visible=model.rightSleeve.visible=false;
            model.leftLeg.visible=model.rightLeg.visible=model.leftPants.visible=model.rightPants.visible=false;
            return;
        }
        boolean block=SpecialCosmetics.hasBlockHead(state.headEquipment);
        // Vanilla skulls live in wornHeadType, while cosmetic skulls also occupy headEquipment.
        // Both must fully replace the player's head and outer hat/skin layer.
        boolean skullHead=state.wornHeadType!=null
                || SpecialCosmetics.isMobHead(state.headEquipment)
                || SpecialCosmetics.isPlayerHead(state.headEquipment);
        boolean boneHead=SpecialCosmetics.isBone(state.headEquipment);
        boolean oceanHead=state.headEquipment.is(net.minecraft.world.item.Items.NAUTILUS_SHELL);
        boolean oceanChest=state.chestEquipment.is(net.minecraft.world.item.Items.NAUTILUS_SHELL);
        boolean oceanLegs=state.legsEquipment.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)||state.feetEquipment.is(net.minecraft.world.item.Items.NAUTILUS_SHELL);
        model.head.visible=!skullHead&&!boneHead;
        model.hat.visible=!skullHead&&!boneHead&&!oceanHead&&state.showHat;
        model.head.xScale=model.head.yScale=model.head.zScale=block&&!skullHead&&!boneHead?.99f:1f;
        if(oceanChest){model.jacket.visible=false;model.leftSleeve.visible=false;model.rightSleeve.visible=false;}
        if(oceanLegs){model.leftPants.visible=false;model.rightPants.visible=false;}
        boolean golem=SpecialCosmetics.isSnowGolem(state.headEquipment,state.chestEquipment,state.legsEquipment,state.feetEquipment);
        if(golem) model.head.y += 2.0f;
        boolean arms=golem||SpecialCosmetics.isRodLike(state.chestEquipment), legs=golem||SpecialCosmetics.specialLegs(state.legsEquipment,state.feetEquipment);
        if(arms){model.body.visible=model.jacket.visible=false;model.leftArm.visible=model.rightArm.visible=false;model.leftSleeve.visible=model.rightSleeve.visible=false;}
        if(legs){model.leftLeg.visible=model.rightLeg.visible=false;model.leftPants.visible=model.rightPants.visible=false;}
    }
}
