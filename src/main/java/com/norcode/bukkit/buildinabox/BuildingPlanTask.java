package com.norcode.bukkit.buildinabox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.minecraft.server.v1_5_R3.Packet61WorldEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.v1_5_R3.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.scheduler.BukkitTask;

import com.norcode.bukkit.buildinabox.util.RandomFireworksGenerator;
import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldedit.CuboidClipboard;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;

public abstract class BuildingPlanTask implements Runnable {

    public static final HashSet<Integer> postLayerBlockIds = new HashSet<Integer>();
    public static final HashSet<Integer> postBuildBlockIds = new HashSet<Integer>();
    public final List<BuildingPlanTask.BlockUpdate> postLayerQueue = new ArrayList<BuildingPlanTask.BlockUpdate>();
    public final List<BuildingPlanTask.BlockUpdate> postBuildQueue = new ArrayList<BuildingPlanTask.BlockUpdate>();
    private BukkitTask task;
    static {
        // These are placed after each vertical layer is complete.
        postLayerBlockIds.add(Material.BED_BLOCK.getId());
        postLayerBlockIds.add(Material.COCOA.getId());
        postLayerBlockIds.add(Material.TORCH.getId());
        postLayerBlockIds.add(Material.REDSTONE_TORCH_ON.getId());
        postLayerBlockIds.add(Material.REDSTONE_TORCH_OFF.getId());
        postLayerBlockIds.add(Material.WALL_SIGN.getId());
        postLayerBlockIds.add(Material.WOOD_BUTTON.getId());
        postLayerBlockIds.add(Material.STONE_BUTTON.getId());
        postLayerBlockIds.add(Material.TRAP_DOOR.getId());
        postLayerBlockIds.add(Material.TRIPWIRE_HOOK.getId());
        // These are placed VERY last.
        postBuildBlockIds.add(Material.VINE.getId());
        postBuildBlockIds.add(Material.WOODEN_DOOR.getId());
        postBuildBlockIds.add(Material.IRON_DOOR.getId());
        postBuildBlockIds.add(Material.LEVER.getId());
        
    }

    CuboidClipboard clipboard;
    // location in the clipboard.
    BlockVector cursor;
    // ground-blocks replaced by the build and restored when it finishes
    HashMap<BlockVector, BaseBlock> replacedBlocks = new HashMap<BlockVector, BaseBlock>();
    // a Location in the world matching the cursor
    Location worldCursor;
    // the world coordinates of the lower limit of the schematic.
    BlockVector origin;
    // a list of xz pairs 
    ArrayList<BlockVector> xzPoints;
    // the current position in the above list.
    int ptr = -1;
    // TOP_DOWN or BOTTOM_UP
    BlockFace buildDirection = BlockFace.SELF;
    // whether or not to shuffle the list of xz pairs between layers.
    boolean shuffle;
    private List<Player> nearbyPlayers;

    private BuildChest buildChest;
    private BuildInABox plugin;
    private int blocksPerTick;

    public BuildingPlanTask(BuildChest buildChest, CuboidClipboard clipboard, BlockFace buildDirection, int blocksPerTick, boolean shuffle) {
        this.plugin = BuildInABox.getInstance();
        this.buildChest = buildChest;
        this.buildDirection = buildDirection;
        this.clipboard = clipboard;
        this.shuffle = shuffle;
        this.blocksPerTick = blocksPerTick;
        Location cl = buildChest.getLocation();
        Vector off = clipboard.getOffset();
        origin = new BlockVector(cl.getBlockX() + off.getBlockX(), cl.getBlockY() + off.getBlockY(), cl.getBlockZ() + off.getBlockZ());
    }

    private void layerComplete(int finishedLayerY) {
        for (int i=0;i<postLayerQueue.size();i++) {
            processBlockUpdate(postLayerQueue.get(i), false);
        }
        postLayerQueue.clear();
        onLayerComplete(finishedLayerY);
    };

    public void onLayerComplete(int finishedLayerY) {};

    private void complete() {
        for (int i=0;i<postBuildQueue.size();i++) {
            processBlockUpdate(postBuildQueue.get(i), false);
        }
        postBuildQueue.clear();
        onComplete();
    };

    public void onComplete() {};

    public void start() {
        // initialize clipboard and world cursor positions.
        this.cursor = new BlockVector(0, (buildDirection == BlockFace.DOWN ? this.clipboard.getSize().getBlockY()-1:0), 0);
        worldCursor = new Location(buildChest.getBlock().getWorld(), origin.getBlockX(), origin.getBlockY(), origin.getBlockZ());
        // setup the xz coord list
        ptr = -1;
        xzPoints = new ArrayList<BlockVector>(clipboard.getSize().getBlockX() * clipboard.getSize().getBlockZ());
        for (int x=0; x<this.clipboard.getSize().getBlockX();x++) {
            for (int z=0;z<this.clipboard.getSize().getBlockZ();z++) {
                xzPoints.add(new BlockVector(x,0,z));
            }
        }
        if (this.shuffle) {
            Collections.shuffle(xzPoints);
        }
        // Collect the nearby players for sending fake block breaks etc.
        nearbyPlayers.clear();
        double maxDistance = 16 + (Math.max(clipboard.getSize().getBlockX(), clipboard.getSize().getBlockZ())/2);
        Location center = new Location(
                worldCursor.getWorld(), 
                worldCursor.getX() + clipboard.getSize().getBlockX()/2,
                worldCursor.getY() + clipboard.getSize().getBlockY()/2,
                worldCursor.getZ() + clipboard.getSize().getBlockZ()/2);
        for (Player p: worldCursor.getWorld().getPlayers()) {
            if (p.getLocation().distance(center) < maxDistance) {
                nearbyPlayers.add(p);
            }
            nearbyPlayers.add(p);
        }
        plugin.getServer().getScheduler().runTaskLater(plugin, this,1);
    }

    /**
     * process the block at the current cursor position.
     * @return true if this should count against blocksPerTick
     */
    public abstract boolean processBlockUpdate(BlockUpdate blockUpdate, boolean canQueue);

    public void run() {
        boolean finished = false;
        for (int i=0;i<blocksPerTick;i++) {
            if (moveCursor()) {
                if (!processBlockUpdate(new BlockUpdate(cursor, clipboard.getPoint(cursor)), true)) {
                    i--;
                    continue;
                }
            } else {
                finished = true;
                break;
            }
        }
        if (!finished) {
            plugin.getServer().getScheduler().runTaskLater(plugin, this, 1);
        }
    }

    public Location getWorldLocationFor(BlockVector v) {
        return new Location(worldCursor.getWorld(), 
                origin.getBlockX() + v.getBlockX(), 
                origin.getBlockY() + v.getBlockY(), 
                origin.getBlockZ() + v.getBlockZ());
    }

    /*
     * moves the cursor to the next position in the clipboard to be processed.
     */
    boolean moveCursor() {
        int y = cursor.getBlockY();
        ptr++;
        if (ptr >= xzPoints.size()) {
            ptr = 0;
            y += buildDirection.getModY();
            layerComplete(y-buildDirection.getModY());
            if (y<0|y>=clipboard.getSize().getBlockY()) {
                complete();
                return false;
            }
            if (this.shuffle) {
                Collections.shuffle(xzPoints);
            }
        }
        BlockVector v = xzPoints.get(ptr);
        cursor = new BlockVector(v.getBlockX(),y,v.getBlockZ());
        return true;
    }

    protected void launchFireworks(int fireworksLevel) {
        for (int i=0;i<fireworksLevel;i++) {
            BuildInABox.getInstance().getServer().getScheduler().runTaskLater(BuildInABox.getInstance(), new Runnable() {
                public void run() {
                    BlockVector vec;
                    Firework fw;
                    for (int ptc=0;ptc<20&&ptc<xzPoints.size();ptc++) {
                        vec = xzPoints.get(ptc);
                        worldCursor.setZ(origin.getBlockZ() + vec.getBlockZ());
                        worldCursor.setX(origin.getBlockX() + vec.getBlockX());
                        fw = (Firework) worldCursor.getWorld().spawnEntity(worldCursor, EntityType.FIREWORK);
                        RandomFireworksGenerator.assignRandomFireworkMeta(fw);
                    }
                }
            }, i*10);
        }
    }

    public void copyFromClipboard(BaseBlock bb, Player attributeToPlayer) {
        // Send a BlockPlace event for loggers to rollback maybe.
        BlockState bs = worldCursor.getBlock().getState();
        BlockPlaceEvent bpe = new BlockPlaceEvent(worldCursor.getBlock(), bs, 
                worldCursor.getBlock().getRelative(BlockFace.DOWN), null, attributeToPlayer, true);
        plugin.getServer().getPluginManager().callEvent(bpe);
        worldCursor.getBlock().setTypeIdAndData(bb.getType(), (byte) bb.getData(), true);
        
    }

    public void sendAnimationPacket(int x, int y, int z, int type) {
        Packet61WorldEvent packet = new Packet61WorldEvent(2001, x, y, z, type, false);
        for (Player p: nearbyPlayers) {
            if (p.isOnline()) {
                ((CraftPlayer) p).getHandle().playerConnection.sendPacket(packet);
            }
        }
    }

    public static class BlockUpdate {
        private BlockVector vector;
        private BaseBlock baseBlock;
        public BlockUpdate(BlockVector vector, BaseBlock baseBlock) {
            this.vector = vector;
            this.baseBlock = baseBlock;
        }
        public BlockVector getVector() {
            return vector;
        }
        public void setVector(BlockVector vector) {
            this.vector = vector;
        }
        public BaseBlock getBlock() {
            return baseBlock;
        }
        public void setBlock(BaseBlock baseBlock) {
            this.baseBlock = baseBlock;
        }
    }

}
