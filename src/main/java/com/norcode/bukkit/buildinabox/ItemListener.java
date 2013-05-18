package com.norcode.bukkit.buildinabox;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemListener implements Listener {
    BuildInABox plugin;
    public ItemListener(BuildInABox plugin) {
        this.plugin = plugin;
    }

    private void deleteBuildChest(ItemStack stack) {
        ChestData data = plugin.getDataStore().fromItemStack(stack);
        if (data != null) {
            plugin.getDataStore().deleteChest(data.getId());
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        deleteBuildChest(event.getEntity().getItemStack());
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        plugin.checkCarrying(event.getPlayer());
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onInventoryCloseEvent(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        plugin.checkCarrying((Player) event.getPlayer());
    }
}
