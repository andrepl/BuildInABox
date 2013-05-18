package com.norcode.bukkit.buildinabox;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemListener implements Listener {

    private void deleteBuildChest(ItemStack stack) {
        if (stack.getType().equals(Material.ENDER_CHEST)) {
            if (stack.hasItemMeta()) {
                ItemMeta meta = stack.getItemMeta();
                if (meta.hasLore() && meta.getLore().size() > 1) {
                    if (meta.getLore().get(0).equals(BuildInABox.LORE_HEADER)) {
                        int id;
                        try {
                            id = Integer.parseInt(meta.getLore().get(1).substring(2), 16);
                        } catch (IllegalArgumentException ex) {
                            return;
                        }
                        BuildInABox.getInstance().getDataStore().deleteChest(id);
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.MONITOR)
    public void onItemDespawn(ItemDespawnEvent event) {
        deleteBuildChest(event.getEntity().getItemStack());
    }

}
