package com.norcode.bukkit.buildinabox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.norcode.bukkit.buildinabox.util.CuboidClipboard;

import com.norcode.bukkit.schematica.Clipboard;
import com.norcode.bukkit.schematica.ClipboardBlock;
import net.minecraft.server.v1_5_R3.NBTTagCompound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.material.EnderChest;
import org.bukkit.metadata.FixedMetadataValue;

import com.sk89q.jnbt.CompoundTag;
import com.sk89q.jnbt.IntTag;
import com.sk89q.jnbt.Tag;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.TileEntityBlock;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import org.bukkit.util.BlockVector;

public class BuildChest {
    BuildInABox plugin;
    private boolean previewing = false;
    private BuildingPlan plan;
    private LockingTask lockingTask = null;
    private BuildingPlanTask buildTask = null;
    private ChestData data;
    private boolean building = false;
    private long lastClicked = -1;
    private Action lastClickType = null;

    public BuildChest(ChestData data) {
        this.plugin = BuildInABox.getInstance();
        this.data = data;
        this.plan = BuildInABox.getInstance().getDataStore()
                .getBuildingPlan(data.getPlanName());
    }

    public ChestData getData() {
        return data;
    }
    public int getId() {
        return data.getId();
    }

    public boolean isLocking() {
        return lockingTask != null;
    }

    public LockingTask getLockingTask() {
        return lockingTask;
    }

    public boolean isLocked() {
        return data.getLockedBy() != null;
    }

    public String getLockedBy() {
        return data.getLockedBy();
    }

    public Location getLocation() {
        return data.getLocation();
    }

    public BuildingPlan getPlan() {
        return plan;
    }

    public void endPreview(final Player player) {
        if (!previewing) return;
        buildTask = new BuildingPlanTask(plan.getRotatedClipboard(getEnderChest().getFacing()), BuildChest.this, BlockFace.DOWN, 50, true) {
                    @Override
                    public void onComplete() {
                        previewing = false;
                        getBlock().removeMetadata("buildInABox", plugin);
                        getBlock().setTypeIdAndData(0, (byte) 0, false);
                        getBlock().getWorld().dropItem(getBlock().getLocation().add(0.5,0.5,0.5), data.toItemStack());
                        data.setLocation(null);
                        data.setLastActivity(System.currentTimeMillis());
                        plugin.getDataStore().saveChest(data);
                    }

                    @Override
                    public BlockProcessResult processBlockUpdate(
                            BlockUpdate update) {
                        Location wc = getWorldLocationFor(update.getPos());
                        player.sendBlockChange(wc, wc.getBlock().getType(), (byte) wc.getBlock().getData());
                        return BlockProcessResult.PROCESSED;
                    }
        };
        buildTask.start();
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public EnderChest getEnderChest() {
        return (EnderChest) getBlock().getState().getData();
    }

    public Block getBlock() {
        if (data.getLocation() != null) {
            try {
                return data.getLocation().getBlock();
            } catch (NullPointerException ex) {
                return null;
            }
        }
        return null;
    }

    public void preview(final Player player) {
        final long previewDuration = (plugin.getConfig().getInt("preview-duration", 5000) * 20)/1000; // millis to ticks.
        final boolean checkBuildPermissions = plugin.getConfig().getBoolean("check-build-permissions", true);
        previewing = true;
        buildTask = new BuildingPlanTask(
                plan.getRotatedClipboard(getEnderChest().getFacing()),
                BuildChest.this, BlockFace.UP, 50, false) {
            @Override
            public void onRunStart() {
                plugin.exemptPlayer(player);
            }
            @Override
            public void onRunEnd() {
                plugin.unexemptPlayer(player);
            }
            @Override
            public void onComplete() {
                if (cancelled) {
                    player.sendMessage(BuildInABox.getErrorMsg("building-wont-fit",
                            plan.getDisplayName()));
                } else {
                    player.sendMessage(getDescription());
                }
                Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                    public void run() {
                        endPreview(player);
                    }
                }, cancelled ? 1 : previewDuration);
            }
            @Override
            public BlockProcessResult processBlockUpdate(
                    BlockUpdate update) {
                ClipboardBlock bb = update.getBlock();
                Location wc = getWorldLocationFor(update.getPos());
                if (wc.equals(getBlock().getLocation())) {
                    return BlockProcessResult.DISCARD;
                }
                if (wc.getBlockY() >= getBlock().getLocation().getBlockY() && !BuildingPlan.coverableBlocks.contains(wc.getBlock().getType())) {
                    cancelled = true;
                    return BlockProcessResult.DISCARD;
                }
                if (checkBuildPermissions) {
                    FakeBlockPlaceEvent event = new FakeBlockPlaceEvent(wc, player);
                    plugin.getServer().getPluginManager().callEvent(event);
                    if (event.isCancelled() && event.wasCancelled()) {
                        cancelled = true;
                        return BlockProcessResult.DISCARD;
                    }
                }
                player.sendBlockChange(wc, bb.getType(), (byte) bb.getData());
                return BlockProcessResult.PROCESSED;
            }
        };
        buildTask.start();
    }

    public Set<Chunk> protectBlocks(Clipboard clipboard) {
        HashSet<Chunk> loadedChunks = new HashSet<Chunk>();
        if (clipboard == null) {
            Location chestLoc = getBlock().getLocation();
            EnderChest ec = (EnderChest) Material.getMaterial(BuildInABox.BLOCK_ID).getNewData(chestLoc.getBlock().getData());
            BlockFace dir = ec.getFacing();
            clipboard = plan.getRotatedClipboard(dir);
        }
        Location loc;
        BlockVector offset = clipboard.getOffset();
        BlockVector origin = new BlockVector(getBlock().getX(), getBlock().getY(), getBlock().getZ());
        for (int x=0;x<clipboard.getSize().getBlockX();x++) {
            for (int y = 0;y<clipboard.getSize().getBlockY();y++) {
                for (int z=0;z<clipboard.getSize().getBlockZ();z++) {
                    if (clipboard.getBlock(x,y,z).getType() > 0) {
                        BlockVector v = origin.add(offset).toBlockVector();
                        loc = new Location(getBlock().getWorld(), v.getBlockX()+x, v.getBlockY()+y, v.getBlockZ()+z);
                        loadedChunks.add(loc.getChunk());
                        getBlock().getWorld().getBlockAt(loc).setMetadata("biab-block", new FixedMetadataValue(plugin, this));
                    }
                }
            }
        }
        return loadedChunks;
    }

    public void build(final Player player) {
        buildTask = new BuildingPlanTask(plan.getRotatedClipboard(getEnderChest().getFacing()), BuildChest.this, BlockFace.DOWN, 50, false) {
            @Override
            public void onComplete() {
                previewing = false;
                startBuild(player);
            }

            @Override
            public BlockProcessResult processBlockUpdate(
                    BlockUpdate update) {
                Location wc = getWorldLocationFor(update.getPos());
                player.sendBlockChange(wc, wc.getBlock().getType(), (byte) wc.getBlock().getData());
                return BlockProcessResult.PROCESSED;
            }
        };
        buildTask.start();
    }

    private void startBuild(final Player player) {
        double cost = plugin.getConfig().getDouble("build-cost", 0);
        if (cost > 0 && BuildInABox.hasEconomy()) {
            if (!BuildInABox.getEconomy().withdrawPlayer(player.getName(), cost).transactionSuccess()) {
                player.sendMessage(BuildInABox.getErrorMsg("insufficient-funds", BuildInABox.getEconomy().format(cost)));
                return;
            }
        }
        previewing = false;
        building = true;
        player.sendMessage(BuildInABox.getNormalMsg("building", plan.getDisplayName()));
        final World world = player.getWorld();
        final boolean allowPickup = plugin.getConfig().getBoolean("allow-pickup", true);
        data.setLocation(getLocation());
        data.setLastActivity(System.currentTimeMillis());
        plugin.getDataStore().saveChest(data);
        final List<Player> nearby = new ArrayList<Player>();
        for (Player p: world.getPlayers()) {
            if (p.getLocation().distance(getLocation()) < 16) {
                nearby.add(p);
            }
        }
        Clipboard clipboard = plan.getRotatedClipboard(BlockFace.NORTH);
        if (data.getTileEntities() != null) {
            NBTTagCompound tag;
            for (Entry<BlockVector, NBTTagCompound> entry: data.getTileEntities().entrySet()) {
                tag = (NBTTagCompound) entry.getValue().clone();
                tag.setInt("x", entry.getKey().getBlockX());
                tag.setInt("y", entry.getKey().getBlockY());
                tag.setInt("z", entry.getKey().getBlockZ());
                clipboard.getBlock(entry.getKey()).setTag(tag);
            }
        }
        clipboard.rotate2D(BuildInABox.getRotationDegrees(BlockFace.NORTH, getEnderChest().getFacing()));
        List<BlockVector> vectorQueue = clipboard.getPasteQueue(plugin.getConfig().getBoolean("build-animation.shuffle", true), null);
        final boolean protectBlocks = plugin.getConfig().getBoolean("protect-buildings", true);
        final BlockVector origin = getLocation().toVector().add(clipboard.getOffset()).toBlockVector();
        plugin.getBuildManager().scheduleTask(
            new BuildManager.BuildTask(clipboard, vectorQueue, plugin.getConfig().getInt("build-animation.blocks-per-tick", 5)) {

                private void saveReplacedBlock(BlockVector c, Location wc) {
                    // save blocks below ground level for restoration.
                    getData().getReplacedBlocks().put(c,
                            new ClipboardBlock(wc.getBlock().getTypeId(),
                                    wc.getBlock().getData()));
                }

                @Override
                public void processBlock(BlockVector clipboardPoint) {
                    int x = origin.getBlockX() + clipboardPoint.getBlockX();
                    int y = origin.getBlockY() + clipboardPoint.getBlockY();
                    int z = origin.getBlockZ() + clipboardPoint.getBlockZ();

                    this.clipboard.copyBlockToWorld(clipboard.getBlock(clipboardPoint), new Location(world, x, y, z));
                }

                @Override
                public void onComplete() {
                    building = false;
                    data.clearTileEntities();
                    if (!plugin.getConfig().getBoolean("allow-pickup")) {
                        plugin.getDataStore().deleteChest(data.getId());
                        getBlock().removeMetadata("buildInABox", plugin);
                    }
                    int fireworksLevel = plugin.getConfig().getInt("build-animation.fireworks", 0);
                    if (fireworksLevel > 0) {
                        //TODO: launchFireworks(fireworksLevel);
                    }
                    building = false;
                }
            });
//        buildTask = new BuildingPlanTask(clipboard, this, BlockFace.UP,
//
//                @Override
//                public void onRunStart() {
//                    plugin.exemptPlayer(player);
//                }
//                @Override
//                public void onRunEnd() {
//                    plugin.unexemptPlayer(player);
//                }
//                @Override
//                public BuildingPlanTask.BlockProcessResult processBlockUpdate(BuildingPlanTask.BlockUpdate blockUpdate) {
//                    ClipboardBlock bb = blockUpdate.getBlock();
//                    BlockVector c = blockUpdate.getPos();
//                    Location wc = getWorldLocationFor(c);
//                    if (bb.getType() == 0) return BlockProcessResult.DISCARD;
//                    if (wc.equals(getBlock().getLocation())) return BlockProcessResult.DISCARD;
//                    if (blockUpdate.isCanQueue()) {
//                        if (BuildingPlanTask.postBuildBlockIds.contains(bb.getType())) {
//                            return BlockProcessResult.QUEUE_FINAL;
//                        } else if (postLayerBlockIds.contains(bb.getType())) {
//                            return BlockProcessResult.QUEUE_AFTER_LAYER;
//                        }
//                    }
//                    if (c.getBlockY() < getBlock().getY()) {
//                        saveReplacedBlock(c, wc);
//                    }
//                    sendAnimationPacket(wc.getBlockX(), wc.getBlockY(), wc.getBlockZ(), bb.getType());
//                    copyFromClipboard(bb,wc,player);
//                    if (wc.equals(data.getLocation())) {
//                        wc.getBlock().setMetadata("buildInABox", new FixedMetadataValue(plugin, BuildChest.this));
//                    } else if (protectBlocks && allowPickup) {
//                        wc.getBlock().setMetadata("biab-block", new FixedMetadataValue(plugin, true));
//                    }
//                    return BlockProcessResult.PROCESSED;
//                }
//        };
//        buildTask.start();
    }

    public void unlock(Player player) {
        long total = plugin.getConfig().getLong("unlock-time", 10);
        if (data.getLockedBy().equals(player.getName())) {
            total = plugin.getConfig().getLong("unlock-time-own", 5);
        }
        double cost = plugin.getConfig().getDouble("unlock-cost", 0);
        if (cost > 0 && BuildInABox.hasEconomy()) {
            if (!BuildInABox.getEconomy()
                    .withdrawPlayer(player.getName(), cost)
                    .transactionSuccess()) {
                player.sendMessage(BuildInABox.getErrorMsg(
                        "insufficient-funds",
                        BuildInABox.getEconomy().format(cost)));
                return;
            }
        }
        lockingTask = new UnlockingTask(player.getName(), total);
        lockingTask.run();
    }

    public void lock(Player player) {
        long total = plugin.getConfig().getLong("lock-time", 10);
        double cost = plugin.getConfig().getDouble("lock-cost", 0);
        if (cost > 0 && BuildInABox.hasEconomy()) {
            if (!BuildInABox.getEconomy()
                    .withdrawPlayer(player.getName(), cost)
                    .transactionSuccess()) {
                player.sendMessage(BuildInABox.getErrorMsg(
                        "insufficient-funds",
                        BuildInABox.getEconomy().format(cost)));
                return;
            }
        }
        lockingTask = new LockingTask(player.getName(), total);
        lockingTask.run();
    }

    public void pickup(final Player player) {
        double cost = plugin.getConfig().getDouble("pickup-cost", 0);
        if (cost > 0 && BuildInABox.hasEconomy()) {
            if (!BuildInABox.getEconomy()
                    .withdrawPlayer(player.getName(), cost)
                    .transactionSuccess()) {
                player.sendMessage(BuildInABox.getErrorMsg(
                        "insufficient-funds",
                        BuildInABox.getEconomy().format(cost)));
                return;
            }
        }
        final int blocksPerTick = plugin.getConfig().getInt(
                "pickup-animation.blocks-per-tick", 20);
        final boolean shuffle = plugin.getConfig().getBoolean("pickup-animation.shuffle", true);
        final List<Player> nearby = new ArrayList<Player>();
        for (Player p : player.getWorld().getPlayers()) {
            nearby.add(p);
        }
        if (!isLocked()) {
            player.sendMessage(BuildInABox.getNormalMsg("removing", this.getPlan()
                    .getDisplayName()));
            building = true;
            data.clearTileEntities();
            buildTask = new BuildingPlanTask(plan.getRotatedClipboard(getEnderChest().getFacing()), this, BlockFace.DOWN, blocksPerTick, shuffle) {

                @Override
                public void onComplete() {
                    BaseBlock bb;
                    clipboard.rotate2D(CuboidClipboard.getRotationDegrees(getEnderChest().getFacing(), BlockFace.NORTH));
                    Vector v;
                    for (int x=0;x<clipboard.getSize().getBlockX();x++) {
                        for (int z=0;z<clipboard.getSize().getBlockZ();z++) {
                            for (int y=0;y<clipboard.getSize().getBlockY();y++) {
                                v = new Vector(x,y,z);
                                if (x == -clipboard.getOffset().getBlockX() && y == -clipboard.getOffset().getBlockY() && z == -clipboard.getOffset().getBlockZ()) {
                                    continue;
                                }
                                bb = clipboard.getPoint(v);
                                if (bb instanceof TileEntityBlock) {
                                    TileEntityBlock teb = bb;
                                    HashMap<String, Tag> values = new HashMap<String, Tag>();
                                    if (teb.getNbtData() != null && teb.getNbtData().getValue() != null) {
                                        for (Entry<String, Tag> e: teb.getNbtData().getValue().entrySet()) {
                                            values.put(e.getKey(), e.getValue());
                                        }
                                        CompoundTag tag = new CompoundTag("", values);
                                        data.setTileEntities(new BlockVector(x,y,z), tag);
                                    }
                                }
                            }
                        }
                    }

                    getBlock().setType(Material.AIR);
                    getBlock().removeMetadata("buildInABox", plugin);
                    getBlock().removeMetadata("biab-block", plugin);
                    data.getLocation().getWorld().dropItem(new Location(data.getLocation().getWorld(), data.getLocation().getX() + 0.5, data.getLocation().getY() + 0.5, data.getLocation().getZ() + 0.5), data.toItemStack());
                    data.setLocation(null);
                    data.setLastActivity(System.currentTimeMillis());
                    data.setReplacedBlocks(null);
                    plugin.getDataStore().saveChest(data);
                    int fireworksLevel = plugin.getConfig().getInt("pickup-animation.fireworks", 0);
                    if (fireworksLevel > 0) {
                        launchFireworks(fireworksLevel);
                    }
                    building = false;
                    player.sendMessage(BuildInABox.getSuccessMsg("removal-complete"));
                    return;
                }

                @Override
                public BlockProcessResult processBlockUpdate(BlockUpdate blockUpdate) {
                    BaseBlock bb = blockUpdate.getBlock();
                    BlockVector c = blockUpdate.getPos();
                    Location wc = getWorldLocationFor(c);
                    if (bb.getType() == 0) return BlockProcessResult.DISCARD;
                    if (bb.getType() == BuildInABox.BLOCK_ID) {
                        if (wc.getBlock().hasMetadata("buildInABox")) {
                            return BlockProcessResult.DISCARD;
                        }
                    }
                    if (blockUpdate.isCanQueue()) {
                        if (postBuildBlockIds.contains(bb.getType())) {
                            return BlockProcessResult.QUEUE_FINAL;
                        } else if (!postLayerBlockIds.contains(bb.getType())) {
                            return BlockProcessResult.QUEUE_AFTER_LAYER;
                        }
                    }
                    if (bb instanceof TileEntityBlock) {
                        if (((TileEntityBlock)bb).hasNbtData()) {
                            BlockState state = wc.getBlock().getState();
                            BaseBlock worldBlock = bukkitWorld.getBlock(new Vector(wc.getBlockX(), wc.getBlockY(), wc.getBlockZ()));
                            clipboard.setBlock(c, worldBlock);
                            if (state instanceof org.bukkit.inventory.InventoryHolder) {
                                org.bukkit.inventory.InventoryHolder chest = (org.bukkit.inventory.InventoryHolder) state;
                                Inventory inven = chest.getInventory();
                                if (chest instanceof Chest) {
                                    inven = ((Chest) chest).getBlockInventory();
                                }
                                inven.clear();
                            }
                        }
                    }
                    sendAnimationPacket(wc.getBlockX(), wc.getBlockY(), wc.getBlockZ(), bb.getType());
                    if (c.getBlockY() < -clipboard.getOffset().getBlockY()) {
                        if (data.getReplacedBlocks().containsKey(c)) {
                            BaseBlock replacement = data.getReplacedBlocks().get(c);
                            if (replacement != null) {
                                wc.getBlock().setTypeIdAndData(replacement.getType(), (byte) replacement.getData(), false);
                            }
                        } else {
                            wc.getBlock().setTypeIdAndData(0,(byte) 0, false);
                        }
                    } else {
                        wc.getBlock().setTypeIdAndData(0, (byte)0, false);
                    }
                    wc.getBlock().removeMetadata("biab-block", BuildInABox.getInstance());
                    wc.getBlock().removeMetadata("buildInABox", BuildInABox.getInstance());
                    return BlockProcessResult.PROCESSED;
                }
            };
            buildTask.start();
        } else {
            player.sendMessage(BuildInABox.getErrorMsg("building-is-locked",
                    getPlan().getDisplayName(), getLockedBy()));
        }
    }

    public class LockingTask implements Runnable {
        public boolean cancelled = false;
        public String lockingPlayer;
        long totalTime;
        long startTime;

        public LockingTask(String playerName, long totalTimeSeconds) {
            this.startTime = System.currentTimeMillis();
            this.totalTime = totalTimeSeconds * 1000;
            this.lockingPlayer = playerName;
        }

        protected String getCancelMessage() {
            return BuildInABox.getErrorMsg("lock-cancelled-self");
        }

        protected String getSuccessMessage() {
            return BuildInABox.getSuccessMsg("lock-success-self", getPlan()
                    .getName());
        }

        protected String getProgressMessage(int percentage) {
            return BuildInABox.getNormalMsg("lock-progress", getPlan()
                    .getName(), percentage);
        }

        protected String getLockedBy() {
            return lockingPlayer;
        }

        public void cancel() {
            Player player = plugin.getServer().getPlayer(lockingPlayer);
            cancelled = true;
            if (player.isOnline()) {
                player.sendMessage(getCancelMessage());
                data.setLockedBy(getLockedBy() == null ? lockingPlayer : null);
                data.setLastActivity(System.currentTimeMillis());
                lockingTask = null;
            }
        }

        public void run() {
            if (cancelled)
                return;
            Player player = plugin.getServer().getPlayer(lockingPlayer);
            if (!player.isOnline()) {
                cancel();
            } else {
                // check distance from chest;
                try {
                    double distance = player.getLocation().distance(
                            data.getLocation());
                    if (distance > plugin.getConfig().getDouble(
                            "max-locking-distance", 5)) {
                        cancel();
                        return;
                    }
                } catch (IllegalArgumentException ex) {
                    // Cross-world distance check
                    cancel();
                    return;
                }
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > totalTime)
                    elapsed = totalTime;
                int pct = (int) Math
                        .floor((elapsed / (double) totalTime) * 100);
                if (pct < 100) {
                    player.sendMessage(getProgressMessage(pct));
                    plugin.getServer().getScheduler()
                            .runTaskLater(plugin, this, 20);
                } else {
                    data.setLockedBy(getLockedBy());
                    data.setLastActivity(System.currentTimeMillis());
                    plugin.getDataStore().saveChest(data);
                    lockingTask = null;
                    player.sendMessage(getSuccessMessage());
                }
            }
        }
    }

    public class UnlockingTask extends LockingTask {
        public UnlockingTask(String playerName, long totalTime) {
            super(playerName, totalTime);
        }

        @Override
        public String getCancelMessage() {
            return BuildInABox.getErrorMsg("unlock-cancelled-self");
        }

        @Override
        public String getSuccessMessage() {
            return BuildInABox.getSuccessMsg("unlock-success-self", getPlan()
                    .getName());
        }

        @Override
        public String getProgressMessage(int percentage) {
            return BuildInABox.getNormalMsg("unlock-progress", getPlan()
                    .getName(), percentage);
        }

        @Override
        public String getLockedBy() {
            return null;
        }
    }

    public String[] getDescription() {
        List<String> desc = new ArrayList<String>(2);
        String header = ChatColor.GOLD + getPlan().getName();
        if (previewing || plugin.getConfig().getBoolean("allow-locking", true)) {
            header += " - "
                    + (previewing ? ChatColor.GREEN
                            + BuildInABox.getMsg("preview")
                            : (isLocked() ? ChatColor.RED
                                    + BuildInABox.getMsg("locked")
                                    + ChatColor.WHITE + " [" + ChatColor.GOLD
                                    + data.getLockedBy() + ChatColor.WHITE
                                    + "]" : ChatColor.GREEN
                                    + BuildInABox.getMsg("unlocked")));
        }
        desc.add(header);
        if (previewing) {
            desc.add(ChatColor.GOLD
                    + BuildInABox.getMsg("left-click-to-cancel")
                    + ChatColor.WHITE + " | " + ChatColor.GOLD
                    + BuildInABox.getMsg("right-click-to-confirm"));
        } else if (isLocked()) {
            desc.add(ChatColor.GOLD
                    + BuildInABox.getMsg("right-click-twice-to-unlock"));
        } else {
            String instructions = ChatColor.GOLD
                    + BuildInABox.getMsg("left-click-twice-to-pickup");
            if (plugin.getConfig().getBoolean("allow-locking", true)) {
                instructions += ChatColor.WHITE + " | " + ChatColor.GOLD
                        + BuildInABox.getMsg("right-click-twice-to-lock");
            }
            desc.add(instructions);
        }
        String[] sa = new String[desc.size()];
        return desc.toArray(sa);
    }

    public void updateActivity() {
        data.setLastActivity(System.currentTimeMillis());
        plugin.getDataStore().saveChest(data);
    }

    public boolean isBuilding() {
        return building;
    }

    public long getLastClicked() {
        return lastClicked;
    }

    public Action getLastClickType() {
        return lastClickType;
    }

    public void setLastClicked(long lastClicked) {
        this.lastClicked = lastClicked;
    }

    public void setLastClickType(Action lastClickType) {
        this.lastClickType = lastClickType;
    }

    public void unprotect() {
        Clipboard clipboard = getPlan().getRotatedClipboard(this.getEnderChest().getFacing());
        Location loc = null;
        BlockVector offset = clipboard.getOffset();
        Vector origin = new Vector(getBlock().getX(), getBlock().getY(),
                getBlock().getZ());
        for (int x = 0; x < clipboard.getSize().getBlockX(); x++) {
            for (int y = 0; y < clipboard.getSize().getBlockY(); y++) {
                for (int z = 0; z < clipboard.getSize().getBlockZ(); z++) {
                    if (clipboard.getBlock(x, y, z).getType() > 0) {
                        Vector v = origin.add(offset);
                        loc = new Location(getBlock().getWorld(), v.getBlockX()
                                + x, v.getBlockY() + y, v.getBlockZ() + z);
                        getBlock().getWorld().getBlockAt(loc)
                                .removeMetadata("biab-block", plugin);
                        getBlock().getWorld().getBlockAt(loc)
                                .removeMetadata("buildInABox", plugin);
                    }
                }
            }
        }
        getBlock().breakNaturally();
    }
}