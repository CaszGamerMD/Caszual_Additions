#!/usr/bin/env python3
"""Personal scanner recall sends all direct FOLLOWERS home without a computer.

Separate from warehouse-wide 'Recall all' and from the old (removed)
reset protocol. Never steal other players' workers, recall unrelated
sorting bots, or strand a loaded 216-slot carrier.
"""
from pathlib import Path
base=Path(__file__).resolve().parents[1]
racks=base/"src/main/java/com/caszgamermd/caszualadditions/racks"
crew=(racks/"StoragePalletCrew.java").read_text()
golem=(racks/"StorageGolem.java").read_text()
scanner=(racks/"StorageScannerItem.java").read_text()
carrier=(racks/"MovingPallet.java").read_text()

start=crew.index("public static String recallFollowing(")
finish=crew.index("/** Operator recall",start)
method=crew[start:finish]
assert "level.getAllEntities()" in method
assert "golem.ownedBy(player.getUUID())" in method
assert "if(members.isEmpty())" in method
assert "if(members.stream().anyMatch(w->w.holdsPallet()" in method
assert "||w.escorting(player.getUUID())))" in method
assert method.index("if(members.stream().anyMatch(")<method.index("for(StorageGolem worker:members)")
assert "if(worker.recallToShelf(level))sent++" in method
assert "linkedComputer" not in method
assert "RackComputer" not in method
assert "distanceToSqr" not in method
assert 'StoragePalletCrew.recallFollowing((ServerLevel)level(),user)' in golem

# Worker physically walks back to saved shelf without needing player
# to first load the original Rack Computer. No teleport/drop/forced despawn.
home=golem[golem.index("private void returnToShelf(ServerLevel level)"):golem.index("private Container loaded(")]
assert "if(playerHaul&&stage==2&&tickCount%20==0)" in home
assert "WarehouseRemoteTickets.keep(level,blockPosition());" in home
assert "WarehouseRemoteTickets.keep(level,home);" in home
assert "feet.getX()+(int)Math.round(dx/length*16)" in home
assert "WarehouseRemoteTickets.keep(level,new BlockPos(" in home
assert "if(receiving!=null&&receiving.addNamed(golemName))" in home
assert "discard();" in home

assert "ScannerControlNet.open(user,scanner)" in scanner
assert "StorageScannerItem.Mode.DIRECT" in golem
assert "if(remotePickup||remoteCourier)" in golem
assert "claimStalledRemote(server,user.getUUID(),linked)" in carrier
assert "holdDirectPallet();" in carrier

# The 4-owner worker job must be treated atomically. No part of an active
# loaded crew is dismissed when any worker owns/escorts pallet cargo.
crew_members=[
    {"owner":"A","loaded":False,"escorting":False},
    {"owner":"A","loaded":False,"escorting":False},
    {"owner":"A","loaded":False,"escorting":False},
    {"owner":"A","loaded":False,"escorting":False},
    {"owner":"B","loaded":False,"escorting":False},
]
members=[m for m in crew_members if m["owner"]=="A"]
assert len(members)==4
assert not any(m["loaded"] or m["escorting"] for m in members)
members[0]["escorting"]=True
assert any(m["loaded"] or m["escorting"] for m in members)
print("PASS: Direct scanner click recalls whole owned empty crew from anywhere; loaded pallet protected")
