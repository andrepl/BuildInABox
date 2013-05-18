package com.norcode.bukkit.buildinabox;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;


public class YamlDataStore extends DataStore {
    private BuildInABox plugin;
    private ConfigAccessor planCfg;
    private ConfigAccessor chestCfg;
    private int nextChestId = 0;
    private HashMap<Integer, ChestData> chests = new HashMap<Integer, ChestData>();
    private HashMap<String, BuildingPlan> plans = new HashMap<String, BuildingPlan>();
    private boolean dirty = false;

    public YamlDataStore(BuildInABox plugin) {
        this.plugin = plugin;
        this.planCfg = new ConfigAccessor(plugin, "plans.yml");
        this.chestCfg = new ConfigAccessor(plugin, "chests.yml");
    }

    @Override
    public void load() {
        this.planCfg.reloadConfig();
        this.chestCfg.reloadConfig();
        this.chests.clear();
        this.plans.clear();
        ConfigurationSection sec;

        // Load Plans
        for (String key: this.planCfg.getConfig().getKeys(false)) {
            sec = this.planCfg.getConfig().getConfigurationSection(key);
            BuildingPlan plan = new BuildingPlan(plugin, 
                    sec.getString("name"), 
                    sec.getString("filename"), 
                    sec.getStringList("description"));
            this.plans.put(plan.getName().toLowerCase(), plan);
        }

        // Load Chests
        int maxId = 0;
        int id;
        Location loc;
        for (String key: this.chestCfg.getConfig().getKeys(false)) {
            sec = this.chestCfg.getConfig().getConfigurationSection(key);
            id = sec.getInt("id", 0);
            if (id > 0) {
                if (id > maxId) maxId = id;
                loc = deserializeLocation(sec.getString("location"));
                HashMap<BlockVector, CompoundTag> tileEntities = deserializeTileEntities(sec.getString("tile-entities"));
                HashMap<BlockVector, BaseBlock> replacedBlocks = deserializeReplacedBlocks(sec.getString("replaced-blocks"));
                chests.put(id, new ChestData(id, sec.getString("plan"), sec.getString("locked-by"), sec.getLong("last-activity"), loc, tileEntities, replacedBlocks));
            }
        }
        nextChestId = maxId;
    }

    @Override
    public void save() {
        if (this.dirty) {
            this.dirty = false;
            this.chestCfg.saveConfig();
            this.planCfg.saveConfig();
        }
    }

    @Override
    public ChestData getChest(int id) {
        plugin.debug("Looking up chest " + id + " from " + chests);
        return this.chests.get(id);
    }

    @Override
    public ChestData createChest(String plan) {
        nextChestId++;
        ConfigurationSection sec = this.chestCfg.getConfig().createSection(Integer.toString(nextChestId));
        long now = System.currentTimeMillis();
        sec.set("plan", plan);
        sec.set("last-activity", now);
        sec.set("id", nextChestId);
        setDirty();
        ChestData data = new ChestData(nextChestId, plan, null, now, null, null, null);
        chests.put(nextChestId, data);
        return data;
    }

    @Override
    public void saveChest(ChestData data) {
        ConfigurationSection sec = this.chestCfg.getConfig().getConfigurationSection(Integer.toString(data.getId()));
        sec.set("plan",  data.getPlanName());
        sec.set("last-activity", data.getLastActivity());
        sec.set("location", serializeLocation(data.getLocation()));
        sec.set("locked-by", data.getLockedBy());
        sec.set("replaced-blocks", serializeReplacedBlocks(data.getReplacedBlocks()));
        sec.set("tile-entities", serializeTileEntities(data.getTileEntities()));
        this.setDirty();
    }

    @Override
    public void deleteChest(int id) {
        plugin.debug("Deleting Chest ID#" + id);
        this.chests.remove(id);
        this.chestCfg.getConfig().set(Integer.toString(id), null);
        this.setDirty();
    }

    @Override
    public void saveBuildingPlan(BuildingPlan plan) {
        ConfigurationSection sec = planCfg.getConfig().getConfigurationSection(plan.getName());
        if (sec == null) {
            sec = planCfg.getConfig().createSection(plan.getName());
        }
        sec.set("name", plan.getName());
        sec.set("filename", plan.getFilename());
        sec.set("description", plan.getDescription());
        this.plans.put(plan.getName().toLowerCase(), plan);
        setDirty();
    }

    @Override
    public void deleteBuildingPlan(BuildingPlan plan) {
        this.plans.remove(plan.getName().toLowerCase());
        planCfg.getConfig().set(plan.getName(), null);
    }

    @Override
    public Collection<ChestData> getAllChests() {
        return chests.values();
    }

    @Override
    public Collection<BuildingPlan> getAllBuildingPlans() {
        return plans.values();
    }

    @Override
    public BuildingPlan getBuildingPlan(String name) {
        return plans.get(name.toLowerCase());
    }

    private void setDirty() {
        this.dirty = true;
    }
    
}
