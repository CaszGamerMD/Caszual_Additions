package com.caszgamermd.caszualadditions.utils.cosmetics;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public final class FlowerCrownRender {
 private static final Identifier AZALEA=Identifier.withDefaultNamespace("textures/block/azalea_leaves.png");
 private static final Identifier OAK=Identifier.withDefaultNamespace("textures/block/oak_leaves.png");
 private static final Identifier CHERRY=Identifier.withDefaultNamespace("textures/block/cherry_leaves.png");
 private static final Identifier DARK_OAK=Identifier.withDefaultNamespace("textures/block/dark_oak_leaves.png");
 private static final Identifier PALE_OAK=Identifier.withDefaultNamespace("textures/block/pale_oak_leaves.png");
 private static final Identifier MOSS=Identifier.withDefaultNamespace("textures/block/moss_block.png");
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

 private static Identifier bandTexture(String name){
  if(name.equals("wither_rose"))return DARK_OAK;
  if(name.equals("pink_petals"))return CHERRY;
  if(name.contains("eyeblossom"))return PALE_OAK;
  if(name.equals("spore_blossom"))return MOSS;
  if(name.equals("dandelion")||name.equals("oxeye_daisy")||name.equals("lily_of_the_valley"))return OAK;
  return AZALEA;
 }

 private static void band(ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline,Identifier texture){
  pose.pushPose();
  head.translateAndRotate(pose);
  collector.order(1).submitModelPart(BAND,pose,RenderTypes.entityCutout(texture),light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);
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

 private static void denseRing(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o,float scale){
  flower(s,h,p,c,l,o,-.31f,-.48f,-.25f,scale,-8,0,-16);
  flower(s,h,p,c,l,o,-.10f,-.51f,-.31f,scale+ .02f,-6,0,-5);
  flower(s,h,p,c,l,o,.10f,-.51f,-.31f,scale+ .02f,-6,0,5);
  flower(s,h,p,c,l,o,.31f,-.48f,-.25f,scale,-8,0,16);
  flower(s,h,p,c,l,o,-.36f,-.48f,.02f,scale-.02f,0,78,-14);
  flower(s,h,p,c,l,o,.36f,-.48f,.02f,scale-.02f,0,-78,14);
  flower(s,h,p,c,l,o,-.20f,-.47f,.30f,scale-.03f,8,180,-8);
  flower(s,h,p,c,l,o,.20f,-.47f,.30f,scale-.03f,8,180,8);
 }

 private static void tulipCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.30f,-.54f,-.22f,.27f,-16,0,-10);
  flower(s,h,p,c,l,o,0,-.60f,-.30f,.34f,-5,0,0);
  flower(s,h,p,c,l,o,.30f,-.54f,-.22f,.27f,-16,0,10);
  flower(s,h,p,c,l,o,-.34f,-.49f,.10f,.22f,0,78,-12);
  flower(s,h,p,c,l,o,.34f,-.49f,.10f,.22f,0,-78,12);
 }

 private static void pomCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.27f,-.58f,-.18f,.34f,-18,0,-12);
  flower(s,h,p,c,l,o,0,-.65f,-.29f,.40f,-4,0,0);
  flower(s,h,p,c,l,o,.27f,-.58f,-.18f,.34f,-18,0,12);
 }

 private static void sideSpray(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.34f,-.51f,-.14f,.28f,-12,35,-18);
  flower(s,h,p,c,l,o,-.38f,-.57f,.02f,.25f,-4,70,-8);
  flower(s,h,p,c,l,o,-.30f,-.50f,.18f,.22f,8,110,4);
  flower(s,h,p,c,l,o,.18f,-.46f,-.28f,.19f,-8,0,8);
 }

 private static void droopingCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.30f,-.46f,-.18f,.22f,16,20,-22);
  flower(s,h,p,c,l,o,-.38f,-.39f,.02f,.19f,28,72,-8);
  flower(s,h,p,c,l,o,.30f,-.46f,-.18f,.22f,16,-20,22);
  flower(s,h,p,c,l,o,.38f,-.39f,.02f,.19f,28,-72,8);
 }

 private static void witherCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.30f,-.48f,-.23f,.29f,-18,0,-24);
  flower(s,h,p,c,l,o,.04f,-.54f,-.31f,.31f,-6,0,4);
  flower(s,h,p,c,l,o,.34f,-.46f,.02f,.23f,4,-72,22);
  flower(s,h,p,c,l,o,-.18f,-.44f,.29f,.20f,12,180,-12);
 }

 private static void torchCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.28f,-.58f,-.20f,.30f,-18,0,-12);
  flower(s,h,p,c,l,o,0,-.69f,-.27f,.42f,-2,0,0);
  flower(s,h,p,c,l,o,.28f,-.58f,-.20f,.30f,-18,0,12);
 }

 private static void sunflowerCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,0,-.70f,-.27f,.40f,-2,0,0);
  flower(s,h,p,c,l,o,-.32f,-.54f,-.10f,.25f,-10,48,-16);
  flower(s,h,p,c,l,o,.32f,-.54f,-.10f,.25f,-10,-48,16);
 }

 private static void tallSideCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.34f,-.60f,-.12f,.28f,-10,40,-18);
  flower(s,h,p,c,l,o,-.18f,-.53f,-.28f,.24f,-8,12,-8);
  flower(s,h,p,c,l,o,.18f,-.49f,-.27f,.21f,-8,-10,8);
  flower(s,h,p,c,l,o,.34f,-.46f,.02f,.18f,0,-78,12);
 }

 private static void roseBushCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.30f,-.53f,-.22f,.27f,-14,0,-18);
  flower(s,h,p,c,l,o,0,-.59f,-.31f,.30f,-6,0,0);
  flower(s,h,p,c,l,o,.30f,-.53f,-.22f,.27f,-14,0,18);
  flower(s,h,p,c,l,o,-.33f,-.46f,.13f,.20f,6,88,-12);
  flower(s,h,p,c,l,o,.33f,-.46f,.13f,.20f,6,-88,12);
 }

 private static void openEyeCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,0,-.58f,-.34f,.36f,-4,0,0);
  flower(s,h,p,c,l,o,-.31f,-.48f,-.16f,.22f,-10,35,-18);
  flower(s,h,p,c,l,o,.31f,-.48f,-.16f,.22f,-10,-35,18);
  flower(s,h,p,c,l,o,0,-.46f,.31f,.18f,10,180,0);
 }

 private static void closedEyeCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.28f,-.45f,-.24f,.20f,-18,0,-12);
  flower(s,h,p,c,l,o,0,-.48f,-.30f,.22f,-12,0,0);
  flower(s,h,p,c,l,o,.28f,-.45f,-.24f,.20f,-18,0,12);
  flower(s,h,p,c,l,o,-.24f,-.43f,.28f,.17f,14,180,-8);
  flower(s,h,p,c,l,o,.24f,-.43f,.28f,.17f,14,180,8);
 }

 private static void petalsCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,-.34f,-.42f,-.22f,.20f,58,0,-18);
  flower(s,h,p,c,l,o,-.11f,-.46f,-.31f,.20f,62,0,-6);
  flower(s,h,p,c,l,o,.11f,-.46f,-.31f,.20f,62,0,6);
  flower(s,h,p,c,l,o,.34f,-.42f,-.22f,.20f,58,0,18);
  flower(s,h,p,c,l,o,-.32f,-.41f,.17f,.18f,62,115,-12);
  flower(s,h,p,c,l,o,.32f,-.41f,.17f,.18f,62,-115,12);
 }

 private static void sporeCrown(ItemStack s,ModelPart h,PoseStack p,SubmitNodeCollector c,int l,int o){
  flower(s,h,p,c,l,o,0,-.54f,.26f,.34f,8,180,0);
  flower(s,h,p,c,l,o,-.31f,-.45f,-.10f,.19f,24,55,-18);
  flower(s,h,p,c,l,o,.31f,-.45f,-.10f,.19f,24,-55,18);
 }

 public static void crown(ItemStack stack,ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  String name=BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
  band(head,pose,collector,light,outline,bandTexture(name));

  if(name.contains("tulip")) tulipCrown(stack,head,pose,collector,light,outline);
  else switch(name){
   case "dandelion" -> denseRing(stack,head,pose,collector,light,outline,.20f);
   case "poppy" -> denseRing(stack,head,pose,collector,light,outline,.25f);
   case "blue_orchid" -> sideSpray(stack,head,pose,collector,light,outline);
   case "allium" -> pomCrown(stack,head,pose,collector,light,outline);
   case "azure_bluet" -> denseRing(stack,head,pose,collector,light,outline,.18f);
   case "oxeye_daisy" -> denseRing(stack,head,pose,collector,light,outline,.22f);
   case "cornflower" -> sideSpray(stack,head,pose,collector,light,outline);
   case "lily_of_the_valley" -> droopingCrown(stack,head,pose,collector,light,outline);
   case "wither_rose" -> witherCrown(stack,head,pose,collector,light,outline);
   case "torchflower" -> torchCrown(stack,head,pose,collector,light,outline);
   case "sunflower" -> sunflowerCrown(stack,head,pose,collector,light,outline);
   case "lilac", "peony" -> tallSideCrown(stack,head,pose,collector,light,outline);
   case "rose_bush" -> roseBushCrown(stack,head,pose,collector,light,outline);
   case "pitcher_plant" -> pomCrown(stack,head,pose,collector,light,outline);
   case "open_eyeblossom" -> openEyeCrown(stack,head,pose,collector,light,outline);
   case "closed_eyeblossom" -> closedEyeCrown(stack,head,pose,collector,light,outline);
   case "pink_petals" -> petalsCrown(stack,head,pose,collector,light,outline);
   case "spore_blossom" -> sporeCrown(stack,head,pose,collector,light,outline);
   default -> denseRing(stack,head,pose,collector,light,outline,.22f);
  }
 }
}
