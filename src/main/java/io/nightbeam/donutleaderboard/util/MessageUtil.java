package io.nightbeam.donutleaderboard.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class MessageUtil {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final JavaPlugin plugin;
    private FileConfiguration messages;
    private String prefix;

    public MessageUtil(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveResource("messages.yml", false);
        messages = YamlConfiguration.loadConfiguration(plugin.getDataFolder().toPath().resolve("messages.yml").toFile());
        prefix = messages.getString("prefix", plugin.getConfig().getString("messages.prefix", "<gold><bold>Leaderboard</bold> <dark_gray>» "));
    }

    public String raw(String path, String def) {
        String value = messages.getString(path);
        return value == null || value.isBlank() ? def : value;
    }

    public String format(String path, String def, Map<String, String> placeholders) {
        String template = raw(path, def);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace("%" + entry.getKey() + "%", entry.getValue());
            }
        }
        return template;
    }

    public String format(String path, String def, String... pairs) {
        return format(path, def, toMap(pairs));
    }

    public Component component(String message) {
        if (message.contains("<") && message.contains(">")) {
            try {
                return MINI.deserialize(message);
            } catch (Exception ignored) {
                // legacy fallback
            }
        }
        return LEGACY.deserialize(message);
    }

    public void send(CommandSender sender, String path, String def, String... pairs) {
        sender.sendMessage(component(prefix + format(path, def, pairs)));
    }

    public List<Component> lore(String path, Map<String, String> placeholders) {
        List<String> lines = messages.getStringList(path);
        List<Component> out = new ArrayList<>(lines.size());
        for (String line : lines) {
            String formatted = line;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
            }
            out.add(component(formatted));
        }
        return out;
    }

    private static Map<String, String> toMap(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(pairs[i], pairs[i + 1]);
        }
        return map;
    }
}
