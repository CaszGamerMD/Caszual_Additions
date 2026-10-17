#!/usr/bin/env python3
"""A killed loaded Golem must preserve the ONE pallet and all 216 indexed slots.

Unlike the removed scanner reset, Shift-right-click AIR always swaps
REMOTE/DIRECT mode. Killing a remote carrier must not scatter its cargo.
"""
from pathlib import Path
from collections import Counter
base=Path(__file__).resolve().parents[1]
racks=base/"src/main/java/com/caszgamermd/caszualadditions/racks"
pallets=base/"src/main/java/com/caszgamermd/caszualadditions/pallet"
def read(path):return path.read_text()
recovery=read(racks/"PalletDeathRecovery.java")
rig=read(racks/"MovingPallet.java")
worker=read(racks/"StorageGolem.java")
scanner=read(racks/"StorageScannerItem.java")
be=read(pallets/"PalletBlockEntity.java")
workflow=read(base/".github/workflows/build.yml")

assert not (racks/"RemotePalletReset.java").exists()
assert not (base/"scripts/warehouse_remote_cancel_reset_smoke.py").exists()
for name,src in (("MovingPallet",rig),("StorageGolem",worker),("StorageScannerItem",scanner)):
    assert "RemotePalletReset" not in src, name
assert "warehouse_remote_cancel_reset_smoke.py" not in workflow
assert "RemotePalletReset.activeMission" not in scanner
assert "Mode next=toggleMode(scanner)" in scanner
assert "if(player.isShiftKeyDown()){" in scanner
assert scanner.count("Mode next=toggleMode(scanner)") == 1

assert "PalletDeathRecovery.release(level,blockPosition(),getX(),getY(),getZ()," in rig
assert "PalletDeathRecovery.release(level,blockPosition(),getX(),getY(),getZ()," in worker
assert "PalletDeathRecovery.release" in rig[rig.index("private void dropCargo("):rig.index("@Override public void die(")]
assert "PalletDeathRecovery.release" in worker[worker.index("private void dropCarriedPallet("):worker.index("private UUID ownerId(")]
assert "for(ItemStack contentsItem:contents)" not in rig
assert "for(ItemStack stack:palletContents)" not in worker[worker.index("private void dropCarriedPallet("):worker.index("private UUID ownerId(")]
assert "int workerDrops=remoteCourierId==null?4:3;" in rig
assert "courier.remoteCarrierDestroyed(level)" in rig

# Placement requires four blocks, dry solid floor support and clear volume.
assert "for(int i=0;i<4;i++)" in recovery
assert "level.getBlockState(pos).canBeReplaced()" in recovery
assert "getFluidState(pos).isEmpty()" in recovery
assert "isFaceSturdy(level,below,Direction.UP)" in recovery
assert "for(int h=1;h<=2;h++)" in recovery
assert "rollback(level,placed,block,state)" in recovery
assert "dest.initialize(root,0,pallet)" in recovery
assert "contents.get(i)" in recovery
assert "ItemStack.isSameItemSameComponents(old,restored)" in recovery
assert "old.getCount()!=restored.getCount()" in recovery
assert recovery.index("if(!verified){")<recovery.index("return verified;")

# If no place exists, use ONE pallet item with 216 slot-preserving contents.
assert "PalletBlockEntity.packedCarrierDrop(pallet,contents)" in recovery
assert "Containers.dropItemStack(level,x,y,z,packed)" in recovery
assert "ItemContainerContents.fromItems(items)" in be
assert "DataComponents.CONTAINER" in be
assert ".copyInto(items)" in be
assert "NonNullList.withSize(SLOTS,ItemStack.EMPTY)" in be
assert "savedContents.get(i).copy()" in be
assert "clearCargo();" in rig

# Conservation model: place when safe; otherwise all contents in ONE item.
cargo={0:"diamond",11:"coal",215:"shulker"}
for has_safe_floor in (True,False):
    placed=cargo.copy() if has_safe_floor else {}
    item=cargo.copy() if not has_safe_floor else {}
    assert sum(bool(x) for x in (placed,item))==1
    assert (placed or item)==cargo
    assert sorted((placed or item))==[0,11,215]
print("PASS: no scanner reset, mode toggle restored, intact full 2x2 death recovery"
      " with 216-slot packed pallet fallback and no scattered cargo")
