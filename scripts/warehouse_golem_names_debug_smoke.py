#!/usr/bin/env python3
"""Golem names and Rack Computer Debug menu must survive shelf/rig cycles."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/"src/main/java/com/caszgamermd/caszualadditions/racks"
ui=root/"src/client/java/com/caszgamermd/caszualadditions/racks"
read=lambda p:p.read_text()
names=read(src/"WarehouseGolemNames.java")
shelf=read(src/"StorageGolemShelfEntity.java")
worker=read(src/"StorageGolem.java")
rig=read(src/"MovingPallet.java")
dispatch=read(src/"StorageGolemDispatch.java")
direct=read(src/"StoragePalletCrew.java")
remote=read(src/"RemotePalletDispatch.java")
pickup=read(src/"RemotePalletPickupCrew.java")
screen=read(ui/"RackComputerScreen.java")
net=read(src/"RackComputerNet.java")
scanner=read(src/"StorageScannerItem.java")
shelfblock=read(src/"StorageGolemShelfBlock.java")
debug=read(src/"WarehouseGolemDebug.java")

pool=names.split("POOL={",1)[1].split("};",1)[0]
import re
options=re.findall(r'"([A-Za-z]+)"',pool)
assert len(options)==40,(len(options),options)
assert len(set(options))==40
assert "stored_golem_name_" in shelf
assert 'out.putBoolean("debug_chat",debugChat)' in shelf
assert 'out.putBoolean("debug_statuses",debugStatuses)' in shelf
assert 'debugChat=in.getBooleanOr("debug_chat",false)' in shelf
assert 'debugStatuses=in.getBooleanOr("debug_statuses",false)' in shelf
assert "String name=shelf.takeNamed();" in dispatch
assert "worker.setGolemName(name)" in dispatch
assert "shelf.addNamed(name)" in dispatch
assert "withdrawnNames.add(name)" in direct
assert "shelves.get(i).addNamed(names.get(i))" in direct
assert "deployedNames.add(name)" in remote
assert "shelves.get(i).addNamed(deployedNames.get(i))" in remote
assert "member.remotePickupIndex(),member.golemName()" in pickup
assert "rig.setCrewName(3,courier.golemName())" in pickup
assert "crewNames[i]=golem.golemName();" in rig
assert "golem.setGolemName(crewNames[i]);" in rig
assert '"crew_name_"+i' in rig
assert '"golem_name"' in worker
assert "receiving.addNamed(golemName)" in worker
assert "WarehouseGolemNames.fromToken(held)" in shelfblock
assert "WarehouseGolemNames.namedToken(name)" in shelfblock
assert "String displayed=showDebugStatuses()?golemName+\" | \"+label:golemName;" in worker
assert "setCustomNameVisible(debug)" in rig
assert "page==3" in screen
assert 'Component.literal("DEBUG")' in screen
assert "debugChatButton" in screen and "debugStatusButton" in screen
assert "new RackComputerNet.DebugControl(" in screen
assert 'root.addProperty("debugChat"' in net
assert 'root.addProperty("debugStatuses"' in net
assert "for(StorageGolemShelfEntity shelf:shelves)" in net
assert "shelf.setDebug(control.chat(),control.statuses())" in net
assert "debugChat" in debug
assert "ClientboundSetActionBarTextPacket(message)" in debug
assert "if(verbose)player.sendSystemMessage(message);" in debug
assert "WarehouseGolemDebug.actionBar(player,Component.literal(text))" in scanner
assert "Debug chat is OFF." in net
assert "WarehouseDiagnostics.audit(level,computer)" in net
print("PASS: forty unique names, named shelf tokens, four-worker persistence, Debug UI and quiet chat defaults")
