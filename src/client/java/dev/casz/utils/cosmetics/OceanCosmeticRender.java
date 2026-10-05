package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
public final class OceanCosmeticRender {
 private OceanCosmeticRender(){}
 private static void item(ItemStack stack,ModelPart part,PoseStack pose,SubmitNodeCollector collector,int light,int outline,float x,float y,float z,float scale,float rx,float rz){
  pose.pushPose();part.translateAndRotate(pose);pose.translate(x,y,z);pose.mulPose(Axis.XP.rotationDegrees(rx));pose.mulPose(Axis.ZP.rotationDegrees(rz));pose.scale(scale,scale,scale);
  var s=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForTopItem(s,stack,ItemDisplayContext.FIXED,null,null,0);s.submit(pose,collector,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,outline);pose.popPose();
 }
 public static void head(ItemStack shell,ModelPart p,PoseStack ps,SubmitNodeCollector c,int l,int o){item(shell,p,ps,c,l,o,-.28f,-.28f,.02f,.55f,18,-22);item(shell,p,ps,c,l,o,.25f,-.12f,.14f,.38f,-10,30);}
 public static void chest(ItemStack shell,ModelPart body,ModelPart la,ModelPart ra,PoseStack ps,SubmitNodeCollector c,int l,int o){item(shell,body,ps,c,l,o,-.34f,.12f,.12f,.52f,15,-25);item(shell,body,ps,c,l,o,.31f,.38f,.10f,.42f,-8,28);var kelp=new ItemStack(net.minecraft.world.item.Items.KELP);item(kelp,la,ps,c,l,o,0,.18f,.08f,.48f,0,8);item(kelp,ra,ps,c,l,o,0,.32f,.08f,.48f,0,-8);}
 public static void legs(ModelPart ll,ModelPart rl,PoseStack ps,SubmitNodeCollector c,int l,int o){var kelp=new ItemStack(net.minecraft.world.item.Items.KELP);item(kelp,ll,ps,c,l,o,0,.30f,.10f,.5f,0,15);item(kelp,rl,ps,c,l,o,0,.05f,.10f,.5f,0,-18);}
 public static void feet(ItemStack shell,ModelPart ll,ModelPart rl,PoseStack ps,SubmitNodeCollector c,int l,int o){item(shell,ll,ps,c,l,o,0,.62f,.08f,.35f,12,15);item(shell,rl,ps,c,l,o,0,.58f,.08f,.35f,-8,-15);}
}
