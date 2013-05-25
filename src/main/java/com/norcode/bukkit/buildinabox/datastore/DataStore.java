package com.norcode.bukkit.buildinabox.datastore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.BuildingPlan;
import com.norcode.bukkit.buildinabox.ChestData;
import com.norcode.bukkit.buildinabox.util.Base64;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.NBTInputStream;
import com.sk89q.jnbt.NBTOutputStream;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.blocks.BaseBlock;

public abstract class DataStore {

    protected static final Gson gson = new Gson();

    public abstract void load();
    public abstract void save();

    public static String serializeLocation(Location loc) {
        if (loc == null) {
            return null;
        }
        return loc.getWorld().getName() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
    }

    public static Location deserializeLocation(String s) {
        if (s == null) {
            return null;
        }
        String[] parts = s.split(";");
        if (parts.length == 4) {
            World world = BuildInABox.getInstance().getServer().getWorld(parts[0]);
            return new Location(world, Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        }
        return null;
    }

    public String serializeVector(BlockVector v) {
        if (v == null) {
            return null;
        }
        return v.getBlockX() + ";" + v.getBlockY() + ";" + v.getBlockZ();
    }

    public BlockVector deserializeVector(String s) {
        if (s == null) {
            return null;
        }
        String[] parts = s.split(";");
        try {
            return new BlockVector(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (IllegalArgumentException ex) {
        } catch (ArrayIndexOutOfBoundsException ex) {
        }
        return null;
    }

    public String serializeCompoundTag(CompoundTag tag) {
        if (tag == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            NBTOutputStream stream = new NBTOutputStream(baos);
            stream.writeTag(tag);
            stream.close();
            return new String(Base64.encodeToByte(baos.toByteArray(), false));
        } catch (IOException e) {
        }
        return null;
    }

    public CompoundTag deserializeCompoundTag(String s) {
        if (s == null) {
            return null;
        }
        try {
            NBTInputStream stream = new NBTInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.decode(s.getBytes()))));
            Tag tag = stream.readTag();
            stream.close();
            if (tag instanceof CompoundTag) {
                return (CompoundTag) tag;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String serializeTileEntities(HashMap<BlockVector, CompoundTag> ents) {
        if (ents == null) {
            return null;
        }
        HashMap<String, String> obj = new HashMap<String, String>();
        for (Entry<BlockVector, CompoundTag> entry: ents.entrySet()) {
            obj.put(serializeVector(entry.getKey()), serializeCompoundTag(entry.getValue()));
        }
        return gson.toJson(obj);
    }

    public HashMap<BlockVector, CompoundTag> deserializeTileEntities(String s) {
        if (s == null) {
            return null;
        }
        HashMap<BlockVector, CompoundTag> contents = new HashMap<BlockVector, CompoundTag>();
        final java.lang.reflect.Type type = new TypeToken<Map<String, String>>(){}.getType();
        HashMap<String, String> data = gson.fromJson(s, type);
        for (String k: data.keySet()) {
            contents.put(deserializeVector(k), deserializeCompoundTag(data.get(k)));
        }
        return contents;
    }

    public String serializeReplacedBlocks(HashMap<BlockVector, BaseBlock> replacedBlocks) {
        String repr = "";
        for (Entry<BlockVector, BaseBlock> e: replacedBlocks.entrySet()) {
            repr += serializeVector(e.getKey()) + ":" + serializeBaseBlock(e.getValue()) + "|";
        }
        if (repr.length() > 1) {
            repr = repr.substring(0,repr.length()-1);
        }
        return repr;
    }

    public String serializeBaseBlock(BaseBlock bb) {
        String s = Integer.toString(bb.getType());
        if (bb.getData() != 0) {
            s += "|" + bb.getData();
        }
        return s;
    }

    public BaseBlock deserializeBaseBlock(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            return new BaseBlock(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return new BaseBlock(Integer.parseInt(s));
    }

    public HashMap<BlockVector, BaseBlock> deserializeReplacedBlocks(String s) {
        HashMap<BlockVector, BaseBlock> results = new HashMap<BlockVector, BaseBlock>();
        if (s == null || s.equals("")) {
            return null;
        }
        if (s.startsWith("{")) {
            // old format
            final java.lang.reflect.Type type = new TypeToken<Map<String,Map<String,Integer>>>(){}.getType();
            HashMap<String, Map<String, Integer>> data = gson.fromJson(s, type);
            if (data == null) {
                return null;
            }
            BlockVector v;
            BaseBlock bb;
            for (Entry<String, Map<String, Integer>> e: data.entrySet()) {
                v = deserializeVector(e.getKey());
                bb = new BaseBlock(e.getValue().get("t"), e.getValue().get("d"));
                results.put(v, bb);
            }
        } else {
            // new format
            String[] bss;
            for (String bs: s.split("\\|")) {
                bss = bs.split(":");
                if (bss.length == 2) {
                    results.put(deserializeVector(bss[0]), deserializeBaseBlock(bss[1]));
                }
            }
        }
        return results;
    }

    public ChestData fromItemStack(ItemStack stack) {
        if (stack != null && stack.getTypeId() == BuildInABox.BLOCK_ID) {
            if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta.getLore().get(0).startsWith(BuildInABox.LORE_PREFIX) || meta.getLore().get(0).equals(ChatColor.GOLD + "Build-in-a-Box")) {
                    if (meta.getLore().size() > 1) {
                        int chestId;
                        ChestData data;
                        boolean update = false;
                        BuildingPlan planCheck = getBuildingPlan(meta.getLore().get(1).substring(2).toLowerCase());
                        if (planCheck != null) {
                            data = createChest(planCheck.getName());
                            List<String> newLore = new ArrayList<String>();
                            newLore.add(meta.getLore().get(0));
                            newLore.add(ChatColor.BLACK + Integer.toHexString(data.getId()));
                            meta.setLore(newLore);
                            update = true;
                        } else {
                            try {
                                chestId = Integer.parseInt(meta.getLore().get(1).substring(2), 16);
                                data = getChest(chestId);
                                if (data == null) {
                                    return null;
                                }
                            } catch (IllegalArgumentException ex) {
                                return null;
                            }
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
        if (block.getTypeId() == BuildInABox.BLOCK_ID) {
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

    public abstract BuildingPlan getBuildingPlan(String name);
    public abstract void saveBuildingPlan(BuildingPlan plan);
    public abstract void deleteBuildingPlan(BuildingPlan plan);
    public abstract Collection<ChestData> getAllChests();
    public abstract Collection<BuildingPlan> getAllBuildingPlans();
}
