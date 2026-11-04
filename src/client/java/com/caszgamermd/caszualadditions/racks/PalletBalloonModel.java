package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;

/** Basket, suspension rig and large striped hot-air envelope. */
public final class PalletBalloonModel extends EntityModel<LivingEntityRenderState>{
    public PalletBalloonModel(ModelPart root){super(root);}
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        var root=mesh.getRoot();
        // Four pallet-width basket rails with an open passenger compartment.
        root.addOrReplaceChild("basket",CubeListBuilder.create()
                .texOffs(0,0).addBox(-24,-15,-24,48,4,48)
                .texOffs(0,54).addBox(-24,-11,-24,4,11,4)
                .texOffs(0,54).addBox(20,-11,-24,4,11,4)
                .texOffs(0,54).addBox(-24,-11,20,4,11,4)
                .texOffs(0,54).addBox(20,-11,20,4,11,4)
                .texOffs(0,70).addBox(-24,-11,-24,48,2,3)
                .texOffs(0,70).addBox(-24,-11,21,48,2,3)
                .texOffs(0,70).addBox(-24,-11,-21,3,2,42)
                .texOffs(0,70).addBox(21,-11,-21,3,2,42),
                PartPose.offset(0,30,0));
        // Four suspension cables from the basket corners to the envelope.
        root.addOrReplaceChild("ropes",CubeListBuilder.create()
                .texOffs(100,0).addBox(-22,-82,-22,2,53,2)
                .texOffs(100,0).addBox(20,-82,-22,2,53,2)
                .texOffs(100,0).addBox(-22,-82,20,2,53,2)
                .texOffs(100,0).addBox(20,-82,20,2,53,2),
                PartPose.offset(0,30,0));
        // The bulbous envelope is a stepped, faceted sphere built entirely
        // from vanilla cubes, with radial ribs and a contrasting cap.
        root.addOrReplaceChild("envelope",CubeListBuilder.create()
                .texOffs(0,96).addBox(-25,-111,-25,50,12,50)
                .texOffs(0,96).addBox(-31,-99,-31,62,18,62)
                .texOffs(0,96).addBox(-34,-81,-34,68,24,68)
                .texOffs(0,96).addBox(-31,-57,-31,62,18,62)
                .texOffs(0,96).addBox(-25,-39,-25,50,12,50)
                .texOffs(0,0).addBox(-15,-117,-15,30,6,30)
                .texOffs(0,0).addBox(-7,-123,-7,14,6,14),
                PartPose.offset(0,30,0));
        // Burner and heat shield above the basket.
        root.addOrReplaceChild("burner",CubeListBuilder.create()
                .texOffs(100,60).addBox(-7,-25,-7,14,5,14)
                .texOffs(100,80).addBox(-4,-31,-4,8,6,8),
                PartPose.offset(0,30,0));
        return LayerDefinition.create(mesh,160,160);
    }
}
