package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletData;
import com.caszgamermd.caszualadditions.pallet.PalletFilterCarrier;
import com.caszgamermd.caszualadditions.pallet.PalletMenu;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** One 216-slot pallet and filter controller for all nine transfer cells. */
public final class PtpBlockEntity extends BlockEntity
        implements Container, ExtendedMenuProvider<BlockPos>, PalletFilterCarrier {
    public static final int SLOTS=216;
    private NonNullList<ItemStack> contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
    private ItemStack pallet=ItemStack.EMPTY,filterUpgrade=ItemStack.EMPTY;
    private int filterMode,filterScope;
    private String filterTarget="",filterSignature="";
    private boolean input=true,sealed;
    public PtpBlockEntity(BlockPos pos,BlockState state){super(RackContent.PTP_ENTITY,pos,state);}
    public boolean input(){return input;}
    public void setInput(boolean v){input=v;setChanged();}
    public boolean sealed(){return sealed;}
    public void setSealed(boolean v){if(hasPallet()){sealed=v;setChanged();}}
    public boolean hasPallet(){return !pallet.isEmpty();}
    public ItemStack palletStack(){return pallet.copy();}
    public ItemStack displayItem(int slot){return slot>=0&&slot<SLOTS?contents.get(slot).copy():ItemStack.EMPTY;}
    public boolean emptyCargo(){for(ItemStack s:contents)if(!s.isEmpty())return false;return true;}
    public boolean hasAdapter(){
        if(level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int p:new int[]{1,3,7,9})
            if(level.getBlockState(PtpBlock.cell(root,p)).is(RackContent.PTP_HOPPER))return true;
        return false;
    }
    public boolean hasWrap(){
        if(level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int p:new int[]{2,4,6,8})
            if(level.getBlockState(PtpBlock.cell(root,p)).is(RackContent.PTP_WRAP))return true;
        return false;
    }
    public boolean installPallet(ItemStack stack){
        if(hasPallet()||level==null)return false;
        BlockPos root=worldPosition.offset(-1,0,-1);
        for(int y=1;y<=3;y++)for(int z=0;z<3;z++)for(int x=0;x<3;x++){
            BlockPos p=root.offset(x,y,z);
            if(level.isOutsideBuildHeight(p)||!level.hasChunkAt(p)
                    ||!level.getBlockState(p).canBeReplaced())return false;
        }
        pallet=stack.copyWithCount(1);
        filterTarget=PalletData.filter(stack);
        filterMode=PalletData.filterMode(stack);
        filterScope=PalletData.filterScope(stack);
        filterSignature=PalletData.filterSignature(stack);
        filterUpgrade=PalletData.filterInstalled(stack)
                ?new ItemStack(RackContent.PALLET_FILTER):ItemStack.EMPTY;
        setChanged();return true;
    }
    public ItemStack removePallet(){
        return !sealed&&emptyCargo()?dropPallet():ItemStack.EMPTY;
    }
    public ItemStack dropPallet(){
        if(!hasPallet())return ItemStack.EMPTY;
        ItemStack result=PalletData.applyFilterSettings(pallet,this);
        pallet=ItemStack.EMPTY;filterUpgrade=ItemStack.EMPTY;
        filterTarget="";filterSignature="";sealed=false;
        setChanged();return result;
    }
    @Override public int getContainerSize(){return SLOTS;}
    @Override public boolean isEmpty(){return emptyCargo();}
    @Override public ItemStack getItem(int slot){
        return !sealed&&slot>=0&&slot<SLOTS?contents.get(slot):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot,int count){
        if(sealed)return ItemStack.EMPTY;
        ItemStack out=ContainerHelper.removeItem(contents,slot,count);
        if(!out.isEmpty())setChanged();return out;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        if(sealed)return ItemStack.EMPTY;
        ItemStack out=ContainerHelper.takeItem(contents,slot);
        if(!out.isEmpty())setChanged();return out;
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return !sealed&&hasPallet()&&allows(stack);
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=SLOTS||sealed||(!stack.isEmpty()&&!canPlaceItem(slot,stack)))return;
        contents.set(slot,stack.copy());setChanged();
    }
    @Override public void clearContent(){
        if(!sealed){contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);setChanged();}
    }
    @Override public boolean stillValid(Player player){
        return !sealed&&Container.stillValidBlockEntity(this,player,6.0F);
    }
    @Override public Component getDisplayName(){
        return Component.literal("Pallet Transfer Point - "+(input?"Input":"Output"));
    }
    @Override public @Nullable AbstractContainerMenu createMenu(int id,Inventory inv,Player player){
        return hasPallet()&&!sealed?new PalletMenu(id,inv,this):null;
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){return worldPosition;}
    @Override public ItemStack filterUpgrade(){return filterUpgrade.copy();}
    @Override public void setFilterUpgrade(ItemStack value){
        filterUpgrade=value.is(RackContent.PALLET_FILTER)?value.copyWithCount(1):ItemStack.EMPTY;setChanged();
    }
    @Override public int filterMode(){return filterMode;}
    @Override public void setFilterMode(int v){filterMode=Math.clamp(v,0,1);setChanged();}
    @Override public int filterScope(){return filterScope;}
    @Override public void setFilterScope(int v){filterScope=Math.clamp(v,0,3);setChanged();}
    @Override public String filterTarget(){return filterTarget;}
    @Override public void setFilterTarget(String v){filterTarget=v;setChanged();}
    @Override public String filterComponentSignature(){return filterSignature;}
    @Override public void setFilterComponentSignature(String v){filterSignature=v;setChanged();}
    @Override public void setChanged(){
        super.setChanged();
        if(level!=null){BlockState s=getBlockState();level.sendBlockUpdated(worldPosition,s,s,3);}
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        ContainerHelper.saveAllItems(out,contents);
        if(hasPallet())out.store("pallet",ItemStack.CODEC,pallet);
        if(!filterUpgrade.isEmpty())out.store("filter_upgrade",ItemStack.CODEC,filterUpgrade);
        out.putString("filter_target",filterTarget);
        out.putString("filter_signature",filterSignature);
        out.putInt("filter_mode",filterMode);
        out.putInt("filter_scope",filterScope);
        out.putBoolean("input",input);out.putBoolean("sealed",sealed);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        contents=NonNullList.withSize(SLOTS,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        pallet=in.read("pallet",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        filterUpgrade=in.read("filter_upgrade",ItemStack.CODEC).orElse(ItemStack.EMPTY);
        filterTarget=in.getStringOr("filter_target","");
        filterSignature=in.getStringOr("filter_signature","");
        filterMode=Math.clamp(in.getIntOr("filter_mode",0),0,1);
        filterScope=Math.clamp(in.getIntOr("filter_scope",0),0,3);
        input=in.getBooleanOr("input",true);
        sealed=in.getBooleanOr("sealed",false)&&hasPallet();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup){return saveWithoutMetadata(lookup);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
