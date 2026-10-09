package com.caszgamermd.caszualadditions.utils;

import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticGuide;
import com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticGuideScreen;
import com.caszgamermd.caszualadditions.utils.mixin.MinecraftScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry;
import net.minecraft.client.particle.FireflyParticle;
import net.minecraft.client.particle.SingleQuadParticle;

public final class CaszUtilsClient {
    public static void initialize() {
        com.caszgamermd.caszualadditions.utils.cosmetics.CosmeticsClient.initialize();

        ClientPlayNetworking.registerGlobalReceiver(
                CosmeticGuide.OpenGuide.TYPE,
                (payload, context) -> context.client().execute(() ->
                        ((MinecraftScreenAccessor)(Object) context.client())
                                .caszual_additions$setScreen(new CosmeticGuideScreen()))
        );

        FireflyGlass.PARTICLES.forEach((color, type) -> {
            int rgb = color.getTextureDiffuseColor();
            ParticleProviderRegistry.getInstance().register(type, sprites -> {
                var vanilla = new FireflyParticle.FireflyProvider(sprites);
                return (options, level, x, y, z, dx, dy, dz, random) -> {
                    var particle = vanilla.createParticle(options, level, x, y, z, dx, dy, dz, random);
                    if (particle instanceof SingleQuadParticle quad) {
                        quad.setColor(
                                ((rgb >> 16) & 255) / 255f,
                                ((rgb >> 8) & 255) / 255f,
                                (rgb & 255) / 255f
                        );
                    }
                    return particle;
                };
            });
        });
    }
}
