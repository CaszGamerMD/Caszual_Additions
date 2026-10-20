#!/usr/bin/env python3
"""Industrial hex paint / primer / remover: source, model and networking checks."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
j=root/"src/main/java/com/caszgamermd/caszualadditions/painting"
c=root/"src/client/java/com/caszgamermd/caszualadditions/painting"
a=root/"src/main/resources/assets/caszual_additions"
data=root/"src/main/resources/data/caszual_additions"
read=lambda p:p.read_text()
registry=read(j/"IndustrialPaintContent.java")
logic=read(j/"IndustrialFinishLogic.java")
entity=read(j/"IndustrialFinishEntity.java")
brush=read(j/"IndustrialBrushItem.java")
can=read(j/"IndustrialPaintCanBlockEntity.java")
network=read(j/"IndustrialPaintNet.java")
renderer=read(c/"IndustrialFinishRenderer.java")
screen=read(c/"IndustrialPaintScreen.java")
assert all(n in registry for n in ("PAINT_CAN","PRIMER_CAN","BRUSH","REMOVER","FINISH","SURFACE"))
assert "PlayerBlockBreakEvents.AFTER.register" in registry
assert "industrial_paint_can" in registry and "industrial_primer_can" in registry
assert "industrial_color" in brush and "industrial_primer" in brush
assert "context.getClickedPos()" in brush
assert "IndustrialFinishLogic.apply(" in brush
assert "IndustrialFinishLogic.remove(" in read(j/"IndustrialRemoverItem.java")
assert "CompoundTag" in brush and "CustomData.of(tag)" in brush
assert "data.copyTag()" in brush
assert 'out.putInt("paint_rgb",color)' in can
assert 'color=in.getIntOr("paint_rgb"' in can
assert 'out.putString("target_block"' in entity
assert "existing.equals(entityData.get(TARGET))" in entity
assert "level.getEntitiesOfClass(" in logic and "e.anchor().equals(pos)" in logic
assert "warehouse(BlockState state)" in logic
assert "Apply Industrial Primer" in logic
assert "if(old!=null)" in logic
assert "player.distanceToSqr" in network
assert 'request.action()==1' in network and 'request.action()==0' in network
assert "ClientPlayNetworking.send(new IndustrialPaintNet.Request(" in screen
assert "text.matches(\"[0-9a-fA-F]{6}\")" in screen
assert "IndustrialPaintContent.SURFACE.defaultBlockState()" in renderer
assert "state.finish.tintLayers().add(entity.color())" in renderer
assert "target.getShape(world,at).toAabbs()" in renderer
assert "state.finish.submit(" in renderer
assert "IndustrialPaintClient.initialize();" in read(root/"src/client/java/com/caszgamermd/caszualadditions/CaszualAdditionsClient.java")
assert "IndustrialPaintContent.initialize();" in read(root/"src/main/java/com/caszgamermd/caszualadditions/CaszualAdditions.java")
lang=json.loads((a/"lang/en_us.json").read_text())
for name in ("industrial_paint_can","industrial_primer_can"):
    assert f"block.caszual_additions.{name}" in lang
    assert (data/f"loot_table/blocks/{name}.json").exists()
for name in ("industrial_paint_brush","industrial_paint_remover"):
    assert f"item.caszual_additions.{name}" in lang
for name in ("industrial_paint_can","industrial_primer_can",
             "industrial_paint_brush","industrial_paint_remover"):
    assert (a/f"models/item/{name}.json").exists()
    assert (a/f"items/{name}.json").exists()
shell=json.loads((a/"models/block/industrial_paint_surface.json").read_text())
assert any("tintindex" in v for e in shell["elements"]
           for v in e.get("faces",{}).values())
print("PASS: saved hex can, primer and reusable brush, model-shaped finish, stripping on break")
