package com.norcode.bukkit.buildinabox.listeners;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.norcode.bukkit.buildinabox.BuildChest;
import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.ChestData;
import com.norcode.bukkit.buildinabox.BuildChest.UnlockingTask;

public class PlayerListener implements Listener {
    BuildInABox plugin;
    public PlayerListener(BuildInABox plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock().getType().equals(Material.ENDER_CHEST)) {
            if (event.getClickedBlock().hasMetadata("buildInABox")) {
                BuildChest bc = (BuildChest) event.getClickedBlock().getMetadata("buildInABox").get(0).value();
                if (bc.isBuilding()) {
                    event.setCancelled(true);
                    event.setUseInteractedBlock(Result.DENY);
                    event.setUseItemInHand(Result.DENY);
                    return;
                }
                bc.updateActivity();
                if (bc.isPreviewing()) {
                    if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                        // Cancel
                        bc.endPreview(event.getPlayer());
                    } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        // build
                        bc.build(event.getPlayer());
                    }
                } else {
                    long now = System.currentTimeMillis();
                    if (bc.isLocking()) {
                        if (!bc.getLockingTask().lockingPlayer.equals(event.getPlayer().getName())) {
                            String msgKey = "lock-attempt-cancelled";
                            if (bc.getLockingTask() instanceof UnlockingTask) {
                                msgKey = "un" + msgKey;
                            }
                            event.getPlayer().sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GRAY + plugin.getMsg(msgKey, bc.getLockingTask().lockingPlayer));
                        }
                        bc.getLockingTask().cancel();
                    } else if (now - bc.getLastClicked() < 2000 && bc.getLastClickType().equals(event.getAction())) {
                        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                            // pick up
                            if (!bc.isLocked()) {
                                bc.pickup(event.getPlayer());
                                
                            }
                        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                            // lock/unlock
                            if (plugin.getConfig().getBoolean("allow-locking", true)) {
                                if (bc.isLocked()) {
                                    bc.unlock(event.getPlayer());
                                } else {
                                    bc.lock(event.getPlayer());
                                }
                            }
                        }
                        bc.setLastClicked(-1);
                        bc.setLastClickType(null);
                    } else {
                        bc.setLastClicked(now);
                        bc.setLastClickType(event.getAction());
                        event.getPlayer().sendMessage(bc.getDescription());
                    }
                }
                event.setCancelled(true);
                event.setUseInteractedBlock(Result.DENY);
                event.setUseItemInHand(Result.DENY);
            }
        }
    }


    @EventHandler(ignoreCancelled=true)
    public void onPlaceEnderchest(final BlockPlaceEvent event) {
        if (event.getBlock().getType().equals(Material.ENDER_CHEST)) {
            if (plugin.getConfig().getBoolean("prevent-placing-enderchests", false)) {
                ChestData data = plugin.getDataStore().fromItemStack(event.getItemInHand());
                if (data == null) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGH)
    public void onBlockPlace(final BlockPlaceEvent event) {
        ChestData data = plugin.getDataStore().fromItemStack(event.getItemInHand());
        if (data != null) {
            if (!event.getPlayer().hasPermission("biab.place")) {
                event.getPlayer().sendMessage(plugin.getMsg("no-permission"));
                event.setCancelled(true);
                return;
            }
            data.setLocation(event.getBlock().getLocation());
            data.setLastActivity(System.currentTimeMillis());
            final BuildChest bc = new BuildChest(data);
            event.getBlock().setMetadata("buildInABox", new FixedMetadataValue(plugin, bc));
            event.getPlayer().getInventory().setItemInHand(null);
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                public void run() {
                    if (event.getPlayer().isOnline()) {
                        bc.preview(event.getPlayer());
                        plugin.checkCarrying(event.getPlayer());
                    }
                }
            }, 1);
        }
    }

}
