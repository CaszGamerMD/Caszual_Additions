package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.animal.golem.SnowGolemModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
public final class SnowGolemCosmeticRender {
    private static final Identifier TEXTURE=Identifier.withDefaultNamespace("textures/entity/snow_golem/snow_golem.png");
    private final ModelPart root=SnowGolemModel.createBodyLayer().bakeRoot();
    private final java.util.Map<ModelPart,ModelPart> geometry=new java.util.IdentityHashMap<>();
    public SnowGolemCosmeticRender(){var baked=SnowGolemModel.createBodyLayer().bakeRoot();for(String name:new String[]{"upper_body","lower_body","left_arm","right_arm"}){var part=baked.getChild(name);part.x=part.y=part.z=part.xRot=part.yRot=part.zRot=0;geometry.put(root.getChild(name),part);}}
    public void body(HumanoidModel<?> player,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
        pose.pushPose();player.body.translateAndRotate(pose);submit(root.getChild("upper_body"),pose,collector,light,outline);pose.popPose();submit(root.getChild("lower_body"),pose,collector,light,outline);
        for(boolean left:new boolean[]{false,true}){var arm=root.getChild(left?"left_arm":"right_arm");arm.resetPose();var source=left?player.leftArm:player.rightArm;arm.x=source.x;arm.y=source.y+3;arm.z=source.z;arm.xRot=source.xRot;arm.yRot+=source.yRot;arm.zRot+=source.zRot;submit(arm,pose,collector,light,outline);}
    }
    
    public void hand(ModelPart playerArm,PoseStack pose,SubmitNodeCollector collector,int light){pose.pushPose();playerArm.translateAndRotate(pose);pose.mulPose(Axis.ZP.rotationDegrees(90));var arm=root.getChild("left_arm");arm.resetPose();arm.x=arm.y=arm.z=0;arm.xRot=arm.yRot=arm.zRot=0;submit(arm,pose,collector,light,0);pose.popPose();}
    private void submit(ModelPart part,PoseStack pose,SubmitNodeCollector collector,int light,int outline){pose.pushPose();part.translateAndRotate(pose);collector.order(1).submitModelPart(geometry.get(part),pose,RenderTypes.entityCutout(TEXTURE),light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);pose.popPose();}
}