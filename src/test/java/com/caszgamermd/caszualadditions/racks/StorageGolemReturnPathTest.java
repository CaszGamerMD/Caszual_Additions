package com.caszgamermd.caszualadditions.racks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** A flat accessible detour MUST win over climbing a one-block obstacle. */
final class StorageGolemReturnPathTest {
    private static Set<BlockPos> blocked(int y,int[][] xz){
        Set<BlockPos> cells=new HashSet<>();
        for(int[] xy:xz)cells.add(new BlockPos(xy[0],y,xy[1]));
        return cells;
    }

    @Test void walksAroundSingleObstacleOnSameFloor(){
        BlockPos start=new BlockPos(0,64,0),home=new BlockPos(5,64,0);
        Set<BlockPos> walls=blocked(64,new int[][]{{2,0}});
        List<BlockPos> path=StorageGolemReturnPath.route(start,home,
                pos->!walls.contains(pos)&&!pos.equals(home));
        assertNotNull(path);
        assertFalse(path.isEmpty());
        BlockPos previous=start;
        boolean tookDetour=false;
        for(BlockPos turn:path){
            assertEquals(64,turn.getY(),"Home route must never change elevation");
            assertTrue(previous.getX()==turn.getX()
                    ||previous.getZ()==turn.getZ(),"No diagonal shortcuts");
            int dx=Integer.compare(turn.getX(),previous.getX());
            int dz=Integer.compare(turn.getZ(),previous.getZ());
            for(BlockPos point=previous;!point.equals(turn);
                    point=point.offset(dx,0,dz)){
                assertFalse(walls.contains(point),"Cannot route through blocking crate");
                if(point.getZ()!=0)tookDetour=true;
            }
            previous=turn;
        }
        assertTrue(tookDetour,"Must detour on same Y rather than jumping over crate");
        int finalDistance=Math.abs(previous.getX()-home.getX())
                +Math.abs(previous.getZ()-home.getZ());
        assertTrue(finalDistance>=1&&finalDistance<=2);
        assertTrue(StorageGolemReturnPath.canDock(previous,home,
                pos->!walls.contains(pos)&&!pos.equals(home)));
    }

    @Test void enclosedShelfCannotTriggerJumpShortcut(){
        BlockPos home=new BlockPos(5,64,0),start=new BlockPos(0,64,0);
        Set<BlockPos> walls=blocked(64,new int[][]{
                {4,0},{6,0},{5,-1},{5,1}
        });
        assertNull(StorageGolemReturnPath.route(start,home,
                pos->!walls.contains(pos)&&!pos.equals(home)));
    }

    @Test void wrongFloorDoesNotPretendToBeFlat(){
        assertNull(StorageGolemReturnPath.route(
                new BlockPos(0,65,0),new BlockPos(4,64,0),pos->true));
    }

    @Test void higherStandingFloorStillHasAnAccessibleShelfApproach(){
        BlockPos actualShelf=new BlockPos(5,64,5);
        BlockPos from=new BlockPos(2,65,5);
        BlockPos shelfPlane=new BlockPos(actualShelf.getX(),from.getY(),actualShelf.getZ());
        List<BlockPos> path=StorageGolemReturnPath.route(from,shelfPlane,
                pos->!pos.equals(shelfPlane));
        assertNotNull(path);
        assertFalse(path.isEmpty());
        for(BlockPos point:path)assertEquals(65,point.getY());
        assertTrue(StorageGolemReturnPath.canDock(path.getLast(),shelfPlane,
                pos->!pos.equals(shelfPlane)));
    }

    @Test void twoDeepAisleCanDockWithoutCrowdingOneBlock(){
        BlockPos shelf=new BlockPos(5,64,5);
        BlockPos outer=shelf.north(2);
        assertTrue(StorageGolemReturnPath.canDock(outer,shelf,
                pos->!pos.equals(shelf)));
        assertEquals(List.of(),StorageGolemReturnPath.route(outer,shelf,
                pos->!pos.equals(shelf)));
        assertFalse(StorageGolemReturnPath.canDock(outer,shelf,
                pos->!pos.equals(shelf)&&!pos.equals(shelf.north())));
    }

    @Test void exactStationRouteAvoidsCrateWithoutJumping(){
        BlockPos start=new BlockPos(0,70,0),target=new BlockPos(8,70,0);
        Set<BlockPos> crates=blocked(70,new int[][]{{4,0},{4,1},{4,-1}});
        List<BlockPos> route=StorageGolemReturnPath.routeTo(start,target,
                p->!crates.contains(p));
        assertNotNull(route);
        assertEquals(target,route.getLast());
        for(BlockPos turn:route)assertEquals(70,turn.getY());
        assertNull(StorageGolemReturnPath.routeTo(start,target.above(),
                p->true),"Never invent a ladder or jump through a rack");
    }

    @Test void exactWorkstationCannotBeInsideBlockingPallet(){
        BlockPos start=new BlockPos(0,64,0),target=new BlockPos(3,64,0);
        assertNull(StorageGolemReturnPath.routeTo(start,target,
                p->!p.equals(target)));
    }

    @Test void nearbySideNeedsNoPath(){
        BlockPos home=new BlockPos(5,64,5),start=home.north();
        assertEquals(List.of(),StorageGolemReturnPath.route(start,home,
                pos->!pos.equals(home)));
    }
}
