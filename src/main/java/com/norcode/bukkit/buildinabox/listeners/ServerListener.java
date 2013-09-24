package com.norcode.bukkit.buildinabox.listeners;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.buildinabox.landprotection.GriefPreventionProvider;
import com.norcode.bukkit.buildinabox.landprotection.WorldGuardProvider;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;

public class ServerListener implements Listener {
    BuildInABox plugin;

    public ServerListener(BuildInABox plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void vaultEnabled(PluginEnableEvent event) {
        if (event.getPlugin() != null && event.getPlugin().getName().equalsIgnoreCase("Vault")) {
            plugin.enableEconomy();
        } else if (event.getPlugin() != null && (event.getPlugin().getName().equalsIgnoreCase("NoCheatPlus") || event.getPlugin().getName().equalsIgnoreCase("AntiCheat"))) {
            plugin.setupAntiCheat();
        }
    }

    @EventHandler
    public void vaultDisabled(PluginDisableEvent event) {
        if (event.getPlugin() != null && event.getPlugin().getName().equalsIgnoreCase("Vault")) {
            plugin.disableEconomy();
        } else if (event.getPlugin() != null && (event.getPlugin().getName().equalsIgnoreCase("NoCheatPlus") || event.getPlugin().getName().equalsIgnoreCase("AntiCheat"))) {
            plugin.setupAntiCheat();
        }
    }

    @EventHandler
    public void landProtectionEnabled(PluginEnableEvent event) {
        String name = event.getPlugin().getName();
        if (!plugin.getLandProtection().containsKey(name)) {
            if (name.equals("GriefPrevention")) {
                plugin.getLandProtection().put(name, new GriefPreventionProvider(plugin));
            } else if (name.equals("WorldGuard")) {
                plugin.getLandProtection().put(name, new WorldGuardProvider(plugin));
            }
        }
    }

    public void landProtectionDisabled(PluginDisableEvent event) {
        String name = event.getPlugin().getName();
        if (plugin.getLandProtection().containsKey(name)) {
            plugin.getLandProtection().remove(name);
        }
    }
}
