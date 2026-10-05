package com.caszgamermd.caszualadditions.xp;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
public final class XpNetwork {
 private XpNetwork(){}
 public static List<XpTankBlockEntity> tanks(ServerLevel level,BlockPos start){
  var out=new ArrayList<XpTankBlockEntity>();var seen=new HashSet<BlockPos>();var q=new ArrayDeque<BlockPos>();q.add(start);
  while(!q.isEmpty()&&seen.size()<4096){var p=q.removeFirst();if(!seen.add(p))continue;var be=level.getBlockEntity(p);
   if(be instanceof XpTankBlockEntity tank){out.add(tank);for(var d:Direction.values())q.add(p.relative(d));}
   else if(p.equals(start)&&(be instanceof XpChargerBlockEntity||be instanceof XpDrainBlockEntity||be instanceof XpShowerBlockEntity)){for(var d:Direction.values())q.add(p.relative(d));}
  } return out;
 }
 public static int stored(ServerLevel l,BlockPos p){return tanks(l,p).stream().mapToInt(XpTankBlockEntity::stored).sum();}
 public static int space(ServerLevel l,BlockPos p){return Math.max(0,capacity(l,p)-stored(l,p));}
 public static int capacity(ServerLevel l,BlockPos p){return tanks(l,p).size()*XpTankBlockEntity.CAPACITY;}
 public static int extract(ServerLevel l,BlockPos p,int amount){int left=amount;for(var t:tanks(l,p)){int take=Math.min(left,t.stored());t.setStored(t.stored()-take);left-=take;if(left==0)break;}return amount-left;}
 public static int insert(ServerLevel l,BlockPos p,int amount){int left=amount;for(var t:tanks(l,p)){int put=Math.min(left,XpTankBlockEntity.CAPACITY-t.stored());t.setStored(t.stored()+put);left-=put;if(left==0)break;}return amount-left;}
}
