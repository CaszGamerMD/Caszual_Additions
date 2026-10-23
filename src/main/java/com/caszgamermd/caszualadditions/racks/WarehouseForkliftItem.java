package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Place a physical forklift, or use this item on a rack to assign the
 * nearest parked forklift one specific job. No unrequested rack shuffling.
 */
public final class WarehouseForkliftItem extends Item {
    /** How far a player can aim the controller at an empty destination rack. */
    public static final double RACK_SELECTION_REACH = 20.0D;
    private final boolean spawner;
    public WarehouseForkliftItem(Properties p){this(p,true);}
    public WarehouseForkliftItem(Properties p,boolean spawner){
        super(p);this.spawner=spawner;
    }
    private static BlockPos selected(net.minecraft.world.item.ItemStack tool,ServerLevel world){
        var data=tool.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if(data==null)return null;
        var tag=data.copyTag();
        if(!tag.getStringOr("forklift_source_dimension","")
                .equals(world.dimension().toString()))return null;
        if(!tag.contains("forklift_source_x")||!tag.contains("forklift_source_y")
                ||!tag.contains("forklift_source_z"))return null;
        return new BlockPos(tag.getIntOr("forklift_source_x",0),
                tag.getIntOr("forklift_source_y",0),tag.getIntOr("forklift_source_z",0));
    }
    private static void select(net.minecraft.world.item.ItemStack tool,ServerLevel world,BlockPos root){
        var data=tool.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var tag=data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
        tag.putString("forklift_source_dimension",world.dimension().toString());
        tag.putInt("forklift_source_x",root.getX());
        tag.putInt("forklift_source_y",root.getY());
        tag.putInt("forklift_source_z",root.getZ());
        tool.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag));
    }
    @Override public InteractionResult useOn(UseOnContext context){
        if(!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;
        BlockPos clicked=context.getClickedPos();
        var player=context.getPlayer();
        if(player==null)return InteractionResult.PASS;
        if(!spawner){
            var staged=StorageGroundPalletTransfer.wrappedController(level,clicked);
            if(staged!=null){
                select(context.getItemInHand(),level,staged.getBlockPos());
                player.sendSystemMessage(Component.literal(
                        "Forklift Controller: source pallet selected at "
                        +staged.getBlockPos().toShortString()+". Click an empty stacked rack."));
                return InteractionResult.SUCCESS;
            }
        }
        RackBlockEntity rack=RackBlock.controller(level,clicked);
        if(rack!=null){
            if(!spawner){
                BlockPos source=selected(context.getItemInHand(),level);
                if(source==null&&rack.hasPallet()){
                    select(context.getItemInHand(),level,rack.getBlockPos());
                    player.sendSystemMessage(Component.literal(
                            "Forklift Controller: source rack "
                            +rack.getBlockPos().toShortString()
                            +" selected. Click a DIFFERENT empty rack to deliver."));
                    return InteractionResult.SUCCESS;
                }
                if(source!=null&&rack.hasPallet()){
                    player.sendSystemMessage(Component.literal(
                            source.equals(rack.getBlockPos())
                                ?"Forklift Controller: same 2x2x3 rack; all 12 frame "
                                 +"blocks share one pallet. Select a different empty rack."
                                :"Forklift Controller: destination rack "
                                 +rack.getBlockPos().toShortString()
                                 +" already contains a pallet. Nothing overwritten."));
                    return InteractionResult.SUCCESS;
                }
            }
            return dispatch(level,player,context.getItemInHand(),rack);
        }
        if(!spawner||context.getClickedFace()!=net.minecraft.core.Direction.UP)
            return InteractionResult.PASS;
        BlockPos at=clicked.above();
        // Reserve a 2x2 painted parking bay, not merely a decorative
        // particle. Its cells physically reject later block placements.
        for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++){
            BlockPos tile=at.offset(dx,0,dz);
            if(!level.hasChunkAt(tile)||!level.getBlockState(tile).canBeReplaced()
                    ||!level.getBlockState(tile.above()).canBeReplaced()
                    ||!level.getBlockState(tile.below()).isCollisionShapeFullBlock(
                            level,tile.below()))
                return InteractionResult.FAIL;
        }
        WarehouseForklift lift=RackContent.FORKLIFT_TYPE.create(level,EntitySpawnReason.EVENT);
        if(lift==null)return InteractionResult.FAIL;
        lift.setPos(at.getX()+1.0,at.getY(),at.getZ()+1.0);
        lift.setYRot(player.getYRot());
        java.util.List<BlockPos> painted=new java.util.ArrayList<>();
        for(int dx=0;dx<2;dx++)for(int dz=0;dz<2;dz++){
            BlockPos tile=at.offset(dx,0,dz);
            if(!level.setBlock(tile,RackContent.FORKLIFT_PARKING.defaultBlockState(),3)){
                for(BlockPos rollback:painted)level.removeBlock(rollback,false);
                return InteractionResult.FAIL;
            }
            painted.add(tile);
        }
        lift.setParking(at.offset(1,0,1));
        if(!level.addFreshEntity(lift)){
            for(BlockPos rollback:painted)level.removeBlock(rollback,false);
            return InteractionResult.FAIL;
        }
        if(!player.getAbilities().instabuild)context.getItemInHand().shrink(1);
        player.sendSystemMessage(Component.literal(
                "Forklift parked in a protected 2x2 bay. Use Warehouse Scanner in FORKLIFT "
                +"mode to summon and assign a Golem driver."));
        return InteractionResult.SUCCESS;
    }

    /** Item.use is invoked after an ordinary block click misses (beyond the
     * vanilla interaction range). Trace a REAL unobstructed line of sight
     * on both client and server; we never select a rack behind a wall.
     * Only empty rack destinations can be chosen at extended range.
     */
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand){
        if(spawner)return InteractionResult.PASS;
        Vec3 eye=player.getEyePosition();
        Vec3 end=eye.add(player.getViewVector(1.0F).scale(RACK_SELECTION_REACH));
        BlockHitResult hit=level.clip(new ClipContext(eye,end,
                ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        if(hit.getType()!=HitResult.Type.BLOCK)return InteractionResult.PASS;
        RackBlockEntity rack=RackBlock.controller(level,hit.getBlockPos());
        if(rack==null||rack.hasPallet())return InteractionResult.PASS;
        if(!(level instanceof ServerLevel server))return InteractionResult.SUCCESS;
        // Recheck server-side using the server's own view vector and world.
        if(eye.distanceToSqr(hit.getLocation())>RACK_SELECTION_REACH*RACK_SELECTION_REACH)
            return InteractionResult.FAIL;
        return dispatch(server,player,player.getItemInHand(hand),rack);
    }

    /** Close-range useOn and long-range targeting share the same validated
     * dispatch logic, so a remote order cannot bypass forklift safety rules. */
    private InteractionResult dispatch(ServerLevel level,Player player,ItemStack tool,
            RackBlockEntity rack){
        BlockPos root=rack.getBlockPos();
        if(!level.hasChunkAt(root)||!level.mayInteract(player,root)
                ||!player.mayUseItemAt(root,net.minecraft.core.Direction.UP,tool))
            return InteractionResult.FAIL;
        BlockPos selectedSource=spawner?null:selected(tool,level);
        RackBlockEntity sourceRack=selectedSource==null?null
                :RackBlock.controller(level,selectedSource);
        if(sourceRack!=null&&sourceRack.getBlockPos().equals(root)){
            player.sendSystemMessage(Component.literal(
                    "Forklift Controller: source and destination resolve to "
                    +"the same 2x2x3 rack "+root.toShortString()));
            return InteractionResult.SUCCESS;
        }
        if(!rack.hasPallet()&&!rack.isEmpty()){
            player.sendSystemMessage(Component.literal(
                    "Forklift Controller: destination rack "+root.toShortString()
                    +" contains "+rack.occupiedSlots()
                    +" stored item stacks despite no visible pallet. "
                    +"Cannot overwrite them."));
            return InteractionResult.SUCCESS;
        }
        WarehouseForklift nearest=null;
        double best=24*24;
        boolean onboard=false;
        for(WarehouseForklift lift:level.getEntitiesOfClass(
                WarehouseForklift.class,new AABB(root).inflate(24),f->!f.busy())){
            if(lift.hasOnboardPallet()){onboard=true;continue;}
            double d=lift.blockPosition().distSqr(root);
            if(d<best){best=d;nearest=lift;}
        }
        if(nearest==null){
            player.sendSystemMessage(Component.literal(onboard
                    ?"Forklift Controller: idle forklift still has a packed "
                     +"pallet in its Jack. Recover or finish that load before dispatch."
                    :"Forklift Controller: no empty idle forklift within "
                     +"24 blocks of this rack."));
            return InteractionResult.SUCCESS;
        }
        boolean started=sourceRack!=null&&sourceRack.hasPallet()
                ?nearest.orderRackToRack(level,sourceRack,rack)
                :nearest.order(level,rack,selectedSource);
        if(started&&!spawner)
            tool.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        player.sendSystemMessage(Component.literal(started
                ?"Forklift dispatched: "+(rack.hasPallet()
                    ?"retrieve rack pallet to aisle"
                    :"collect wrapped floor pallet and place into rack")
                :"Forklift cannot start: "+nearest.status()
                    +" | rack "+root.toShortString()
                    +". Stored items and packed pallets were not changed."));
        return InteractionResult.SUCCESS;
    }
}
