package io.nightbeam.donutleaderboard.model;

import org.bukkit.Material;
import org.bukkit.Statistic;

public record CategoryDefinition(
        String id,
        String displayName,
        CategorySourceType sourceType,
        Statistic statistic,
        String placeholder,
        Material menuIcon,
        boolean enabled,
        int menuSlot) {

    public boolean isPlaceholderCategory() {
        return sourceType == CategorySourceType.PLACEHOLDER;
    }

    public boolean isVaultCategory() {
        return sourceType == CategorySourceType.VAULT_BALANCE;
    }
}
