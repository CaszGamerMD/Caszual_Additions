package com.caszgamermd.caszualadditions.racks;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The warehouse ID remains the computer's immutable position, while its
 * player-assigned name survives world reloads and item registry changes. */
public final class WarehouseComputerBlockEntity extends BlockEntity {
    private String name="Warehouse";
    // Server-owned, per-warehouse settings; no spawn/virtual item creation.
    private int orderPackers=1;
    private BlockPos orderOutput=BlockPos.ZERO;
    public int orderPackers(){return orderPackers;}
    public BlockPos orderOutput(){return orderOutput;}
    public void configurePackers(int count){
        orderPackers=Math.clamp(count,1,4);setChanged();
    }
    public void selectOrderOutput(BlockPos ptp){
        orderOutput=ptp==null?BlockPos.ZERO:ptp.immutable();
        setChanged();
    }
    public WarehouseComputerBlockEntity(BlockPos pos,BlockState state){
        super(RackContent.COMPUTER_ENTITY,pos,state);
    }
    public String warehouseName(){return name;}
    public void rename(String requested){
        String sanitized=requested==null?"":requested.strip()
                .replaceAll("[\\p{Cntrl}]", "");
        if(sanitized.length()>40)sanitized=sanitized.substring(0,40);
        name=sanitized.isBlank()?"Warehouse":sanitized;
        setChanged();
        if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),
                getBlockState(),3);
    }
    public static String name(ServerLevel level,BlockPos pos){
        return level.getBlockEntity(pos) instanceof WarehouseComputerBlockEntity be
                ?be.warehouseName():"Warehouse";
    }
    @Override protected void saveAdditional(ValueOutput out){
        super.saveAdditional(out);out.putString("warehouse_name",name);
        out.putInt("order_packers",orderPackers);
        out.store("order_output_ptp",BlockPos.CODEC,orderOutput);
    }
    @Override protected void loadAdditional(ValueInput in){
        super.loadAdditional(in);
        name=in.getStringOr("warehouse_name","Warehouse");
        orderPackers=Math.clamp(in.getIntOr("order_packers",1),1,4);
        orderOutput=in.read("order_output_ptp",BlockPos.CODEC)
                .orElse(BlockPos.ZERO);
    }
}
