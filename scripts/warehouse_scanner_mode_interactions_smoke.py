#!/usr/bin/env python3
"""Prevent Scanner Direct/Remote interference and unauthorised Golem enlistment."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]
server=base/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=base/"src/client/java/com/caszgamermd/caszualadditions/racks"
def j(s):return (server/s).read_text()
scanner=j("StorageScannerItem.java")
terminal=j("RackComputerBlock.java")
shelf=j("StorageGolemShelfBlock.java")
crew=j("StoragePalletCrew.java")
net=j("RackComputerNet.java")
screen=(client/"RackComputerScreen.java").read_text()
audit=j("WarehouseDiagnostics.java")
assert "public enum Mode { REMOTE, DIRECT }" in scanner
assert 'tag.putString("scanner_mode",next.name());' in scanner
assert "if(next==Mode.DIRECT)clearPickup(scanner);" in scanner
assert 'if(mode(scanner)!=Mode.REMOTE)return "";' in scanner
assert 'if(player.isShiftKeyDown())onComputerClick(server,user,scanner,clicked);' in scanner
assert "if(player.isShiftKeyDown())" in scanner
assert "Mode next=toggleMode(scanner);" in scanner
assert "REMOTE mode only requests a swap" in scanner
assert "queuePickup(scanner,old.getBlockPos());" in scanner
assert "dispatchPending(scanner,server,user,true)" in scanner
assert 'if(dispatched(result)||result.startsWith("INVALID:"))' in scanner
assert "remaining.add(target);" in scanner
assert "if(server.getBlockEntity(clicked) instanceof StorageGolemShelfEntity)" in scanner
assert "StoragePalletCrew.enlistFromShelf(level,player,home,shelf)" in scanner
assert "StoragePalletCrew.pickupGround(server,user,clicked)" in scanner
assert "StoragePalletCrew.interactPtp(server,user,point)" in scanner
assert "StoragePalletCrew.interactRack(server,user,rack)" in scanner
assert "StoragePalletCrew.deliverGround(server,user,placement)" in scanner
assert "StoragePalletCrew.returnLoadedCrew" not in scanner
assert "WarehouseDiagnostics.audit" not in scanner
assert "StoragePalletCrew.enlist(level,player,computer)" not in scanner
assert "held.is(RackContent.STORAGE_SCANNER_ITEM)&&player.isShiftKeyDown()" in terminal
assert "else ServerPlayNetworking.send(user,new RackComputerNet.Open(pos))" in terminal
assert "StorageScannerItem.onShelfClick(server,user,held,pos)" in shelf
assert "List<StorageGolemShelfEntity> connected=RackComputerNet.nearbyShelves(level,computer)" in crew
assert "shelf->shelf.getBlockPos().equals(selectedShelf)" in crew
assert "if(!valid)return" in crew
assert "private static String enlistAt(" in crew
assert "StoragePalletCrew.enlist(level,player,computer)" not in net
assert 'case 0 -> result="DIRECT Scanner:' in net
assert 'Component.literal("SCANNER HELP")' in screen
assert "case 3 -> {" in net and "WarehouseDiagnostics.audit(level,computer)" in net
assert 'Component.literal("DIAGNOSTICS")' in screen
assert "requestGolem(3)" in screen
assert "Rack Computer Diagnostics" in audit
print("PASS: shift-to-link computer, shift-air toggle, remote-only swaps, direct shelf summon, direct pickup/placement and computer diagnostics")
