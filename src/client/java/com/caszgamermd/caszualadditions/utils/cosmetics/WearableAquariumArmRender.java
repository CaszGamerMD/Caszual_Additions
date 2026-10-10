package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * The first-person companion to the full-body WearableAquariumLayer.
 *
 * Reuses the exact arm meshes, textures, inset and narrow frame of the worn
 * aquarium. The normal first-person skin hand is replaced, not textured over.
 * No actual water or entities are spawned and held items remain vanilla.
 */
public final class WearableAquariumArmRender {
    private static final ModelPart WIDE_WATER=WearableAquariumLayer.bakeArm(false,.07f,false);
    private static final ModelPart WIDE_GLASS=WearableAquariumLayer.bakeArm(false,0,false);
    private static final ModelPart WIDE_FRAME=WearableAquariumLayer.bakeArm(false,0,true);
    private static final ModelPart SLIM_WATER=WearableAquariumLayer.bakeArm(true,.07f,false);
    private static final ModelPart SLIM_GLASS=WearableAquariumLayer.bakeArm(true,0,false);
    private static final ModelPart SLIM_FRAME=WearableAquariumLayer.bakeArm(true,0,true);

    private WearableAquariumArmRender(){}

    /** Called from ItemInHandRenderer's first-person hand transform. */
    public static void firstPerson(PoseStack pose,SubmitNodeCollector collector,int light,boolean slim){
        pose.pushPose();
        // First-person arm anchor starts at Y=0; the full-body arm is -2..10.
        // Retain vanilla hand reach/attack transforms applied by the caller.
        pose.scale(1f/16f,-1f/16f,1f/16f);
        pose.translate(0,2,0);
        submit(slim?SLIM_WATER:WIDE_WATER,pose,collector,
               WearableAquariumLayer.WATER,light,true,0);
        submit(slim?SLIM_GLASS:WIDE_GLASS,pose,collector,
               WearableAquariumLayer.GLASS,light,true,2);
        submit(slim?SLIM_FRAME:WIDE_FRAME,pose,collector,
               WearableAquariumLayer.FRAME,light,false,3);
        pose.popPose();
    }

    /** Used by AvatarRenderer.renderHand, including the map-hand path. */
    public static void renderHand(ModelPart sourceArm,PoseStack pose,
                                  SubmitNodeCollector collector,int light,boolean slim){
        pose.pushPose();
        sourceArm.resetPose();
        sourceArm.translateAndRotate(pose);
        submit(slim?SLIM_WATER:WIDE_WATER,pose,collector,
               WearableAquariumLayer.WATER,light,true,0);
        submit(slim?SLIM_GLASS:WIDE_GLASS,pose,collector,
               WearableAquariumLayer.GLASS,light,true,2);
        submit(slim?SLIM_FRAME:WIDE_FRAME,pose,collector,
               WearableAquariumLayer.FRAME,light,false,3);
        pose.popPose();
    }

    private static void submit(ModelPart part,PoseStack pose,
                               SubmitNodeCollector collector,
                               net.minecraft.resources.Identifier texture,
                               int light,boolean transparent,int order){
        part.visible=true;
        collector.order(order).submitModelPart(
            part,pose,transparent?RenderTypes.entityTranslucent(texture):RenderTypes.entityCutout(texture),
            light,OverlayTexture.NO_OVERLAY,null,-1,null,0);
    }
}
