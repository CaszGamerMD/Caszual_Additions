#!/usr/bin/env python3
"""XP Charger three independent Mending slots and portable tank regression."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
xp=root/"src/main/java/com/caszgamermd/caszualadditions/xp"
charger=(xp/"XpChargerBlockEntity.java").read_text()
block=(xp/"XpChargerBlock.java").read_text()
repair=(xp/"XpRepair.java").read_text()
tank=(xp/"XpTankBlockEntity.java").read_text()
tankblock=(xp/"XpTankBlock.java").read_text()
plugin=(root/"src/main/java/com/caszgamermd/caszualadditions/compat/jade/XpChargerJadePlugin.java").read_text()
client=(root/"src/client/java/com/caszgamermd/caszualadditions/compat/jade/XpChargerJadeTooltip.java").read_text()
main=(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualAdditions.java").read_text()
items=(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualItemGroups.java").read_text()
assert "public static final int SLOTS=3;" in charger
assert "NonNullList.withSize(SLOTS,ItemStack.EMPTY)" in charger
assert "ContainerHelper.saveAllItems(out,items)" in charger
assert "ContainerHelper.loadAllItems(in,items)" in charger
assert 'in.read("item",ItemStack.CODEC)' in charger # existing one-item saves
assert "private static final int[] ALL_SLOTS={0,1,2};" in charger
assert "return canPlaceItem(slot,stack);" in charger
assert "!items.get(slot).isDamaged()" in charger
assert "if(player.isShiftKeyDown())" in block
assert "for(int i=0;i<charger.getContainerSize();i++)" in block
assert "charger.firstFree()" in block
assert "charger.firstReady()" in block
assert "if(!player.getAbilities().instabuild)held.shrink(1);" in block
assert "popResource(level,pos,item)" in block
assert "for(int i=0;i<charger.getContainerSize();i++)" in repair
assert "XpNetwork.extract(level,charger.getBlockPos(),xp)" in repair
assert 'PREFIX+"slots"' in plugin
assert 'PREFIX+"occupied"' in plugin
assert 'PREFIX+"slotName"+slot' in plugin
assert 'PREFIX+"slotDamage"+slot' in plugin
assert 'Slot "+(i+1)' in client
# A dropped tank receives ONLY the individual controller balance; linked
# tanks are not read/copied into the dropped item.
assert "public static final int CAPACITY = 30970;" in tank
assert 'caszual_xp_tank_stored' in tank
assert 'component.copyTag().getIntOr("caszual_xp_tank_stored",0)' in tank
assert "data.putInt(" in tank and "xp);" in tank
assert "stack.set(DataComponents.CUSTOM_DATA,CustomData.of(data))" in tank
assert "if(xp<=0)return;" in tank
assert "public ItemStack asItem()" in tank
assert "protected List<ItemStack> getDrops(BlockState state,LootParams.Builder builder)" in tankblock
assert "super.getDrops(state,builder)" in tankblock
assert "if(stack.is(XpBlocks.XP_TANK))tank.saveToItem(stack)" in tankblock
assert "tank.setStored(XpTankBlockEntity.itemXp(stack))" in tankblock
assert "level.scheduleTick(pos,this,1)" in tankblock
assert "getCloneItemStack(LevelReader level,BlockPos pos," in tankblock
assert "XpNetwork.stored(" not in tankblock # don't duplicate the connected network
assert "EnchantmentPrinterContent.initialize()" in main
assert "EnchantmentPrinterContent.ITEM" in items
print("PASS: three independent charger docks, legacy item migration, portable per-tank XP, printer Creative registration")
