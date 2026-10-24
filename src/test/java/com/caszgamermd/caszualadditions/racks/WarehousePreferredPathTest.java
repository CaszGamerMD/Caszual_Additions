package com.caszgamermd.caszualadditions.racks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Frozen lake and rolling-terrain path tests with no live world required. */
final class WarehousePreferredPathTest {
    private static BlockPos at(int x,int y,int z){
        return new BlockPos(x,y,z);
    }

    private static void checkedSteps(BlockPos start,List<BlockPos> route,
            Set<BlockPos> floor){
        BlockPos previous=start;
        for(BlockPos step:route){
            assertTrue(floor.contains(step),"Every waypoint needs a dry solid floor");
            assertEquals(1,Math.abs(step.getX()-previous.getX())
                    +Math.abs(step.getZ()-previous.getZ()),
                    "Never route diagonally or skip a cardinal waypoint");
            assertTrue(Math.abs(step.getY()-previous.getY())<=1,
                    "One-block ascents and descents only");
            previous=step;
        }
    }

    @Test void ordinaryLevelDetourWinsOverShortUnnecessaryClimb(){
        BlockPos from=at(0,64,0),to=at(4,64,0);
        Set<BlockPos> cells=new HashSet<>();
        for(int x=0;x<=4;x++)for(int z=-1;z<=1;z++)
            if(!(x==2&&z==0))cells.add(at(x,64,z));
        cells.add(at(2,65,0)); // short but unnecessary jump
        List<BlockPos> path=WarehousePreferredPath.leg(
                from,to,cells::contains,BlockPos.ZERO);
        assertNotNull(path);
        assertEquals(to,path.getLast());
        assertTrue(path.stream().allMatch(p->p.getY()==64),
                "A reasonably short flat detour must win");
        checkedSteps(from,path,cells);
    }

    @Test void blockedIceOrDirtLevelCanClimbAndDropOneBlock(){
        BlockPos from=at(0,64,0),to=at(4,64,0);
        Set<BlockPos> cells=Set.of(
                from,at(1,64,0),at(2,65,0),at(3,64,0),to);
        List<BlockPos> path=WarehousePreferredPath.leg(
                from,to,cells::contains,BlockPos.ZERO);
        assertNotNull(path,"One-block terrain ridge is not an impassable wall");
        assertEquals(to,path.getLast());
        assertTrue(path.stream().anyMatch(p->p.getY()==65));
        checkedSteps(from,path,cells);
    }

    @Test void longFlatDetourIsNotAnAbsoluteBanOnClimbing(){
        BlockPos from=at(0,64,0),to=at(4,64,0);
        Set<BlockPos> cells=new HashSet<>();
        for(int x:new int[]{0,1,3,4})cells.add(at(x,64,0));
        // A long but possible walk on the same height around the ridge.
        for(int z=0;z<=18;z++){
            cells.add(at(1,64,z));
            cells.add(at(3,64,z));
        }
        for(int x=1;x<=3;x++)cells.add(at(x,64,18));
        cells.add(at(2,65,0));
        List<BlockPos> path=WarehousePreferredPath.leg(
                from,to,cells::contains,BlockPos.ZERO);
        assertNotNull(path);
        assertEquals(to,path.getLast());
        assertTrue(path.stream().anyMatch(p->p.getY()==65),
                "A very long flat detour should lose to a necessary short step");
        checkedSteps(from,path,cells);
    }

    @Test void descendingToAnotherLevelIsAllowedAndMustFinish(){
        BlockPos from=at(0,65,0),to=at(3,64,0);
        Set<BlockPos> cells=Set.of(from,at(1,65,0),at(2,64,0),to);
        List<BlockPos> path=WarehousePreferredPath.leg(
                from,to,cells::contains,BlockPos.ZERO);
        assertNotNull(path);
        assertEquals(to,path.getLast());
        checkedSteps(from,path,cells);
    }

    @Test void waterGapAndTwoBlockCliffDoNotBecomeFakeRoutes(){
        BlockPos from=at(0,64,0),to=at(2,64,0);
        Set<BlockPos> gap=Set.of(from,to);
        assertNull(WarehousePreferredPath.leg(
                from,to,gap::contains,BlockPos.ZERO),
                "An unfrozen water gap must halt the mission");
        Set<BlockPos> cliff=Set.of(from,at(1,66,0),to);
        assertNull(WarehousePreferredPath.leg(
                from,to,cliff::contains,BlockPos.ZERO),
                "Two-block wall must not become a jump shortcut");
    }
    @Test void distantPackingStationIsReachedThroughRealPartialWalkingLegs(){
        BlockPos current=at(0,64,0),target=at(84,64,0);
        Set<BlockPos> floor=new HashSet<>();
        for(int x=0;x<=84;x++)floor.add(at(x,64,0));
        int legs=0;
        while(!current.equals(target)&&legs<8){
            List<BlockPos> path=WarehousePreferredPath.leg(
                    current,target,floor::contains,BlockPos.ZERO);
            assertNotNull(path,"Each 36-block warehouse leg must remain reachable");
            assertFalse(path.isEmpty());
            checkedSteps(current,path,floor);
            BlockPos next=path.getLast();
            assertTrue(next.getX()>current.getX(),
                    "A partial path may not stall on the same cell");
            current=next;
            legs++;
        }
        assertEquals(target,current,"Physical golem should finish at its service aisle");
        assertTrue(legs>=3,"A distant warehouse cannot use an imaginary 84-block jump");
    }

}
