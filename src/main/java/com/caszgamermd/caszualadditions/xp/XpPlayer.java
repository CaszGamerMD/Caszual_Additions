package com.caszgamermd.caszualadditions.xp;
import net.minecraft.world.entity.player.Player;
public final class XpPlayer {
 private XpPlayer(){}
 public static int total(Player p){return Math.max(0,p.totalExperience);}
 public static void remove(Player p,int amount){p.giveExperiencePoints(-Math.min(amount,total(p)));}
}
