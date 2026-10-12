package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletBlock;
import com.caszgamermd.caszualadditions.pallet.PalletBlockEntity;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * ONE physical moving pallet and FOUR attached, animated golem models.
 * Only this entity saves or moves the cargo. No separate golem entities
 * navigate while lifting or carrying; all four corner walkers use this
 * entity's exact position and leg phase.
 */
public final class MovingPallet extends PathfinderMob {
    private static final EntityDataAccessor<ItemStack> DISPLAY_PALLET=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> DISPLAY_CARGO=
            SynchedEntityData.defineId(MovingPallet.class,EntityDataSerializers.INT);
    private String owner="";
    private final BlockPos[] shelves={BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO,BlockPos.ZERO};
    private ItemStack pallet=ItemStack.EMPTY;
    private NonNullList<ItemStack> contents=NonNullList.withSize(216,ItemStack.EMPTY);
    private boolean delivered,deathDropsReleased;

    public MovingPallet(EntityType<? extends MovingPallet> type,Level level){
        super(type,level);
        setPersistenceRequired();
    }
    public static AttributeSupplier.Builder attributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH,40)
                .add(Attributes.MOVEMENT_SPEED,.26)
                .add(Attributes.FOLLOW_RANGE,48);
    }
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){
        super.defineSynchedData(builder);
        builder.define(DISPLAY_PALLET,ItemStack.EMPTY);
        builder.define(DISPLAY_CARGO,0);
    }
    public ItemStack displayPallet(){return entityData.get(DISPLAY_PALLET);}
    public int visibleCargo(){return entityData.get(DISPLAY_CARGO);}
    public boolean owns(UUID player){return owner.equals(player.toString());}
    public boolean loaded(){return !pallet.isEmpty();}
    public boolean fromWarehouse(List<StorageGolemShelfEntity> group){
        for(BlockPos shelf:shelves){
            boolean found=false;
            for(StorageGolemShelfEntity member:group)
                if(member.getBlockPos().equals(shelf)){found=true;break;}
            if(!found)return false;
        }
        return true;
    }
    /** Populate data BEFORE spawning. The previous lead worker still
     * retains ownership until spawning is accepted by the server. */
    public boolean prepare(UUID player,List<StorageGolem> workers,
            ItemStack deck,NonNullList<ItemStack> items){
        if(!pallet.isEmpty()||deck.isEmpty()||items.size()!=216||workers.size()!=4)
            return false;
        boolean[] index=new boolean[4];
        for(StorageGolem golem:workers){
            int i=golem.crewIndex();
            if(i<0||i>=4||index[i]||!golem.ownedBy(player))return false;
            index[i]=true;
            shelves[i]=golem.shelfHome();
        }
        owner=player.toString();
        pallet=deck.copyWithCount(1);
        for(int i=0;i<216;i++)contents.set(i,items.get(i).copy());
        syncDisplay();
        return true;
    }
    private void syncDisplay(){
        entityData.set(DISPLAY_PALLET,pallet.copy());
        int used=0;
        for(ItemStack item:contents)if(!item.isEmpty())used++;
        entityData.set(DISPLAY_CARGO,Math.min(27,used));
    }
    private boolean allFit(Container dest){
        for(int i=0;i<216;i++)
            if(!contents.get(i).isEmpty()&&!dest.canPlaceItem(i,contents.get(i)))
                return false;
        return true;
    }
    private boolean verify(Container dest){
        for(int i=0;i<216;i++){
            ItemStack expected=contents.get(i),real=dest.getItem(i);
            if(expected.isEmpty()!=real.isEmpty())return false;
            if(!expected.isEmpty()&&(!ItemStack.isSameItemSameComponents(expected,real)
                    ||expected.getCount()!=real.getCount()))return false;
        }
        return true;
    }
    private void clearCargo(){
        pallet=ItemStack.EMPTY;
        contents=NonNullList.withSize(216,ItemStack.EMPTY);
        delivered=true;
        syncDisplay();
    }
    public boolean deliverToRack(RackBlockEntity rack){
        if(!loaded()||rack.hasPallet()||!rack.isEmpty())return false;
        rack.install(pallet);
        if(!rack.hasPallet()||!allFit(rack)){
            rack.dropPallet();return false;
        }
        for(int i=0;i<216;i++)if(!contents.get(i).isEmpty())
            rack.setItem(i,contents.get(i).copy());
        if(!verify(rack)){
            rack.clearContent();rack.dropPallet();return false;
        }
        clearCargo();
        return true;
    }
    public boolean deliverToGround(ServerLevel level,BlockPos pos,Direction facing){
        if(!loaded()||!(pallet.getItem() instanceof BlockItem item)
                ||!(item.getBlock() instanceof PalletBlock block))return false;
        BlockState root=block.defaultBlockState().setValue(PalletBlock.PART,0)
                .setValue(PalletBlock.FACING,facing).setValue(PalletBlock.LEGACY,false);
        for(int i=0;i<4;i++){
            BlockPos piece=PalletBlock.partPos(pos,i,root);
            if(level.isOutsideBuildHeight(piece)
                    ||!level.getWorldBorder().isWithinBounds(piece)
                    ||!level.hasChunkAt(piece)
                    ||!level.getBlockState(piece).canBeReplaced())return false;
        }
        if(!level.setBlock(pos,root,3))return false;
        boolean complete=true;
        for(int i=1;i<4;i++)
            if(!level.setBlock(PalletBlock.partPos(pos,i,root),
                    root.setValue(PalletBlock.PART,i),3)){
                complete=false;break;
            }
        PalletBlockEntity dest=level.getBlockEntity(pos)
                instanceof PalletBlockEntity be?be:null;
        if(dest==null)complete=false;
        if(complete){
            dest.initialize(pos,0,pallet);
            if(!allFit(dest))complete=false;
        }
        if(complete){
            for(int i=0;i<216;i++)if(!contents.get(i).isEmpty())
                dest.setItem(i,contents.get(i).copy());
            complete=verify(dest);
        }
        if(!complete){
            if(dest!=null)dest.clearContent();
            for(int i=0;i<4;i++){
                BlockPos where=PalletBlock.partPos(pos,i,root);
                BlockState placed=level.getBlockState(where);
                if(placed.getBlock()==block
                        &&placed.getValue(PalletBlock.PART)==i)
                    level.removeBlock(where,false);
            }
            return false;
        }
        clearCargo();
        return true;
    }
    /** Recreate precisely four *empty* walking crew members once the
     * pallet has been delivered. On partial spawn failure, roll them back
     * and retry; do not manufacture new shelf tokens. */
    private boolean restoreWorkers(ServerLevel level){
        UUID id;
        try{id=UUID.fromString(owner);}catch(IllegalArgumentException ex){return false;}
        java.util.ArrayList<StorageGolem> spawned=new java.util.ArrayList<>();
        for(int i=0;i<4;i++){
            StorageGolem golem=RackContent.STORAGE_GOLEM_TYPE.create(level,EntitySpawnReason.EVENT);
            if(golem==null){for(StorageGolem worker:spawned)worker.discard();return false;}
            golem.assignPlayerCrew(shelves[i],i,id);
            golem.setPos(getX()+(i%2==0?-.65:.65),getY(),
                    getZ()+(i<2?-.65:.65));
            if(!level.addFreshEntity(golem)){
                for(StorageGolem worker:spawned)worker.discard();
                return false;
            }
            spawned.add(golem);
        }
        discard();
        return true;
    }
    @Override public void tick(){
        super.tick();
        if(!(level() instanceof ServerLevel server))return;
        if(delivered){
            if(tickCount%20==0)restoreWorkers(server);
            return;
        }
        UUID id;
        try{id=UUID.fromString(owner);}catch(IllegalArgumentException ex){return;}
        var player=server.getPlayerByUUID(id);
        if(player==null||player.isSpectator())return;
        // Exactly one navigation and collision body for all four corner
        // figures, with no teleporting through walls or individual drift.
        double x=player.getX()-player.getDirection().getStepX()*3.2;
        double z=player.getZ()-player.getDirection().getStepZ()*3.2;
        if(distanceToSqr(x,player.getY(),z)>5)
            if(tickCount%12==0)getNavigation().moveTo(x,player.getY(),z,.88);
        if(distanceToSqr(x,player.getY(),z)<2)getNavigation().stop();
    }
    private void dropCargo(ServerLevel level){
        if(pallet.isEmpty())return;
        Containers.dropItemStack(level,getX(),getY(),getZ(),pallet.copy());
        for(ItemStack contentsItem:contents)
            if(!contentsItem.isEmpty())
                Containers.dropItemStack(level,getX(),getY(),getZ(),contentsItem.copy());
        clearCargo();
    }
    @Override public void die(net.minecraft.world.damagesource.DamageSource cause){
        if(level() instanceof ServerLevel level&&!deathDropsReleased){
            deathDropsReleased=true;
            dropCargo(level);
            for(int i=0;i<4;i++)
                Containers.dropItemStack(level,getX(),getY(),getZ(),
                        new ItemStack(RackContent.STORAGE_GOLEM_ITEM));
        }
        super.die(cause);
    }
    @Override protected void addAdditionalSaveData(ValueOutput out){
        super.addAdditionalSaveData(out);
        out.putString("owner",owner);
        out.putBoolean("delivered",delivered);
        for(int i=0;i<4;i++)out.store("shelf_"+i,BlockPos.CODEC,shelves[i]);
        if(!pallet.isEmpty())out.store("pallet",ItemStack.CODEC,pallet);
        ContainerHelper.saveAllItems(out,contents);
    }
    @Override protected void readAdditionalSaveData(ValueInput in){
        super.readAdditionalSaveData(in);
        owner=in.getStringOr("owner","");
        delivered=in.getBooleanOr("delivered",false);
        for(int i=0;i<4;i++)
            shelves[i]=in.read("shelf_"+i,BlockPos.CODEC).orElse(BlockPos.ZERO);
        pallet=in.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        contents=NonNullList.withSize(216,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        syncDisplay();
    }
}
