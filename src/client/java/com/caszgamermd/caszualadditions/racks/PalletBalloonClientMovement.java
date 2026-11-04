package com.caszgamermd.caszualadditions.racks;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** Reads the pilot's remappable movement keys; first passenger alone steers. */
public final class PalletBalloonClientMovement {
    private PalletBalloonClientMovement(){}
    public static void initialize(){
        ClientTickEvents.START_CLIENT_TICK.register(client->{
            if(client.player==null||!(client.player.getVehicle() instanceof PalletBalloon balloon)
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
        });
    }
}
