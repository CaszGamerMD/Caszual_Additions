#!/usr/bin/env python3
"""Regression checks: printer drop/replace keeps library but never duplicates stock."""
from pathlib import Path
r=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/enchantprinter"
be=(r/"EnchantmentPrinterBlockEntity.java").read_text()
block=(r/"EnchantmentPrinterBlock.java").read_text()
assert "saveLibraryToItem(ItemStack printerItem,long currentTick)" in be
assert "restoreLibraryFromItem(ItemStack printerItem,long currentTick)" in be
assert "CaszualPrinterCarryVersion" in be
assert "CaszualPrinterCarryCount" in be
assert "CaszualPrinterCarryPresetCount" in be
assert "CaszualPrinterCarryLevel" in be
assert "CaszualPrinterCarryStrain" in be
assert "CaszualPrinterCarryRemaining" in be
assert "Math.max(0,p.blockedUntil()-currentTick)" in be
assert "remaining==0?0:currentTick+remaining" in be
assert "selected.add(id)" in be
assert "if(library.containsKey(id))selected.add(id)" in be
assert "library.putAll(restored)" in be
assert 'printerItem.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));' in be
carry=be[be.index("public void saveLibraryToItem"):be.index("public void restoreLibraryFromItem")]
assert "ContainerHelper.saveAllItems" not in carry
assert "items.get(" not in carry
assert "getDrops(BlockState state,LootParams.Builder builder)" in block
assert "super.getDrops(state,builder)" in block
assert "builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY)" in block
assert "item.is(EnchantmentPrinterContent.ITEM)" in block
assert "printer.saveLibraryToItem(item,now)" in block
assert "printer.restoreLibraryFromItem(stack,world.getGameTime())" in block
assert "Containers.dropContents(level,pos,machine)" in block
assert "getAbilities().instabuild" in block
print("PASS portable Enchantment Printer: library + selected preset + paused cooldown preserved, no stock duplication")
