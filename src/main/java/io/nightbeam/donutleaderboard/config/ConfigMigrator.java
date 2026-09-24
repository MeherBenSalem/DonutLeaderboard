package io.nightbeam.donutleaderboard.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;

public final class ConfigMigrator {

    public static final int CURRENT_VERSION = 2;

    private ConfigMigrator() {
    }

    public static void migrate(FileConfiguration config) {
        int version = config.getInt("config-version", 0);
        if (version >= CURRENT_VERSION) {
            return;
        }
        if (version < 1) {
            ensureDefaultsV1(config);
            version = 1;
        }
        if (version < 2) {
            ensureDefaultsV2(config);
            version = 2;
        }
        config.set("config-version", CURRENT_VERSION);
    }

    private static void ensureDefaultsV1(FileConfiguration config) {
        config.addDefault("storage.type", "SQLITE");
        config.addDefault("cache.refresh-seconds", 120);
        config.addDefault("periods.timezone", "UTC");
        config.addDefault("gui.rows", 6);
        config.addDefault("gui.entries-per-page", 21);
        config.addDefault("bstats.enabled", false);
    }

    private static void ensureDefaultsV2(FileConfiguration config) {
        config.addDefault("gui.show-viewer-rank-slot", 49);
        config.addDefault("categories.custom", new LinkedHashMap<String, Object>());
        if (!config.isConfigurationSection("categories.builtin")) {
            Map<String, Object> builtin = new LinkedHashMap<>();
            builtin.put("balance", Map.of("enabled", true, "slot", 10));
            builtin.put("kills", Map.of("enabled", true, "slot", 11));
            config.set("categories.builtin", builtin);
        }
    }
}
