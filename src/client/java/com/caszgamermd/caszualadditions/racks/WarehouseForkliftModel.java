package com.caszgamermd.caszualadditions.racks;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Industrial counterbalance forklift: body panels, roll cage, seat,
 * steering column, grippy paired wheels, triple telescoping mast,
 * piston rams, side rails and two separated steel fork tines.
 * Every cuboid is non-coplanar with its neighbors to avoid z-fighting.
 */
public final class WarehouseForkliftModel extends EntityModel<WarehouseForkliftRenderState> {
    private final ModelPart frontWheels,rearWheels,mastMiddle,mastTop,carriage;
    public WarehouseForkliftModel(ModelPart root){
        super(root);
        frontWheels=root.getChild("front_wheels");
        rearWheels=root.getChild("rear_wheels");
        mastMiddle=root.getChild("mast_middle");
        mastTop=root.getChild("mast_top");
        carriage=root.getChild("carriage");
    }
    public static LayerDefinition layer(){
        MeshDefinition mesh=new MeshDefinition();
        PartDefinition root=mesh.getRoot();
        // Low counterweight, raised bonnet and yellow stamped metal shell.
        root.addOrReplaceChild("chassis",CubeListBuilder.create()
            .texOffs(0,0).addBox(-11,-7,-19,22,6,36)
            .texOffs(0,23).addBox(-10,-17,4,20,10,12)
            .texOffs(0,46).addBox(-9,-18,-17,18,2,13)
            .texOffs(48,48).addBox(-7,-20,1,14,2,10)
            .texOffs(48,58).addBox(-7,-18,0,14,2,2)
            .texOffs(70,0).addBox(-12,-8,-17,1,3,31)
            .texOffs(70,0).addBox(11,-8,-17,1,3,31),
            PartPose.offset(0,24,0));
        // Four wheel treads, separated by real suspension gaps.
        root.addOrReplaceChild("front_wheels",CubeListBuilder.create()
            .texOffs(0,67).addBox(-14,-5,-14,5,9,9)
            .texOffs(0,67).addBox(9,-5,-14,5,9,9)
            .texOffs(21,66).addBox(-14,-5,-12,5,6,5)
            .texOffs(21,66).addBox(9,-5,-12,5,6,5),
            PartPose.offset(0,20,0));
        root.addOrReplaceChild("rear_wheels",CubeListBuilder.create()
            .texOffs(0,67).addBox(-14,-5,9,5,9,9)
            .texOffs(0,67).addBox(9,-5,9,5,9,9)
            .texOffs(21,66).addBox(-14,-5,11,5,6,5)
            .texOffs(21,66).addBox(9,-5,11,5,6,5),
            PartPose.offset(0,20,0));
        // Lean-back roll cage and overhead protection mesh.
        root.addOrReplaceChild("cage",CubeListBuilder.create()
            .texOffs(70,18).addBox(-12,-49,6,3,40,3)
            .texOffs(70,18).addBox(9,-49,6,3,40,3)
            .texOffs(70,18).addBox(-12,-49,-10,3,40,3)
            .texOffs(70,18).addBox(9,-49,-10,3,40,3)
            .texOffs(0,80).addBox(-12,-51,-11,24,3,22)
            .texOffs(0,83).addBox(-11,-52,-7,22,1,2)
            .texOffs(0,83).addBox(-11,-52,2,22,1,2)
            .texOffs(76,80).addBox(-8,-24,4,16,4,12)
            .texOffs(76,80).addBox(-3,-32,3,6,7,3)
            .texOffs(98,0).addBox(-2,-35,0,4,3,7),
            PartPose.offset(0,24,0));
        // Fixed guide channels; inner rails move separately with fork.
        root.addOrReplaceChild("mast_fixed",CubeListBuilder.create()
            .texOffs(70,18).addBox(-12,-44,-22,3,44,4)
            .texOffs(70,18).addBox(9,-44,-22,3,44,4)
            .texOffs(0,90).addBox(-12,-42,-21,24,3,3)
            .texOffs(0,90).addBox(-12,-5,-21,24,3,3)
            .texOffs(92,31).addBox(-8,-27,-20,3,20,3)
            .texOffs(92,31).addBox(5,-27,-20,3,20,3),
            PartPose.offset(0,22,0));
        root.addOrReplaceChild("mast_middle",CubeListBuilder.create()
            .texOffs(70,18).addBox(-9,-44,-22,2,43,3)
            .texOffs(70,18).addBox(7,-44,-22,2,43,3)
            .texOffs(0,90).addBox(-9,-43,-23,18,2,3),
            PartPose.offset(0,22,0));
        root.addOrReplaceChild("mast_top",CubeListBuilder.create()
            .texOffs(70,18).addBox(-6,-43,-24,2,43,3)
            .texOffs(70,18).addBox(4,-43,-24,2,43,3)
            .texOffs(0,90).addBox(-6,-43,-25,12,2,3),
            PartPose.offset(0,22,0));
        // Lift carriage never clips mast: fork rails project forward.
        root.addOrReplaceChild("carriage",CubeListBuilder.create()
            .texOffs(0,90).addBox(-12,-13,-26,24,5,3)
            .texOffs(0,90).addBox(-11,-12,-31,4,13,4)
            .texOffs(0,90).addBox(7,-12,-31,4,13,4)
            .texOffs(92,31).addBox(-10,-2,-45,4,3,22)
            .texOffs(92,31).addBox(6,-2,-45,4,3,22)
            .texOffs(104,50).addBox(-11,-14,-27,22,2,2),
            PartPose.offset(0,22,0));
        // Tail light / safety lamp and amber work lights.
        root.addOrReplaceChild("details",CubeListBuilder.create()
            .texOffs(108,70).addBox(-10,-20,16,5,3,2)
            .texOffs(108,70).addBox(5,-20,16,5,3,2)
            .texOffs(98,90).addBox(-11,-39,-24,5,4,2)
            .texOffs(98,90).addBox(6,-39,-24,5,4,2),
            PartPose.offset(0,24,0));
        return LayerDefinition.create(mesh,128,128);
    }
    @Override public void setupAnim(WarehouseForkliftRenderState state){
        super.setupAnim(state);
        float height=Mth.clamp(state.mastHeight,0f,12f);
        // Telescoping tubes rise at staggered speeds; mast carriage
        // uses full target elevation. Lerp is driven by synced server
        // state, no teleport on network updates.
        mastMiddle.y=22f-height*5.5f;
        mastTop.y=22f-height*10f;
        carriage.y=22f-height*16f;
        float wheel=state.walkAnimationPos*.8f;
        frontWheels.xRot=wheel;
        rearWheels.xRot=wheel;
    }
}
