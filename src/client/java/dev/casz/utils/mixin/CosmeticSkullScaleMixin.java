package dev.casz.utils.mixin;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.casz.utils.cosmetics.SpecialCosmetics;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
@Mixin(CustomHeadLayer.class)
public abstract class CosmeticSkullScaleMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method="submit",at=@At(value="INVOKE",target="Lnet/minecraft/client/model/HeadedModel;translateToHead(Lcom/mojang/blaze3d/vertex/PoseStack;)V",shift=At.Shift.AFTER))
    private void caszutils$keepBlockSize(PoseStack pose,SubmitNodeCollector collector,int light,LivingEntityRenderState state,float yaw,float pitch,org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci){
        if(state instanceof AvatarRenderState avatar){
            if(SpecialCosmetics.hasBlockHead(avatar.headEquipment)) pose.scale(1f/.99f,1f/.99f,1f/.99f);
            if(SpecialCosmetics.isMobHead(avatar.headEquipment)){
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));
                pose.scale(1.28f,1.28f,1.28f);
            }
        }
    }
    @ModifyArgs(method="submit",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V",ordinal=1))
    private void caszutils$normalHeadSize(Args args,PoseStack pose,SubmitNodeCollector collector,int light,LivingEntityRenderState state,float yaw,float pitch){
        if(state instanceof AvatarRenderState avatar&&SpecialCosmetics.isPlayerHead(avatar.headEquipment)){args.set(0,1f);args.set(1,1f);args.set(2,1f);}
    }
}