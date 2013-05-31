package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.norcode.bukkit.schematica.ClipboardBlock;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.util.BlockVector;

public class ChestData {
    private int id;
    private String planName;
    private String lockedBy;
    private long lastActivity;
    private Location location;
    private HashMap<BlockVector, ClipboardBlock> replacedBlocks;
    private HashMap<BlockVector, NBTTagCompound> tileEntities;

    public ChestData(int id, String planName, String lockedBy, long lastActivity, Location location, HashMap<BlockVector, NBTTagCompound> tileEntities, HashMap<BlockVector, ClipboardBlock> replacedBlocks) {
        this.id = id;
        this.planName = planName;
        this.lockedBy = lockedBy;
        this.lastActivity = lastActivity;
        this.location = location;
        if (tileEntities == null) {
            tileEntities = new HashMap<BlockVector, NBTTagCompound>();
        }
        if (replacedBlocks == null) {
            replacedBlocks = new HashMap<BlockVector, ClipboardBlock>();
        }
        this.replacedBlocks = replacedBlocks;
        this.tileEntities = tileEntities;
    }

    public void setTileEntity(BlockVector vec, NBTTagCompound tag) {
        tileEntities.put(vec, tag);
    }

    public void clearTileEntities() {
        tileEntities.clear();
    }

    public void setReplacedBlocks(Map<BlockVector, ClipboardBlock> baseBlocks) {
        if (this.replacedBlocks == null) {
            this.replacedBlocks = new HashMap<BlockVector, ClipboardBlock>();
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
    public HashMap<BlockVector, NBTTagCompound> getTileEntities() {
        return tileEntities;
    }
    public ItemStack toItemStack() {
        Material mat = Material.getMaterial(BuildInABox.getInstance().cfg.getChestBlockId());
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = BuildInABox.getInstance().getServer().getItemFactory().getItemMeta(mat);
        List<String> lore = new ArrayList<String>();
        lore.add(BuildInABox.LORE_PREFIX + BuildInABox.LORE_HEADER);
        BuildingPlan plan = BuildInABox.getInstance().getDataStore().getBuildingPlan(planName);
        lore.add(ChatColor.BLACK + Integer.toHexString(getId()));
        if (plan != null) {
            if (plan.getDescription() != null) {
                lore.addAll(plan.getDescription());
            }
            meta.setLore(lore);
            meta.setDisplayName(plan.getDisplayName());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    public HashMap<BlockVector, ClipboardBlock> getReplacedBlocks() {
        return replacedBlocks;
    }
}
