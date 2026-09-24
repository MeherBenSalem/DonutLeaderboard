package io.nightbeam.donutleaderboard.gui;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.listener.GuiListener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardGuiMockBukkitTest {

    private static ServerMock server;
    private static DonutLeaderboardPlugin plugin;

    @BeforeAll
    static void bootServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DonutLeaderboardPlugin.class);
        server.getPluginManager().registerEvents(new GuiListener(plugin.guiManager()), plugin);
    }

    @AfterAll
    static void shutdownServer() {
        MockBukkit.unmock();
    }

    @Test
    void mockBukkitSanity() {
        assertTrue(server != null && plugin.isEnabled());
    }

    @Test
    void opensCategoryMenuWithSingleInventory() {
        PlayerMock player = server.addPlayer("NAIZO");
        plugin.guiManager().openCategoryMenu(player);
        server.getScheduler().performTicks(5);
        assertNotNull(player.getOpenInventory().getTopInventory());
        assertTrue(player.getOpenInventory().getTopInventory().getHolder() instanceof CategoryMenuGui);
    }

    @Test
    void clickIsCancelledAndDoesNotStealItems() {
        PlayerMock player = server.addPlayer("Tester");
        plugin.guiManager().openCategoryMenu(player);
        server.getScheduler().performTicks(5);
        ItemStack before = player.getInventory().getItem(0);
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, 10);
        server.getScheduler().performTicks(2);
        assertTrue(player.getInventory().getItem(0) == before || (before == null && player.getInventory().getItem(0) == null));
    }
}
