package com.caszgamermd.caszualadditions.doors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Texture-driven three-dimensional doors and trapdoors.
 *
 * This is deliberately client-only. During resource reload, vanilla-compatible
 * block models are reconstructed using the actual opacity of their active
 * resource-pack textures. Transparent openings then have genuine interior
 * walls, not only a transparent decal on a flat rectangle.
 *
 * All DoorBlock and TrapDoorBlock registrations are considered, including
 * third-party blocks. Models using nonstandard geometry are left untouched.
 */
public final class VoxelDoorsClient {
    private static final Logger LOGGER = LoggerFactory.getLogger("caszual_additions/voxel_doors");

    private static final List<String> DOOR_VARIANTS = List.of(
            "_bottom_left", "_bottom_right", "_bottom_left_open", "_bottom_right_open",
            "_top_left", "_top_right", "_top_left_open", "_top_right_open");
    private static final List<String> TRAPDOOR_VARIANTS = List.of("_bottom", "_top", "_open");

    private VoxelDoorsClient() {
    }

    public static void initialize() {
        PreparableModelLoadingPlugin.register(
                (store, executor) -> CompletableFuture.supplyAsync(
                        () -> prepare(store.resourceManager()), executor),
                (models, context) -> context.modifyModelOnLoad().register((original, loadContext) -> {
                    BlockModel replacement = models.get(loadContext.id());
                    return replacement == null ? original : replacement;
                })
        );
    }

    private static Map<Identifier, BlockModel> prepare(ResourceManager resources) {
        Map<Identifier, BlockModel> models = new HashMap<>();
        int doors = 0;
        int trapdoors = 0;

        for (Block block : BuiltInRegistries.BLOCK) {
            boolean door = block instanceof DoorBlock;
            boolean trapdoor = block instanceof TrapDoorBlock;
            if (!door && !trapdoor) {
                continue;
            }

            Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);
            List<String> variants = door ? DOOR_VARIANTS : TRAPDOOR_VARIANTS;
            int added = 0;
            for (String suffix : variants) {
                Identifier id = Identifier.fromNamespaceAndPath(blockId.getNamespace(),
                        "block/" + blockId.getPath() + suffix);
                try {
                    BlockModel model = buildModel(resources, id, door);
                    if (model != null) {
                        models.put(id, model);
                        added++;
                    }
                } catch (Exception error) {
                    LOGGER.debug("Skipping unsupported door model {}", id, error);
                }
            }
            if (added != 0) {
                if (door) doors++;
                else trapdoors++;
            }
        }

        LOGGER.info("Generated depth-aware models for {} doors and {} trapdoors ({} model variants)",
                doors, trapdoors, models.size());
        return Map.copyOf(models);
    }

    private static BlockModel buildModel(ResourceManager resources, Identifier modelId, boolean door)
            throws IOException {
        JsonObject original = readJson(resources, modelLocation(modelId));
        if (original == null || !original.has("parent") || !original.has("textures")) {
            return null;
        }

        String parentName = original.get("parent").getAsString();
        // Only replace Minecraft-shaped models; keep special modded geometries
        // (sliding / double-wide / animated doors) exactly as their authors made them.
        String expected = door ? "minecraft:block/door_" : "minecraft:block/template_";
        if (!parentName.startsWith(expected)
                || (!door && !parentName.contains("trapdoor_"))) {
            return null;
        }

        Identifier parentId = Identifier.parse(parentName);
        JsonObject template = readJson(resources, modelLocation(parentId));
        if (template == null || !template.has("elements")) {
            return null;
        }
        JsonArray elements = template.getAsJsonArray("elements");
        if (elements.size() != 1) {
            return null;
        }
        JsonObject element = elements.get(0).getAsJsonObject();
        JsonArray from = element.getAsJsonArray("from");
        JsonArray to = element.getAsJsonArray("to");
        JsonObject templateFaces = element.getAsJsonObject("faces");

        double[] a = readCoords(from);
        double[] b = readCoords(to);
        int axis = thinAxis(a, b);
        if (axis < 0) {
            return null;
        }

        String textureName = door ? (modelId.getPath().contains("_top_") ? "top" : "bottom") : "texture";
        // Also recognize _top_left/_top_right (which do not end in _top_).
        if (door && (modelId.getPath().endsWith("_top_left")
                || modelId.getPath().endsWith("_top_right"))) {
            textureName = "top";
        }

        JsonObject textureSlots = original.getAsJsonObject("textures");
        if (!textureSlots.has(textureName)) {
            return null;
        }
        String textureRef = textureSlots.get(textureName).getAsString();
        if (textureRef.startsWith("#")) {
            return null;
        }
        Identifier textureId = Identifier.parse(textureRef);
        BufferedImage texture = readImage(resources,
                Identifier.fromNamespaceAndPath(textureId.getNamespace(),
                        "textures/" + textureId.getPath() + ".png"));
        if (texture == null || texture.getWidth() < 16 || texture.getHeight() < 16) {
            return null;
        }

        String[] main = switch (axis) {
            case 0 -> new String[] {"west", "east"};
            case 1 -> new String[] {"up", "down"};
            default -> new String[] {"north", "south"};
        };
        if (!templateFaces.has(main[0]) || !templateFaces.has(main[1])) {
            return null;
        }

        boolean[][] solid = readMask(texture, templateFaces.getAsJsonObject(main[0]));
        List<int[]> rectangles = mergeOpaquePixels(solid);
        if (rectangles.isEmpty()) {
            return null;
        }

        JsonArray threeDimensionalElements = new JsonArray();
        for (int[] rect : rectangles) {
            threeDimensionalElements.add(makeElement(a, b, axis, rect, templateFaces,
                    "#" + textureName));
        }

        JsonObject generated = new JsonObject();
        generated.addProperty("parent", parentName);
        generated.add("textures", textureSlots.deepCopy());
        generated.addProperty("ambientocclusion", false);
        generated.add("elements", threeDimensionalElements);
        return BlockModel.fromStream(new StringReader(generated.toString()));
    }

    private static boolean[][] readMask(BufferedImage image, JsonObject frontFace) {
        boolean[][] opaque = new boolean[16][16];
        JsonArray uv = frontFace.getAsJsonArray("uv");
        boolean flipX = uv != null && uv.get(0).getAsDouble() > uv.get(2).getAsDouble();
        boolean flipY = uv != null && uv.get(1).getAsDouble() > uv.get(3).getAsDouble();

        // The first frame gives stable geometry for animated texture packs.
        for (int row = 0; row < 16; row++) {
            for (int col = 0; col < 16; col++) {
                int px = (flipX ? 15 - col : col) * image.getWidth() / 16;
                int py = (flipY ? 15 - row : row) * 16 / 16;
                // Sample near the center of the source texel.
                px = Math.min(image.getWidth() - 1, px + Math.max(0, image.getWidth() / 32));
                py = Math.min(15, py);
                opaque[row][col] = ((image.getRGB(px, py) >>> 24) & 255) >= 32;
            }
        }
        return opaque;
    }

    private static List<int[]> mergeOpaquePixels(boolean[][] opaque) {
        boolean[][] used = new boolean[16][16];
        List<int[]> rectangles = new ArrayList<>();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                if (!opaque[y][x] || used[y][x]) {
                    continue;
                }
                int width = 0;
                while (x + width < 16 && opaque[y][x + width] && !used[y][x + width]) {
                    width++;
                }
                int height = 1;
                outer: while (y + height < 16) {
                    for (int dx = 0; dx < width; dx++) {
                        if (!opaque[y + height][x + dx] || used[y + height][x + dx]) {
                            break outer;
                        }
                    }
                    height++;
                }
                for (int dy = 0; dy < height; dy++) {
                    for (int dx = 0; dx < width; dx++) {
                        used[y + dy][x + dx] = true;
                    }
                }
                rectangles.add(new int[] {x, y, width, height});
            }
        }
        return rectangles;
    }

    private static JsonObject makeElement(double[] a, double[] b, int axis, int[] rect,
                                          JsonObject templateFaces, String texture) {
        double x = rect[0], y = rect[1], w = rect[2], h = rect[3];
        double[] lo = a.clone();
        double[] hi = b.clone();
        switch (axis) {
            case 0 -> {
                lo[1] = b[1] - (y + h) * (b[1] - a[1]) / 16;
                hi[1] = b[1] - y * (b[1] - a[1]) / 16;
                lo[2] = a[2] + x * (b[2] - a[2]) / 16;
                hi[2] = a[2] + (x + w) * (b[2] - a[2]) / 16;
            }
            case 1 -> {
                lo[0] = a[0] + x * (b[0] - a[0]) / 16;
                hi[0] = a[0] + (x + w) * (b[0] - a[0]) / 16;
                lo[2] = a[2] + y * (b[2] - a[2]) / 16;
                hi[2] = a[2] + (y + h) * (b[2] - a[2]) / 16;
            }
            default -> {
                lo[0] = a[0] + x * (b[0] - a[0]) / 16;
                hi[0] = a[0] + (x + w) * (b[0] - a[0]) / 16;
                lo[1] = b[1] - (y + h) * (b[1] - a[1]) / 16;
                hi[1] = b[1] - y * (b[1] - a[1]) / 16;
            }
        }
        JsonObject out = new JsonObject();
        out.add("from", coords(lo));
        out.add("to", coords(hi));
        JsonObject faces = new JsonObject();
        String[] main = switch (axis) {
            case 0 -> new String[] {"west", "east"};
            case 1 -> new String[] {"up", "down"};
            default -> new String[] {"north", "south"};
        };
        for (String direction : main) {
            JsonObject originalFace = templateFaces.getAsJsonObject(direction);
            JsonObject face = new JsonObject();
            face.addProperty("texture", texture);
            JsonArray full = originalFace.getAsJsonArray("uv");
            if (full != null) {
                double u0 = full.get(0).getAsDouble();
                double v0 = full.get(1).getAsDouble();
                double u1 = full.get(2).getAsDouble();
                double v1 = full.get(3).getAsDouble();
                face.add("uv", coords(new double[] {
                        u0 + (u1 - u0) * x / 16, v0 + (v1 - v0) * y / 16,
                        u0 + (u1 - u0) * (x + w) / 16,
                        v0 + (v1 - v0) * (y + h) / 16
                }));
            }
            if (originalFace.has("rotation")) {
                face.add("rotation", originalFace.get("rotation").deepCopy());
            }
            faces.add(direction, face);
        }

        // Exposed cuts are genuine sides. Use opaque texels from the same art
        // instead of the vanilla thin-block edge mapping, which can sample
        // transparent pixels and make the new walls invisible.
        String[] sides = switch (axis) {
            case 0 -> new String[] {"north", "south", "down", "up"};
            case 1 -> new String[] {"west", "east", "north", "south"};
            default -> new String[] {"west", "east", "down", "up"};
        };
        JsonArray left = coords(new double[] {x + 0.15, y, x + 0.85, y + h});
        JsonArray right = coords(new double[] {x + w - 0.85, y, x + w - 0.15, y + h});
        JsonArray bottom = coords(new double[] {x, y + h - 0.85, x + w, y + h - 0.15});
        JsonArray top = coords(new double[] {x, y + 0.15, x + w, y + 0.85});
        JsonArray[] sideUVs = {left, right, bottom, top};
        for (int i = 0; i < sides.length; i++) {
            JsonObject face = new JsonObject();
            face.addProperty("texture", texture);
            face.add("uv", sideUVs[i]);
            faces.add(sides[i], face);
        }
        out.add("faces", faces);
        return out;
    }

    private static int thinAxis(double[] from, double[] to) {
        int thin = -1;
        for (int axis = 0; axis < 3; axis++) {
            double span = to[axis] - from[axis];
            if (Math.abs(span - 3.0) <= 0.001) {
                if (thin != -1) return -1;
                thin = axis;
            } else if (Math.abs(span - 16.0) > 0.001) {
                return -1;
            }
        }
        return thin;
    }

    private static double[] readCoords(JsonArray array) {
        return new double[] {
                array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()
        };
    }

    private static JsonArray coords(double[] values) {
        JsonArray array = new JsonArray();
        for (double value : values) array.add(value);
        return array;
    }

    private static Identifier modelLocation(Identifier modelId) {
        return Identifier.fromNamespaceAndPath(modelId.getNamespace(),
                "models/" + modelId.getPath() + ".json");
    }

    private static JsonObject readJson(ResourceManager resources, Identifier id) throws IOException {
        var res = resources.getResource(id);
        if (res.isEmpty()) return null;
        try (Reader reader = res.get().openAsReader()) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static BufferedImage readImage(ResourceManager resources, Identifier id) throws IOException {
        var res = resources.getResource(id);
        if (res.isEmpty()) return null;
        try (var stream = res.get().open()) {
            return ImageIO.read(stream);
        }
    }
}
