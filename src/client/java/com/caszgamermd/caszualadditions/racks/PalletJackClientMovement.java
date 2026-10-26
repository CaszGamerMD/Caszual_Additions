package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Vehicle input needs to come from the CONTROLLING client's real keybinds,
 * not from Player.xxa/zza (usually zero while mounted in 26.2).
 * Respect remapped movement keys; W/S and A/D each cancel out.
 */
public final class PalletJackClientMovement {
    private PalletJackClientMovement(){}
    public static void initialize(){
        ClientTickEvents.START_CLIENT_TICK.register(client->{
            if(client.player==null
                    ||!(client.player.getVehicle() instanceof PalletJackVehicle jack))
                return;
            double x=0,z=0;
            if(client.screen==null){
                x=(client.options.keyRight.isDown()?1:0)
                        -(client.options.keyLeft.isDown()?1:0);
                z=(client.options.keyUp.isDown()?1:0)
                        -(client.options.keyDown.isDown()?1:0);
            }
            double length=Math.hypot(x,z);
            if(length>1){x/=length;z/=length;}
            jack.setClientMovement(new Vec3(x,0,z));
        });
    }
}
