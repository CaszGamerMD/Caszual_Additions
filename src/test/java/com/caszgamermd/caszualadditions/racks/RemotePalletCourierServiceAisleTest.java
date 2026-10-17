package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The courier must stop at the actual 2x2 pallet, not an arbitrary point
 * four to seven blocks away from its corner or an unrelated nearby pallet. */
final class RemotePalletCourierServiceAisleTest {
    private static BlockPos at(int x,int y,int z){
        return new BlockPos(x,y,z);
    }

    @Test void everyFaceHasAValidClosePerimeter(){
        BlockPos[] footprint={at(20,64,40),at(21,64,41)};
        for(BlockPos service:new BlockPos[]{
                at(20,64,38),at(21,64,37), // north (2 / 3)
                at(20,64,43),at(21,64,44), // south
                at(18,64,40),at(17,64,41), // west
                at(23,64,40),at(24,64,41)  // east
        })assertTrue(RemotePalletCourierRoute.nearTargetService(
                service,footprint),service.toShortString());
    }

    @Test void oldDistantServiceAislesNoLongerCountAsArrival(){
        BlockPos[] footprint={at(20,64,40),at(21,64,41)};
        for(BlockPos wrong:new BlockPos[]{
                at(20,64,34),at(20,64,46),
                at(14,64,40),at(27,64,40),
                at(20,64,40),at(21,64,41),
                at(21,64,39),at(21,64,42)
        })assertFalse(RemotePalletCourierRoute.nearTargetService(
                wrong,footprint),wrong.toShortString());
    }

    @Test void unrelatedPalletServiceSpotCannotSatisfySavedTarget(){
        BlockPos[] assigned={at(20,64,40),at(21,64,41)};
        BlockPos[] unrelated={at(27,64,40),at(28,64,41)};
        BlockPos courier=at(27,64,38);
        assertTrue(RemotePalletCourierRoute.nearTargetService(
                courier,unrelated));
        assertFalse(RemotePalletCourierRoute.nearTargetService(
                courier,assigned));
    }

    @Test void oneBlockTerrainShiftOkayButNotCliff(){
        BlockPos[] footprint={at(20,64,40),at(21,64,41)};
        assertTrue(RemotePalletCourierRoute.nearTargetService(
                at(20,65,38),footprint));
        assertTrue(RemotePalletCourierRoute.nearTargetService(
                at(20,63,38),footprint));
        assertFalse(RemotePalletCourierRoute.nearTargetService(
                at(20,66,38),footprint));
    }
}
