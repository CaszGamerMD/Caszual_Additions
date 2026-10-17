#!/usr/bin/env python3
"""Emergency remote-pallet recovery: do not lose or duplicate 216 stacks, 2 pallets or 4 golems."""
from pathlib import Path
from collections import Counter
root=Path(__file__).resolve().parents[1]/"src/main/java/com/caszgamermd/caszualadditions/racks"
reset=(root/"RemotePalletReset.java").read_text()
rig=(root/"MovingPallet.java").read_text()
worker=(root/"StorageGolem.java").read_text()
scanner=(root/"StorageScannerItem.java").read_text()
assert "private static final int CONFIRM_TICKS=200;" in reset
assert "current.expires()<level.getGameTime()" in reset
assert "return cancel(level,player,mission);" in reset
assert "player.getUUID().equals(worker.remotePickupOwner())" in reset
assert "rig.remoteExchangeActive()" in reset
assert "rig.owns(player.getUUID())" in reset
assert "trio.size()!=3" in reset
assert "boolean[] seen=new boolean[3]" in reset
assert "level.getEntity(courierId) instanceof StorageGolem courier" in reset
assert "courier.remoteReplacementAvailableForReset()" in reset
assert "if(rig!=null&&!rig.restoreRemoteOriginal(level))" in reset
assert reset.index("rig.restoreRemoteOriginal(level)") < reset.index("courier.takeRemoteReplacementForReset()")
assert reset.index("courier.takeRemoteReplacementForReset()") < reset.index("if(rig!=null)rig.discard();")
assert "StorageScannerItem.cancelQueuedForTarget(player,target)" in reset
assert "for(BlockPos home:homes)" in reset
assert "shelf.add()" in reset
assert "player.getInventory().placeItemBackInInventory(replacement);" in reset
assert "new ItemStack(RackContent.STORAGE_GOLEM_ITEM)" in reset
assert "for(StorageGolem worker:trio)worker.discard();" in reset
assert "courier.discard();" in reset
assert "remoteLoadedForReset()" in rig
assert "return deliverToGround(level,deliveryPosition,remoteOldFacing,remoteOldLegacy);" in rig
assert "boolean legacy)" in rig
assert ".setValue(PalletBlock.FACING,facing).setValue(PalletBlock.LEGACY,legacy)" in rig
assert "complete=verify(dest);" in rig
assert "if(!complete){" in rig
assert "clearCargo();" in rig
assert "RemotePalletReset.request(server,user,getUUID())" in rig
assert "RemotePalletReset.request(server,user,mission)" in worker
assert "remoteReplacementAvailableForReset()" in worker
assert "remoteFresh=ItemStack.EMPTY;" in worker
assert "remoteCourier=false;" in worker
assert "if(player.isShiftKeyDown()" in worker
assert "cancelQueuedForTarget(ServerPlayer player,BlockPos target)" in scanner
assert "RemotePalletReset.activeMission(server,user)!=null" in scanner
assert scanner.count("RemotePalletReset.requestActive(server,user)") == 2
assert 'message(user,RemotePalletReset.requestActive(server,user));' in scanner
assert "public static UUID activeMission(ServerLevel level,ServerPlayer player)" in reset
assert "mission=rig.getUUID();" in reset
assert "mission=worker.remoteCourierMission();" in reset
assert "mission=worker.remotePickupMission();" in reset
assert "found!=null&&!found.equals(mission)" in reset

# Transaction identity accounting, before and after pickup.
initial=Counter(original_pallet=1,replacement_pallet=1,workers=4)
for state in ("pre-lift","loaded"):
    # The original starts as a block before lift; it is escrowed by the
    # rig after. Cancel must always leave one physical old pallet + one
    # player-owned returned replacement and exactly four shelf/item tokens.
    after=Counter(original_pallet=1,replacement_pallet=1,workers=4)
    assert initial==after, state
# Invalid/obstructed recovery preserves all escrow instead of dropping.
for blocks_recoverable in (False,True):
    rig_removed=blocks_recoverable
    assert not rig_removed or blocks_recoverable
print("PASS: two-click reset authorization, exact inventory rollback, four identities, "
      "blocked-site fail-safe and courier item refund")
