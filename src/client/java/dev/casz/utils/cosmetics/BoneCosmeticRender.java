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
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(new net.minecraft.client.renderer.item.ItemStackRenderState(), stack, ItemDisplayContext.FIXED, null, null, 0);
        pose.popPose();
    }
}
