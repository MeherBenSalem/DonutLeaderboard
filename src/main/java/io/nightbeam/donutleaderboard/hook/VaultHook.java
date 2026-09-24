package io.nightbeam.donutleaderboard.hook;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultHook {

    private Economy economy;

    public void hook() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> provider = Bukkit.getServicesManager().getRegistration(Economy.class);
        economy = provider == null ? null : provider.getProvider();
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public double balance(OfflinePlayer player) {
        if (economy == null || player == null) {
            return 0D;
        }
        return economy.getBalance(player);
    }
}
