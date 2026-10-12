package com.caszgamermd.caszualadditions.racks;

/**
 * Pure four-worker checkpoint math, isolated so the coordination invariant
 * can be exercised by JUnit without loading a Minecraft world.
 */
public final class WarehouseTeamProgress {
    public static final int WORKERS=4;
    public static final int ALL=(1<<WORKERS)-1;
    private WarehouseTeamProgress(){}
    public static int checkedMask(int mask){return mask&ALL;}
    public static int arrive(int mask,int worker){
        if(worker<0||worker>=WORKERS)return checkedMask(mask);
        return checkedMask(mask)|(1<<worker);
    }
    public static int count(int mask){return Integer.bitCount(checkedMask(mask));}
    public static boolean complete(int mask){return checkedMask(mask)==ALL;}
}
