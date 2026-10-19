package com.caszgamermd.caszualadditions.xp;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class XpPlayerTest {
    @Test void vanillaLevelBoundaries(){
        assertEquals(0,XpPlayer.pointsToLevel(0));
        assertEquals(7,XpPlayer.pointsToLevel(1));
        assertEquals(352,XpPlayer.pointsToLevel(16));
        assertEquals(394,XpPlayer.pointsToLevel(17));
        assertEquals(1395,XpPlayer.pointsToLevel(30));
        assertEquals(1507,XpPlayer.pointsToLevel(31));
        assertEquals(1628,XpPlayer.pointsToLevel(32));
        assertEquals(7,XpPlayer.pointsForNextLevel(0));
        assertEquals(37,XpPlayer.pointsForNextLevel(15));
        assertEquals(42,XpPlayer.pointsForNextLevel(16));
        assertEquals(112,XpPlayer.pointsForNextLevel(30));
        assertEquals(121,XpPlayer.pointsForNextLevel(31));
    }

    @Test void deductPointsAcrossOneAndMultipleLevels(){
        for(int total:new int[]{0,1,7,8,38,351,352,353,393,394,395,
                                 1490,1506,1507,1508,1627,1628,1629,30970,
                                 250000,Integer.MAX_VALUE}){
            int level=XpPlayer.levelForPoints(total);
            assertTrue(XpPlayer.pointsToLevel(level)<=total,"start "+total);
            assertTrue(XpPlayer.pointsToLevel(level+1)>total,"next "+total);
            // Subtract 40 XP (one automatic Drain tick) and verify exact
            // rebasing even when a level threshold lies in the middle.
            int after=Math.max(0,total-40);
            int lower=XpPlayer.levelForPoints(after);
            assertTrue(XpPlayer.pointsToLevel(lower)<=after);
            assertTrue(XpPlayer.pointsToLevel(lower+1)>after);
            int within=(int)(after-XpPlayer.pointsToLevel(lower));
            assertTrue(within>=0&&within<XpPlayer.pointsForNextLevel(lower));
        }
        assertEquals(15,XpPlayer.levelForPoints(352-40));
        assertEquals(16,XpPlayer.levelForPoints(394-40));
        assertEquals(0,XpPlayer.levelForPoints(0));
        assertEquals(0,XpPlayer.levelForPoints(6));
    }
}
