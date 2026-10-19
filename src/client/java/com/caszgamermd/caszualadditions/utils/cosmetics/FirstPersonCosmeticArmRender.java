package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Replace the real vanilla first-person hand mesh at AvatarRenderer.renderHand.
 *
 * The old ItemInHandRenderer TAIL hook submitted a second cosmetic arm AFTER
 * vanilla submitted its skin arm. The two models overlapped (or the second
 * appeared behind held tools), so the player's actual arm did not change.
 * Here the vanilla arm is never submitted: use its original, correctly
 * animated arm pivot for the replacement model only.
 */
public final class FirstPersonCosmeticArmRender {
    private static final ModelPart WIDE_BLOCK=arm(4,64,32);
    private static final ModelPart SLIM_BLOCK=arm(3,64,32);
    private static final ModelPart WIDE_BONE=arm(2,64,32);
    private static final ModelPart SLIM_BONE=arm(2,64,32);
    private FirstPersonCosmeticArmRender(){}

    /** A player-arm UV rectangle at the vanilla arm pivot. Geometry is
     * baked in model pixels and is scaled by ModelPart rendering just like
     * the regular skin, not manually scaled to 1/16 a second time.
     */
    private static ModelPart arm(int width,int texW,int texH){
        var mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cosmetic_arm",
                CubeListBuilder.create().texOffs(40,16)
                        .addBox(-2f-width/2f,-2,-2,width,12,4),
                PartPose.ZERO);
        return LayerDefinition.create(mesh,texW,texH)
                .bakeRoot().getChild("cosmetic_arm");
    }

    public static void drawBlock(ModelPart vanillaArm,PoseStack pose,
            SubmitNodeCollector collector,int light,boolean slim,ItemStack outfit){
        if(!(outfit.getItem() instanceof BlockItem item))return;
        draw(vanillaArm,pose,collector,light,slim?SLIM_BLOCK:WIDE_BLOCK,
                BlockOutfitTextures.texture(item.getBlock()));
    }

    public static void drawBone(ModelPart vanillaArm,PoseStack pose,
            SubmitNodeCollector collector,int light,boolean slim){
        draw(vanillaArm,pose,collector,light,slim?SLIM_BONE:WIDE_BONE,
                BlockOutfitTextures.texture(Blocks.BONE_BLOCK));
    }

    private static void draw(ModelPart vanillaArm,PoseStack pose,
            SubmitNodeCollector collector,int light,ModelPart replacement,
            Identifier texture){
        pose.pushPose();
        // Vanilla renderHand performs this same reset on every frame.
        vanillaArm.resetPose();
        vanillaArm.translateAndRotate(pose);
        replacement.visible=true;
        collector.order(1).submitModelPart(replacement,pose,
                RenderTypes.entityTranslucent(texture),light,
                OverlayTexture.NO_OVERLAY,null,-1,null,0);
        pose.popPose();
    }
}
