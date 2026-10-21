#!/usr/bin/env python3
"""INPUT receiving and OUTPUT shipping aisles remain distinct."""
from pathlib import Path
base=Path(__file__).resolve().parents[1]
server=base/"src/main/java/com/caszgamermd/caszualadditions/racks"
client=base/"src/client/java/com/caszgamermd/caszualadditions/racks"
priority=(server/"WarehouseAislePriority.java").read_text()
network=(server/"RackNetwork.java").read_text()
tag=(server/"RackBlock.java").read_text()
carrier=(server/"MovingPallet.java").read_text()
renderer=(client/"RackBlockEntityRenderer.java").read_text()

# Case-insensitive *exact* tag match: "Input" marks the aisle; similar
# unrelated names like "Input-2", "Input PTP" and "Not Input" don't.
assert '"Input".equalsIgnoreCase(label.trim())' in priority
assert 'return rack!=null&&isInput(RackNetwork.aisle(level,rack));' in priority
assert 'if(rack.hasPallet()||!rack.isEmpty())continue;' in priority
assert 'if(!isInputAisle(level,rack))continue;' in priority
assert 'if(!WarehouseSortingReach.groundRack(level,rack))continue;' in priority
assert 'if(plan==null||plan.height()!=0)continue;' in priority
assert 'exclude!=null&&exclude.equals(rack.getBlockPos())' in priority
assert 'else rack.setAisleName(name.equals("Aisle Tag") ? "Aisle" : name);' in tag
assert 'if(aisleNeighbors(level,rack,other)&&seen.add(other.getBlockPos()))' in network

# The green/red tint is applied to the Aisle Tag's actual rack-end text,
# not the entire dark nameplate or unrelated front rack label.
assert 'WarehouseAislePriority.isInput(state.aisleLabel)' in renderer
assert '?0xff55dd66:WarehouseAislePriority.isOutput(state.aisleLabel)' in renderer
assert '?0xffff5555:0xffffde9b' in renderer
assert 'front,x,z,2.31f,0xffe4f0f5' in renderer
assert 'submitFacingPlate(state,poses,collector,state.aisleLabel,' in renderer

# Remote recovery flags persist through chunk unloads/restarts, while
# manually recalled direct crews keep their old PTP-first ordering.
assert 'private boolean remotePickupReturn;' in carrier
join=carrier[carrier.index("public boolean joinRemoteCourier"):carrier.index("public boolean prepareRecoveredRemote")]
assert 'remotePickupReturn=true;' in join
direct=carrier[carrier.index("public String returnToWarehouse"):carrier.index("@Override protected net.minecraft.world.InteractionResult mobInteract")]
assert 'remotePickupReturn=false;' in direct
assert 'out.putBoolean("remote_pickup_return",remotePickupReturn);' in carrier
assert 'remotePickupReturn=in.getBooleanOr("remote_pickup_return",false);' in carrier

return_body=carrier[carrier.index("private void tickWarehouseReturn"):carrier.index("private void tickDelivery")]
i_priority=return_body.index('WarehouseAislePriority.firstAvailableInputRack(')
i_ptp=return_body.index('for(PtpBlockEntity candidate:warehouse.points())')
i_rack_fallback=return_body.index('for(RackBlockEntity candidate:warehouse.racks())')
assert i_priority<i_ptp<i_rack_fallback
assert 'RackBlockEntity rack=remotePickupReturn' in return_body
assert 'if(!outboundStaging&&!remotePickupReturn&&rack==null){' in return_body[i_priority:i_ptp]
assert 'if(!outboundStaging&&rack==null&&(remotePickupReturn||point==null)){' in return_body[i_ptp:i_rack_fallback]
assert 'done=deliverToRack(rack);' in return_body
assert 'point.setInput(true);' in return_body
assert 'INPUT AISLE RACK' in return_body
assert 'if(!done)' in return_body
assert 'restoreWorkers(level);' in return_body
assert 'skipReturnRackUntil=tickCount+400' in return_body
assert '"WAITING FOR HELP: NO VACANT INPUT OR STANDARD RACK"' in return_body
assert 'INPUT RACK BLOCKED: TRYING ANOTHER RECEIVING POINT' in return_body
assert 'WarehouseSortingReach.groundRack(level,candidate)' in return_body
assert '"Output".equalsIgnoreCase(label.trim())' in priority
assert 'return rack!=null&&isOutput(RackNetwork.aisle(level,rack));' in priority
assert 'firstAvailableOutputRack(' in priority
assert 'stagedOutputRacks(' in priority
assert 'if(isOutputAisle(level,rack)&&rack.hasPallet())ready.add(rack);' in priority
assert 'if(!isOutputAisle(level,rack))continue;' in priority
assert 'if(plan==null||plan.height()!=0)continue;' in priority
assert 'outboundStaging' in carrier
assert 'out.putBoolean("outbound_staging",outboundStaging);' in carrier
assert 'outboundStaging=in.getBooleanOr("outbound_staging",false);' in carrier
assert 'WarehouseAislePriority.firstAvailableOutputRack(' in return_body
assert '||WarehouseAislePriority.isOutputAisle(level,candidate)' in return_body
scanner=(server/"StorageScannerItem.java").read_text()
crew=(server/"StoragePalletCrew.java").read_text()
assert 'mode(scanner)==Mode.DIRECT&&player.isShiftKeyDown()' in scanner
assert 'StoragePalletCrew.stageForOutput(level,player,computer)' in scanner
assert 'rig.stageForOutput(level,computer)' in crew
print("PASS: Input green / Output red; reserved shipping bays, safe Golem staging and inbound priority")
