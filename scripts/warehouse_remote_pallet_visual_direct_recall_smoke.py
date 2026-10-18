#!/usr/bin/env python3
"""Remote pallet visual and scanner-controlled Direct Golem recall regression."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
golem=(src/"StorageGolem.java").read_text()
renderer=(client/"StorageGolemRenderer.java").read_text()
scanner=(src/"StorageScannerItem.java").read_text()
rig=(src/"MovingPallet.java").read_text()

# Server-only mission fields cannot be read from a client entity.
assert "DISPLAY_PALLET_ACTION=" in golem
assert "builder.define(DISPLAY_PALLET_ACTION,0);" in golem
assert "int action=remoteCourier&&remoteCourierPhase==0" in golem
assert "&&!remoteFresh.isEmpty()?2:playerHaul&&stage==1?1:0" in golem
assert "entityData.set(DISPLAY_PALLET_ACTION,action);" in golem
assert "entityData.get(DISPLAY_PALLET_ACTION)==2" in golem
assert "entityData.get(DISPLAY_PALLET_ACTION)==1" in golem
assert "syncPalletAction();" in golem
assert "syncRemoteFreshVisual();" in golem
assert "entityData.set(DISPLAY_PALLET,remoteFresh.copy());" in golem
assert "state.pushingEmptyPallet=golem.pushingPalletPose();" in renderer
assert "state.carryingPallet=golem.carryingPalletPose();" in renderer
assert "if(state.pushingEmptyPallet){" in renderer
assert "for(int board=0;board<4;board++)" in renderer

# Direct mode only. No remote mission cancellation or inventory deletion.
assert "StorageScannerItem.Mode.DIRECT" in golem
assert "&&ownedBy(user.getUUID())" in golem
assert "if(stage==1||holdsPallet())" in golem
assert "recallToShelf((ServerLevel)level())" in golem
assert "DIRECT: pallet crew is carrying a shipment." in golem
assert "remotePickup||remoteCourier" in golem
assert "StorageScannerItem.Mode.DIRECT" in rig
assert "StorageScannerItem.linkedComputer(" in rig
assert "returnToWarehouse(server,linked)" in rig
assert "fromWarehouse(WarehouseConnections.shelvesForComputer(server,linked))" in rig
assert "if(remoteCourierId!=null&&deliveryMode>=4&&deliveryMode<=6)" in rig
assert 'static BlockPos linkedComputer(ItemStack scanner,ServerLevel world)' in scanner
assert "Mode next=toggleMode(scanner);" in scanner
print("PASS: synced courier pallet, individual direct recall, loaded-rig return; scanner toggle unaffected")
