package com.norcode.bukkit.buildinabox.datastore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.BuildingPlan;
import com.norcode.bukkit.buildinabox.ChestData;
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
            return new String(Base64.encodeBase64(baos.toByteArray()));
        } catch (IOException e) {
        }
        return null;
    }

    public CompoundTag deserializeCompoundTag(String s) {
        if (s == null) {
            return null;
        }
        BuildInABox.getInstance().debug("Attempting to deserialize: " + s);
        try {
            NBTInputStream stream = new NBTInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.decodeBase64(s.getBytes()))));
            Tag tag = stream.readTag();
            if (tag instanceof CompoundTag) {
                BuildInABox.getInstance().debug("Deserialized: " + tag);
                return (CompoundTag) tag;
            } else {
                BuildInABox.getInstance().debug("Didn't Deserialize (not compound?): " + tag);
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
        if (s.contains(",")) {
            String[] parts = s.split(",");
            return new BaseBlock(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
        return new BaseBlock(Integer.parseInt(s));
    }

    public HashMap<BlockVector, BaseBlock> deserializeReplacedBlocks(String s) {
        HashMap<BlockVector, BaseBlock> results = new HashMap<BlockVector, BaseBlock>();
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
            for (String bs: s.split("|")) {
                bss = bs.split(":");
                results.put(deserializeVector(bss[0]), deserializeBaseBlock(bss[1]));
            }
        }
        return results;
    }

    public ChestData fromItemStack(ItemStack stack) {
        if (stack != null && stack.getType().equals(Material.ENDER_CHEST)) {
            if (stack.hasItemMeta() && stack.getItemMeta().hasLore()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta.getLore().get(0).equals(BuildInABox.LORE_HEADER)) {
                    if (meta.getLore().size() > 0) {
                        try {
                            return getChest(Integer.parseInt(meta.getLore().get(1).substring(2), 16));
                        } catch (IllegalArgumentException ex) {
                        }
                    }
                }
            }
        }
        return null;
    }

    public ChestData fromBlock(Block block) {
        if (block.getType().equals(Material.ENDER_CHEST)) {
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
