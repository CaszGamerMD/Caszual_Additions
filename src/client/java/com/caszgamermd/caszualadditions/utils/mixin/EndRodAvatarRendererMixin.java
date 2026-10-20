package com.caszgamermd.caszualadditions.utils.mixin;

import com.caszgamermd.caszualadditions.utils.cosmetics.Cosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.EndRodCosmeticLayer;
import com.caszgamermd.caszualadditions.utils.cosmetics.EndRodRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.FirstPersonCosmeticArmRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.WearableAquariumArmRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.SnowGolemCosmeticRender;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds cosmetic body layers and replaces vanilla first-person hand meshes.
 * ItemInHandRenderer delegates its first-person skin arm to renderHand,
 * including the usual empty/held-item poses. Hooking here replaces the arm
 * at the original vanilla pivot instead of superimposing a second arm.
 */
@Mixin(AvatarRenderer.class)
public abstract class EndRodAvatarRendererMixin
        extends LivingEntityRenderer<Avatar, AvatarRenderState, PlayerModel> {
    @Unique private boolean caszual_additions$slim;
    @Unique private SnowGolemCosmeticRender caszual_additions$snow;

    protected EndRodAvatarRendererMixin(
            EntityRendererProvider.Context context,PlayerModel model,float shadow){
        super(context,model,shadow);
    }

    @Inject(method="<init>",at=@At("TAIL"))
    private void caszual_additions$cosmeticLayers(
            EntityRendererProvider.Context context,boolean slim,CallbackInfo ci){
        caszual_additions$slim=slim;
        caszual_additions$snow=new SnowGolemCosmeticRender();
        addLayer(new EndRodCosmeticLayer(this,slim));
        addLayer(new com.caszgamermd.caszualadditions.antiwarden.WoolGearLayer(
                this,context));
        addLayer(new com.caszgamermd.caszualadditions.utils.cosmetics.WearableAquariumLayer(
                this,slim,context));
    }

    @Inject(method="renderHand",at=@At("HEAD"),cancellable=true)
    private void caszual_additions$replaceFirstPersonHand(
            PoseStack pose,SubmitNodeCollector collector,int light,
            Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci){
        var player=Minecraft.getInstance().player;
        if(player==null||player.isInvisible())return;
        var chest=Cosmetics.get(player,EquipmentSlot.CHEST);

        // All cosmetic arms use exactly this ONE interception point.
        // Returning without cancel keeps the vanilla arm/skin unchanged.
        if(SpecialCosmetics.isWearableAquarium(chest)){
            WearableAquariumArmRender.renderHand(
                    arm,pose,collector,light,caszual_additions$slim);
        }else if(SpecialCosmetics.isSnowGolem(
                Cosmetics.get(player,EquipmentSlot.HEAD),chest,
                Cosmetics.get(player,EquipmentSlot.LEGS),
                Cosmetics.get(player,EquipmentSlot.FEET))){
            caszual_additions$snow.hand(arm,pose,collector,light);
        }else if(SpecialCosmetics.isBone(chest)){
            FirstPersonCosmeticArmRender.drawBone(
                    arm,pose,collector,light,caszual_additions$slim);
        }else if(SpecialCosmetics.isEndRod(chest)){
            arm.resetPose();
            boolean right=arm==getModel().rightArm;
            arm.zRot=right?.1f:-.1f;
            float center=(caszual_additions$slim?.5f:1f)/16f;
            EndRodRender.limb(chest,arm,pose,collector,
                    right?-center:center,-.125f,0);
        }else if(chest.getItem() instanceof BlockItem){
            FirstPersonCosmeticArmRender.drawBlock(
                    arm,pose,collector,light,caszual_additions$slim,chest);
        }else{
            return;
        }
        // The cosmetic took over the vanilla hand draw. Do NOT submit a
        // separate vanilla arm after it, even if an item is held.
        ci.cancel();
    }
}
