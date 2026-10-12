package com.caszgamermd.caszualadditions.racks;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Regression checks for a 4-person pallet handoff: no early commit. */
final class WarehouseTeamProgressTest {
    @Test void requiresFourUniqueArrivals() {
        int mask=0;
        for(int i=0;i<3;i++){
            mask=WarehouseTeamProgress.arrive(mask,i);
            assertFalse(WarehouseTeamProgress.complete(mask),
                    "A three-worker arrival must never commit a full pallet");
        }
        assertEquals(3,WarehouseTeamProgress.count(mask));
        mask=WarehouseTeamProgress.arrive(mask,3);
        assertTrue(WarehouseTeamProgress.complete(mask));
        assertEquals(4,WarehouseTeamProgress.count(mask));
    }
    @Test void repeatedArrivalsCannotFakeOtherWorkers() {
        int mask=0;
        for(int i=0;i<100;i++)mask=WarehouseTeamProgress.arrive(mask,2);
        assertEquals(1,WarehouseTeamProgress.count(mask));
        assertFalse(WarehouseTeamProgress.complete(mask));
    }
    @Test void malformedWorkerIndicesCannotEscalateProgress() {
        assertEquals(0,WarehouseTeamProgress.arrive(0,-1));
        assertEquals(0,WarehouseTeamProgress.arrive(0,4));
        assertEquals(0,WarehouseTeamProgress.arrive(0,99));
        assertEquals(15,WarehouseTeamProgress.checkedMask(-1));
        assertEquals(4,WarehouseTeamProgress.count(0xFF));
    }
    @Test void reloadRoundTripMaintainsExactCheckpointCount() {
        int before=WarehouseTeamProgress.arrive(
                WarehouseTeamProgress.arrive(0,0),3);
        int loaded=WarehouseTeamProgress.checkedMask(before);
        assertEquals(2,WarehouseTeamProgress.count(loaded));
        assertFalse(WarehouseTeamProgress.complete(loaded));
        assertTrue(WarehouseTeamProgress.complete(
                WarehouseTeamProgress.arrive(
                    WarehouseTeamProgress.arrive(loaded,1),2)));
    }
}
