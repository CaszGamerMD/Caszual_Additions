package dev.casz.utils.cosmetics;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Brightness;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EndRodBlock;
public final class EndRodRender {
    private EndRodRender(){}
    public static void limb(ItemStack stack,ModelPart part,PoseStack pose,SubmitNodeCollector collector,float centerX,float topY,int outline){
        pose.pushPose();part.translateAndRotate(pose);pose.translate(centerX,topY,0);pose.scale(1,.75f,1);block(stack,pose,collector,outline);pose.popPose();
    }
    public static void ribCage(ItemStack stack,ModelPart body,PoseStack pose,SubmitNodeCollector collector,int outline){
        pose.pushPose();body.translateAndRotate(pose);
        for(float y:new float[]{.125f,.375f,.625f}){segment(stack,pose,collector,.325f,y,-.225f,Axis.ZP,90,.65f,outline);segment(stack,pose,collector,.325f,y,.225f,Axis.ZP,90,.65f,outline);segment(stack,pose,collector,-.325f,y,-.225f,Axis.XP,90,.45f,outline);segment(stack,pose,collector,.325f,y,-.225f,Axis.XP,90,.45f,outline);}
        segment(stack,pose,collector,0,0,.275f,Axis.XP,0,.75f,outline);pose.popPose();
    }
    private static void segment(ItemStack stack,PoseStack pose,SubmitNodeCollector collector,float x,float y,float z,Axis axis,float angle,float length,int outline){
        pose.pushPose();pose.translate(x,y,z);pose.mulPose(axis.rotationDegrees(angle));pose.scale(length,length,length);block(stack,pose,collector,outline);pose.popPose();
    }
    public static void block(ItemStack stack,PoseStack pose,SubmitNodeCollector collector,int outline){
        var block=((BlockItem)stack.getItem()).getBlock();var state=block.defaultBlockState().setValue(EndRodBlock.FACING,Direction.UP);
        var parts=new ArrayList<BlockStateModelPart>();Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42),parts);
        pose.pushPose();pose.translate(-.5f,0,-.5f);collector.order(1).submitBlockModel(pose,RenderTypes.cutoutMovingBlock(),parts,new int[]{-1},Brightness.FULL_BRIGHT.pack(),OverlayTexture.NO_OVERLAY,outline);pose.popPose();
    }
}