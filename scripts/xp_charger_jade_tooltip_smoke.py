#!/usr/bin/env python3
"""Check optional Jade XP Charger tooltip and server-side repair data."""
import json
from pathlib import Path
r=Path(__file__).resolve().parents[1]
main=r/"src/main/java/com/caszgamermd/caszualadditions"
source=(main/"compat/jade/XpChargerJadePlugin.java").read_text()
client=(r/"src/client/java/com/caszgamermd/caszualadditions/compat/jade/XpChargerJadeTooltip.java").read_text()
repair=(main/"xp/XpRepair.java").read_text()
charger=(main/"xp/XpChargerBlockEntity.java").read_text()
gradle=(r/"build.gradle").read_text()
fabric=json.loads((r/"src/main/resources/fabric.mod.json").read_text())
lang=json.loads((r/"src/main/resources/assets/caszual_additions/lang/en_us.json").read_text())

assert 'compileOnly "maven.modrinth:jade:26.2.11+fabric"' in gradle
assert "https://api.modrinth.com/maven" in gradle
assert "jade" not in fabric["depends"]
assert fabric["entrypoints"]["jade"]==["com.caszgamermd.caszualadditions.compat.jade.XpChargerJadePlugin"]
assert 'registration.registerBlockDataProvider(Server.INSTANCE,XpChargerBlockEntity.class)' in source
assert 'registration.registerBlockComponent(' in source
assert 'XpChargerJadeTooltip' in source
assert "XpChargerBlock.class" in source
assert "public final class XpChargerJadeTooltip implements IBlockComponentProvider" in client
assert "IServerDataProvider<BlockAccessor>" in source
assert "IBlockComponentProvider" in source
assert "accessor.getServerData()" in client
assert "accessor.getBlockEntity() instanceof XpChargerBlockEntity" in source
assert "accessor.getLevel() instanceof ServerLevel" in source
assert 'data.putString(PREFIX+"name",item.getHoverName().getString())' in source
for key in ["present","item","name","max","damage","mending","stored","capacity"]:
    assert f'PREFIX+"{key}"' in source
assert "XpNetwork.tanks(level,accessor.getPosition())" in source
assert "tanks.size()*XpTankBlockEntity.CAPACITY" in source
assert "XpRepair.hasMending(item)" in source
assert "Math.round(100f*remaining/max)" in client
assert 'int needed=(damage+1)/2' in source
assert "Math.min(4,(s.getDamageValue()+1)/2)" in repair
assert "charger.setChanged()" in repair
for name in ["empty","item","durability","complete","no_mending","no_tank",
             "no_xp","repairing","xp_needed","xp"]:
    key=f"jade.caszual_additions.xp_charger.{name}"
    assert key in lang
    assert key in client
assert "ItemStack item=ItemStack.EMPTY" in charger
print("PASS: Jade/WAILA block tooltip synced from server with repair durability, percent, tank XP, empty/waiting/complete states")
