package com.caszgamermd.caszualadditions.racks;

import com.caszgamermd.caszualadditions.pallet.PalletContent;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Priority cancellation stops new picking without voiding reserved cargo. */
public final class BuilderCancelOverrideGameTest {
    @GameTest(maxTicks=70,skyAccess=true)
    public void cancelOverridesIncompleteManifestAndPreservesPalletStock(
            GameTestHelper h){
        h.setBlock(3,2,3,RackContent.PTP_TRANSFER.defaultBlockState()
                .setValue(PtpBlock.PART,5));
        BlockPos pos=h.absolutePos(new BlockPos(3,2,3));
        if(!(h.getLevel().getBlockEntity(pos) instanceof PtpBlockEntity ptp)){
            h.fail("PTP center block entity missing");return;
        }
        ptp.setInput(false);
        if(!ptp.installPallet(new ItemStack(PalletContent.WOODEN_PALLET_ITEM))){
            h.fail("Cannot mount real pallet for cancel test: "
                    +ptp.palletInstallProblem());return;
        }
        UUID request=UUID.randomUUID(),owner=UUID.randomUUID();
        BlockPos table=pos.offset(20,0,0),computer=pos.offset(-20,0,0);
        if(!ptp.beginBuilderOutput(request,owner,table,computer,
                List.of(new PtpBlockEntity.OutputOrder("minecraft:stone",500)))){
            h.fail("Could not create unfinished Builder manifest");return;
        }
        int active=ptp.reserveOutput("minecraft:stone",32);
        if(active==0){h.fail("No physical picker lease");return;}
        // A partially packed pallet is already owned by the warehouse.
        ItemStack physical=new ItemStack(Items.COBBLESTONE,5);
        ptp.setItem(0,physical.copy());
        if(ptp.getItem(0).getCount()!=5){
            h.fail("Physical cargo was not staged");return;
        }
        if(!ptp.cancelBuilderFulfillment(request,owner,table,computer)
                ||ptp.pendingOutputOrders()!=0
                ||ptp.reserveOutput("minecraft:stone",1)!=0
                ||!ptp.hasReservation(active)
                ||ptp.getItem(0).getCount()!=5){
            h.fail("Cancel did not stop picking while preserving picker and cargo");
            return;
        }
        // A returning picker relinquishes its lease; only then may the
        // cancelled order free its UUID. Existing packed items stay put.
        if(ptp.finalizeCancelledBuilder(request,owner,table,computer)){
            h.fail("Cancelled request released a picker still in flight");return;
        }
        ptp.releaseOutput(active);
        if(!ptp.finalizeCancelledBuilder(request,owner,table,computer)
                ||ptp.builderReserved()||ptp.builderCancellationRequested()
                ||ptp.getItem(0).getCount()!=5){
            h.fail("Cancel failed to release ticket with cargo intact");return;
        }
        h.succeed();
    }
}
