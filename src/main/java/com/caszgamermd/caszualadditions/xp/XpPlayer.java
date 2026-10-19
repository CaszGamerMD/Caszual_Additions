package com.caszgamermd.caszualadditions.xp;

import net.minecraft.world.entity.player.Player;

/**
 * Spendable XP is represented by the visible level and progress bar.
 * Player.totalExperience is NOT reliable for this: it is the lifetime XP
 * counter and can differ from the points currently available, particularly
 * after enchanting or modded XP changes.
 */
public final class XpPlayer {
    private XpPlayer(){}

    /** Exact vanilla total XP needed to START a given level. Use long
     * arithmetic to prevent overflow at high XP levels. */
    public static long pointsToLevel(int level){
        long l=Math.max(0,level);
        if(l<=16)return l*l+6*l;
        if(l<=31)return (5*l*l-81*l+720)/2;
        return (9*l*l-325*l+4440)/2;
    }

    public static int pointsForNextLevel(int level){
        int l=Math.max(0,level);
        if(l<=15)return 2*l+7;
        if(l<=30)return 5*l-38;
        return 9*l-158;
    }

    /** The exact spendable XP according to the player HUD. Rounded point
     * progress prevents float precision from silently losing a point.
     */
    public static int total(Player p){
        long base=pointsToLevel(p.experienceLevel);
        int cost=pointsForNextLevel(p.experienceLevel);
        int progress=Math.clamp(Math.round(Math.clamp(p.experienceProgress,0f,1f)*cost),
                0,cost-1);
        return (int)Math.min(Integer.MAX_VALUE,base+progress);
    }

    /** Finds level using binary search, not a potentially huge loop. */
    public static int levelForPoints(int points){
        long xp=Math.max(0,points);
        int low=0,high=30000;
        while(low<high){
            int mid=low+(high-low+1)/2;
            if(pointsToLevel(mid)<=xp)low=mid;
            else high=mid-1;
        }
        return low;
    }

    /** Apply an actual XP balance to BOTH bar and level; never clamp a
     * deduction to the current level's progress alone.
     */
    public static void setTotal(Player player,int balance){
        int points=Math.max(0,balance);
        int level=levelForPoints(points);
        int within=(int)(points-pointsToLevel(level));
        player.experienceLevel=level;
        player.experienceProgress=(float)within/pointsForNextLevel(level);
        player.totalExperience=points;
        // Vanilla ServerPlayer XP synchronization continues in its normal
        // player update; also run its XP mutator to trigger change tracking.
        player.giveExperiencePoints(0);
    }

    /** Number of actual XP points deducted; safe across level boundaries. */
    public static int remove(Player player,int amount){
        if(amount<=0)return 0;
        int available=total(player);
        int deduction=Math.min(available,amount);
        if(deduction>0)setTotal(player,available-deduction);
        return deduction;
    }
}
