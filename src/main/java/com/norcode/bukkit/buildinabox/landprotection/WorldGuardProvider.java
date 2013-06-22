package com.norcode.bukkit.buildinabox.landprotection;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuardProvider implements ILandProtection {

    private BuildInABox biabPlugin;
    private WorldGuardPlugin wgPlugin;

    public WorldGuardProvider(BuildInABox biabPlugin) {
        this.biabPlugin = biabPlugin;
        this.wgPlugin = (WorldGuardPlugin) biabPlugin.getServer().getPluginManager().getPlugin("WorldGuard");
    }

    @Override
    public boolean playerCanBuild(Player player, Location min, Location max) {
        ProtectedRegion region = new ProtectedCuboidRegion(null, new BlockVector(min.getBlockX(), min.getBlockY(), min.getBlockZ()), new BlockVector(max.getBlockX(), max.getBlockY(), max.getBlockZ()));
        return wgPlugin.getRegionManager(max.getWorld()).overlapsUnownedRegion(region, wgPlugin.wrapPlayer(player));
    }

    @Override
    public boolean playerCanBuild(Player player, Location location) {
        return wgPlugin.canBuild(player, location);
    }
}
