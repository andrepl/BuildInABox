package com.norcode.bukkit.buildinabox.datastore;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.BuildingPlan;
import com.norcode.bukkit.buildinabox.ChestData;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class DataStore {

    protected static final Gson gson = new Gson();

    public abstract void load();
    public abstract void save();


    public ChestData fromItemStack(ItemStack stack) {
        if (stack != null && stack.getTypeId() == BuildInABox.getInstance().cfg.getChestBlockId()) {
            if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta.getLore().get(0).startsWith(BuildInABox.LORE_PREFIX) || meta.getLore().get(0).equals(ChatColor.GOLD + "Build-in-a-Box")) {
                    if (meta.getLore().size() > 1) {
                        int chestId;
                        ChestData data = null;
                        boolean update = false;
                        // This mess is for compatibility with an earlier method of storing the plan.
                        if (meta.getLore().get(1).substring(0,2).equals(ChatColor.DARK_GRAY.toString())) {
                            BuildingPlan planCheck = getBuildingPlan(meta.getLore().get(1).substring(2).toLowerCase());
                            if (planCheck != null) {
                                data = createChest(planCheck.getName());
                                List<String> newLore = new ArrayList<String>();
                                newLore.add(meta.getLore().get(0));
                                newLore.add(ChatColor.BLACK + Integer.toHexString(data.getId()));
                                meta.setLore(newLore);
                                update = true;
                            }
                        } else if (meta.getLore().get(1).substring(0,2).equals(ChatColor.BLACK.toString())) {
                            try {
                                chestId = Integer.parseInt(meta.getLore().get(1).substring(2), 16);
                                data = getChest(chestId);
                                if (data == null) {
                                    return null;
                                }
                            } catch (IllegalArgumentException ex) {
                                BuildingPlan planCheck = getBuildingPlan(meta.getLore().get(1).substring(2).toLowerCase());
                                if (planCheck != null) {
                                    data = createChest(planCheck.getName());
                                    List<String> newLore = new ArrayList<String>();
                                    newLore.add(meta.getLore().get(0));
                                    newLore.add(ChatColor.BLACK + Integer.toHexString(data.getId()));
                                    meta.setLore(newLore);
                                    update = true;
                                }
                            }
                        }
                        if (data == null) {
                            return null;
                        }
                        BuildingPlan plan = getBuildingPlan(data.getPlanName());
                        if (plan != null) {
                            if (!plan.getDisplayName().equals(meta.getDisplayName())) {
                                meta.setDisplayName(plan.getDisplayName());
                                update = true;
                            }
                            if (!update) {
                                if (!meta.getLore().subList(2, meta.getLore().size()).equals(plan.getDescription())) {
                                    List<String> newLore = new ArrayList<String>();
                                    for (int i=0;i<2;i++) {
                                        newLore.add(meta.getLore().get(i));
                                    }
                                    newLore.addAll(plan.getDescription());
                                    update = true;
                                }
                            }
                            if (update) {
                                stack.setItemMeta(meta);
                                return data;
                            }
                            return data;
                        }
                    }
                }
            }
        }
        return null;
    }

    public ChestData fromBlock(Block block) {
        if (block.getTypeId() == BuildInABox.getInstance().cfg.getChestBlockId()) {
            if (block.hasMetadata("buildInABox")) {
                return (ChestData) block.getMetadata("buildInABox").get(0).value();
            }
        }
        return null;
    }

    public abstract ChestData getChest(int id);
    public abstract ChestData createChest(String plan); 
    public abstract void saveChest(ChestData data);
    public abstract void deleteChest(int id);


    public abstract void setWorldChests(World world, Collection<ChestData> chests);
    public abstract void clearWorldChests(World world);
    public abstract Collection<ChestData> getWorldChests(World world);
    public abstract BuildingPlan getBuildingPlan(String name);
    public abstract void saveBuildingPlan(BuildingPlan plan);
    public abstract void deleteBuildingPlan(BuildingPlan plan);
    public abstract Collection<ChestData> getAllChests();
    public abstract Collection<BuildingPlan> getAllBuildingPlans();
}
