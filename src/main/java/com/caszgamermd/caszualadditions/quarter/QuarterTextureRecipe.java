package com.caszgamermd.caszualadditions.quarter;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public final class QuarterTextureRecipe extends CustomRecipe {
    public QuarterTextureRecipe(CraftingBookCategory category){super(category);}

    @Override public boolean matches(CraftingInput input,Level level){
        int frames=0,blocks=0;
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);
            if(stack.isEmpty())continue;
            if(stack.is(QuarterBlocks.QUARTER_FRAME))frames++;
            else if(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock()!=QuarterBlocks.QUARTER_BLOCK)blocks++;
            else return false;
        }
        return frames==8&&blocks==1;
    }

    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider provider){
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);
            if(stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock()!=QuarterBlocks.QUARTER_BLOCK)
                return QuarterBlocks.textured(blockItem.getBlock().defaultBlockState(),8);
        }
        return ItemStack.EMPTY;
    }

    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=9;}

    @Override public RecipeSerializer<QuarterTextureRecipe> getSerializer(){return QuarterBlocks.QUARTER_TEXTURE_RECIPE;}
}
