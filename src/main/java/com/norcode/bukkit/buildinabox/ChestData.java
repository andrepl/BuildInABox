package com.norcode.bukkit.buildinabox;

import com.norcode.bukkit.schematica.ClipboardBlock;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestData {
    private int id;
    private String planName;
    private String lockedBy;
    private long lastActivity;
    private String worldName;
    private Integer x;
    private Integer y;
    private Integer z;
    private HashMap<BlockVector, ClipboardBlock> replacedBlocks;
    private HashMap<BlockVector, NBTTagCompound> tileEntities;

    public ChestData(int id, String planName, String lockedBy, long lastActivity, String worldName, Integer x, Integer y, Integer z, HashMap<BlockVector, NBTTagCompound> tileEntities, HashMap<BlockVector, ClipboardBlock> replacedBlocks) {
        this.id = id;
        this.planName = planName;
        this.lockedBy = lockedBy;
        this.lastActivity = lastActivity;
        this.worldName = worldName;
        this.x = x;
        this.y = y;
        this.z = z;
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
    public void setLocation(Location location) {
        if (location == null) {
            this.worldName = null;
            this.x = null;
            this.y = null;
            this.z = null;
            return;
        }
        this.worldName = location.getWorld().getName();
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
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

    public String getSerializedLocation() {
        if (worldName == null) {
            return null;
        }
        return worldName + ";" + x + ";" + y + ";" + z;
    }

    public String getWorldName() {
        return worldName;
    }

    public void setWorldName(String worldName) {
        this.worldName = worldName;
    }

    public Integer getX() {
        return x;
    }

    public void setX(Integer x) {
        this.x = x;
    }

    public Integer getY() {
        return y;
    }

    public void setY(Integer y) {
        this.y = y;
    }

    public Integer getZ() {
        return z;
    }

    public void setZ(Integer z) {
        this.z = z;
    }
}
