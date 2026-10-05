package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
public final class EndRodCosmeticLayer extends RenderLayer<AvatarRenderState,PlayerModel>{
    private final boolean slim;private final SnowGolemCosmeticRender snow=new SnowGolemCosmeticRender();
    public EndRodCosmeticLayer(RenderLayerParent<AvatarRenderState,PlayerModel> renderer,boolean slim){super(renderer);this.slim=slim;}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch){
        if(state.isSpectator||state.isInvisible)return;var model=getParentModel();pose.pushPose();model.root().translateAndRotate(pose);
        if(SpecialCosmetics.isSnowGolem(state.headEquipment,state.chestEquipment,state.legsEquipment,state.feetEquipment)){snow.body(model,pose,collector,light,state.outlineColor);pose.popPose();return;}
        if(SpecialCosmetics.isBone(state.headEquipment)){ /* skull is supplied by the vanilla skeleton-skull head item in a follow-up render pass */ }
        if(SpecialCosmetics.isEndRod(state.headEquipment)){pose.pushPose();model.head.translateAndRotate(pose);pose.scale(1f/.99f,1f/.99f,1f/.99f);pose.translate(0,-.125f,.0625f);pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(1,-1,-1);EndRodRender.block(state.headEquipment,pose,collector,state.outlineColor);pose.popPose();}
        if(SpecialCosmetics.isCrystalCluster(state.headEquipment))CrystalCosmeticRender.spikes(state.headEquipment,model,pose,collector,light,state.outlineColor);
        if(SpecialCosmetics.isEndRod(state.chestEquipment)){float center=(slim?.5f:1f)/16f;EndRodRender.limb(state.chestEquipment,model.rightArm,pose,collector,-center,-.125f,state.outlineColor);EndRodRender.limb(state.chestEquipment,model.leftArm,pose,collector,center,-.125f,state.outlineColor);EndRodRender.ribCage(state.chestEquipment,model.body,pose,collector,state.outlineColor);}
        if(SpecialCosmetics.rodLegs(state.legsEquipment,state.feetEquipment)){var rods=SpecialCosmetics.isEndRod(state.legsEquipment)?state.legsEquipment:state.feetEquipment;EndRodRender.limb(rods,model.rightLeg,pose,collector,0,0,state.outlineColor);EndRodRender.limb(rods,model.leftLeg,pose,collector,0,0,state.outlineColor);}
        pose.popPose();
    }
}