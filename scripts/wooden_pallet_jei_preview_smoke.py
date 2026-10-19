#!/usr/bin/env python3
"""The JEI pallet sample must NOT override the custom wood-type recipe."""
from pathlib import Path
import json
r=Path(__file__).resolve().parents[1]
plugin=(r/"src/main/java/com/caszgamermd/caszualadditions/compat/jei/CaszualJeiPlugin.java").read_text()
recipe=(r/"src/main/java/com/caszgamermd/caszualadditions/pallet/WoodenPalletRecipe.java").read_text()
actual=json.loads((r/"src/main/resources/data/caszual_additions/recipe/wooden_pallet.json").read_text())
assert actual=={"type":"caszual_additions:wooden_pallet"}
assert "registerRecipes(IRecipeRegistration registration)" in plugin
assert "createShapedRecipeBuilder(" in plugin
assert "CraftingBookCategory.MISC" in plugin
assert "new SlotDisplay.ItemStackSlotDisplay(" in plugin
assert "new ItemStack(PalletContent.WOODEN_PALLET_ITEM)" in plugin
assert ".define('P',Ingredient.of(ItemTags.PLANKS))" in plugin
assert ".define('C',Ingredient.of(Items.CHEST))" in plugin
assert '.pattern("PCP")' in plugin
assert '.pattern("CCC")' in plugin
assert plugin.count('.pattern("PCP")')==2
assert 'registration.addRecipes(RecipeTypes.CRAFTING,' in plugin
assert '"wooden_pallet_jei_preview"' in plugin
assert "PalletData.applyWood(" in recipe
assert "List.of(" in recipe
assert "ItemTags.PLANKS" in recipe
assert "Items.CHEST" in recipe
assert not (r/"src/main/resources/data/caszual_additions/recipe/wooden_pallet_jei_preview.json").exists()
print("PASS wooden pallet: JEI display cycles #minecraft:planks without replacing real textured pallet recipe")
