package com.caszgamermd.caszualadditions.utils.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.caszgamermd.caszualadditions.utils.cosmetics.Cosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.EndRodCosmeticLayer;
import com.caszgamermd.caszualadditions.utils.cosmetics.EndRodRender;
import com.caszgamermd.caszualadditions.utils.cosmetics.SpecialCosmetics;
import com.caszgamermd.caszualadditions.utils.cosmetics.WearableAquariumArmRender;
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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(AvatarRenderer.class)
public abstract class EndRodAvatarRendererMixin extends LivingEntityRenderer<Avatar,AvatarRenderState,PlayerModel>{
    @Unique private boolean caszual_additions$slim;@Unique private com.caszgamermd.caszualadditions.utils.cosmetics.SnowGolemCosmeticRender caszual_additions$snow;
    protected EndRodAvatarRendererMixin(EntityRendererProvider.Context context,PlayerModel model,float shadow){super(context,model,shadow);}
    @Inject(method="<init>",at=@At("TAIL")) private void caszual_additions$rodLayer(EntityRendererProvider.Context context,boolean slim,CallbackInfo ci){caszual_additions$slim=slim;caszual_additions$snow=new com.caszgamermd.caszualadditions.utils.cosmetics.SnowGolemCosmeticRender();addLayer(new EndRodCosmeticLayer(this,slim));addLayer(new com.caszgamermd.caszualadditions.utils.cosmetics.WearableAquariumLayer(this,slim,context));}
    @Inject(method="renderHand",at=@At("HEAD"),cancellable=true)
    private void caszual_additions$rodHand(PoseStack pose,SubmitNodeCollector collector,int light,Identifier skin,ModelPart arm,boolean sleeve,CallbackInfo ci){
        var player=Minecraft.getInstance().player;if(player==null)return;var rod=Cosmetics.get(player,EquipmentSlot.CHEST);
        if(SpecialCosmetics.isWearableAquarium(rod)){
            WearableAquariumArmRender.renderHand(arm,pose,collector,light,caszual_additions$slim);
            ci.cancel();
            return;
        }
        if(SpecialCosmetics.isSnowGolem(Cosmetics.get(player,EquipmentSlot.HEAD),rod,Cosmetics.get(player,EquipmentSlot.LEGS),Cosmetics.get(player,EquipmentSlot.FEET))){arm.resetPose();caszual_additions$snow.hand(arm,pose,collector,light);ci.cancel();return;}
        if(!SpecialCosmetics.isEndRod(rod))return;arm.resetPose();boolean right=arm==getModel().rightArm;arm.zRot=right?.1f:-.1f;float center=(caszual_additions$slim?.5f:1f)/16f;EndRodRender.limb(rod,arm,pose,collector,right?-center:center,-.125f,0);ci.cancel();
    }
}