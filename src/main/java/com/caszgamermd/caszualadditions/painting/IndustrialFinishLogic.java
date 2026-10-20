package com.caszgamermd.caszualadditions.painting;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;

/** Server-authoritative paint placement and removal. */
public final class IndustrialFinishLogic {
    private IndustrialFinishLogic(){}
    public static boolean warehouse(BlockState state){
        var id=net.minecraft.core.registries.BuiltInRegistries.BLOCK
                .getKey(state.getBlock());
        return id!=null&&id.getNamespace().equals("caszual_additions")
                &&(id.getPath().contains("rack")
                    ||id.getPath().contains("pallet")
                    ||id.getPath().contains("ptp")
                    ||id.getPath().contains("warehouse")
                    ||id.getPath().contains("scaffolding")
                    ||id.getPath().contains("industrial_grade_concrete")
                    ||id.getPath().contains("storage_golem_shelf")
                    ||id.getPath().contains("shulker_tank")
                    ||id.getPath().contains("plastic_curtains"));
    }
    public static IndustrialFinishEntity existing(ServerLevel level,BlockPos pos){
        // Tiny search, never a dimension-wide scan.
        List<IndustrialFinishEntity> coatings=level.getEntitiesOfClass(
                IndustrialFinishEntity.class,new AABB(pos).inflate(.55),
                e->e.anchor().equals(pos)&&!e.isRemoved());
        return coatings.isEmpty()?null:coatings.getFirst();
    }
    public static String apply(ServerLevel level,BlockPos pos,boolean primer,int rgb){
        if(!level.hasChunkAt(pos)||level.getBlockState(pos).isAir())
            return "No solid block at that position.";
        BlockState state=level.getBlockState(pos);
        if(state.getFluidState().isSource()&&!state.getFluidState().isEmpty())
            return "Liquids cannot be painted.";
        IndustrialFinishEntity old=existing(level,pos);
        if(!primer&&!warehouse(state)&&(old==null||!old.primed()))
            return "Apply Industrial Primer to this non-warehouse block first.";
        if(old!=null){
            old.finish(primer,primer?0xffb0b5b9:rgb);
            return primer?"Surface primed. Ready for paint.":"Industrial paint applied.";
        }
        IndustrialFinishEntity coating=IndustrialPaintContent.FINISH.create(level,
                net.minecraft.world.entity.EntitySpawnReason.EVENT);
        if(coating==null)return "Unable to create coating.";
        coating.attach(level,pos,primer,primer?0xffb0b5b9:rgb);
        if(!level.addFreshEntity(coating))return "Unable to attach coating.";
        return primer?"Surface primed. Ready for paint.":"Industrial paint applied.";
    }
    public static boolean remove(ServerLevel level,BlockPos pos){
        IndustrialFinishEntity old=existing(level,pos);
        if(old==null)return false;
        old.discard();
        return true;
    }
}