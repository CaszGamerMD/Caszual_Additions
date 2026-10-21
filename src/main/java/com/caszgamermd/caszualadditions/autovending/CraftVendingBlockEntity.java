package com.caszgamermd.caszualadditions.autovending;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/** Nine real, reusable recipe samples + 27 real stocked ingredient slots.
 * Every vending operation is simulated against a copy of the cabinet FIRST,
 * and commits only when ALL configured crafts are possible.
 */
public final class CraftVendingBlockEntity extends BlockEntity
        implements Container,ExtendedMenuProvider<BlockPos> {
    public static final int RECIPE_SLOTS=9,STOCK_SLOTS=27,TOTAL=36;
    public static final int MAX_BATCHES=16;
    private NonNullList<ItemStack> contents=NonNullList.withSize(TOTAL,ItemStack.EMPTY);
    private int batches=1;
    private UUID lastCustomer;
    private long lastVendTick=Long.MIN_VALUE;

    public CraftVendingBlockEntity(BlockPos pos,BlockState state){
        super(CraftVendingContent.ENTITY,pos,state);
    }

    public int batches(){return batches;}
    public void setBatches(int count){
        batches=Math.clamp(count,1,MAX_BATCHES);
        setChanged();
    }

    private CraftingInput recipeInput(){
        List<ItemStack> samples=new ArrayList<>(RECIPE_SLOTS);
        for(int i=0;i<RECIPE_SLOTS;i++)
            samples.add(contents.get(i).isEmpty()?ItemStack.EMPTY
                    :contents.get(i).copyWithCount(1));
        return CraftingInput.of(3,3,samples);
    }

    private @Nullable RecipeHolder<CraftingRecipe> matched(ServerLevel world,
            CraftingInput input){
        return world.getServer().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING,input,world).orElse(null);
    }

    public ItemStack preview(){
        if(!(level instanceof ServerLevel world))return ItemStack.EMPTY;
        CraftingInput input=recipeInput();
        RecipeHolder<CraftingRecipe> holder=matched(world,input);
        if(holder==null)return ItemStack.EMPTY;
        ItemStack output=holder.value().assemble(input);
        return output.isItemEnabled(world.enabledFeatures())?output:ItemStack.EMPTY;
    }

    /**
     * A configured amount means complete recipe operations. The screen also
     * displays the resulting number of items (e.g. 4 planks per operation).
     * Successful vending dispenses outputs AND container remainders; a full
     * inventory drops items safely beside the player.
     */
    public Component vend(ServerLevel world,Player customer){
        if(lastVendTick==world.getGameTime()
                &&customer.getUUID().equals(lastCustomer))
            return Component.literal("Already vended this tick.");
        lastVendTick=world.getGameTime();
        lastCustomer=customer.getUUID();
        CraftingInput input=recipeInput();
        RecipeHolder<CraftingRecipe> holder=matched(world,input);
        if(holder==null)return Component.literal("No matching 3×3 crafting recipe configured.");
        CraftingRecipe recipe=holder.value();
        ItemStack result=recipe.assemble(input);
        if(result.isEmpty()||!result.isItemEnabled(world.enabledFeatures()))
            return Component.literal("Recipe currently produces no usable item.");

        // Work on copied inventory, not the live machine. This includes
        // recipes with duplicate ingredients and NBT-sensitive samples.
        NonNullList<ItemStack> planned=NonNullList.withSize(STOCK_SLOTS,ItemStack.EMPTY);
        for(int slot=0;slot<STOCK_SLOTS;slot++)
            planned.set(slot,contents.get(RECIPE_SLOTS+slot).copy());
        List<ItemStack> produced=new ArrayList<>();
        List<ItemStack> leftovers=recipe.getRemainingItems(input);
        for(int craft=0;craft<batches;craft++){
            for(int i=0;i<RECIPE_SLOTS;i++){
                ItemStack required=contents.get(i);
                if(required.isEmpty())continue;
                boolean found=false;
                for(int slot=0;slot<STOCK_SLOTS;slot++){
                    ItemStack stock=planned.get(slot);
                    if(!stock.isEmpty()&&ItemStack.isSameItemSameComponents(stock,required)){
                        stock.shrink(1);
                        if(stock.isEmpty())planned.set(slot,ItemStack.EMPTY);
                        found=true;
                        break;
                    }
                }
                if(!found)return Component.literal(
                        "Insufficient ingredients: missing "+required.getHoverName().getString()
                                +". Storage has not been consumed.");
            }
            produced.add(result.copy());
            // Milk buckets, bottles, and other remainder items are returned.
            // Never delete them or duplicate input stack components.
            for(ItemStack remainder:leftovers)
                if(!remainder.isEmpty())produced.add(remainder.copy());
        }
        // Commit exactly once. Recipe samples are NEVER consumed.
        for(int i=0;i<STOCK_SLOTS;i++)
            contents.set(RECIPE_SLOTS+i,planned.get(i));
        setChanged();
        for(ItemStack stack:produced)
            customer.getInventory().placeItemBackInInventory(stack.copy());
        return Component.literal("Vended "+(batches*result.getCount())+" × "
                +result.getHoverName().getString()+".");
    }

    @Override public int getContainerSize(){return TOTAL;}
    @Override public boolean isEmpty(){
        for(ItemStack stack:contents)if(!stack.isEmpty())return false;
        return true;
    }
    @Override public ItemStack getItem(int slot){
        return slot>=0&&slot<TOTAL?contents.get(slot):ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot,int count){
        if(slot<0||slot>=TOTAL)return ItemStack.EMPTY;
        ItemStack moved=ContainerHelper.removeItem(contents,slot,count);
        if(!moved.isEmpty())setChanged();
        return moved;
    }
    @Override public ItemStack removeItemNoUpdate(int slot){
        if(slot<0||slot>=TOTAL)return ItemStack.EMPTY;
        ItemStack moved=ContainerHelper.takeItem(contents,slot);
        if(!moved.isEmpty())setChanged();
        return moved;
    }
    @Override public void setItem(int slot,ItemStack stack){
        if(slot<0||slot>=TOTAL)return;
        contents.set(slot,stack);
        if(stack.getCount()>getMaxStackSize(stack))
            stack.setCount(getMaxStackSize(stack));
        setChanged();
    }
    @Override public void clearContent(){
        for(int i=0;i<TOTAL;i++)contents.set(i,ItemStack.EMPTY);
        setChanged();
    }
    @Override public boolean stillValid(Player player){
        return Container.stillValidBlockEntity(this,player,8.0F);
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);
        out.putInt("batches",batches);
        ContainerHelper.saveAllItems(out,contents);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        contents=NonNullList.withSize(TOTAL,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(in,contents);
        batches=Math.clamp(in.getIntOr("batches",1),1,MAX_BATCHES);
    }
    @Override public Component getDisplayName(){
        return Component.translatable("container.caszual_additions.crafting_vending_machine");
    }
    @Override public @Nullable AbstractContainerMenu createMenu(
            int id,Inventory inventory,Player player){
        return new CraftVendingMenu(id,inventory,this);
    }
    @Override public BlockPos getScreenOpeningData(ServerPlayer player){
        return getBlockPos();
    }
}
