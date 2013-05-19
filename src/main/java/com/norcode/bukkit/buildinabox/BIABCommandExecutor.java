package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class BIABCommandExecutor implements TabExecutor {
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
        } else if (action.equals("list")) {
            cmdList(sender, args);
            return true;
        }
        sender.sendMessage(BuildInABox.getErrorMsg("unexpected-argument", action));
        return true;
    }
    private void cmdList(CommandSender sender, LinkedList<String> args) {
        int page = 1;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.peek());
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(BuildInABox.getErrorMsg("invalid-page", args.peek()));
                return;
            }
        }
        int numPages = (int) Math.ceil(plugin.getDataStore().getAllBuildingPlans().size() / 8.0f);
        if (numPages == 0) {
            sender.sendMessage(BuildInABox.getNormalMsg("no-building-plans"));
            return;
        }
        List<BuildingPlan> plans = new ArrayList<BuildingPlan>(plugin.getDataStore().getAllBuildingPlans());
        List<String> lines = new ArrayList<String>();
        lines.add(BuildInABox.getNormalMsg("available-building-plans", page, numPages));
        for (int i=8*(page-1);i<8*(page);i++) {
            if (i<plans.size()) {
                lines.add(ChatColor.GOLD + " * " + ChatColor.GRAY + plans.get(i).getName());
            }
        }
        sender.sendMessage(lines.toArray(new String[lines.size()]));
    }

    public void cmdSave(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.save")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        } else if (!(sender instanceof Player)) {
            sender.sendMessage(BuildInABox.getErrorMsg("cannot-use-from-console"));
            return;
        }
        String buildingName = args.pop();
        BuildingPlan plan = BuildingPlan.fromClipboard(plugin, (Player) sender, buildingName);
        if (plan != null) {
            sender.sendMessage(BuildInABox.getSuccessMsg("building-plan-saved", buildingName));
            plugin.getDataStore().saveBuildingPlan(plan);
        }
    }

    public void cmdGive(CommandSender sender, LinkedList<String> args) {
        if (!sender.hasPermission("biab.give")) {
            sender.sendMessage(BuildInABox.getErrorMsg("no-permission"));
            return;
        }
        Player targetPlayer = null;
        if (args.size() >= 2) {
            String name = args.pop();
            List<Player> matches = plugin.getServer().matchPlayer(name);
            if (matches.size() == 1) {
                targetPlayer = matches.get(0);
            } else {
                sender.sendMessage(BuildInABox.getErrorMsg("unknown-player", name));
                return;
            }
        }
        if (targetPlayer == null) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(BuildInABox.getErrorMsg("cannot-give-to-console"));
                return;
            } else {
                targetPlayer = (Player) sender;
            }
        }
        if (args.size() == 1) {
            String planName = args.pop().toLowerCase();
            BuildingPlan plan = plugin.getDataStore().getBuildingPlan(planName);
            if (plan == null) {
                sender.sendMessage(BuildInABox.getErrorMsg("unknown-building-plan", planName));
                return;
            }
            ChestData data = plugin.getDataStore().createChest(plan.getName());
            ItemStack stack = data.toItemStack();
            if (targetPlayer.getInventory().addItem(stack).size() > 0) {
                targetPlayer.getWorld().dropItem(targetPlayer.getLocation(), stack);
            }
        } else {
            sender.sendMessage(BuildInABox.getNormalMsg("cmd-give-usage"));
        }
    }
//
//    @Override
//    public List<String> onTabComplete(CommandSender sender, Command command,
//            String label, String[] params) {
//        BuildInABox.getInstance().debug("TAB:" + args);
//        return new ArrayList<String>();
//    }

    public List<String> onTabComplete(CommandSender sender, Command cmd,
            String label, String[] params) {
      LinkedList<String> args = new LinkedList<String>(Arrays.asList(params));
      LinkedList<String> results = new LinkedList<String>();
      String action = null;
      if (args.size() >= 1) {
          action = args.pop().toLowerCase();
      } else {
          return results;
      }
      
      if (args.size() == 0) {
          if ("list".startsWith(action)) {
              results.add("list");
          } 
          if ("save".startsWith(action) && sender.hasPermission("biab.save")) {
              results.add("save");
          }
          if ("give".startsWith(action) && sender.hasPermission("biab.give")) {
              results.add("give");
          }
      } else if (args.size() == 1) {
          if (action.equals("save") || action.equals("give")) {
              if (sender.hasPermission("biab." + action)) {
                  for (BuildingPlan plan: plugin.getDataStore().getAllBuildingPlans()) {
                      if (plan.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                          results.add(plan.getName());
                      }
                  }
                  if (action.equals("give")) {
                      for (Player p: plugin.getServer().getOnlinePlayers()) {
                          if (p.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                              results.add(p.getName());
                          }
                      }
                  }
              }
          }
      } else if (args.size() == 2) {
          if (action.equals("give")) {
              for (BuildingPlan plan: plugin.getDataStore().getAllBuildingPlans()) {
                  if (plan.getName().toLowerCase().startsWith(args.peek().toLowerCase())) {
                      results.add(plan.getName());
                  }
              }
          }
      }
      return results;
    }
}
