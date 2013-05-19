package com.norcode.bukkit.buildinabox.listeners;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block.getType().equals(Material.ENDER_CHEST)) {
            if (block.hasMetadata("buildInABox")) {
                BuildChest bc = (BuildChest) block.getMetadata("buildInABox").get(0).value();
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
                        bc.endPreview(player);
                    } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        // build
                        bc.build(player);
                    }
                } else {
                    long now = System.currentTimeMillis();
                    if (bc.isLocking()) {
                        if (!bc.getLockingTask().lockingPlayer.equals(player.getName())) {
                            if (plugin.getConfig().getBoolean("allow-unlocking-others", true)) {
                                if (player.hasPermission("biab.unlock.others")) {
                                    String msgKey = "lock-attempt-cancelled";
                                    if (bc.getLockingTask() instanceof UnlockingTask) {
                                        msgKey = "un" + msgKey;
                                    }
                                    player.sendMessage(BuildInABox.getNormalMsg(msgKey, bc.getLockingTask().lockingPlayer));
                                    bc.getLockingTask().cancel();
                                } else {
                                    player.sendMessage(BuildInABox.getErrorMsg("no-permission"));
                                }
                            }
                        } else {
                            bc.getLockingTask().cancel();
                        }
                    } else if (now - bc.getLastClicked() < plugin.getConfig().getInt("double-click-interval",2000) && bc.getLastClickType().equals(event.getAction())) {
                        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                            // pick up
                            if (!bc.isLocked()) {
                                if (player.hasPermission("biab.pickup")) {
                                    bc.pickup(player);
                                } else {
                                    player.sendMessage(BuildInABox.getErrorMsg("no-permission"));
                                }
                            }
                        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                            // lock/unlock
                            if (plugin.getConfig().getBoolean("allow-locking", true)) {
                                if (bc.isLocked()) {
                                    if (bc.getLockedBy().equals(player.getName())) {
                                        if (player.hasPermission("biab.unlock")) {
                                            bc.unlock(player);
                                        } else {
                                            player.sendMessage(BuildInABox.getErrorMsg("no-permission"));
                                        }
                                    } else if (plugin.getConfig().getBoolean("allow-unlocking-others")) {
                                        if (player.hasPermission("biab.unlock.others")) {
                                            bc.unlock(player);
                                        } else {
                                            player.sendMessage(BuildInABox.getErrorMsg("no-perimssion"));
                                        }
                                    }
                                } else {
                                    if (player.hasPermission("biab.lock")) {
                                        bc.lock(player);
                                    } else {
                                        player.sendMessage(BuildInABox.getErrorMsg("no-perimssion"));
                                    }
                                }
                            }
                        }
                        bc.setLastClicked(-1);
                        bc.setLastClickType(null);
                    } else {
                        bc.setLastClicked(now);
                        bc.setLastClickType(event.getAction());
                        player.sendMessage(bc.getDescription());
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
                event.getPlayer().sendMessage(BuildInABox.getErrorMsg("no-permission"));
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