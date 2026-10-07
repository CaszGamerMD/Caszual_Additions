package dev.casz.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class FlowerCrownRender {
 private static final Identifier LEAF_TEXTURE=Identifier.withDefaultNamespace("textures/block/azalea_leaves.png");
 private static final ModelPart BAND=createBand().bakeRoot().getChild("band");

 private FlowerCrownRender(){}

 public static boolean isFlower(ItemStack s){return FlowerCosmetics.isFlower(s);}

 private static LayerDefinition createBand(){
  var mesh=new MeshDefinition();
  var root=mesh.getRoot();
  var band=CubeListBuilder.create()
      .texOffs(0,0).addBox(-4.25f,-7.35f,-4.25f,8.5f,1.15f,1.0f,new CubeDeformation(0))
      .texOffs(0,0).addBox(-4.25f,-7.35f,3.25f,8.5f,1.15f,1.0f,new CubeDeformation(0))
      .texOffs(0,0).addBox(-4.25f,-7.35f,-3.25f,1.0f,1.15f,6.5f,new CubeDeformation(0))
      .texOffs(0,0).addBox(3.25f,-7.35f,-3.25f,1.0f,1.15f,6.5f,new CubeDeformation(0));
  root.addOrReplaceChild("band",band,PartPose.ZERO);
  return LayerDefinition.create(mesh,16,16);
 }

 private static void band(ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  pose.pushPose();
  head.translateAndRotate(pose);
  collector.order(1).submitModelPart(BAND,pose,RenderTypes.entityCutout(LEAF_TEXTURE),light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);
  pose.popPose();
 }

 private static void flower(ItemStack stack,ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline,
                            float x,float y,float z,float scale,float rx,float ry,float rz){
  pose.pushPose();
  head.translateAndRotate(pose);
  pose.translate(x,y,z);
  pose.mulPose(Axis.XP.rotationDegrees(rx));
  pose.mulPose(Axis.YP.rotationDegrees(ry));
  pose.mulPose(Axis.ZP.rotationDegrees(rz));
  pose.scale(scale,scale,scale);
  var state=new ItemStackRenderState();
  Minecraft.getInstance().getItemModelResolver().updateForTopItem(state,stack,ItemDisplayContext.FIXED,Minecraft.getInstance().level,null,0);
  state.submit(pose,collector,light,OverlayTexture.NO_OVERLAY,outline);
  pose.popPose();
 }

 public static void crown(ItemStack stack,ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  band(head,pose,collector,light,outline);
  // Front garland
  flower(stack,head,pose,collector,light,outline,-.31f,-.47f,-.26f,.24f,-8,0,-16);
  flower(stack,head,pose,collector,light,outline,-.11f,-.51f,-.31f,.27f,-6,0,-6);
  flower(stack,head,pose,collector,light,outline,.11f,-.51f,-.31f,.27f,-6,0,6);
  flower(stack,head,pose,collector,light,outline,.31f,-.47f,-.26f,.24f,-8,0,16);
  // Side blossoms
  flower(stack,head,pose,collector,light,outline,-.36f,-.48f,.02f,.22f,0,78,-14);
  flower(stack,head,pose,collector,light,outline,.36f,-.48f,.02f,.22f,0,-78,14);
  // Back blossoms complete the ring without overwhelming the face
  flower(stack,head,pose,collector,light,outline,-.20f,-.47f,.30f,.20f,8,180,-8);
  flower(stack,head,pose,collector,light,outline,.20f,-.47f,.30f,.20f,8,180,8);
 }
}
