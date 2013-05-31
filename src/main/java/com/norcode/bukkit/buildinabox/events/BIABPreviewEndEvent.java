package com.norcode.bukkit.buildinabox.events;


import com.norcode.bukkit.buildinabox.BuildChest;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

public class BIABPreviewEndEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    private BuildChest buildChest;

    public BIABPreviewEndEvent(Player player, BuildChest buildChest) {
        super(player);
        this.buildChest = buildChest;
    }

    public BuildChest getBuildChest() {
        return buildChest;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
