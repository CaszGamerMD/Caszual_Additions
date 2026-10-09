package com.caszgamermd.caszualadditions.utils.cosmetics;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LightningRodBlock;

public final class HeadItemCosmeticRender {
 private HeadItemCosmeticRender(){}
 public static void lightningRod(ItemStack stack,ModelPart head,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
  if(!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof LightningRodBlock block)) return;
  var state=block.defaultBlockState().setValue(LightningRodBlock.FACING,Direction.UP);
  var parts=new ArrayList<BlockStateModelPart>();
  Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42),parts);
  pose.pushPose();
  head.translateAndRotate(pose);
  pose.scale(1f/.99f,1f/.99f,1f/.99f);
  pose.translate(0,-.125f,-.0625f);
  pose.mulPose(Axis.XP.rotationDegrees(-45));
  pose.scale(1,-1,-1);
  pose.translate(-.5f,0,-.5f);
  collector.order(1).submitBlockModel(pose,RenderTypes.cutoutMovingBlock(),parts,new int[]{-1},Brightness.FULL_BRIGHT.pack(),OverlayTexture.NO_OVERLAY,outline);
  pose.popPose();
 }
}
