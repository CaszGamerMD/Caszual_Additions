package com.caszgamermd.caszualadditions.utils.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
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
    static final Identifier GLASS = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/glass_clear.png");
    static final Identifier WATER = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/contained_water.png");
    static final Identifier FRAME = Identifier.fromNamespaceAndPath("linked_aquariums", "textures/block/frame.png");
    private static final Identifier FISH_TEXTURE = Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final Identifier GUARDIAN_TEXTURE = Identifier.withDefaultNamespace("textures/entity/guardian/guardian.png");
    private static final Identifier COD_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/cod.png");
    private static final Identifier SALMON_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/salmon.png");
    private static final Identifier TROPICAL_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/tropical_a.png");
    private static final Identifier PUFFER_TEXTURE = Identifier.withDefaultNamespace("textures/entity/fish/pufferfish.png");
    private final ModelPart shell, water, frame;
    private final ModelPart fishBody, fishAccent, guardianBody, guardianEye;
    // These are Minecraft's actual baked fish/Guardian body geometries, shared
    // with the corresponding vanilla entity renderers used by the placed tank.
    private final ModelPart codModel,salmonModel,tropicalModel,pufferModel,guardianVanillaModel;

    public WearableAquariumLayer(RenderLayerParent<AvatarRenderState,PlayerModel> renderer, boolean slim, EntityRendererProvider.Context context) {
        super(renderer);
        // A 0.02-pixel inset separates the glass skin from the outer frame.
        // The old flush geometry z-fought with the frame on its outer edges.
        shell = makeHumanoid(slim, .02f, false);
        // Almost flush with the glass. The old .5-pixel inset left an obvious
        // empty-looking rim around the wearer (especially at the head).
        water = makeHumanoid(slim, .07f, false);
        frame = makeHumanoid(slim, 0f, true);
        var fishMesh = new MeshDefinition();
        fishMesh.getRoot().addOrReplaceChild("fish",
            CubeListBuilder.create().texOffs(0,0).addBox(-1.5f,-.65f,-.45f,3f,1.3f,.9f)
                .texOffs(0,8).addBox(-2.5f,-.7f,-.16f,1f,1.4f,.32f),
            PartPose.ZERO);
        fishMesh.getRoot().addOrReplaceChild("accent",
            CubeListBuilder.create().texOffs(4,8).addBox(-.6f,-1.10f,-.25f,1f,.45f,.5f)
                .texOffs(8,8).addBox(.2f,.58f,-.25f,1.1f,.6f,.5f),
            PartPose.ZERO);
        var baked=LayerDefinition.create(fishMesh,16,16).bakeRoot();
        fishBody=baked.getChild("fish");
        fishAccent=baked.getChild("accent");
        var gMesh=new MeshDefinition();
        var spikes=CubeListBuilder.create().texOffs(0,0).addBox(-2f,-2f,-2f,4f,4f,4f)
           .texOffs(0,8).addBox(-.35f,-3f,-.35f,.7f,1.0f,.7f)
           .texOffs(0,8).addBox(-.35f,2f,-.35f,.7f,1.0f,.7f)
           .texOffs(0,8).addBox(-3f,-.35f,-.35f,1f,.7f,.7f)
           .texOffs(0,8).addBox(2f,-.35f,-.35f,1f,.7f,.7f)
           .texOffs(0,8).addBox(-.35f,-.35f,-3f,.7f,.7f,1f)
           .texOffs(0,8).addBox(-.35f,-.35f,2f,.7f,.7f,1f);
        gMesh.getRoot().addOrReplaceChild("body",spikes,PartPose.ZERO);
        gMesh.getRoot().addOrReplaceChild("eye",CubeListBuilder.create().texOffs(0,0)
           .addBox(-.9f,-.9f,-2.25f,1.8f,1.8f,.35f),PartPose.ZERO);
        var guardianModel=LayerDefinition.create(gMesh,32,32).bakeRoot();
        guardianBody=guardianModel.getChild("body");guardianEye=guardianModel.getChild("eye");
        codModel=context.bakeLayer(ModelLayers.COD);
        salmonModel=context.bakeLayer(ModelLayers.SALMON);
        tropicalModel=context.bakeLayer(ModelLayers.TROPICAL_FISH_SMALL);
        pufferModel=context.bakeLayer(ModelLayers.PUFFERFISH_SMALL);
        guardianVanillaModel=context.bakeLayer(ModelLayers.GUARDIAN);
    }

    private static void add(MeshDefinition mesh,String part,float x,float y,float z,float w,float h,float d,int u,int v,float inset,boolean rail) {
        CubeListBuilder cubes;
        if (!rail) {
            cubes=CubeListBuilder.create().texOffs(u,v).addBox(x+inset,y+inset,z+inset,w-inset*2,h-inset*2,d-inset*2);
        } else {
            float b=.23f; cubes=CubeListBuilder.create();
            // Twelve frame segments touch at their ends but never overlap in
            // volume. The original full-length top/bottom rails intersected
            // the corner uprights and drew duplicate coplanar faces.
            for(int i=0;i<2;i++)for(int k=0;k<2;k++)
                cubes.texOffs(0,0).addBox(x+i*(w-b),y,z+k*(d-b),b,h,b);
            for(int i=0;i<2;i++){
                float yy=y+i*(h-b);
                cubes.texOffs(0,0).addBox(x+b,yy,z,w-2*b,b,b)
                    .texOffs(0,0).addBox(x+b,yy,z+d-b,w-2*b,b,b)
                    .texOffs(0,0).addBox(x,yy,z+b,b,b,d-2*b)
                    .texOffs(0,0).addBox(x+w-b,yy,z+b,b,b,d-2*b);
            }
        }
        mesh.getRoot().addOrReplaceChild(part,cubes,PartPose.ZERO);
    }

    /** Reuse the third-person mesh for matching first-person arm geometry. */
    static ModelPart bakeArm(boolean slim,float inset,boolean rails){
        return makeHumanoid(slim,inset,rails).getChild("right_arm");
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
        // setupAnim hides the original player parts when a wearable is equipped.
        // The aquarium itself is independent baked geometry and must be visible
        // even when the original player's model is completely hidden.
        pose.pushPose();
        original.translateAndRotate(pose);
        geometry.visible = true;
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

    private static boolean guardian(ItemStack stack) {
        if(!stack.is(Items.AIR) && net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(
              Identifier.fromNamespaceAndPath("linked_aquariums","mob_net"))) {
            return stack.getOrDefault(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.EMPTY)
               .copyTag().getString("terrarium_type").orElse("").equals("minecraft:guardian");
        }
        return false;
    }
    private static int cost(ItemStack stack){return guardian(stack)?2:SpecialCosmetics.isAquariumFishBucket(stack)?1:0;}
    private static int color(ItemStack fish) {
        if(guardian(fish))return 0xff428b80;
        if(fish.is(Items.TROPICAL_FISH_BUCKET))
            return 0xff000000|fish.getOrDefault(DataComponents.TROPICAL_FISH_BASE_COLOR,DyeColor.ORANGE).getTextureDiffuseColor();
        if(fish.is(Items.SALMON_BUCKET)) return 0xffe99b86;
        if(fish.is(Items.PUFFERFISH_BUCKET)) return 0xffedcb54;
        return 0xffa7a078;
    }
    private static int accent(ItemStack fish) {
        if(guardian(fish))return 0xffed7d37;
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

        // Capacity belongs to the entire aquarium, with Guardians occupying two slots.
        var stored=state.chestEquipment.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY);
        var fish=new java.util.ArrayList<ItemStack>();int used=0;
        for(var candidate:stored.nonEmptyItemCopyStream().toList()){
            int weight=cost(candidate);if(weight==0||used+weight>4)continue;
            fish.add(candidate);used+=weight;
        }
        int guardianCount=0;
        for(var resident:fish)if(guardian(resident))guardianCount++;
        int guardianOrdinal=0;
        for(int i=0;i<fish.size();i++){
            ItemStack bucket=fish.get(i);
            float time=state.ageInTicks, phase=i*1.8f;
            boolean fixedGuardian=guardian(bucket);
            pose.pushPose();
            if(fixedGuardian){
                // Anchor to the head, never the swimming torso. Follow head
                // rotation: one central guardian or two eye-like positions.
                player.head.translateAndRotate(pose);
                float horizontal=guardianCount==1?0f:(guardianOrdinal==0?-.1125f:.1125f);
                guardianOrdinal++;
                pose.translate(horizontal,-.25f,-.065f);
            }else{
                float x=(float)Math.sin(time*.017f+phase)*.060f;
                float y=.02f-.28f*(float)Math.sin(time*.010f+phase);
                float z=(float)Math.cos(time*.013f+phase)*.030f;
                player.body.translateAndRotate(pose);
                pose.translate(x,y,z);
                pose.mulPose(Axis.YP.rotationDegrees(90f+(float)Math.sin(time*.009f+phase)*30f+(i%2==0?0:180)));
            }
            ModelPart resident=guardian(bucket)?guardianVanillaModel:
                bucket.is(Items.COD_BUCKET)?codModel:
                bucket.is(Items.SALMON_BUCKET)?salmonModel:
                bucket.is(Items.PUFFERFISH_BUCKET)?pufferModel:tropicalModel;
            Identifier skin=guardian(bucket)?GUARDIAN_TEXTURE:
                bucket.is(Items.COD_BUCKET)?COD_TEXTURE:
                bucket.is(Items.SALMON_BUCKET)?SALMON_TEXTURE:
                bucket.is(Items.PUFFERFISH_BUCKET)?PUFFER_TEXTURE:TROPICAL_TEXTURE;
            float scale=fixedGuardian?(guardianCount==1?.22f:.13f):.32f;
            pose.scale(scale,scale,scale);
            collector.order(1).submitModelPart(resident,pose,RenderTypes.entityCutout(skin),
                light,OverlayTexture.NO_OVERLAY,null,
                bucket.is(Items.TROPICAL_FISH_BUCKET)?color(bucket):0xFFFFFFFF,null,state.outlineColor);
            pose.popPose();
        }
        drawAll(shell,player,pose,collector,light,state.outlineColor,GLASS,true,2);
        drawAll(frame,player,pose,collector,light,state.outlineColor,FRAME,false,3);
        pose.popPose();
    }
}
