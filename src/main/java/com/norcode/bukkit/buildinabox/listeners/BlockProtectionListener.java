package com.norcode.bukkit.buildinabox.listeners;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.norcode.bukkit.buildinabox.BuildInABox;

public class BlockProtectionListener implements Listener {

    @EventHandler(ignoreCancelled=true)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || (event.getBlock().getTypeId() == BuildInABox.BLOCK_ID && event.getBlock().hasMetadata("buildInABox"))) {
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
