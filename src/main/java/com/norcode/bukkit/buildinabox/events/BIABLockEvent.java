package com.norcode.bukkit.buildinabox.events;

import com.norcode.bukkit.buildinabox.BuildChest;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class BIABLockEvent extends PlayerEvent implements Cancellable {
    public static enum Type {
        LOCK_ATTEMPT,
        LOCK_SUCCESS,
        LOCK_CANCEL,
        UNLOCK_ATTEMPT,
        UNLOCK_SUCCESS,
        UNLOCK_CANCEL
    }
    private static final HandlerList handlers = new HandlerList();
    private BuildChest buildChest;
    private boolean cancelled = false;
    private Type type;

    public BIABLockEvent(Player player, BuildChest buildChest, Type type) {
        super(player);
        this.buildChest = buildChest;
    }

    public BuildChest getBuildChest() {
        return buildChest;
    }

    public Type getType() {
        return type;
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
