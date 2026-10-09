package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public final class BoneCosmeticRender {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/block/bone_block_side.png");
    private final ModelPart root = createLayer().bakeRoot();
    private final ModelPart head = root.getChild("head");
    private final ModelPart body = root.getChild("body");
    private final ModelPart leftArm = root.getChild("left_arm");
    private final ModelPart rightArm = root.getChild("right_arm");
    private final ModelPart leftLeg = root.getChild("left_leg");
    private final ModelPart rightLeg = root.getChild("right_leg");

    private static LayerDefinition createLayer() {
        var mesh = new MeshDefinition();
        var r = mesh.getRoot();
        r.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(0)), PartPose.offset(0,0,0));
        var torso = CubeListBuilder.create()
            .texOffs(0,0).addBox(-1,-1,-1,2,14,2)
            .texOffs(0,0).addBox(-4,0,-1,8,2,2)
            .texOffs(0,0).addBox(-4,4,-1,8,2,2)
            .texOffs(0,0).addBox(-4,8,-1,8,2,2)
            .texOffs(0,0).addBox(-4,11,-1,8,2,2);
        r.addOrReplaceChild("body", torso, PartPose.offset(0,0,0));
        r.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-1,2,12,2), PartPose.offset(-5,2,0));
        r.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-1,2,12,2), PartPose.offset(5,2,0));
        r.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0,0).addBox(-1,0,-1,2,12,2), PartPose.offset(-2,12,0));
        r.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(0,0).addBox(-1,0,-1,2,12,2), PartPose.offset(2,12,0));
        return LayerDefinition.create(mesh,16,16);
    }

    public void skeleton(PlayerModelAccess player, PoseStack pose, SubmitNodeCollector collector, ItemStack ignored, boolean showHead, boolean showChest, boolean showLegs, int light, int outline) {
        // Head is rendered by the vanilla skeleton-skull layer so it has a real face.
        if (showChest) {
            submitAttached(body, player.body(), pose, collector, light, outline);
            submitAttached(rightArm, player.rightArm(), pose, collector, light, outline);
            submitAttached(leftArm, player.leftArm(), pose, collector, light, outline);
        }
        if (showLegs) {
            submitAttached(rightLeg, player.rightLeg(), pose, collector, light, outline);
            submitAttached(leftLeg, player.leftLeg(), pose, collector, light, outline);
        }
    }

    private void submitAttached(ModelPart geometry, ModelPart playerPart, PoseStack pose, SubmitNodeCollector collector, int light, int outline) {
        pose.pushPose();
        playerPart.translateAndRotate(pose);
        geometry.x=geometry.y=geometry.z=0;
        geometry.xRot=geometry.yRot=geometry.zRot=0;
        collector.order(1).submitModelPart(geometry, pose, RenderTypes.entityCutout(TEXTURE), light, OverlayTexture.NO_OVERLAY, null, -1, null, outline);
        pose.popPose();
    }

    public interface PlayerModelAccess { ModelPart head(); ModelPart body(); ModelPart leftArm(); ModelPart rightArm(); ModelPart leftLeg(); ModelPart rightLeg(); }
}
