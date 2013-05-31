package com.norcode.bukkit.buildinabox.listeners;

import java.util.EnumSet;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.norcode.bukkit.buildinabox.BuildChest;
import com.norcode.bukkit.buildinabox.BuildInABox;

public class BlockProtectionListener implements Listener {
    BuildInABox plugin;
    EnumSet lockableBlockTypes = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, 
            Material.TRAP_DOOR, Material.WOODEN_DOOR, Material.IRON_DOOR, Material.FURNACE,
            Material.DISPENSER, Material.DROPPER, Material.HOPPER, Material.BREWING_STAND,
            Material.JUKEBOX, Material.ANVIL, Material.BURNING_FURNACE, Material.BEACON);
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
    
    @EventHandler(ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (plugin.cfg.isLockingEnabled()) {
            if (lockableBlockTypes.contains(event.getClickedBlock().getType())) {
                if (event.getClickedBlock().hasMetadata("biab-block")) {
                    BuildChest bc = (BuildChest) event.getClickedBlock().getMetadata("biab-block").get(0).value();
                    if (bc.isLocked()) {
                        event.getPlayer().sendMessage(BuildInABox.getErrorMsg("building-is-locked", bc.getPlan().getDisplayName(), bc.getLockedBy()));
                        event.setCancelled(true);
                        event.setUseInteractedBlock(Result.DENY);
                    }
                }
            }
        }
    }
}
