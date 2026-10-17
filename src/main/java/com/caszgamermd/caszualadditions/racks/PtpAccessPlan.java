package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** A PTP is a 3x3 floor workstation, NOT a place where the Golems may
 * stand. Match a rack's four-worker collection and outer unloading aisle:
 * two workers at the near edge, two behind, all OUTSIDE the station.
 * The selected face remains fixed for this job even when workers move. */
public final class PtpAccessPlan {
    private final BlockPos center;
    private final Direction facing;

    private PtpAccessPlan(BlockPos center,Direction facing){
        this.center=center.immutable();this.facing=facing;
    }
    public Direction facing(){return facing;}
    public BlockPos goal(int worker){
        int depth=2+(worker/2);
        int column=worker%2;
        return center.relative(facing,depth)
                .relative(facing.getClockWise(),column);
    }
    public BlockPos service(){return center.relative(facing,3);}
    public static PtpAccessPlan find(ServerLevel level,PtpBlockEntity ptp,
            BlockPos crewOrigin){
        PtpAccessPlan best=null;
        double nearest=Double.POSITIVE_INFINITY;
        for(Direction side:Direction.Plane.HORIZONTAL){
            PtpAccessPlan plan=new PtpAccessPlan(ptp.getBlockPos(),side);
            boolean clear=true;
            // Four worker positions, plus the 3x3 collision-free area for
            // the loaded carrier to assemble without intersecting the PTP.
            for(int worker=0;worker<4&&clear;worker++)
                if(!StorageGolemReturnPath.walkable(level,plan.goal(worker)))
                    clear=false;
            BlockPos service=plan.service();
            for(int x=-1;x<=1&&clear;x++)for(int z=-1;z<=1&&clear;z++)
                if(!StorageGolemReturnPath.walkable(level,service.offset(x,0,z)))
                    clear=false;
            if(!clear)continue;
            double score=plan.service().distSqr(crewOrigin);
            if(score<nearest){nearest=score;best=plan;}
        }
        return best;
    }
}
