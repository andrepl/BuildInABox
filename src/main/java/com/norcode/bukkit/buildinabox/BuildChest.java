package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import net.minecraft.server.v1_5_R3.Packet61WorldEvent;

import org.bukkit.craftbukkit.v1_5_R3.CraftServer;
import org.bukkit.craftbukkit.v1_5_R3.CraftWorld;

import org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.EnderChest;
import org.bukkit.scheduler.BukkitTask;

import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public class BuildChest {
    final static long PREVIEW_DURATION = 20*4;
    BuildInABox plugin;
    private Location location;
    private boolean previewing = false;
    private BuildingPlan plan;
    private String lockedBy = null;
    private LockingTask lockingTask = null;

    private BukkitTask buildAnimationTask = null;
    
    public BuildChest(BuildInABox plugin, Location loc, BuildingPlan plan, String lockedBy) {
        this.plugin = plugin;
        this.location = loc;
        this.plan = plan;
        this.lockedBy = lockedBy;
    }

    public boolean isLocking() {
        return lockingTask != null;
    }

    LockingTask getLockingTask() {
        return lockingTask;
    }

    public boolean isLocked() {
        return lockedBy != null;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public Location getLocation() {
        return location;
    }

    public BuildingPlan getPlan() {
        return plan;
    }

    public ItemStack toItemStack() {
        ItemStack stack = new ItemStack(Material.ENDER_CHEST);
        ItemMeta meta = plugin.getServer().getItemFactory().getItemMeta(Material.ENDER_CHEST);
        List<String> lore = new ArrayList<String>();
        lore.add("Build-in-a-Box");
        meta.setLore(lore);
        meta.setDisplayName(plan.getName());
        stack.setItemMeta(meta);
        return stack;
    }
    
    public void endPreview(final Player player) {
        if (previewing && location.getBlock().getType().equals(Material.ENDER_CHEST)) {
            plan.clearPreview(player.getName(), location.getBlock());
            location.getBlock().setType(Material.AIR);
            location.getWorld().dropItem(new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5), toItemStack());
            plugin.getDataStore().removeChest(this);
            previewing = false;
        }
    }

    public boolean isPreviewing() {
        return previewing;
    }

    public void preview(final Player player) {
        previewing = true;
        
        if (plan.sendPreview(player, location.getBlock())) {
            player.sendMessage(getDescription());
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    endPreview(player);
                }
            }, PREVIEW_DURATION);
        } else {
            endPreview(player);
            player.sendMessage(ChatColor.GOLD + getPlan().getName() + " won't fit here.");
        }
    }

    public void protectBlocks() {
        plan.protectBlocks(location.getBlock(), null);
    }

    public void build(Player player) {
        if (previewing && location.getBlock().getType().equals(Material.ENDER_CHEST)) {
            plan.clearPreview(player.getName(), location.getBlock());
            previewing = false;
        }
        buildAnimationTask = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            private int ticks = 0;
            private CuboidClipboard clipboard = getPlan().getRotatedClipboard(getEnderChest().getFacing());
            private Random rand = new Random();
            private Location point;
            private HashSet<Vector> alreadyShown = new HashSet<Vector>();
            private int x;
            private int y;
            private int z;
            private BaseBlock bb;
            private Packet61WorldEvent packet;
            private List<Player> nearby = null;
            public void run() {
                ticks ++;
                if (ticks == 40) {
                    if (buildAnimationTask != null) {
                        buildAnimationTask.cancel();
                        plugin.getLogger().info("Stopping Animation.");
                        buildAnimationTask = null;
                        return;
                    }
                }
                for (int i=0;i<10;i++) {
                    x = rand.nextInt(clipboard.getSize().getBlockX());
                    y = rand.nextInt(clipboard.getSize().getBlockY());
                    z = rand.nextInt(clipboard.getSize().getBlockZ());
                    Vector vec = new Vector(x,y,z);
                    if (alreadyShown.contains(vec)) {
                        continue;
                    } else {
                        alreadyShown.add(vec);
                    }
                    BaseBlock bb = clipboard.getPoint(vec);
                    point = new Location(
                        location.getWorld(),
                        location.getX() + clipboard.getOffset().getBlockX() + x, 
                        location.getY() + clipboard.getOffset().getBlockY() + y, 
                        location.getZ() + clipboard.getOffset().getBlockZ() + z
                    );
                    Packet61WorldEvent packet = new Packet61WorldEvent(2001, point.getBlockX(), point.getBlockY(), point.getBlockZ(), bb.getType(), false);
                    if (nearby == null) {
                        nearby = new ArrayList<Player>();
                        for (Player p: location.getWorld().getPlayers()) {
                            try {
                                if (p.getLocation().distance(location) < 32) {
                                    nearby.add(p);
                                }
                            } catch (IllegalArgumentException ex) {
                            }
                        }
                    }
                    for (Player p: nearby) {
                        ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet);
                    }
                    point.getBlock().setTypeIdAndData(bb.getType(), (byte) bb.getData(), false);
                }
            }
        }, 0, 1);
        plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            public void run() {
                plan.build(location.getBlock());
            }
        }, 40);
        this.previewing = false;
    }


    public void unlock(Player player) {
        long total = plugin.getConfig().getLong("unlock-time", 10);
        if (lockedBy.equals(player.getName())) {
            total = plugin.getConfig().getLong("unlock-time-own", 5);
        }
        lockingTask = new UnlockingTask(player.getName(), total);
        lockingTask.run();
    }

    public void lock(Player player) {
        long total = plugin.getConfig().getLong("lock-time", 10);
        lockingTask = new LockingTask(player.getName(), total);
        lockingTask.run();
    }

    public void pickup(Player player) {
        if (!isLocked()) {
            plan.pickup(this.getLocation().getBlock());
            location.getBlock().setType(Material.AIR);
            location.getWorld().dropItem(new Location(location.getWorld(), location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5), toItemStack());
            plugin.getDataStore().removeChest(this);
        } else {
            player.sendMessage(ChatColor.GOLD + getPlan().getName() + " is locked by " + ChatColor.GOLD + getLockedBy());
        }
    }

    public EnderChest getEnderChest() {
        return (EnderChest) location.getBlock().getState().getData();
    }

    class LockingTask implements Runnable {
        public boolean cancelled = false;
        String lockingPlayer;
        long totalTime;
        long startTime;
        public LockingTask(String playerName, long totalTimeSeconds) {
            this.startTime = System.currentTimeMillis();
            this.totalTime = totalTimeSeconds * 1000;
            this.lockingPlayer = playerName;
        }

        protected String getCancelMessage() {
            return ChatColor.RED + "Locking attempt cancelled.";
        }

        protected String getSuccessMessage() {
            return ChatColor.GREEN + "Successfully Locked " + getPlan().getName() + ".";
        }

        protected String getProgressMessage(int percentage) {
            return ChatColor.GOLD + "Locking " + getPlan().getName() + "... " + percentage + "%";
        }

        protected String getLockedBy() {
            return lockingPlayer;
        }

        public void cancel() {
            Player player = plugin.getServer().getPlayer(lockingPlayer);
            cancelled = true;
            if (player.isOnline()) {
                player.sendMessage(getCancelMessage());
                lockedBy = getLockedBy() == null ? lockingPlayer : null;
                lockingTask = null;
            }
        }

        public void run() {
            if (cancelled) return;
            Player player = plugin.getServer().getPlayer(lockingPlayer);
            if (!player.isOnline()) {
                cancel();
            } else {
                // check distance from chest;
                try {
                    double distance = player.getLocation().distance(location);
                    if (distance > plugin.getConfig().getDouble("max-locking-distance", 5)) {
                        cancel();
                        return;
                    }
                } catch (IllegalArgumentException ex) {
                    // Cross-world distance check
                    cancel();
                    return;
                }
                long elapsed = System.currentTimeMillis() - startTime;
                if (elapsed > totalTime) elapsed = totalTime;
                int pct = (int)Math.floor((elapsed / (double) totalTime) * 100);
                if (pct < 100) {
                    player.sendMessage(getProgressMessage(pct));
                    plugin.getServer().getScheduler().runTaskLater(plugin, this, 20);
                } else {
                    lockedBy = getLockedBy();
                    lockingTask = null;
                    player.sendMessage(getSuccessMessage());
                }
            }
        }
    }

    class UnlockingTask extends LockingTask {
        public UnlockingTask(String playerName, long totalTime) {
            super(playerName, totalTime);
        }

        @Override
        public String getCancelMessage() {
            return ChatColor.RED + "Unlock attempt Cancelled.";
        }

        @Override
        public String getSuccessMessage() {
            return ChatColor.GREEN + "Successfully Unlocked " + getPlan().getName() + ".";
        }

        @Override
        public String getProgressMessage(int percentage) {
            return ChatColor.GOLD + "Unlocking " + getPlan().getName() + "... " + percentage + "%";
        }

        @Override
        public String getLockedBy() {
            return null;
        }
    }

    public String[] getDescription() {
        List<String> desc = new ArrayList<String>(2);
        desc.add(ChatColor.GOLD + getPlan().getName() + " - " + (previewing ? ChatColor.GREEN + "PREVIEW" : (isLocked() ? ChatColor.RED + "LOCKED " + ChatColor.WHITE + "[" + ChatColor.GOLD + lockedBy + ChatColor.WHITE + "]" : ChatColor.GREEN + "UNLOCKED")));
        if (previewing) {
            desc.add(ChatColor.GOLD + "Left click to cancel " + ChatColor.WHITE + "|" + ChatColor.GOLD + " Right click to confirm");
        } else if (isLocked()) {
            desc.add(ChatColor.GOLD + "Right click twice to unlock");
        } else {
            desc.add(ChatColor.GOLD + "Left click twice to pick up " + ChatColor.WHITE + "|" + ChatColor.GOLD + " Right click twice to lock");
        }
        String[] sa = new String[desc.size()];
        return desc.toArray(sa);
    }
}
