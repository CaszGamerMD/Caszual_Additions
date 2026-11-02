#!/usr/bin/env python3
"""Structural checks for safe Enchantment Printer registration and cost flow."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
server=root/"src/main/java/com/caszgamermd/caszualadditions/enchantprinter"
client=root/"src/client/java/com/caszgamermd/caszualadditions/enchantprinter"
be=(server/"EnchantmentPrinterBlockEntity.java").read_text()
menu=(server/"EnchantmentPrinterMenu.java").read_text()
screen=(client/"EnchantmentPrinterScreen.java").read_text()
content=(server/"EnchantmentPrinterContent.java").read_text()
mod=(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualAdditions.java").read_text()
mod_client=(root/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java").read_text()
assert "EnchantmentPrinterContent.initialize()" in mod
assert "EnchantmentPrinterScreen::new" in mod_client
assert "registerGlobalReceiver(EnchantmentPrinterStatePayload.TYPE" in mod_client
assert 'EnchantmentPrinterStatePayload.TYPE,EnchantmentPrinterStatePayload.CODEC' in content
for x in ["getEnchantmentsForCrafting(donor)","EnchantmentHelper.canStoreEnchantments(target)",
          "EnchantmentHelper.isEnchantmentCompatible(others,enchant)",
          "updateEnchantments(output,mutable->","experienceLevel<xp",
          "player.giveExperienceLevels(-xp)","getItem(LAPIS).shrink(lapis)",
          "getItem(EMERALD).shrink(emerald)","donor.shrink(1)",
          "new Pattern(level,0,0)","risk(p.strain())","blockedUntil()","JAM_TICKS=2400"]:
    assert x in be,x
assert be.index("All validation is completed")<be.index("player.giveExperienceLevels(-xp)")
assert "PrinterSelectedCount" in be and "PrinterLibraryCount" in be
assert "CASE" not in be
assert "selected.add(key)" in be
assert "isSupportedItem(target)" in be
assert "if(!stillValid(player)" in menu
assert "menu.clickMenuButton" not in screen
assert "RUN-OUT" in screen and "PRINT SELECTED" in screen
assert "LEARN / REFILL" in screen
files=[
 "assets/caszual_additions/blockstates/enchantment_printer.json",
 "assets/caszual_additions/items/enchantment_printer.json",
 "assets/caszual_additions/models/block/enchantment_printer.json",
 "assets/caszual_additions/models/item/enchantment_printer.json",
 "data/caszual_additions/recipe/enchantment_printer.json",
 "data/caszual_additions/loot_table/blocks/enchantment_printer.json"
]
for name in files:json.loads((root/"src/main/resources"/name).read_text())
print("PASS Enchantment Printer: registration, safe cost/selection flow, 2-minute cooldown, resources and GUI")
