package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class ChestData {
    private int id;
    private String planName;
    private String lockedBy;
    private long lastActivity;
    private Location location;
    private HashMap<BlockVector, BaseBlock> replacedBlocks;
    private HashMap<BlockVector, CompoundTag> tileEntities;

    public ChestData(int id, String planName, String lockedBy, long lastActivity, Location location, HashMap<BlockVector, CompoundTag> tileEntities, HashMap<BlockVector, BaseBlock> replacedBlocks) {
        this.id = id;
        this.planName = planName;
        this.lockedBy = lockedBy;
        this.lastActivity = lastActivity;
        this.location = location;
        if (tileEntities == null) {
            tileEntities = new HashMap<BlockVector, CompoundTag>();
        }
        if (replacedBlocks == null) {
            replacedBlocks = new HashMap<BlockVector, BaseBlock>();
        }
        this.replacedBlocks = replacedBlocks;
        this.tileEntities = tileEntities;
    }

    public void setTileEntities(BlockVector vec, CompoundTag tag) {
        tileEntities.put(vec, tag);
    }

    public void clearTileEntities() {
        tileEntities.clear();
    }

    public void setReplacedBlocks(Map<BlockVector, BaseBlock> baseBlocks) {
        if (this.replacedBlocks == null) {
            this.replacedBlocks = new HashMap<BlockVector, BaseBlock>();
        } else {
            this.replacedBlocks.clear();
        }
        if (baseBlocks != null) {
            this.replacedBlocks.putAll(baseBlocks);
        }
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getPlanName() {
        return planName;
    }
    public void setPlanName(String planName) {
        this.planName = planName;
    }
    public String getLockedBy() {
        return lockedBy;
    }
    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
    }
    public long getLastActivity() {
        return lastActivity;
    }
    public void setLastActivity(long lastActivity) {
        this.lastActivity = lastActivity;
    }
    public Location getLocation() {
        return location;
    }
    public void setLocation(Location location) {
        this.location = location;
    }
    public HashMap<BlockVector, CompoundTag> getTileEntities() {
        return tileEntities;
    }
    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(Material.getMaterial(BuildInABox.BLOCK_ID));
        ItemMeta meta = BuildInABox.getInstance().getServer().getItemFactory().getItemMeta(Material.getMaterial(BuildInABox.BLOCK_ID));
        List<String> lore = new ArrayList<String>();
        lore.add(BuildInABox.LORE_PREFIX + BuildInABox.LORE_HEADER);
        lore.add(ChatColor.BLACK + Integer.toHexString(getId()));
        BuildingPlan plan = BuildInABox.getInstance().getDataStore().getBuildingPlan(planName);
        if (plan != null) {
            if (plan.getDescription() != null) {
                lore.addAll(plan.getDescription());
            }
            meta.setLore(lore);
            meta.setDisplayName(planName);
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public HashMap<BlockVector, BaseBlock> getReplacedBlocks() {
        return replacedBlocks;
    }
}
