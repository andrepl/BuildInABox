package com.norcode.bukkit.buildinabox.datastore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import com.norcode.bukkit.schematica.ClipboardBlock;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.Location;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.BuildingPlan;
import com.norcode.bukkit.buildinabox.ChestData;
import org.bukkit.util.BlockVector;

public class EbeanDataStore extends YamlDataStore {
    private HashMap<Integer, ChestBean> chestBeans = new HashMap<Integer, ChestBean>();
    public EbeanDataStore(BuildInABox plugin) {
        super(plugin);
    }

    @Override
    public void load() {
        try {
            plugin.getDatabase().find(ChestBean.class).findRowCount();
        } catch (PersistenceException ex) {
            plugin.installDDL();
        }
        loadPlans();
        loadChests();
    }

    @Override
    void loadChests() {
        this.chestBeans.clear();
        this.chests.clear();
        // Load Chests
        Location loc;
        for (ChestBean bean: this.plugin.getDatabase().find(ChestBean.class).findList()) {
            loc = deserializeLocation(bean.getLocation());
            HashMap<BlockVector, NBTTagCompound> tileEntities = deserializeTileEntities(bean.getTileEntities());
            HashMap<BlockVector, ClipboardBlock> replacedBlocks = deserializeReplacedBlocks(bean.getReplacedBlocks());
            chests.put(bean.getId(), new ChestData(bean.getId(), bean.getPlanName(), bean.getLockedBy(), bean.getLastActivity(), loc, tileEntities, replacedBlocks));
            chestBeans.put(bean.getId(), bean);
        }

    }

    @Override
    public void save() {
        if (this.dirty) {
            this.dirty = false;
            this.planCfg.saveConfig();
            saveToDB();
        }
    }

    private void saveToDB() {
        ChestBean bean;
        List<ChestBean> toSave = new ArrayList<ChestBean>();
        for (ChestData cd: chests.values()) {
            bean = chestBeans.get(cd.getId());
            if (cd.getLastActivity() != bean.getLastActivity()) {
                bean.setLastActivity(cd.getLastActivity());
                bean.setLocation(serializeLocation(cd.getLocation()));
                bean.setLockedBy(cd.getLockedBy());
                bean.setPlanName(cd.getPlanName());
                bean.setReplacedBlocks(serializeReplacedBlocks(cd.getReplacedBlocks()));
                bean.setTileEntities(serializeTileEntities(cd.getTileEntities()));
                toSave.add(bean);
            }
        }
        plugin.getDatabase().save(toSave);
    }

    @Override
    public ChestData createChest(String planName) {
        long now = System.currentTimeMillis();
        ChestBean bean = plugin.getDatabase().createEntityBean(ChestBean.class);
        BuildingPlan plan = getBuildingPlan(planName);
        if (plan != null) {
            bean.setPlanName(plan.getName());
            bean.setLastActivity(now);
            ChestData data = new ChestData(bean.getId(), planName, null, now, null, null, null);
            chests.put(bean.getId(), data);
            chestBeans.put(bean.getId(), bean);
            return data;
        }
        return null;
    }

    @Override
    public void saveChest(ChestData data) {
        this.chests.put(data.getId(), data);
        this.dirty = true;
    }

    @Override
    public void deleteChest(int id) {
        ChestBean bean = chestBeans.get(id);
        plugin.getDatabase().delete(bean);
        chests.remove(id);
        chestBeans.remove(id);
    }


    @Entity
    @Table(name="biab_chestdata")
    public static class ChestBean {
        @Id private int id;
        @Column private String planName;
        @Column private String lockedBy;
        @Column private long lastActivity;
        @Column private String location;
        @Column @Lob private String replacedBlocks;
        @Column @Lob private String tileEntities;
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
        public String getLocation() {
            return location;
        }
        public void setLocation(String location) {
            this.location = location;
        }
        public String getReplacedBlocks() {
            return replacedBlocks;
        }
        public void setReplacedBlocks(String replacedBlocks) {
            this.replacedBlocks = replacedBlocks;
        }
        public String getTileEntities() {
            return tileEntities;
        }
        public void setTileEntities(String tileEntities) {
            this.tileEntities = tileEntities;
        }
    }

}
