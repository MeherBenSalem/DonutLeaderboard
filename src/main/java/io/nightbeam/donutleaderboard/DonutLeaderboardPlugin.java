package io.nightbeam.donutleaderboard;

import io.nightbeam.donutleaderboard.command.LeaderboardCommand;
import io.nightbeam.donutleaderboard.config.CategoryRegistry;
import io.nightbeam.donutleaderboard.config.ConfigMigrator;
import io.nightbeam.donutleaderboard.gui.GuiManager;
import io.nightbeam.donutleaderboard.hook.PlaceholderService;
import io.nightbeam.donutleaderboard.hook.VaultHook;
import io.nightbeam.donutleaderboard.listener.GuiListener;
import io.nightbeam.donutleaderboard.listener.StatListener;
import io.nightbeam.donutleaderboard.papi.DonutLeaderboardExpansion;
import io.nightbeam.donutleaderboard.period.PeriodBounds;
import io.nightbeam.donutleaderboard.service.LeaderboardService;
import io.nightbeam.donutleaderboard.service.PeriodResetScheduler;
import io.nightbeam.donutleaderboard.storage.DatabaseManager;
import io.nightbeam.donutleaderboard.storage.LeaderboardStorage;
import io.nightbeam.donutleaderboard.storage.MigrationRunner;
import io.nightbeam.donutleaderboard.util.MessageUtil;
import io.nightbeam.donutleaderboard.util.SchedulerAdapter;
import java.time.ZoneId;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class DonutLeaderboardPlugin extends JavaPlugin {

    private SchedulerAdapter scheduler;
    private MessageUtil messages;
    private DatabaseManager databaseManager;
    private LeaderboardStorage storage;
    private CategoryRegistry categoryRegistry;
    private VaultHook vaultHook;
    private PlaceholderService placeholderService;
    private LeaderboardService leaderboardService;
    private PeriodResetScheduler periodResetScheduler;
    private GuiManager guiManager;
    private SchedulerAdapter.CancellableTask refreshTask;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        ConfigMigrator.migrate(getConfig());
        saveConfig();

        scheduler = new SchedulerAdapter(this);
        messages = new MessageUtil(this);

        try {
            databaseManager = DatabaseManager.fromConfig(this);
            MigrationRunner.migrate(databaseManager.dataSource());
            storage = new LeaderboardStorage(databaseManager.dataSource());
        } catch (Exception ex) {
            getLogger().severe("Database setup failed: " + ex.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        categoryRegistry = new CategoryRegistry(this);
        vaultHook = new VaultHook();
        vaultHook.hook();
        placeholderService = new PlaceholderService();
        placeholderService.hook();

        PeriodBounds periodBounds = new PeriodBounds(ZoneId.of(getConfig().getString("periods.timezone", "UTC")));
        leaderboardService = new LeaderboardService(this, scheduler, storage, categoryRegistry, vaultHook, placeholderService, periodBounds);
        leaderboardService.reload(getConfig());

        guiManager = new GuiManager(this);
        periodResetScheduler = new PeriodResetScheduler(this, scheduler, leaderboardService, categoryRegistry, periodBounds);
        periodResetScheduler.start();

        long refreshTicks = Math.max(20L, getConfig().getInt("cache.refresh-seconds", 120) * 20L);
        refreshTask = scheduler.runGlobalRepeating(() -> leaderboardService.refreshAll(false), refreshTicks, refreshTicks);

        LeaderboardCommand command = new LeaderboardCommand(this);
        var leaderboardCommand = getCommand("leaderboard");
        if (leaderboardCommand != null) {
            leaderboardCommand.setExecutor(command);
            leaderboardCommand.setTabCompleter(command);
        }

        Bukkit.getPluginManager().registerEvents(new GuiListener(guiManager), this);
        Bukkit.getPluginManager().registerEvents(
                new StatListener(storage, leaderboardService, periodBounds, scheduler), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DonutLeaderboardExpansion(this).register();
        }

        if (getConfig().getBoolean("bstats.enabled", false)) {
            new Metrics(this, 24242);
        }

        leaderboardService.refreshAll(true);
        getLogger().info("DonutLeaderboard enabled (Folia=" + scheduler.isFolia() + ").");
    }

    @Override
    public void onDisable() {
        if (refreshTask != null) {
            refreshTask.cancel();
        }
        if (periodResetScheduler != null) {
            periodResetScheduler.stop();
        }
        if (scheduler != null) {
            scheduler.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
    }

    public void reloadPlugin() {
        guiManager.closeAllForReload();
        reloadConfig();
        ConfigMigrator.migrate(getConfig());
        saveConfig();
        messages.reload();
        categoryRegistry.reload(getConfig());
        vaultHook.hook();
        placeholderService.hook();
        leaderboardService.reload(getConfig());
        leaderboardService.refreshAll(true);
    }

    public SchedulerAdapter scheduler() {
        return scheduler;
    }

    public MessageUtil messages() {
        return messages;
    }

    public CategoryRegistry categories() {
        return categoryRegistry;
    }

    public LeaderboardService leaderboardService() {
        return leaderboardService;
    }

    public GuiManager guiManager() {
        return guiManager;
    }
}
