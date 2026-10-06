package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
public final class BoneCosmeticRender {
    private BoneCosmeticRender(){}
    public static void bone(ItemStack stack, ModelPart part, PoseStack pose, SubmitNodeCollector collector, float x, float y, float scale, int light, int outline){
        pose.pushPose(); part.translateAndRotate(pose); pose.translate(x,y,0); pose.mulPose(Axis.ZP.rotationDegrees(90)); pose.scale(scale,scale,scale);
        var state=new net.minecraft.client.renderer.item.ItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.FIXED, Minecraft.getInstance().level, null, 0);
        state.submit(pose,collector,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,outline);
        pose.popPose();
    }
    public static void skeleton(PlayerModelAccess model, PoseStack pose, SubmitNodeCollector collector, ItemStack bone, boolean head, boolean chest, boolean legs, int light, int outline){
        if(head){ var skull=new ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL); bone(skull,model.head(),pose,collector,0,-.42f,.82f,light,outline); }
        if(chest){
            bone(bone,model.rightArm(),pose,collector,0,-.12f,.85f,light,outline); bone(bone,model.leftArm(),pose,collector,0,-.12f,.85f,light,outline);
            for(float y:new float[]{.10f,.30f,.50f}){ bone(bone,model.body(),pose,collector,-.22f,y,.55f,light,outline); bone(bone,model.body(),pose,collector,.22f,y,.55f,light,outline); }
        }
        if(legs){ bone(bone,model.rightLeg(),pose,collector,0,0,.9f,light,outline); bone(bone,model.leftLeg(),pose,collector,0,0,.9f,light,outline); }
    }
    public interface PlayerModelAccess { ModelPart head(); ModelPart body(); ModelPart leftArm(); ModelPart rightArm(); ModelPart leftLeg(); ModelPart rightLeg(); }
}
