package dev.casz.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;

/**
 * Full-body visual replacement. Fish are rendered as small animated mesh parts,
 * rather than spawned as real entities; stored contents come from the synced chest
 * cosmetic stack so remote players see the same four fish.
 */
public final class WearableAquariumLayer extends RenderLayer<AvatarRenderState, PlayerModel> {
    private static final Identifier GLASS = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/glass_clear.png");
    private static final Identifier WATER = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/contained_water.png");
    private static final Identifier FRAME = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/frame.png");
    private static final Identifier FISH_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private final ModelPart shell, water, frame;
    private final ModelPart fishBody, fishAccent;

    public WearableAquariumLayer(RenderLayerParent<AvatarRenderState,PlayerModel> renderer, boolean slim) {
        super(renderer);
        shell = makeHumanoid(slim, 0f, false);
        water = makeHumanoid(slim, .50f, false);
        frame = makeHumanoid(slim, 0f, true);
        var fishMesh = new MeshDefinition();
        fishMesh.getRoot().addOrReplaceChild("fish",
            CubeListBuilder.create().texOffs(0,0).addBox(-2f,-.9f,-.7f,4f,1.8f,1.4f)
                .texOffs(0,8).addBox(-3.5f,-.65f,-.25f,1.5f,1.3f,.5f),
            PartPose.ZERO);
        fishMesh.getRoot().addOrReplaceChild("accent",
            CubeListBuilder.create().texOffs(4,8).addBox(-.8f,-1.5f,-.4f,1.5f,.6f,.8f)
                .texOffs(8,8).addBox(.2f,.7f,-.4f,1.4f,.8f,.8f),
            PartPose.ZERO);
        var baked=LayerDefinition.create(fishMesh,16,16).bakeRoot();
        fishBody=baked.getChild("fish");
        fishAccent=baked.getChild("accent");
    }

    private static void add(MeshDefinition mesh,String part,float x,float y,float z,float w,float h,float d,int u,int v,float inset,boolean rail) {
        CubeListBuilder cubes;
        if (!rail) {
            cubes=CubeListBuilder.create().texOffs(u,v).addBox(x+inset,y+inset,z+inset,w-inset*2,h-inset*2,d-inset*2);
        } else {
            float b=.65f; cubes=CubeListBuilder.create();
            for(int i=0;i<2;i++)for(int k=0;k<2;k++)
                cubes.texOffs(0,0).addBox(x+i*(w-b),y,z+k*(d-b),b,h,b);
            for(int i=0;i<2;i++){
                float yy=y+i*(h-b);
                cubes.texOffs(0,0).addBox(x,yy,z,w,b,b)
                    .texOffs(0,0).addBox(x,yy,z+d-b,w,b,b)
                    .texOffs(0,0).addBox(x,yy,z,b,b,d)
                    .texOffs(0,0).addBox(x+w-b,yy,z,b,b,d);
            }
        }
        mesh.getRoot().addOrReplaceChild(part,cubes,PartPose.ZERO);
    }

    private static ModelPart makeHumanoid(boolean slim,float inset,boolean rails) {
        MeshDefinition mesh=new MeshDefinition();
        add(mesh,"head",-4,-8,-4,8,8,8,0,0,inset,rails);
        add(mesh,"body",-4,0,-2,8,12,4,16,16,inset,rails);
        float width=slim?3:4, armX=slim?-1.5f:-2f;
        add(mesh,"left_arm",armX,-2,-2,width,12,4,40,16,inset,rails);
        add(mesh,"right_arm",armX,-2,-2,width,12,4,40,16,inset,rails);
        add(mesh,"left_leg",-2,0,-2,4,12,4,0,16,inset,rails);
        add(mesh,"right_leg",-2,0,-2,4,12,4,0,16,inset,rails);
        return LayerDefinition.create(mesh,64,64).bakeRoot();
    }

    private void drawPart(ModelPart geometry,ModelPart original,PoseStack pose,
                          SubmitNodeCollector collector,int light,int outline,
                          Identifier texture,boolean transparent,int order) {
        pose.pushPose();
        original.translateAndRotate(pose);
        collector.order(order).submitModelPart(geometry,pose,
            transparent?RenderTypes.entityTranslucent(texture):RenderTypes.entityCutout(texture),
            light,OverlayTexture.NO_OVERLAY,null,-1,null,outline);
        pose.popPose();
    }

    private void drawAll(ModelPart pieces,PlayerModel player,PoseStack pose,
                         SubmitNodeCollector collector,int light,int outline,
                         Identifier texture,boolean transparent,int order) {
        drawPart(pieces.getChild("head"),player.head,pose,collector,light,outline,texture,transparent,order);
        drawPart(pieces.getChild("body"),player.body,pose,collector,light,outline,texture,transparent,order);
        drawPart(pieces.getChild("left_arm"),player.leftArm,pose,collector,light,outline,texture,transparent,order);
        drawPart(pieces.getChild("right_arm"),player.rightArm,pose,collector,light,outline,texture,transparent,order);
        drawPart(pieces.getChild("left_leg"),player.leftLeg,pose,collector,light,outline,texture,transparent,order);
        drawPart(pieces.getChild("right_leg"),player.rightLeg,pose,collector,light,outline,texture,transparent,order);
    }

    private static int color(ItemStack fish) {
        if(fish.is(Items.TROPICAL_FISH_BUCKET))
            return 0xff000000|fish.getOrDefault(DataComponents.TROPICAL_FISH_BASE_COLOR,DyeColor.ORANGE).getTextureDiffuseColor();
        if(fish.is(Items.SALMON_BUCKET)) return 0xffe99b86;
        if(fish.is(Items.PUFFERFISH_BUCKET)) return 0xffedcb54;
        return 0xffa7a078;
    }
    private static int accent(ItemStack fish) {
        return fish.is(Items.TROPICAL_FISH_BUCKET)
            ? 0xff000000|fish.getOrDefault(DataComponents.TROPICAL_FISH_PATTERN_COLOR,DyeColor.WHITE).getTextureDiffuseColor()
            : 0xffe1e3cb;
    }

    @Override
    public void submit(PoseStack pose,SubmitNodeCollector collector,int light,
                       AvatarRenderState state,float yaw,float pitch) {
        if (state.isSpectator || state.isInvisible || !SpecialCosmetics.isWearableAquarium(state.chestEquipment)) return;
        PlayerModel player=getParentModel();
        pose.pushPose();
        player.root().translateAndRotate(pose);
        drawAll(water,player,pose,collector,light,state.outlineColor,WATER,true,0);

        // At most four fish in the entire cosmetic, all swimming inside the torso.
        var stored=state.chestEquipment.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        var fish=stored.nonEmptyItemCopyStream().filter(SpecialCosmetics::isAquariumFishBucket).limit(4).toList();
        for(int i=0;i<fish.size();i++){
            ItemStack bucket=fish.get(i);
            float time=state.ageInTicks, phase=i*1.8f;
            float x=(float)Math.sin(time*.045f+phase)*.085f;
            float y=(2.0f+i*2.45f)/16f+(float)Math.sin(time*.071f+phase)*.013f;
            float z=(float)Math.cos(time*.058f+phase)*.028f;
            pose.pushPose();
            player.body.translateAndRotate(pose);
            pose.translate(x,y,z);
            pose.mulPose(Axis.YP.rotationDegrees(90f+(float)Math.sin(time*.038f+phase)*65f+(i%2==0?0:180)));
            pose.scale(.75f,.75f,.75f);
            collector.order(1).submitModelPart(fishBody,pose,RenderTypes.entityCutout(FISH_TEXTURE),
                light,OverlayTexture.NO_OVERLAY,null,color(bucket),null,state.outlineColor);
            collector.order(1).submitModelPart(fishAccent,pose,RenderTypes.entityCutout(FISH_TEXTURE),
                light,OverlayTexture.NO_OVERLAY,null,accent(bucket),null,state.outlineColor);
            pose.popPose();
        }
        drawAll(shell,player,pose,collector,light,state.outlineColor,GLASS,true,2);
        drawAll(frame,player,pose,collector,light,state.outlineColor,FRAME,false,3);
        pose.popPose();
    }
}
