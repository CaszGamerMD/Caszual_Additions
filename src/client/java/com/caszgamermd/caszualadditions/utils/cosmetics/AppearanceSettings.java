package com.caszgamermd.caszualadditions.utils.cosmetics;

import java.nio.file.Files;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class AppearanceSettings {
    private static final java.nio.file.Path FILE = FabricLoader.getInstance().getConfigDir().resolve("caszual_additions-appearance.properties");
    private static boolean hideArmor;
    public static void load() {
        if (!Files.exists(FILE)) return;
        try (var in = Files.newInputStream(FILE)) {
            var properties = new Properties(); properties.load(in);
            hideArmor = Boolean.parseBoolean(properties.getProperty("hideArmor", "false"));
        } catch (java.io.IOException e) { org.slf4j.LoggerFactory.getLogger("caszual_additions").warn("Cannot load appearance settings", e); }
    }
    public static boolean hideArmor() { return hideArmor; }
    public static void toggle() {
        hideArmor = !hideArmor;
        var properties = new Properties(); properties.setProperty("hideArmor", Boolean.toString(hideArmor));
        try { Files.createDirectories(FILE.getParent()); try (var out = Files.newOutputStream(FILE)) { properties.store(out, "Caszual Additions appearance"); } }
        catch (java.io.IOException e) { org.slf4j.LoggerFactory.getLogger("caszual_additions").warn("Cannot save appearance settings", e); }
    }
}