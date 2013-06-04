package com.norcode.bukkit.buildinabox.listeners;

import com.norcode.bukkit.buildinabox.BuildChest;
import com.norcode.bukkit.buildinabox.BuildInABox;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.EnumSet;
import java.util.Iterator;

public class BlockProtectionListener implements Listener {
    BuildInABox plugin;

    public BlockProtectionListener() {
        plugin = BuildInABox.getInstance();
        
    }
    @EventHandler(ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || (event.getBlock().getTypeId() == plugin.cfg.getChestBlockId() && event.getBlock().hasMetadata("buildInABox"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onExplosion(EntityExplodeEvent event) {
        Iterator<Block> it = event.blockList().iterator();
        Block b;
        while (it.hasNext()) {
            b = it.next();
            if (b.hasMetadata("biab-block") || b.hasMetadata("buildInABox")) {
                it.remove();
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || event.getBlock().hasMetadata("buildInABox")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || event.getBlock().hasMetadata("buildInABox")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBlockFade(BlockFadeEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || event.getBlock().hasMetadata("buildInABox")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block b: event.getBlocks()) {
            if (b.hasMetadata("biab-block") || b.hasMetadata("buildInABox")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block b = event.getRetractLocation().getBlock();
        if (b.hasMetadata("biab-block") || b.hasMetadata("buildInABox")) {
            event.setCancelled(true);
        }
    }

}
