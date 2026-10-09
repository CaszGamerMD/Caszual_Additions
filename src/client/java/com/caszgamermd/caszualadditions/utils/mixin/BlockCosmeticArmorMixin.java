package com.caszgamermd.caszualadditions.utils.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.caszgamermd.caszualadditions.utils.cosmetics.BlockOutfitTextures;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(HumanoidArmorLayer.class)
public abstract class BlockCosmeticArmorMixin {
    @Shadow private HumanoidModel<?> getArmorModel(HumanoidRenderState state,EquipmentSlot slot){throw new AssertionError();}
    @SuppressWarnings({"rawtypes","unchecked"})
    @Inject(method="renderArmorPiece",at=@At("HEAD"),cancellable=true)
    private void caszual_additions$blockArmor(PoseStack pose,SubmitNodeCollector collector,ItemStack stack,EquipmentSlot slot,int light,HumanoidRenderState state,CallbackInfo ci){
        if(!(state instanceof AvatarRenderState)) return;
        if(SpecialCosmetics.isSnowGolem(state.headEquipment,state.chestEquipment,state.legsEquipment,state.feetEquipment)){ci.cancel();return;}
        if(SpecialCosmetics.isRodLike(stack)||((slot==EquipmentSlot.LEGS||slot==EquipmentSlot.FEET)&&SpecialCosmetics.rodLegs(state.legsEquipment,state.feetEquipment))){ci.cancel();return;}
        if(slot==EquipmentSlot.HEAD||!(stack.getItem() instanceof BlockItem item)) return;
        var model=getArmorModel(state,slot); var texture=BlockOutfitTextures.texture(item.getBlock());
        collector.order(1).submitModel((Model)model,state,pose,RenderTypes.entityTranslucent(texture),light,OverlayTexture.NO_OVERLAY,-1,null,state.outlineColor,null); ci.cancel();
    }
}