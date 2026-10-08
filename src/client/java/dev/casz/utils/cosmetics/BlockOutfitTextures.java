package dev.casz.utils.cosmetics;

import com.casz.colorfulrods.RgbBuildingBlocks;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;

public final class BlockOutfitTextures {
    private static final Map<Block, OutfitTexture> CACHE = new HashMap<>();
    private static final int RGB_FRAME_TICKS = 2;
    private static long animationTick;

    private BlockOutfitTextures() {}

    public static Identifier texture(Block block) {
        OutfitTexture texture = CACHE.computeIfAbsent(block, BlockOutfitTextures::create);
        texture.update(animationTick);
        return texture.id;
    }

    private static OutfitTexture create(Block block) {
        var client = Minecraft.getInstance();
        var state = block.defaultBlockState();
        var sprite = client.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite().contents();
        var sourceId = sprite.name().withPath(path -> "textures/" + path + ".png");
        var id = Identifier.fromNamespaceAndPath(
                "caszutils",
                "block_outfit/" + BuiltInRegistries.BLOCK.getKey(block).toString().replace(':', '/')
        );

        NativeImage sourceImage = null;
        try (var stream = client.getResourceManager().getResourceOrThrow(sourceId).open()) {
            sourceImage = NativeImage.read(stream);

            int frameWidth = Math.min(sprite.width(), sourceImage.getWidth());
            int frameHeight = Math.min(sprite.height(), sourceImage.getHeight());
            int tile = Math.max(16, Math.min(512, Math.max(frameWidth, frameHeight)));
            int columns = Math.max(1, sourceImage.getWidth() / frameWidth);
            int rows = Math.max(1, sourceImage.getHeight() / frameHeight);
            int frameCount = columns * rows;

            var armor = new NativeImage(tile * 4, tile * 2, false);
            var tintSource = client.getBlockColors().getTintSource(state, 0);
            int tint = tintSource == null ? -1 : tintSource.color(state);

            fillArmorFrame(
                    armor,
                    sourceImage,
                    frameWidth,
                    frameHeight,
                    columns,
                    tile,
                    0,
                    0,
                    0.0F,
                    tint
            );

            var dynamic = new DynamicTexture(() -> "CaszUtils block outfit " + id, armor);
            client.getTextureManager().register(id, dynamic);

            boolean liveRgb = RgbBuildingBlocks.BLOCKS.containsValue(block)
                    && sprite.isAnimated()
                    && frameCount > 1;

            if (liveRgb) {
                return new OutfitTexture(
                        id,
                        dynamic,
                        sourceImage,
                        frameWidth,
                        frameHeight,
                        columns,
                        frameCount,
                        tile,
                        tint
                );
            }

            sourceImage.close();
            return new OutfitTexture(id);
        } catch (java.io.IOException | RuntimeException e) {
            if (sourceImage != null && !sourceImage.isClosed()) sourceImage.close();
            org.slf4j.LoggerFactory.getLogger("caszutils")
                    .warn("Cannot create block outfit texture for {}", block, e);
            return new OutfitTexture(TextureManager.INTENTIONAL_MISSING_TEXTURE);
        }
    }

    private static void fillArmorFrame(
            NativeImage armor,
            NativeImage source,
            int frameWidth,
            int frameHeight,
            int columns,
            int tile,
            int currentFrame,
            int nextFrame,
            float blend,
            int tint
    ) {
        int currentX = (currentFrame % columns) * frameWidth;
        int currentY = (currentFrame / columns) * frameHeight;
        int nextX = (nextFrame % columns) * frameWidth;
        int nextY = (nextFrame / columns) * frameHeight;

        for (int y = 0; y < tile * 2; y++) {
            int sampleY = (y % tile) * frameHeight / tile;
            for (int x = 0; x < tile * 4; x++) {
                int sampleX = (x % tile) * frameWidth / tile;
                int a = source.getPixel(currentX + sampleX, currentY + sampleY);
                int b = source.getPixel(nextX + sampleX, nextY + sampleY);
                int pixel = blend <= 0.0F ? a : blendArgb(a, b, blend);

                int r = pixel >> 16 & 255;
                int g = pixel >> 8 & 255;
                int blue = pixel & 255;
                if (tint != -1 && r == g && g == blue) {
                    pixel = (pixel & 0xff000000)
                            | ((r * (tint >> 16 & 255) / 255) << 16)
                            | ((g * (tint >> 8 & 255) / 255) << 8)
                            | (blue * (tint & 255) / 255);
                }

                armor.setPixel(x, y, pixel);
            }
        }
    }

    private static int blendArgb(int a, int b, float amount) {
        float inverse = 1.0F - amount;
        int alpha = (int)(((a >>> 24) & 255) * inverse + ((b >>> 24) & 255) * amount);
        int red = (int)(((a >>> 16) & 255) * inverse + ((b >>> 16) & 255) * amount);
        int green = (int)(((a >>> 8) & 255) * inverse + ((b >>> 8) & 255) * amount);
        int blue = (int)((a & 255) * inverse + (b & 255) * amount);
        return alpha << 24 | red << 16 | green << 8 | blue;
    }

    public static void initialize() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> animationTick++);

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(
                new SimpleSynchronousResourceReloadListener() {
                    @Override
                    public Identifier getFabricId() {
                        return Identifier.fromNamespaceAndPath("caszutils", "block_outfits");
                    }

                    @Override
                    public void onResourceManagerReload(ResourceManager manager) {
                        var textures = Minecraft.getInstance().getTextureManager();
                        for (OutfitTexture texture : CACHE.values()) {
                            if (!texture.id.equals(TextureManager.INTENTIONAL_MISSING_TEXTURE)) {
                                textures.release(texture.id);
                            }
                            texture.closeSource();
                        }
                        CACHE.clear();
                        animationTick = 0;
                    }
                }
        );
    }

    private static final class OutfitTexture {
        private final Identifier id;
        private final DynamicTexture dynamic;
        private final NativeImage source;
        private final int frameWidth;
        private final int frameHeight;
        private final int columns;
        private final int frameCount;
        private final int tile;
        private final int tint;
        private long lastTick = Long.MIN_VALUE;

        private OutfitTexture(Identifier id) {
            this(id, null, null, 0, 0, 0, 0, 0, -1);
        }

        private OutfitTexture(
                Identifier id,
                DynamicTexture dynamic,
                NativeImage source,
                int frameWidth,
                int frameHeight,
                int columns,
                int frameCount,
                int tile,
                int tint
        ) {
            this.id = id;
            this.dynamic = dynamic;
            this.source = source;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.columns = columns;
            this.frameCount = frameCount;
            this.tile = tile;
            this.tint = tint;
        }

        private void update(long tick) {
            if (dynamic == null || source == null || frameCount <= 1 || tick == lastTick) return;

            int currentFrame = (int)((tick / RGB_FRAME_TICKS) % frameCount);
            int nextFrame = (currentFrame + 1) % frameCount;
            float blend = (tick % RGB_FRAME_TICKS) / (float)RGB_FRAME_TICKS;

            fillArmorFrame(
                    dynamic.getPixels(),
                    source,
                    frameWidth,
                    frameHeight,
                    columns,
                    tile,
                    currentFrame,
                    nextFrame,
                    blend,
                    tint
            );
            dynamic.upload();
            lastTick = tick;
        }

        private void closeSource() {
            if (source != null && !source.isClosed()) source.close();
        }
    }
}
