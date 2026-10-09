package com.caszgamermd.caszualadditions.utils.cosmetics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Only the layout is client-side. The four cosmetic items themselves remain
 * server-authoritative and are never stored in this preferences file.
 */
public final class CosmeticSlotPreferences {
    private static final Logger LOGGER = LoggerFactory.getLogger("Caszual Additions Cosmetics");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve("caszual-additions-cosmetic-slots.properties");
    private static final Properties SETTINGS = new Properties();
    private static boolean loaded;

    private CosmeticSlotPreferences() {}

    private static void load() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(FILE)) return;
        try (var reader = Files.newBufferedReader(FILE)) {
            SETTINGS.load(reader);
        } catch (IOException | IllegalArgumentException error) {
            LOGGER.warn("Unable to load cosmetic-slot visibility preferences", error);
        }
    }

    private static String playerKey(Minecraft client) {
        return client.player == null ? null : "player." + client.player.getUUID();
    }

    public static boolean expanded(Minecraft client) {
        load();
        String key = playerKey(client);
        return key != null && Boolean.parseBoolean(SETTINGS.getProperty(key, "false"));
    }

    public static void setExpanded(Minecraft client, boolean expanded) {
        load();
        String key = playerKey(client);
        if (key == null) return;
        SETTINGS.setProperty(key, Boolean.toString(expanded));
        try {
            Files.createDirectories(FILE.getParent());
            try (var writer = Files.newBufferedWriter(FILE)) {
                SETTINGS.store(writer, "Caszual Additions - cosmetic inventory visibility per player");
            }
        } catch (IOException error) {
            LOGGER.warn("Unable to save cosmetic-slot visibility preferences", error);
        }
    }
}
