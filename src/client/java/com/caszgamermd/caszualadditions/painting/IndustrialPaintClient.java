package com.caszgamermd.caszualadditions.painting;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderers;

public final class IndustrialPaintClient {
    private IndustrialPaintClient(){}
    public static void initialize(){
        EntityRenderers.register(IndustrialPaintContent.FINISH,IndustrialFinishRenderer::new);
        ClientPlayNetworking.registerGlobalReceiver(IndustrialPaintNet.Open.TYPE,
                (payload,ctx)->ctx.client().execute(()->
                    Minecraft.getInstance().gui.setScreen(
                            new IndustrialPaintScreen(payload.can(),payload.color()))));
    }
}