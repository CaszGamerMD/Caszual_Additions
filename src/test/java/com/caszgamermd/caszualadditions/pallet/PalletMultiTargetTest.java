package com.caszgamermd.caszualadditions.pallet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pure server-side validation of typed multi-target pallet filters. */
final class PalletMultiTargetTest {
    @Test void multipleItemsAndTagsAreValid() {
        assertTrue(PalletFilterNet.validTarget(0,
                "minecraft:stone, minecraft:dirt, minecraft:cobblestone"));
        assertTrue(PalletFilterNet.validTarget(2,
                "minecraft:logs, minecraft:planks"));
        assertTrue(PalletFilterNet.validTarget(1,"minecraft, caszual_additions"));
    }
    @Test void emptyAndMalformedEntriesAreRejected() {
        assertFalse(PalletFilterNet.validTarget(0,"minecraft:stone,"));
        assertFalse(PalletFilterNet.validTarget(0,",minecraft:stone"));
        assertFalse(PalletFilterNet.validTarget(1,"minecraft,  BAD!"));
        assertFalse(PalletFilterNet.validTarget(2,"minecraft:logs,,minecraft:planks"));
        assertFalse(PalletFilterNet.validTarget(0,
                "minecraft:a,minecraft:b,minecraft:c,minecraft:d,minecraft:e,"
                +"minecraft:f,minecraft:g,minecraft:h,minecraft:i"));
    }
    @Test void exactModeRemainsSampleOnly() {
        assertFalse(PalletFilterNet.validTarget(3,"minecraft:diamond_sword"));
        assertFalse(PalletFilterNet.validTarget(3,"minecraft:stone,minecraft:dirt"));
    }
}
