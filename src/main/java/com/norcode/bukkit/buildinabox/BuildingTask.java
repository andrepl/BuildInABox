package com.norcode.bukkit.buildinabox;

import java.util.HashMap;

import org.bukkit.Location;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public abstract class BuildingTask implements Runnable {
    BlockVector cursor = new BlockVector(-1,0,0);
    CuboidClipboard clipboard;
    HashMap<BlockVector, BaseBlock> replacedBlocks = new HashMap<BlockVector, BaseBlock>();
    Location worldCursor;
    BlockVector origin;
    public BuildingTask(BuildChest buildChest, CuboidClipboard clipboard) {
        this.clipboard = clipboard;
        Location cl = buildChest.getLocation();
        Vector off = clipboard.getOffset();
        origin = new BlockVector(cl.getBlockX() + off.getBlockX(), cl.getBlockY() + off.getBlockY(), cl.getBlockZ() + off.getBlockZ());
        worldCursor = new Location(buildChest.getBlock().getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    public abstract void run();
    boolean moveCursor() {
        int x = cursor.getBlockX();
        int y = cursor.getBlockY();
        int z = cursor.getBlockZ();
        x ++;
        if (x >= clipboard.getSize().getBlockX()) {
            x = 0;
            z ++;
            if (z >= clipboard.getSize().getBlockZ()) {
                z = 0;
                y ++;
                if (y >= clipboard.getSize().getBlockY()) {
                    return false;
                }
            }
        }
        cursor = new BlockVector(x,y,z);
        worldCursor.setX(origin.getBlockX() + x);
        worldCursor.setY(origin.getBlockY() + y);
        worldCursor.setZ(origin.getBlockZ() + z);
        return true;
    }

}
