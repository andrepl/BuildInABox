package com.norcode.bukkit.buildinabox;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BIABCommandExecutor implements CommandExecutor {
    BuildInABox plugin;
    public BIABCommandExecutor(BuildInABox buildInABox) {
        this.plugin = buildInABox;
    }

    public boolean onCommand(CommandSender sender, Command command, String alias, String[] params) {
        LinkedList<String> args = new LinkedList<String>(Arrays.asList(params));
        if (args.size() == 0) {
            return false;
        }
        String action = args.pop().toLowerCase();
        if (action.equals("give")) {
            cmdGive(sender, args);
            return true;
        } else if (action.equals("save")) {
            cmdSave(sender, args);
            return true;
        }
        return true;
    }
    public void cmdSave(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            return;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "This command cannot be run from the console.");
            return;
        }
        String buildingName = args.pop();
        BuildingPlan plan = BuildingPlan.fromClipboard(plugin, (Player) sender, buildingName);
        if (plan != null) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.GREEN + "Building plan " + ChatColor.WHITE + plan.getName() + ChatColor.GREEN + " saved!");
            plugin.getDataStore().saveBuildingPlan(plan);
        }
    }

    public void cmdGive(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.give")) {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "You don't have permission to do that.");
            return;
        }
        Player targetPlayer = null;
        if (args.size() >= 2) {
            String name = args.pop();
            List<Player> matches = plugin.getServer().matchPlayer(name);
            if (matches.size() == 1) {
                targetPlayer = matches.get(0);
            } else {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unknown Player: " + name);
                return;
            }
        }
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Cannot give a building to the console.");
                return;
            } else {
                targetPlayer = (Player) sender;
            }
        }
        if (args.size() == 1) {
            String planName = args.pop().toLowerCase();
            BuildingPlan plan = plugin.getDataStore().getBuildingPlan(planName);
            if (plan == null) {
                sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.RED + "Unknown Building Plan: " + planName + ".");
                return;
            }
            ChestData data = plugin.getDataStore().createChest(plan.getName());
            ItemStack stack = data.toItemStack();
            if (targetPlayer.getInventory().addItem(stack).size() > 0) {
                targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), stack);
            }
        } else {
            sender.sendMessage(ChatColor.GOLD + "[Build-in-a-Box] " + ChatColor.WHITE + "Usage: /biab give [<player>] <plan>");
        }
    }
}
