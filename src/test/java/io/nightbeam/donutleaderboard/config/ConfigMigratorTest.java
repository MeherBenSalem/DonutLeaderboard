package io.nightbeam.donutleaderboard.config;

import be.seeseemelk.mockbukkit.MockBukkit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigMigratorTest {

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    void migratesFromZeroToCurrent() {
        YamlConfiguration config = new YamlConfiguration();
        ConfigMigrator.migrate(config);
        assertEquals(ConfigMigrator.CURRENT_VERSION, config.getInt("config-version"));
        assertTrue(config.contains("storage.type"));
        assertTrue(config.contains("gui.show-viewer-rank-slot"));
    }
}
