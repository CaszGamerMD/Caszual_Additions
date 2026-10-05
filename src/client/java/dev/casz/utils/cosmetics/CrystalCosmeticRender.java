package dev.casz.utils.cosmetics;
import java.util.ArrayList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
public final class CrystalCosmeticRender {
    private CrystalCosmeticRender(){}
    public static void spikes(ItemStack stack,PlayerModel model,PoseStack pose,SubmitNodeCollector collector,int light,int outline){
        var state=((BlockItem)stack.getItem()).getBlock().defaultBlockState();if(state.hasProperty(BlockStateProperties.FACING))state=state.setValue(BlockStateProperties.FACING,Direction.UP);
        var parts=new ArrayList<BlockStateModelPart>();Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(state).collectParts(RandomSource.create(42),parts);
        pose.pushPose();model.head.translateAndRotate(pose);pose.scale(1f/.99f,1f/.99f,1f/.99f);
        for(int i=0;i<3;i++)spike(parts,pose,collector,-.48f,-.19f+i*.19f,i==1?.75f:.55f,180,light,outline);pose.popPose();
        pose.pushPose();model.body.translateAndRotate(pose);for(int i=0;i<5;i++)spike(parts,pose,collector,.04f+i*.16f,.12f,new float[]{.65f,.9f,.8f,.65f,.45f}[i],90,light,outline);pose.popPose();
    }
    private static void spike(ArrayList<BlockStateModelPart> parts,PoseStack pose,SubmitNodeCollector collector,float y,float z,float size,float angle,int light,int outline){
        pose.pushPose();pose.translate(0,y,z);pose.mulPose(Axis.XP.rotationDegrees(angle));pose.scale(size,size,size);pose.translate(-.5f,0,-.5f);
        collector.order(1).submitBlockModel(pose,RenderTypes.cutoutMovingBlock(),parts,new int[]{-1},light,OverlayTexture.NO_OVERLAY,outline);pose.popPose();
    }
}