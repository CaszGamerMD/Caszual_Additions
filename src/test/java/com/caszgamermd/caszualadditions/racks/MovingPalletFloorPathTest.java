package com.caszgamermd.caszualadditions.racks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pallet followers never climb a cargo obstacle without a higher player. */
final class MovingPalletFloorPathTest {
    @Test void palletNeverStepsUpWhenPlayerStaysOnGround(){
        BlockPos start=new BlockPos(0,64,0),player=new BlockPos(6,64,0);
        Set<BlockPos> crates=Set.of(new BlockPos(2,64,0));
        List<BlockPos> path=MovingPalletFloorPath.route(start,player,false,
                p->!crates.contains(p));
        assertNotNull(path);
        assertFalse(path.isEmpty());
        BlockPos previous=start;
        for(BlockPos point:path){
            assertTrue(point.getY()<=previous.getY(),
                    "Do not climb a crate to keep up with level-ground player");
            assertTrue(Math.abs(point.getY()-previous.getY())<=1);
            previous=point;
        }
    }

    @Test void palletCanDropOneBlockToFollowDescendingPlayer(){
        BlockPos start=new BlockPos(0,64,0),player=new BlockPos(5,63,0);
        List<BlockPos> path=MovingPalletFloorPath.route(start,player,false,
                p->p.getY()==64||p.getY()==63&&p.getX()>=1);
        assertNotNull(path);
        assertFalse(path.isEmpty());
        BlockPos last=start;
        boolean descended=false;
        for(BlockPos next:path){
            if(next.getY()<last.getY())descended=true;
            assertTrue(next.getY()<=last.getY());
            assertTrue(last.getY()-next.getY()<=1);
            last=next;
        }
        assertTrue(descended);
        assertEquals(63,last.getY());
    }

    @Test void ascentOnlyPossibleWhenPlayerIsHigher(){
        BlockPos start=new BlockPos(0,64,0),higher=new BlockPos(4,65,0);
        assertNull(MovingPalletFloorPath.route(start,higher,false,p->true));
        List<BlockPos> route=MovingPalletFloorPath.route(start,higher,true,
                p->true);
        assertNotNull(route);
        assertTrue(route.stream().anyMatch(p->p.getY()==65));
    }

    @Test void frontDockRouteAvoidsBacksideObstructions(){
        BlockPos from=new BlockPos(-3,64,-3),front=new BlockPos(3,64,2);
        Set<BlockPos> blocked=new HashSet<>();
        for(int x=-2;x<=3;x++)blocked.add(new BlockPos(x,64,0));
        List<BlockPos> path=MovingPalletFloorPath.toRackFront(from,front,
                p->!blocked.contains(p));
        assertNotNull(path);
        assertEquals(front,path.getLast());
        assertFalse(path.stream().anyMatch(blocked::contains));
        BlockPos last=from;
        for(BlockPos point:path){
            assertEquals(1,Math.abs(point.getX()-last.getX())
                    +Math.abs(point.getZ()-last.getZ()));
            assertEquals(64,point.getY());
            last=point;
        }
    }
}
