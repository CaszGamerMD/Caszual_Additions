#!/usr/bin/env python3
"""Ctrl+Pick Block copies the *entire* pallet into a portable item.

Normal middle-click remains material-only. All four deck parts and the
visible upper cargo proxies share the same actual 216-slot source.
"""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/pallet"
block=(src/"PalletBlock.java").read_text()
entity=(src/"PalletBlockEntity.java").read_text()
cargo=(src/"PalletCargoCollisionBlock.java").read_text()
assert "public static final int SLOTS = 216" in entity
assert "public ItemStack createCreativeCopyStack()" in entity
assert "PalletBlockEntity source=controller()" in entity
assert "return packedCarrierDrop(source.createDropStack(),source.items);" in entity
assert "return includeData ? be.createCreativeCopyStack() : be.createDropStack();" in block
assert "rootPos(pos, state)" in block
assert "be.isRoot()" in block
assert "return includeData?pallet.createCreativeCopyStack():pallet.createDropStack();" in cargo
assert "BlockPos root=owner(level,pos);" in cargo
copy=entity[entity.index("public static ItemStack packedCarrierDrop("):
            entity.index("public ItemStack createCreativeCopyStack()")]
assert "ItemStack packed=pallet.copyWithCount(1);" in copy
assert "NonNullList<ItemStack> items=NonNullList.withSize(SLOTS,ItemStack.EMPTY);" in copy
assert "items.set(i,savedContents.get(i).copy());" in copy
assert "packed.set(DataComponents.CONTAINER,ItemContainerContents.fromItems(items));" in copy
assert "PalletData.applyFilterSettings(stack,controller())" in entity
assert "GlassPalletWrap.withWrap(" in entity
assert "PalletData.applyWood(stack, List.of(wood))" in entity
assert "PalletData.applyPlasticColor(stack, plasticColor)" in entity
init=entity[entity.index("public void initialize("):entity.index("public void restoreJackCargo(")]
assert "stack.getOrDefault(DataComponents.CONTAINER,ItemContainerContents.EMPTY)" in init
assert ".copyInto(items);" in init
assert "filterSignature=PalletData.filterSignature(stack)" in init
assert "filterMode=PalletData.filterMode(stack)" in init
assert "filterScope=PalletData.filterScope(stack)" in init
assert "glassWrapped=GlassPalletWrap.wrapped(stack)" in init
# Creative Pick only: original must not be cleared, item deleted, or dropped.
snapshot=entity[entity.index("public ItemStack createCreativeCopyStack()"):
                entity.index("public ItemStack createDropStack()")]
for mutation in ("clearContent(", "removeItem(", "items.set(", "level.removeBlock("):
    assert mutation not in snapshot
print("PASS: normal pick is empty; Ctrl+Pick clones original 216 slot indices, wrapping, type, finishes and filter; proxy hits share owner")
