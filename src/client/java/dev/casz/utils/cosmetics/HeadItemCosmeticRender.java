package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
public final class HeadItemCosmeticRender {
 private HeadItemCosmeticRender(){}
 public static void lightningRod(ItemStack stack,ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  pose.pushPose();head.translateAndRotate(pose);pose.translate(0,-.20,.24);pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.85f,.85f,.85f);
  var s=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForTopItem(s,stack,ItemDisplayContext.FIXED,null,null,0);s.submit(pose,collector,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,outline);pose.popPose();
 }
}
