package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class OceanCosmeticRender {
 private static final Identifier DEEP_TEXTURE=Identifier.withDefaultNamespace("textures/block/prismarine.png");
 private static final Identifier SHELL_TEXTURE=Identifier.withDefaultNamespace("textures/block/calcite.png");
 private static final ModelPart ROOT=createLayer().bakeRoot();

 private static final ModelPart HEAD=ROOT.getChild("head");
 private static final ModelPart HEAD_ACCENT=ROOT.getChild("head_accent");
 private static final ModelPart BODY=ROOT.getChild("body");
 private static final ModelPart BODY_ACCENT=ROOT.getChild("body_accent");
 private static final ModelPart LEFT_SHOULDER=ROOT.getChild("left_shoulder");
 private static final ModelPart RIGHT_SHOULDER=ROOT.getChild("right_shoulder");
 private static final ModelPart LEFT_LEG=ROOT.getChild("left_leg");
 private static final ModelPart RIGHT_LEG=ROOT.getChild("right_leg");
 private static final ModelPart LEFT_FOOT=ROOT.getChild("left_foot");
 private static final ModelPart RIGHT_FOOT=ROOT.getChild("right_foot");

 private OceanCosmeticRender(){}

 private static LayerDefinition createLayer(){
  var mesh=new MeshDefinition();
  var root=mesh.getRoot();

  root.addOrReplaceChild("head",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-4.5f,-8.8f,-1.5f,9,2.3f,6,new CubeDeformation(.10f))
          .texOffs(0,0).addBox(-5.1f,-6.8f,-.5f,1.8f,5.0f,4.5f,new CubeDeformation(.05f))
          .texOffs(0,0).addBox(3.3f,-6.8f,-.5f,1.8f,5.0f,4.5f,new CubeDeformation(.05f)),
      PartPose.ZERO);

  root.addOrReplaceChild("head_accent",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-3.0f,-10.2f,-.2f,6,2.0f,3.7f,new CubeDeformation(.08f))
          .texOffs(0,0).addBox(-1.0f,-11.0f,.6f,2,1.4f,3.0f,new CubeDeformation(.05f)),
      PartPose.ZERO);

  root.addOrReplaceChild("body",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-4.6f,.2f,-2.6f,9.2f,9.2f,2.2f,new CubeDeformation(.08f))
          .texOffs(0,0).addBox(-4.0f,1.0f,2.5f,8,8,2.4f,new CubeDeformation(.10f))
          .texOffs(0,0).addBox(-5.0f,2.0f,3.4f,2.0f,6.0f,2.0f,new CubeDeformation(.02f))
          .texOffs(0,0).addBox(3.0f,2.0f,3.4f,2.0f,6.0f,2.0f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("body_accent",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-1.0f,-.2f,-3.0f,2,10.0f,1.2f,new CubeDeformation(.03f))
          .texOffs(0,0).addBox(-3.3f,2.2f,-2.9f,6.6f,1.0f,1.0f,new CubeDeformation(.02f))
          .texOffs(0,0).addBox(-2.8f,5.2f,-2.9f,5.6f,1.0f,1.0f,new CubeDeformation(.02f))
          .texOffs(0,0).addBox(-2.2f,8.0f,-2.9f,4.4f,1.0f,1.0f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("left_shoulder",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.8f,-2.4f,-2.7f,5.6f,4.2f,5.4f,new CubeDeformation(.08f))
          .texOffs(0,0).addBox(1.6f,-1.4f,-.8f,3.0f,1.4f,2.0f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("right_shoulder",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.8f,-2.4f,-2.7f,5.6f,4.2f,5.4f,new CubeDeformation(.08f))
          .texOffs(0,0).addBox(-4.6f,-1.4f,-.8f,3.0f,1.4f,2.0f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("left_leg",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.4f,.2f,-2.3f,4.8f,7.0f,4.6f,new CubeDeformation(.04f))
          .texOffs(0,0).addBox(1.5f,2.0f,-.8f,3.2f,3.8f,1.6f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("right_leg",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.4f,.2f,-2.3f,4.8f,7.0f,4.6f,new CubeDeformation(.04f))
          .texOffs(0,0).addBox(-4.7f,2.0f,-.8f,3.2f,3.8f,1.6f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("left_foot",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.8f,8.2f,-5.0f,5.6f,3.8f,7.2f,new CubeDeformation(.05f))
          .texOffs(0,0).addBox(1.9f,9.1f,-4.2f,2.8f,1.4f,5.2f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  root.addOrReplaceChild("right_foot",
      CubeListBuilder.create()
          .texOffs(0,0).addBox(-2.8f,8.2f,-5.0f,5.6f,3.8f,7.2f,new CubeDeformation(.05f))
          .texOffs(0,0).addBox(-4.7f,9.1f,-4.2f,2.8f,1.4f,5.2f,new CubeDeformation(.02f)),
      PartPose.ZERO);

  return LayerDefinition.create(mesh,16,16);
 }

 private static void submit(ModelPart geometry,ModelPart playerPart,PoseStack pose,SubmitNodeCollector collector,
                            int light,int outline,Identifier texture){
  pose.pushPose();
  playerPart.translateAndRotate(pose);
  collector.order(1).submitModelPart(geometry,pose,RenderTypes.entityCutout(texture),light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);
  pose.popPose();
 }

 public static void head(ItemStack ignored,ModelPart playerHead,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  submit(HEAD,playerHead,pose,collector,light,outline,DEEP_TEXTURE);
  submit(HEAD_ACCENT,playerHead,pose,collector,light,outline,SHELL_TEXTURE);
 }

 public static void chest(ItemStack ignored,ModelPart body,ModelPart leftArm,ModelPart rightArm,
                          PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  submit(BODY,body,pose,collector,light,outline,DEEP_TEXTURE);
  submit(BODY_ACCENT,body,pose,collector,light,outline,SHELL_TEXTURE);
  submit(LEFT_SHOULDER,leftArm,pose,collector,light,outline,SHELL_TEXTURE);
  submit(RIGHT_SHOULDER,rightArm,pose,collector,light,outline,SHELL_TEXTURE);
 }

 public static void legs(ModelPart leftLeg,ModelPart rightLeg,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  submit(LEFT_LEG,leftLeg,pose,collector,light,outline,DEEP_TEXTURE);
  submit(RIGHT_LEG,rightLeg,pose,collector,light,outline,DEEP_TEXTURE);
 }

 public static void feet(ItemStack ignored,ModelPart leftLeg,ModelPart rightLeg,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  submit(LEFT_FOOT,leftLeg,pose,collector,light,outline,SHELL_TEXTURE);
  submit(RIGHT_FOOT,rightLeg,pose,collector,light,outline,SHELL_TEXTURE);
 }
}
