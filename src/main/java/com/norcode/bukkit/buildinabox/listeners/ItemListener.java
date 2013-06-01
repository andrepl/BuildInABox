package com.norcode.bukkit.buildinabox.listeners;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.ChestData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

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
        for (ItemStack is: event.getInventory().getContents()) {
            ChestData data = BuildInABox.getInstance().getDataStore().fromItemStack(is);
            if (data != null) {
                data.setLastActivity(System.currentTimeMillis());
                BuildInABox.getInstance().getDataStore().saveChest(data);
            }
        }
    }
}
