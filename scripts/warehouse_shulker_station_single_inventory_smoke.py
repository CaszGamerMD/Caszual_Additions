#!/usr/bin/env python3
"""Both station faces resolve to ONE physical 64+3+3 inventory in the LOWER
block. Any legacy upper block entity must migrate its real items safely.
The click routes and dismantling operation must not duplicate the station.
"""
from pathlib import Path
R=Path(__file__).resolve().parents[1]
p=R/"src/main/java/com/caszgamermd/caszualadditions/racks"
block=(p/"ShulkerTankBlock.java").read_text()
station=(p/"ShulkerTankEntity.java").read_text()
jack=(p/"PalletJackVehicle.java").read_text()
assert 'DoubleBlockHalf.LOWER' in block and 'DoubleBlockHalf.UPPER' in block
assert 'return state.getValue(HALF)==DoubleBlockHalf.LOWER' in block
assert 'new ShulkerTankEntity(pos,state):null;' in block
assert 'root(pos,state)' in block
assert 'getBlockEntity(base) instanceof ShulkerTankEntity station' in block
assert 'ShulkerTankEntity tank=controller(level,pos)' in block
assert 'ShulkerTankEntity station=controller(level,pos)' in block
assert 'server.openMenu(station)' in block and 'serverPlayer.openMenu(tank)' in block
assert 'server.removeBlockEntity(top)' in block
assert 'station.absorbLegacyTop(extra)' in block
assert 'Block.popResource(server,base,overflow)' in block
assert block.index('station.absorbLegacyTop(extra)') < block.index('server.removeBlockEntity(top)')
assert block.index('station.absorbLegacyTop(extra)') < block.index('.setValue(HALF,DoubleBlockHalf.UPPER),3)')
assert 'level.removeBlock(other,false)' in block
assert 'Block.popResource(level,root,new ItemStack(this))' in block
assert 'public List<ItemStack> absorbLegacyTop(ShulkerTankEntity top)' in station
assert 'top.boxes.set(i,ItemStack.EMPTY);' in station
assert 'top.working.set(i,ItemStack.EMPTY);' in station
assert 'top.finished.set(i,ItemStack.EMPTY);' in station
assert 'overflow.add(stack.copy())' in station
assert 'top.setChanged()' in station
# Rider must never return early before local physics, or riding packets
# report a stationary mount and freeze the Pallet Jack in place.
travel=jack[jack.index('@Override public void travel(Vec3 input)'):
            jack.index('@Override protected Vec3 getRiddenInput')]
assert 'if(level().isClientSide()){' not in travel
assert 'double forward=Math.max(-1,Math.min(1,input.z));' in travel
assert 'double strafe=Math.max(-1,Math.min(1,input.x));' in travel
assert 'safeFloor(level(),getX()+dx,getZ()+dz,heading)' in travel
assert 'move(MoverType.SELF,new Vec3(dx,0,dz))' in travel
print('PASS: two-block unified station with lossless legacy migration; client-side ridden jack movement')
