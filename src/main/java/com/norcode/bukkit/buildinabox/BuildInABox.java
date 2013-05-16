package com.norcode.bukkit.buildinabox;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import net.h31ix.updater.Updater;
import net.h31ix.updater.Updater.UpdateType;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.norcode.bukkit.buildinabox.BuildChest.UnlockingTask;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;

public class BuildInABox extends JavaPlugin implements Listener {

    private HashMap<String, BuildingPlan> buildingPlans = new HashMap<String, BuildingPlan>();
    private DataStore buildingChests = null;
    private Updater updater = null;
    private long lastClicked = -1;
    private Action lastClickType = null;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        doUpdater();
        new File(getDataFolder(), "schematics").mkdir();
        loadBuildingPlans();
        buildingChests = new DataStore(this, "data.yml");
        buildingChests.reload();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getDataStore().saveConfig();
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerLogin(PlayerLoginEvent event) {

        if (event.getPlayer().hasPermission("biab.admin")) {
            final String playerName = event.getPlayer().getName();
            getServer().getScheduler().runTaskLaterAsynchronously(this, new Runnable() {
                public void run() {
                    Player player = getServer().getPlayer(playerName);
                    if (player != null && player.isOnline()) {
                        getLogger().info("Updater Result: " + updater.getResult());
                        switch (updater.getResult()) {
                        case UPDATE_AVAILABLE:
                            player.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.WHITE + "An update is available at: http://dev.bukkit.org/server-mods/build-in-a-box/");
                            break;
                        case SUCCESS:
                            player.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.WHITE + "An update has been downloaded and will take effect when the server restarts.");
                            break;
                        }
                    }
                }
            }, 20);
        }
    }

    public void doUpdater() {
        String autoUpdate = getConfig().getString("auto-update", "notify-only").toLowerCase();
        if (autoUpdate.equals("true")) {
            updater = new Updater(this, "build-in-a-box", this.getFile(), UpdateType.DEFAULT, true);
        } else if (autoUpdate.equals("false")) {
            getLogger().info("Auto-updater is disabled.  Skipping check.");
        } else {
            updater = new Updater(this, "build-in-a-box", this.getFile(), UpdateType.NO_DOWNLOAD, true);
        }
    }

    public BuildingPlan getPlan(String name) {
        return buildingPlans.get(name.toLowerCase());
    }

    public WorldEditPlugin getWorldEdit() {
        return (WorldEditPlugin) this.getServer().getPluginManager().getPlugin("WorldEdit");
    }

    public void registerPlan(String name, BuildingPlan plan) {
        buildingPlans.put(name.toLowerCase(), plan);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command,
            String label, String[] args) {
        String action = args[0].toLowerCase();
        Player player = (Player) sender;
        String buildingName;
        if (action.equalsIgnoreCase("save")) {
            if (!sender.hasPermission("biab.save")) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            }
            buildingName = args[1];
            BuildingPlan plan = BuildingPlan.fromClipboard(this, player, buildingName);
            if (plan == null) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Error saving schematic.");
            } else {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GREEN + "Building plan " + ChatColor.WHITE + plan.getName() + ChatColor.GREEN + " saved!");
                saveConfig();
            }
            return true;
        } else if (action.equalsIgnoreCase("list")) {
            if (!sender.hasPermission("biab.list")) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            }

            for (BuildingPlan plan: buildingPlans.values()) {
                sender.sendMessage(ChatColor.GOLD + " * " + ChatColor.WHITE + plan.getName());
            }
            return true;
        } else if (action.equalsIgnoreCase("give")) {
            if (!sender.hasPermission("biab.give")) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            }
            Player targetPlayer = null;
            if (args.length == 3) {
                List<Player> playerMatches = getServer().matchPlayer(args[1]);
                if (playerMatches.size() == 0) {
                    sender.sendMessage("Unknown Player: " + args[1]);
                } else if (playerMatches.size() > 1) {
                    sender.sendMessage("Ambiguous Player name: " + args[1]);
                } else {
                    targetPlayer = playerMatches.get(0);
                }
                buildingName = args[2];
            } else {
                if (!(sender instanceof Player)) {
                    sender.sendMessage("This command must be run by a player, or target a player.");
                    return true;
                }
                buildingName = args[1];
                targetPlayer = (Player) sender;
            }
            ItemStack chest = new ItemStack(Material.ENDER_CHEST);
            ItemMeta meta = getServer().getItemFactory().getItemMeta(chest.getType());
            BuildingPlan plan = buildingPlans.get(buildingName);
            if (plan != null) {
                meta.setDisplayName(plan.getName());
                ArrayList<String> lore = new ArrayList<String>();
                lore.add("Build-in-a-Box");
                meta.setLore(lore);
                chest.setItemMeta(meta);
                HashMap<Integer, ItemStack> wontFit = targetPlayer.getInventory().addItem(chest);
                if (wontFit.size() > 0) {
                    targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), chest);
                }
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GREEN + "Gave " + ChatColor.WHITE + plan.getName() + ChatColor.GREEN + " to " + targetPlayer.getName());
            } else {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unknown building plan: '" + args[0] + "'");
            }
            return true;
        }
        return false;
    }

    public void debug(String s) {
        getLogger().info(s);
    }

    public void loadBuildingPlans() {
        buildingPlans.clear();
        ConfigurationSection cfg = getConfig().getConfigurationSection("buildings");
        if (cfg != null) {
            for (String filename: cfg.getKeys(false)) {
                debug("Loading Schematic: " + filename);
                buildingPlans.put(filename.toLowerCase(), new BuildingPlan(this, cfg.getConfigurationSection(filename)));
            }
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getClickedBlock().getType().equals(Material.ENDER_CHEST)) {
            if (event.getClickedBlock().hasMetadata("buildInABox")) {
                BuildChest bc = (BuildChest) event.getClickedBlock().getMetadata("buildInABox").get(0).value();
                if (bc.isPreviewing()) {
                    if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                        // Cancel
                        bc.endPreview(event.getPlayer());
                    } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                        // build
                        bc.build(event.getPlayer());
                    }
                } else {
                    long now = System.currentTimeMillis();
                    if (bc.isLocking()) {
                        if (!bc.getLockingTask().lockingPlayer.equals(event.getPlayer().getName())) {
                            event.getPlayer().sendMessage(bc.getLockingTask().lockingPlayer + "'s " + ((bc.getLockingTask() instanceof UnlockingTask) ? "unlock" : "locking") + " attempt was cancelled.");
                        }
                        bc.getLockingTask().cancel();
                    } else if (now - lastClicked < 2000 && lastClickType.equals(event.getAction())) {
                        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
                            // pick up
                            if (!bc.isLocked()) {
                                bc.pickup(event.getPlayer());
                            }
                        } else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
                            // lock/unlock
                            if (bc.isLocked()) {
                                bc.unlock(event.getPlayer());
                            } else {
                                bc.lock(event.getPlayer());
                            }
                        }
                        lastClicked = -1;
                        lastClickType = null;
                    } else {
                        lastClicked = now;
                        lastClickType = event.getAction();
                        event.getPlayer().sendMessage(bc.getDescription());
                    }
                }
                event.setCancelled(true);
                event.setUseInteractedBlock(Result.DENY);
                event.setUseItemInHand(Result.DENY);
            }
        }
    }

    @EventHandler(ignoreCancelled=true, priority=EventPriority.HIGHEST)
    public void onBlockBreak(final BlockBreakEvent event) {
        if (event.getBlock().hasMetadata("biab-block") || (event.getBlock().getType().equals(Material.ENDER_CHEST) && event.getBlock().hasMetadata("buildInABox"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled=true)
    public void onBlockPlace(final BlockPlaceEvent event) {
        if (event.getItemInHand().getType().equals(Material.ENDER_CHEST)) {
            ItemMeta meta = event.getItemInHand().getItemMeta();
            if (meta != null && meta.hasLore() && meta.getLore().contains("Build-in-a-Box")) {
                String buildingName = meta.getDisplayName();
                final BuildingPlan plan = buildingPlans.get(buildingName);
                if (plan == null) {
                    event.getPlayer().sendMessage("Unknown Building plan: " + buildingName);
                    return;
                }
                final BuildChest bc = buildingChests.addChest(event.getBlock(), plan, null);
                getServer().getScheduler().runTaskLater(this, new Runnable() {
                    public void run() {
                        if (event.getPlayer().isOnline()) {
                            bc.preview(event.getPlayer());
                        }
                    }
                }, 1);
            }
        }
    }
    
    public DataStore getDataStore() {
        return buildingChests;
    }
}
