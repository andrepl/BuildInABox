package com.norcode.bukkit.buildinabox;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;

public class FakeBlockPlaceEvent extends BlockPlaceEvent {
    private boolean wasCancelled = false;
    public FakeBlockPlaceEvent(Location loc, Player thePlayer) {
        super(loc.getBlock(), loc.getBlock().getState(), loc.getBlock().getRelative(BlockFace.DOWN), null, thePlayer, true);
    }
    public boolean wasCancelled() {
        return wasCancelled;
    }
    public void setWasCancelled(boolean wasCancelled) {
        this.wasCancelled = wasCancelled;
    }
}
