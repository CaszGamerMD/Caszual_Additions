package com.caszgamermd.caszualadditions.racks;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
public final class PalletMotionGameTest {
 @GameTest(maxTicks=20,skyAccess=true)
 public void mountedJackMovesOnSupportedFloor(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  var world=h.getLevel();var jack=new PalletJackVehicle(RackContent.PALLET_JACK_VEHICLE_TYPE,world);
  var p=h.absolutePos(new BlockPos(3,2,3));jack.setPos(p.getX()+.5,p.getY(),p.getZ()+.5);
  jack.installJack(new ItemStack(RackContent.PALLET_JACK),p.getY());world.addFreshEntity(jack);
  var player=h.makeMockServerPlayerInLevel();player.setPos(jack.position());player.setYRot(0);
  if(!player.startRiding(jack,true,true)){h.fail("Cannot mount jack");return;}
  double before=jack.getZ();jack.travel(new Vec3(0,0,1));
  if(jack.getZ()<=before+.1){h.fail("Mounted jack did not move on flat floor");return;}
  if(Math.abs(jack.getY()-p.getY())>.015){h.fail("Jack left supported floor height");return;}
  h.succeed();
 }
 @GameTest(maxTicks=20,skyAccess=true)
 public void fourFeetProduceTiltOnHalfBlockStep(GameTestHelper h){
  for(int x=0;x<8;x++)for(int z=0;z<8;z++)h.setBlock(x,1,z,Blocks.STONE);
  for(int z=1;z<6;z++)h.setBlock(4,2,z,Blocks.SMOOTH_STONE_SLAB);
  var center=h.absolutePos(new BlockPos(3,2,3));
  var ground=MovingPalletTerrain.probe(h.getLevel(),center.getX()+.5,center.getY(),center.getZ()+.5,180);
  if(ground.mask()!=15){h.fail("Four terrain probes did not find support");return;}
  float[] tilt=MovingPalletTerrain.tilt(ground.height(),ground.mask());
  if(Math.abs(tilt[1])<.25||Math.abs(tilt[0])>.02){h.fail("Half-block side step did not tilt pallet correctly");return;}
  h.succeed();
 }
}
