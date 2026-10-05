package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.FlowerBlock;
public final class FlowerCrownRender {
 private FlowerCrownRender(){}
 public static boolean isFlower(ItemStack s){return s.getItem() instanceof BlockItem bi&&bi.getBlock() instanceof FlowerBlock;}
 private static void flower(ItemStack s,ModelPart head,PoseStack p,SubmitNodeCollector c,int l,int o,float x,float z,float scale,float tilt){
  p.pushPose();head.translateAndRotate(p);p.translate(x,-.50f,z);p.mulPose(Axis.ZP.rotationDegrees(tilt));p.scale(scale,scale,scale);var r=new ItemStackRenderState();Minecraft.getInstance().getItemModelResolver().updateForTopItem(r,s,ItemDisplayContext.FIXED,null,null,0);r.submit(p,c,l,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,o);p.popPose();
 }
 public static void crown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  String n=BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
  if(n.contains("tulip")){flower(s,h,p,c,l,o,-.28f,.02f,.34f,-12);flower(s,h,p,c,l,o,0,-.04f,.40f,0);flower(s,h,p,c,l,o,.28f,.02f,.34f,12);}
  else if(n.equals("allium")||n.equals("cornflower")){flower(s,h,p,c,l,o,-.25f,.02f,.38f,-18);flower(s,h,p,c,l,o,.02f,-.06f,.48f,3);flower(s,h,p,c,l,o,.27f,.03f,.38f,18);}
  else if(n.equals("wither_rose")){flower(s,h,p,c,l,o,-.30f,.02f,.38f,-20);flower(s,h,p,c,l,o,0,-.08f,.42f,0);flower(s,h,p,c,l,o,.30f,.02f,.38f,20);}
  else {flower(s,h,p,c,l,o,-.30f,.02f,.30f,-18);flower(s,h,p,c,l,o,-.10f,-.06f,.30f,-6);flower(s,h,p,c,l,o,.10f,-.06f,.30f,6);flower(s,h,p,c,l,o,.30f,.02f,.30f,18);}
 }
}
