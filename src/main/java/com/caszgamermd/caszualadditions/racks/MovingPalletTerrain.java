package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Visual ground probes for the four Golem positions. No AI, teleports,
 * block placement or cargo motion: only foot positions / carried-deck tilt.
 *
 * The vanilla model renderer flips the entity X axis before yaw rotation.
 * Apply that SAME transform to the local four corner offsets so terrain
 * sampling stays attached to the visible robots while the crew turns.
 */
public final class MovingPalletTerrain {
    public static final double FOOT_OFFSET=.875;
    private static final double[] LOCAL_X={-FOOT_OFFSET,FOOT_OFFSET,-FOOT_OFFSET,FOOT_OFFSET};
    private static final double[] LOCAL_Z={-FOOT_OFFSET,-FOOT_OFFSET,FOOT_OFFSET,FOOT_OFFSET};
    private MovingPalletTerrain(){}

    public record Ground(float[] height,int mask) {
        public int supporting(){return Integer.bitCount(mask);}
    }
    private static double walkableSurface(ServerLevel level,double x,double baseY,double z){
        int bx=(int)Math.floor(x),bz=(int)Math.floor(z);
        // Support above / below the central feet is allowed, but avoid
        // loading distant chunks or visually anchoring a worker on the
        // underside of a nearby rack, fence or the cargo itself.
        for(int y=(int)Math.floor(baseY+.80);y>=Math.floor(baseY-1.55);y--){
            BlockPos cell=new BlockPos(bx,y,bz);
            if(!level.hasChunkAt(cell))return Double.NaN;
            BlockState state=level.getBlockState(cell);
            VoxelShape shape=state.getCollisionShape(level,cell);
            if(shape.isEmpty())continue;
            double top=Double.NEGATIVE_INFINITY;
            double localX=x-bx,localZ=z-bz;
            for(var box:shape.toAabbs())
                if(localX>=box.minX&&localX<=box.maxX
                        &&localZ>=box.minZ&&localZ<=box.maxZ)
                    top=Math.max(top,y+box.maxY);
            if(top>baseY+.85||top<baseY-1.35)continue;
            return top;
        }
        return Double.NaN;
    }
    public static Ground probe(ServerLevel level,double centerX,double centerY,
            double centerZ,float bodyYaw){
        float[] offsets=new float[4];
        int mask=0;
        double angle=Math.toRadians(180.0-bodyYaw);
        double cos=Math.cos(angle),sin=Math.sin(angle);
        for(int i=0;i<4;i++){
            double x=-LOCAL_X[i]*cos+LOCAL_Z[i]*sin;
            double z=LOCAL_X[i]*sin+LOCAL_Z[i]*cos;
            double ground=walkableSurface(level,centerX+x,centerY,centerZ+z);
            if(Double.isNaN(ground))continue;
            mask|=1<<i;
            offsets[i]=(float)Math.clamp(ground-centerY,-1.30,.85);
        }
        // When exactly three feet are supported, the fourth waits without
        // walking in empty air. Its suspended height stays at the supported
        // crew's average; the pallet keeps balancing on the other three.
        if(Integer.bitCount(mask)==3){
            float total=0;
            for(int i=0;i<4;i++)if((mask&(1<<i))!=0)total+=offsets[i];
            float waiting=total/3.0f;
            for(int i=0;i<4;i++)if((mask&(1<<i))==0)offsets[i]=waiting;
        }
        return new Ground(offsets,mask);
    }
    public static float[] tilt(float[] height,int mask){
        if(Integer.bitCount(mask)<3)return new float[]{0f,0f};
        float dx=((height[1]+height[3])-(height[0]+height[2]))*.5f;
        float dz=((height[2]+height[3])-(height[0]+height[1]))*.5f;
        // Max 30 degrees: no severe bending or rotating cargo into racks.
        return new float[]{
                (float)Math.clamp(Math.atan2(dz,1.75),-.52360,.52360),
                (float)Math.clamp(Math.atan2(dx,1.75),-.52360,.52360)
        };
    }
}

