package dev.casz.utils.cosmetics;

import java.util.HashMap;
import java.util.Map;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

public final class BlockOutfitTextures {
    private static final Map<Block, Identifier> CACHE = new HashMap<>();
    private BlockOutfitTextures() {}
    public static Identifier texture(Block block) { return CACHE.computeIfAbsent(block, BlockOutfitTextures::create); }
    private static Identifier create(Block block) {
        var client = Minecraft.getInstance();
        var state = block.defaultBlockState();
        var sprite = client.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite().contents();
        var source = sprite.name().withPath(path -> "textures/" + path + ".png");
        var id = Identifier.fromNamespaceAndPath("caszutils", "block_outfit/" + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).toString().replace(':', '/'));
        try (var stream = client.getResourceManager().getResourceOrThrow(source).open(); var image = NativeImage.read(stream)) {
            int tile = Math.max(16, Math.min(512, Math.max(sprite.width(), sprite.height())));
            var armor = new NativeImage(tile * 4, tile * 2, false);
            int width = Math.min(sprite.width(), image.getWidth()), height = Math.min(sprite.height(), image.getHeight());
            var tintSource = client.getBlockColors().getTintSource(state, 0);
            int tint = tintSource == null ? -1 : tintSource.color(state);
            for (int y=0;y<tile*2;y++) for(int x=0;x<tile*4;x++) {
                int pixel=image.getPixel((x%tile)*width/tile,(y%tile)*height/tile);
                int r=pixel>>16&255,g=pixel>>8&255,b=pixel&255;
                if(tint!=-1&&r==g&&g==b) pixel=(pixel&0xff000000)|((r*(tint>>16&255)/255)<<16)|((g*(tint>>8&255)/255)<<8)|(b*(tint&255)/255);
                armor.setPixel(x,y,pixel);
            }
            client.getTextureManager().register(id,new DynamicTexture(() -> "CaszUtils block outfit "+id,armor));
            return id;
        } catch (java.io.IOException|RuntimeException e) {
            org.slf4j.LoggerFactory.getLogger("caszutils").warn("Cannot create block outfit texture for {}",block,e);
            return net.minecraft.client.renderer.texture.TextureManager.INTENTIONAL_MISSING_TEXTURE;
        }
    }
    public static void initialize() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            public Identifier getFabricId(){return Identifier.fromNamespaceAndPath("caszutils","block_outfits");}
            public void onResourceManagerReload(ResourceManager manager){
                var textures=Minecraft.getInstance().getTextureManager();
                for(var id:CACHE.values()) if(!id.equals(net.minecraft.client.renderer.texture.TextureManager.INTENTIONAL_MISSING_TEXTURE)) textures.release(id);
                CACHE.clear();
            }
        });
    }
}