package dev.gemsmp;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Saves the gem registry, first-join flags and trust lists to data.yml. */
public final class DataStore {
    private final JavaPlugin plugin;
    private final File file;
    private final YamlConfiguration yaml;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.yaml = YamlConfiguration.loadConfiguration(file);
    }

    private void save() {
        try {
            yaml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save data.yml: " + ex.getMessage());
        }
    }

    // ---- gem registry (only registered ids work, which is what stops forged/duped items) ----

    public void registerGem(UUID id, Gem gem) {
        yaml.set("gems." + id, gem.name());
        save();
    }

    public boolean isRegistered(UUID id) {
        return yaml.contains("gems." + id);
    }

    // ---- first join ----

    public boolean hasReceivedFirstGem(UUID player) {
        return yaml.getBoolean("first-join." + player, false);
    }

    public void markFirstGem(UUID player) {
        yaml.set("first-join." + player, true);
        save();
    }

    // ---- trust ----

    public boolean isTrusted(UUID owner, UUID other) {
        return yaml.getStringList("trusted." + owner).contains(other.toString());
    }

    public void trust(UUID owner, UUID other) {
        String key = "trusted." + owner;
        List<String> list = new ArrayList<>(yaml.getStringList(key));
        if (!list.contains(other.toString())) {
            list.add(other.toString());
        }
        yaml.set(key, list);
        save();
    }

    public void untrust(UUID owner, UUID other) {
        String key = "trusted." + owner;
        List<String> list = new ArrayList<>(yaml.getStringList(key));
        list.remove(other.toString());
        yaml.set(key, list);
        save();
    }

    public List<UUID> trusted(UUID owner) {
        List<UUID> out = new ArrayList<>();
        for (String raw : yaml.getStringList("trusted." + owner)) {
            try {
                out.add(UUID.fromString(raw));
            } catch (IllegalArgumentException ignored) {
                // skip bad entries
            }
        }
        return out;
    }
}
