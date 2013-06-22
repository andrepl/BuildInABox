package com.norcode.bukkit.buildinabox.landprotection;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.norcode.bukkit.griefprevention.GriefPreventionTNG;
import com.norcode.bukkit.griefprevention.data.Claim;
import com.norcode.bukkit.griefprevention.data.PlayerData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class GPTNGProvider implements ILandProtection {

    private BuildInABox biabPlugin;
    private GriefPreventionTNG gpPlugin;

    public GPTNGProvider(BuildInABox biabPlugin) {
        this.biabPlugin = biabPlugin;
        this.gpPlugin = (GriefPreventionTNG) biabPlugin.getServer().getPluginManager().getPlugin("GriefPreventionTNG");
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
        PlayerData pd = gpPlugin.getDataStore().getPlayerData(player.getName());
        Claim claim = gpPlugin.getDataStore().getClaimAt(location, false, pd.getLastClaim());
        pd.setLastClaim(claim);
        if (claim != null) {
            return claim.allowBuild(player) == null;
        }
        return true;
    }
}
