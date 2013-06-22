package com.norcode.bukkit.buildinabox.landprotection;

import com.norcode.bukkit.buildinabox.BuildInABox;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import me.ryanhamshire.GriefPrevention.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GriefPreventionProvider implements ILandProtection {

    private BuildInABox biabPlugin;
    private GriefPrevention gpPlugin;
    public GriefPreventionProvider(BuildInABox biabPlugin) {
        this.biabPlugin = biabPlugin;
        this.gpPlugin = (GriefPrevention) biabPlugin.getServer().getPluginManager().getPlugin("GriefPrevention");
    }

    @Override
    public boolean playerCanBuild(Player player, Location min, Location max) {
        int y = max.getBlockY();
        for (int x=min.getBlockX();x<=max.getBlockX();x++) {
            for (int z=min.getBlockZ();z<=max.getBlockZ();z++) {
                if (!playerCanBuild(player, new Location(max.getWorld(), x, y, z))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean playerCanBuild(Player player, Location location) {
        PlayerData pd = gpPlugin.dataStore.getPlayerData(player.getName());
        Claim claim = gpPlugin.dataStore.getClaimAt(location, false, pd.lastClaim);
        pd.lastClaim = claim;
        if (claim != null) {
            return claim.allowBuild(player) == null;
        }
        return true;
    }
}
