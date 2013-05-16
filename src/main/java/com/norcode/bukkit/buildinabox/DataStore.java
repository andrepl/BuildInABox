package com.norcode.bukkit.buildinabox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

public class DataStore {

    private final String fileName;
    private final BuildInABox plugin;

    private File configFile;
    private FileConfiguration fileConfiguration;

    public DataStore(BuildInABox plugin, String fileName) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");
        if (!plugin.isInitialized())
            throw new IllegalArgumentException("plugin must be initialized");
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void debug(String s) {
        plugin.getLogger().info(s);
    }

    public void reload() {
        reloadConfig();
        ConfigurationSection cfg = null;
        World w;
        Block b;
        BuildingPlan p;
        Set<String> keysToRemove = new HashSet<String>();
        for (String key: getConfig().getKeys(false)) {
            cfg = getConfig().getConfigurationSection(key);
            String[] parts = key.split(";");
            w = plugin.getServer().getWorld(parts[0]);
            if (w == null) {
                debug("Removing record in invalid world: " + parts[0]);
                keysToRemove.add(key);
                continue;
            }
            b = w.getBlockAt(new Location(w, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3])));
            if (b.getType().equals(Material.ENDER_CHEST)) {
                p = plugin.getPlan(cfg.getString("plan"));
                BuildChest bc = new BuildChest(plugin, b.getLocation(), p, cfg.getString("locked-by"));
                b.setMetadata("buildInABox", new FixedMetadataValue(plugin, bc));
                if (plugin.getConfig().getBoolean("protect-buildings", false)) {
                    bc.protectBlocks();
                }
            }
        }

        for (String k: keysToRemove) {
            cfg.set(k, null);
        }
    }

    public void save() {
        saveConfig();
    }

    public void removeChest(BuildChest chest) {
        String key = chest.getLocation().getWorld().getName() + ";";
        key += Integer.toString(chest.getLocation().getBlockX()) + ";";
        key += Integer.toString(chest.getLocation().getBlockY()) + ";";
        key += Integer.toString(chest.getLocation().getBlockZ());
        chest.getLocation().getBlock().removeMetadata("buildInABox", plugin);
        getConfig().set(key, null);
    }

    public BuildChest addChest(Block chest, BuildingPlan plan, Player player) {
        String key = chest.getLocation().getWorld().getName() + ";";
        key += Integer.toString(chest.getLocation().getBlockX()) + ";";
        key += Integer.toString(chest.getLocation().getBlockY()) + ";";
        key += Integer.toString(chest.getLocation().getBlockZ());
        BuildChest bc = new BuildChest(plugin, chest.getLocation(), plan, player == null ? null : player.getName());
        chest.setMetadata("buildInABox", new FixedMetadataValue(plugin, bc));
        ConfigurationSection cfg = getConfig().createSection(key);
        cfg.set("plan", plan.getName());
        cfg.set("locked-by", bc.getLockedBy());
        return bc;
    }

    private void reloadConfig() {
        if (configFile == null) {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder == null)
                throw new IllegalStateException();
            configFile = new File(dataFolder, fileName);
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfiguration.setDefaults(defConfig);
        }
    }

    private FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    public void saveConfig() {
        if (fileConfiguration == null || configFile == null) {
            return;
        } else {
            try {
                getConfig().save(configFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }
    }
}