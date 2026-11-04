package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/** Sends remappable controls to the server; only the first passenger can steer. */
public final class PalletBalloonClientMovement {
    private PalletBalloonClientMovement(){}
    public static void initialize(){
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(client.player==null
                    ||!(client.player.getVehicle() instanceof PalletBalloon balloon)
                    ||balloon.getFirstPassenger()!=client.player)return;
            float forward=0,turn=0,lift=0;
            if(client.gui.screen()==null){
                forward=(client.options.keyUp.isDown()?1:0)
                        -(client.options.keyDown.isDown()?1:0);
                turn=(client.options.keyRight.isDown()?1:0)
                        -(client.options.keyLeft.isDown()?1:0);
                lift=(client.options.keyJump.isDown()?1:0)
                        -(client.options.keySprint.isDown()?1:0);
            }
            balloon.setPilotInput(forward,turn,lift);
            ClientPlayNetworking.send(new PalletBalloonPilotPayload(forward,turn,lift));
        });
    }
}
