package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.Collections;
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
    ArrayList<BlockVector> xyPoints;
    int ptr = -1;
    public BuildingTask(BuildChest buildChest, CuboidClipboard clipboard) {
        this.clipboard = clipboard;
        xyPoints = new ArrayList<BlockVector>(clipboard.getSize().getBlockX() * clipboard.getSize().getBlockY());
        for (int x=0; x<this.clipboard.getSize().getBlockX();x++) {
            for (int z=0;z<this.clipboard.getSize().getBlockZ();z++) {
                xyPoints.add(new BlockVector(x,0,z));
            }
        }
        Location cl = buildChest.getLocation();
        Vector off = clipboard.getOffset();
        origin = new BlockVector(cl.getBlockX() + off.getBlockX(), cl.getBlockY() + off.getBlockY(), cl.getBlockZ() + off.getBlockZ());
        worldCursor = new Location(buildChest.getBlock().getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
    }

    public abstract void run();
    boolean moveCursor() {
        int y = cursor.getBlockY();
        ptr++;
        if (ptr >= xyPoints.size()) {
            ptr = 0;
            y++;
            if (y >= clipboard.getSize().getBlockY()) {
                return false;
            }
            Collections.shuffle(xyPoints);
        }
        BlockVector v = xyPoints.get(ptr);
        cursor = new BlockVector(v.getBlockX(),y,v.getBlockZ());
        worldCursor.setX(origin.getBlockX() + v.getBlockX());
        worldCursor.setY(origin.getBlockY() + y);
        worldCursor.setZ(origin.getBlockZ() + v.getBlockZ());
        return true;
    }

}
