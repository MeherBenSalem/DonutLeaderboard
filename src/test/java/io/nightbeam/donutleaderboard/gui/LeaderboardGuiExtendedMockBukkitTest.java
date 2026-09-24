package io.nightbeam.donutleaderboard.gui;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import io.nightbeam.donutleaderboard.DonutLeaderboardPlugin;
import io.nightbeam.donutleaderboard.listener.GuiListener;
import io.nightbeam.donutleaderboard.model.CategoryKey;
import io.nightbeam.donutleaderboard.model.LeaderboardSnapshot;
import io.nightbeam.donutleaderboard.model.PeriodType;
import io.nightbeam.donutleaderboard.model.RankedEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderboardGuiExtendedMockBukkitTest {

    private static ServerMock server;
    private static DonutLeaderboardPlugin plugin;

    @BeforeAll
    static void boot() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(DonutLeaderboardPlugin.class);
        server.getPluginManager().registerEvents(new GuiListener(plugin.guiManager()), plugin);
    }

    @AfterAll
    static void shutdown() {
        MockBukkit.unmock();
    }

    @Test
    void periodToggleCyclesInGui() {
        PlayerMock player = server.addPlayer("PeriodUser");
        var category = plugin.categories().find("kills");
        plugin.guiManager().openLeaderboard(player, category, PeriodType.ALL_TIME, 0);
        server.getScheduler().performTicks(5);
        assertInstanceOf(LeaderboardGui.class, player.getOpenInventory().getTopInventory().getHolder());
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, LeaderboardGui.PERIOD_SLOT);
        server.getScheduler().performTicks(5);
        var holder = player.getOpenInventory().getTopInventory().getHolder();
        assertInstanceOf(LeaderboardGui.class, holder);
        assertTrue(((LeaderboardGui) holder).period() == PeriodType.DAILY);
    }

    @Test
    void paginationStaysOnBoundary() {
        PlayerMock player = server.addPlayer("Pager");
        var category = plugin.categories().find("kills");
        List<RankedEntry> entries = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            UUID id = UUID.randomUUID();
            entries.add(new RankedEntry(i + 1, id, "P" + i, 100D - i));
        }
        CategoryKey key = new CategoryKey("kills", PeriodType.ALL_TIME);
        plugin.leaderboardService().putSnapshotForTest(key, new LeaderboardSnapshot(key, entries, System.currentTimeMillis(), entries.size()));
        plugin.guiManager().openLeaderboard(player, category, PeriodType.ALL_TIME, 0);
        server.getScheduler().performTicks(5);
        LeaderboardGui gui = (LeaderboardGui) player.getOpenInventory().getTopInventory().getHolder();
        int startPage = gui.pageIndex();
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, LeaderboardGui.PREVIOUS_SLOT);
        server.getScheduler().performTicks(5);
        assertTrue(((LeaderboardGui) player.getOpenInventory().getTopInventory().getHolder()).pageIndex() == startPage);
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, LeaderboardGui.NEXT_SLOT);
        server.getScheduler().performTicks(5);
        assertTrue(((LeaderboardGui) player.getOpenInventory().getTopInventory().getHolder()).pageIndex() > startPage);
        int lastPage = ((LeaderboardGui) player.getOpenInventory().getTopInventory().getHolder()).pageIndex();
        player.simulateInventoryClick(player.getOpenInventory(), ClickType.LEFT, LeaderboardGui.NEXT_SLOT);
        server.getScheduler().performTicks(5);
        assertTrue(((LeaderboardGui) player.getOpenInventory().getTopInventory().getHolder()).pageIndex() == lastPage);
    }

    @Test
    void viewerSlotShowsUnrankedHead() {
        PlayerMock player = server.addPlayer("UnrankedViewer");
        var category = plugin.categories().find("kills");
        CategoryKey key = new CategoryKey("kills", PeriodType.ALL_TIME);
        plugin.leaderboardService().putSnapshotForTest(key, new LeaderboardSnapshot(key, List.of(), System.currentTimeMillis(), 0));
        plugin.guiManager().openLeaderboard(player, category, PeriodType.ALL_TIME, 0);
        server.getScheduler().performTicks(5);
        int slot = plugin.getConfig().getInt("gui.show-viewer-rank-slot", 48);
        ItemStack viewerItem = player.getOpenInventory().getTopInventory().getItem(slot);
        assertTrue(viewerItem != null && viewerItem.getType() == Material.PLAYER_HEAD);
    }

    @Test
    void numberKeyAndDoubleClickAreCancelled() {
        PlayerMock player = server.addPlayer("AntiDupe");
        plugin.guiManager().openCategoryMenu(player);
        server.getScheduler().performTicks(5);
        InventoryClickEvent numberKey = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                10,
                ClickType.NUMBER_KEY,
                InventoryAction.HOTBAR_MOVE_AND_READD,
                0);
        server.getPluginManager().callEvent(numberKey);
        assertTrue(numberKey.isCancelled());

        InventoryClickEvent dbl = new InventoryClickEvent(
                player.getOpenInventory(),
                org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER,
                10,
                ClickType.DOUBLE_CLICK,
                InventoryAction.PICKUP_ALL);
        server.getPluginManager().callEvent(dbl);
        assertTrue(dbl.isCancelled());
    }

    @Test
    void reloadClosesOpenGui() {
        PlayerMock player = server.addPlayer("ReloadCloser");
        plugin.guiManager().openCategoryMenu(player);
        server.getScheduler().performTicks(5);
        assertTrue(plugin.guiManager().getOpenGui(player.getUniqueId()) != null);
        plugin.reloadPlugin();
        server.getScheduler().performTicks(3);
        assertNull(plugin.guiManager().getOpenGui(player.getUniqueId()));
    }
}
