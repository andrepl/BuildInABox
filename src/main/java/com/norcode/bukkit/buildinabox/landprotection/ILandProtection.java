package com.norcode.bukkit.buildinabox.landprotection;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface ILandProtection {
    public boolean playerCanBuild(Player player, Location min, Location max);
    public boolean playerCanBuild(Player player, Location location);
}
