package com.norcode.bukkit.buildinabox.util;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.schematica.ClipboardBlock;
import net.minecraft.server.v1_6_R3.NBTCompressedStreamTools;
import net.minecraft.server.v1_6_R3.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.reflect.TypeToken;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockVector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SerializationUtil {
    protected static final Gson gson = new Gson();

    public static List<Map<String, Object>> serializeItemList(List<ItemStack> list) {
        List<Map<String, Object>> returnVal = new ArrayList<Map<String, Object>>();
        for (ConfigurationSerializable cs : list) {
            returnVal.add(serialize(cs));
        }
        return returnVal;
    }
 
    public static Map<String, Object> serialize(ConfigurationSerializable cs) {
        if (cs == null) return null;
        Map<String, Object> serialized = recreateMap(cs.serialize());
        for (Entry<String, Object> entry : serialized.entrySet()) {
            if (entry.getValue() instanceof ConfigurationSerializable) {
                entry.setValue(serialize((ConfigurationSerializable)entry.getValue()));
            }
        }
        serialized.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(cs.getClass()));
        return serialized;
    }
 
    public static Map<String, Object> recreateMap(Map<String, Object> original) {
        Map<String, Object> map = new HashMap<String, Object>();
        for (Entry<String, Object> entry : original.entrySet()) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
 
    // Time for Deserialization
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static ConfigurationSerializable deserialize(Map<String, Object> map) {
        if (map  == null) {
            return null;
        }
        for (Entry<String, Object> entry : map.entrySet()) {
            // Check if any of its sub-maps are ConfigurationSerializable.  They need to be done first.
            if (entry.getValue() instanceof Map && ((Map)entry.getValue()).containsKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY)) {
                entry.setValue(entry.getValue() == null ? null : deserialize((Map)entry.getValue()));
            }
        }
        return ConfigurationSerialization.deserializeObject(map);
    }
 
    public static List<ItemStack> deserializeItemList(List<Map<String, Object>> itemList) {
        List<ItemStack> returnVal = new ArrayList<ItemStack>();
        for (Map<String, Object> map : itemList) {
            returnVal.add((ItemStack) deserialize(map));
        }
        return returnVal;
    }

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

    public static String serializeVector(BlockVector v) {
        if (v == null) {
            return null;
        }
        return v.getBlockX() + ";" + v.getBlockY() + ";" + v.getBlockZ();
    }

    public static BlockVector deserializeVector(String s) {
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

    public static String serializeCompoundTag(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }
        return new String(Base64.encodeToByte(NBTCompressedStreamTools.a(tag), false));
    }

    public static NBTTagCompound deserializeCompoundTag(String s) {
        if (s == null) {
            return null;
        }
        NBTTagCompound tag = NBTCompressedStreamTools.a(Base64.decode(s.getBytes()));
        return tag;
    }
    public static String serializeTileEntities(HashMap<BlockVector, NBTTagCompound> ents) {
        if (ents == null) {
            return null;
        }
        HashMap<String, String> obj = new HashMap<String, String>();
        for (Entry<BlockVector, NBTTagCompound> entry: ents.entrySet()) {
            obj.put(serializeVector(entry.getKey()), serializeCompoundTag(entry.getValue()));
        }
        return gson.toJson(obj);
    }

    public static HashMap<BlockVector, NBTTagCompound> deserializeTileEntities(String s) {
        if (s == null) {
            return null;
        }
        HashMap<BlockVector, NBTTagCompound> contents = new HashMap<BlockVector, NBTTagCompound>();
        final java.lang.reflect.Type type = new TypeToken<Map<String, String>>(){}.getType();
        HashMap<String, String> data = gson.fromJson(s, type);
        for (String k: data.keySet()) {
            contents.put(deserializeVector(k), deserializeCompoundTag(data.get(k)));
        }
        return contents;
    }

    public static String serializeReplacedBlocks(HashMap<BlockVector, ClipboardBlock> replacedBlocks) {
        String repr = "";
        for (Entry<BlockVector, ClipboardBlock> e: replacedBlocks.entrySet()) {
            repr += serializeVector(e.getKey()) + ":" + serializeClipboardBlock(e.getValue()) + "|";
        }
        if (repr.length() > 1) {
            repr = repr.substring(0,repr.length()-1);
        }
        return repr;
    }

    public static String serializeClipboardBlock(ClipboardBlock bb) {
        String s = Integer.toString(bb.getType());
        if (bb.getData() != 0) {
            s += "|" + bb.getData();
        }
        return s;
    }

    public static ClipboardBlock deserializeBaseBlock(String s) {
        if (s == null || s.equals("")) {
            return null;
        }
        if (s.contains(",")) {
            String[] parts = s.split(",");
            return new ClipboardBlock(Integer.parseInt(parts[0]), (byte) Integer.parseInt(parts[1]));
        }
        return new ClipboardBlock(Integer.parseInt(s), (byte) 0);
    }

    public static HashMap<BlockVector, ClipboardBlock> deserializeReplacedBlocks(String s) {
        HashMap<BlockVector, ClipboardBlock> results = new HashMap<BlockVector, ClipboardBlock>();
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
            ClipboardBlock bb;
            for (Entry<String, Map<String, Integer>> e: data.entrySet()) {
                v = deserializeVector(e.getKey());
                bb = new ClipboardBlock(e.getValue().get("t"), (byte) (int) e.getValue().get("d"));
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
}