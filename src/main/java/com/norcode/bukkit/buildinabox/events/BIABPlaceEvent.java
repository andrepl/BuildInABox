package com.norcode.bukkit.buildinabox.events;

import com.norcode.bukkit.buildinabox.ChestData;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.inventory.ItemStack;


public class BIABPlaceEvent extends PlayerEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private Location location;
    private ItemStack biab;
    private ChestData data;
    private boolean cancelled = false;

    public BIABPlaceEvent(Player player, Location location, ItemStack biab, ChestData data) {
        super(player);
        this.location = location;
        this.biab = biab;
        this.data = data;
    }

    public ChestData getChestData() {
        return data;
    }

    public Location getLocation() {
        return location;
    }

    public ItemStack getItemStack() {
        return biab;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}
