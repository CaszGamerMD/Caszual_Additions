package com.caszgamermd.caszualadditions.utils.mixin;
import com.caszgamermd.caszualadditions.utils.cosmetics.ArmorStandCosmeticLayer;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ArmorStandRenderer.class)
public abstract class ArmorStandCosmeticsMixin extends LivingEntityRenderer<ArmorStand,ArmorStandRenderState,ArmorStandArmorModel>{
    protected ArmorStandCosmeticsMixin(EntityRendererProvider.Context c,ArmorStandArmorModel m,float s){super(c,m,s);}
    @Inject(method="<init>",at=@At("TAIL")) private void caszual_additions$cosmetics(EntityRendererProvider.Context context,CallbackInfo ci){addLayer(new ArmorStandCosmeticLayer(this));}
}
