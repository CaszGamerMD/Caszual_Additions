package com.caszgamermd.caszualadditions.pallet;
import com.caszgamermd.caszualadditions.racks.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
public final class PalletAutomationGameTest {
 @GameTest(maxTicks=20,skyAccess=true)
 public void fourCornersShareOneFilteredInventory(GameTestHelper h){
  var world=h.getLevel();BlockPos root=h.absolutePos(new BlockPos(3,2,3));
  for(Direction facing:Direction.Plane.HORIZONTAL){
   BlockState state=PalletContent.WOODEN_PALLET.defaultBlockState().setValue(PalletBlock.PART,0).setValue(PalletBlock.LEGACY,false).setValue(PalletBlock.FACING,facing);
   world.setBlock(root,state,3);
   PalletContent.WOODEN_PALLET.setPlacedBy(world,root,state,null,new ItemStack(PalletContent.WOODEN_PALLET));
   var controller=(PalletBlockEntity)world.getBlockEntity(root);
   controller.setItem(0,new ItemStack(Items.APPLE,16));
   Set<BlockPos> footprint=new HashSet<>();
   for(int part=0;part<4;part++){
    BlockPos cell=PalletBlock.partPos(root,part,state);footprint.add(cell);
    if(!PalletBlock.rootPos(cell,world.getBlockState(cell)).equals(root)){h.fail("Pallet inverse geometry");return;}
    // This is the exact lookup used by Mining Rig Collection Coordinators.
    if(!(world.getBlockEntity(cell) instanceof Container port)||port.getContainerSize()!=216){h.fail("Missing inventory at part "+part);return;}
    if(port.removeItem(0,1).getCount()!=1){h.fail("Cannot extract through part "+part);return;}
    port.setItem(1,new ItemStack(Items.STONE,part+1));
    if(controller.getItem(1).getCount()!=part+1){h.fail("Write did not reach controller");return;}
    controller.setFilter(new ItemStack(Items.APPLE));
    if(port.canPlaceItem(2,new ItemStack(Items.STONE))){h.fail("Satellite bypasses filter");return;}
    controller.clearFilter();
   }
   if(controller.getItem(0).getCount()!=12){h.fail("Independent or duplicated inventories");return;}
   BlockState dispenser=RackContent.PALLET_STACK_BLOCK.defaultBlockState().setValue(StackOfPalletsBlock.FACING,facing);
   Set<BlockPos> magazine=new HashSet<>(),rack=new HashSet<>();
   for(int part=0;part<4;part++){
    var cell=StackOfPalletsBlock.cell(root,part,0,dispenser);magazine.add(cell);
    if(!StackOfPalletsBlock.root(cell,dispenser.setValue(StackOfPalletsBlock.PART,part)).equals(root)){h.fail("Dispenser inverse geometry");return;}
    rack.add(RackBlock.cell(root,facing.getOpposite(),part));
   }
   if(!footprint.equals(magazine)||!footprint.equals(rack)){h.fail("Different 2x2 placement footprints for "+facing);return;}
   for(int part=3;part>=0;part--)world.removeBlock(PalletBlock.partPos(root,part,state),false);
  }
  h.succeed();
 }
}
