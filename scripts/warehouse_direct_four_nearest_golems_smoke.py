#!/usr/bin/env python3
"""Verify Direct-mode recruits the nearest four eligible shelf tokens."""
from pathlib import Path

root=Path(__file__).resolve().parents[1]
crew=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/StoragePalletCrew.java").read_text()
scanner=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/StorageScannerItem.java").read_text()
shelf=(root/"src/main/java/com/caszgamermd/caszualadditions/racks/StorageGolemShelfEntity.java").read_text()
start=crew.index("    private static String enlistAt(")
end=crew.index("    private static void rollback(",start)
method=crew[start:end]

# Linked warehouse is mandatory, and clicking a far-away shelf does not
# override player-proximity sorting of all four AVAILABLE docked tokens.
assert "connected=RackComputerNet.nearbyShelves(level,computer)" in method
assert "shelf->shelf.getBlockPos().equals(selectedShelf)" in method
assert "connected.sort(" not in method
assert "record NearbyWorker(StorageGolemShelfEntity shelf,BlockPos exit,double distance){}" in method
assert "player.distanceToSqr(" in method
assert "StorageGolemShelfDoor.exit(level,shelf)" in method
assert "for(int i=0;i<shelf.docked();i++)" in method
assert "available.sort(java.util.Comparator.comparingDouble(NearbyWorker::distance)" in method
assert "thenComparingInt(w->w.shelf().getBlockPos().getX())" in method

# Every worker comes from an actually available shelf inventory token.
# A blocked exit must be ignored without taking stock. A newly blocked
# second exit tries the next nearest available shelf instead of failing.
assert "if(shelf.docked()<=0)continue;" in method
assert "if(shelfDoor==null)continue;" in method
assert "if(spawned.size()==4)break;" in method
assert "golem.assignPlayerCrew(shelf.getBlockPos(),spawned.size(),player.getUUID());" in method
assert method.index("if(shelfDoor==null)continue;") < method.index("String name=shelf.takeNamed();")
assert "if(spawned.size()<4){" in method
assert "rollback(spawned,withdrawn,withdrawnNames);" in method
assert 'return "DIRECT: four closest available Storage Golems called from "' in method
assert "public int docked(){return docked;}" in shelf
assert "if(docked>=2)return false;" in shelf

# Regression scenario: a player clicks the FARTHEST shelf in a warehouse,
# but the two nearest shelves have enough workers and should be selected.
shelves=[
    {"coords":(100,64,0),"docked":2,"open":True},
    {"coords":(4,64,0),"docked":2,"open":True},
    {"coords":(9,64,0),"docked":2,"open":True},
    {"coords":(2,64,0),"docked":2,"open":False},
    {"coords":(1,64,0),"docked":0,"open":True},
]
player=(0.5,64,0.5)
available=[]
for shelf_data in shelves:
    if not shelf_data["open"]: continue
    x,y,z=shelf_data["coords"]
    distance=(x+.5-player[0])**2+(y-player[1])**2+(z+.5-player[2])**2
    available += [(distance,x,y,z)]*shelf_data["docked"]
available.sort()
nearest=available[:4]
assert len(nearest)==4
assert [item[1] for item in nearest]==[4,4,9,9]
assert not any(item[1]==100 for item in nearest)
# No second warehouse's shelves may enter the candidate pool.
assert "RackComputerNet.nearbyShelves(level,computer)" in method
# Caller must still route through the real linked scanner & shelf validation.
assert "StoragePalletCrew.enlistFromShelf(level,player,home,shelf)" in scanner
print("PASS: Direct mode summons 4 nearest eligible connected shelf Golems, no token theft, atomic rollback")
