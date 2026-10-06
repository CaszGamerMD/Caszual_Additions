package dev.casz.utils.cosmetics;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
public final class ArmorStandCosmeticLayer extends RenderLayer<ArmorStandRenderState,ArmorStandArmorModel>{
    public ArmorStandCosmeticLayer(RenderLayerParent<ArmorStandRenderState,ArmorStandArmorModel> parent){super(parent);}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,ArmorStandRenderState state,float yaw,float pitch){
        if(state.isInvisible)return; var model=getParentModel(); pose.pushPose(); model.root().translateAndRotate(pose);
        var head=state.headEquipment; var chest=state.chestEquipment; var legs=state.legsEquipment; var feet=state.feetEquipment;
        if(SpecialCosmetics.isSnowGolem(head,chest,legs,feet)){new SnowGolemCosmeticRender().body(model,pose,collector,light,state.outlineColor);pose.popPose();return;}
        if(SpecialCosmetics.isEndRod(head)){pose.pushPose();model.head.translateAndRotate(pose);pose.translate(0,-.125f,.0625f);pose.scale(1,-1,-1);EndRodRender.block(head,pose,collector,state.outlineColor);pose.popPose();}
        if(SpecialCosmetics.isCrystalCluster(head)) CrystalCosmeticRender.spikes(head,model,pose,collector,light,state.outlineColor);
        if(FlowerCrownRender.isFlower(head)) FlowerCrownRender.crown(head,model.head,pose,collector,light,state.outlineColor);
        if(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(head.getItem()).getPath().equals("lightning_rod")) HeadItemCosmeticRender.lightningRod(head,model.head,pose,collector,light,state.outlineColor);
        if(SpecialCosmetics.isEndRod(chest)){EndRodRender.limb(chest,model.rightArm,pose,collector,-.0625f,-.125f,state.outlineColor);EndRodRender.limb(chest,model.leftArm,pose,collector,.0625f,-.125f,state.outlineColor);EndRodRender.ribCage(chest,model.body,pose,collector,state.outlineColor);}
        if(SpecialCosmetics.rodLegs(legs,feet)){var rods=SpecialCosmetics.isEndRod(legs)?legs:feet;if(SpecialCosmetics.isEndRod(rods)){EndRodRender.limb(rods,model.rightLeg,pose,collector,0,0,state.outlineColor);EndRodRender.limb(rods,model.leftLeg,pose,collector,0,0,state.outlineColor);}}
        if(head.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)) OceanCosmeticRender.head(head,model.head,pose,collector,light,state.outlineColor);
        if(chest.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)) OceanCosmeticRender.chest(chest,model.body,model.leftArm,model.rightArm,pose,collector,light,state.outlineColor);
        if(legs.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)) OceanCosmeticRender.legs(legs,model.leftLeg,model.rightLeg,pose,collector,light,state.outlineColor);
        if(feet.is(net.minecraft.world.item.Items.NAUTILUS_SHELL)) OceanCosmeticRender.feet(feet,model.leftLeg,model.rightLeg,pose,collector,light,state.outlineColor);
        boolean bh=SpecialCosmetics.isBone(head),bc=SpecialCosmetics.isBone(chest),bl=SpecialCosmetics.isBone(legs)||SpecialCosmetics.isBone(feet);
        if(bh||bc||bl){var bone=bh?head:bc?chest:SpecialCosmetics.isBone(legs)?legs:feet;BoneCosmeticRender.skeleton(new BoneCosmeticRender.PlayerModelAccess(){
            public net.minecraft.client.model.geom.ModelPart head(){return model.head;} public net.minecraft.client.model.geom.ModelPart body(){return model.body;}
            public net.minecraft.client.model.geom.ModelPart leftArm(){return model.leftArm;} public net.minecraft.client.model.geom.ModelPart rightArm(){return model.rightArm;}
            public net.minecraft.client.model.geom.ModelPart leftLeg(){return model.leftLeg;} public net.minecraft.client.model.geom.ModelPart rightLeg(){return model.rightLeg;}
        },pose,collector,bone,bh,bc,bl,light,state.outlineColor);}
        pose.popPose();
    }
}
