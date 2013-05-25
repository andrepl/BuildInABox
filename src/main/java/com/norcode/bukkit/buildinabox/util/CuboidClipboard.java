// $Id$
/* 
 * Copyright (C) 2010 sk89q <http://www.sk89q.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 * *******************************************************************
 * This file was taken and modified from WorldEdit to ensure correct
 * Block rotate in all cases, independent of the installed WorldEdit
 * Version.
 * 
*/

package com.norcode.bukkit.buildinabox.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.BlockFace;

import com.norcode.bukkit.buildinabox.BuildInABox;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.LocalEntity;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.FurnaceBlock;
import com.sk89q.worldedit.blocks.SkullBlock;

/**
 * The clipboard remembers the state of a cuboid region.
 *
 * @author sk89q
 */
public class CuboidClipboard {
    /**
     * Flip direction.
     */
    public enum FlipDirection {
        NORTH_SOUTH,
        WEST_EAST,
        UP_DOWN
    }

    private BaseBlock[][][] data;
    private Vector offset;
    private Vector origin;
    private Vector size;
    private List<CopiedEntity> entities = new ArrayList<CopiedEntity>();

    // For cloning WorldEdit clipboard.
    private static Field dataField;
    private static Field offsetField;
    private static Field originField;
    private static Field sizeField;
    private static Field entitiesField;
    
    static {
        try {
            dataField = com.sk89q.worldedit.CuboidClipboard.class.getDeclaredField("data");
            dataField.setAccessible(true);
            offsetField = com.sk89q.worldedit.CuboidClipboard.class.getDeclaredField("offset");
            offsetField.setAccessible(true);
            originField = com.sk89q.worldedit.CuboidClipboard.class.getDeclaredField("origin");
            originField.setAccessible(true);
            sizeField = com.sk89q.worldedit.CuboidClipboard.class.getDeclaredField("size");
            sizeField.setAccessible(true);
            entitiesField = com.sk89q.worldedit.CuboidClipboard.class.getDeclaredField("entities");
            entitiesField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } 
    }

    public CuboidClipboard(com.sk89q.worldedit.CuboidClipboard wecb) {
        try {
            this.data = (BaseBlock[][][]) dataField.get(wecb);
            this.offset = (Vector) offsetField.get(wecb);
            this.origin = (Vector) originField.get(wecb);
            this.size = (Vector) sizeField.get(wecb);
            this.entities = (List<CopiedEntity>) entitiesField.get(wecb);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructs the clipboard.
     *
     * @param size
     */
    public CuboidClipboard(Vector size) {
        this.size = size;
        data = new BaseBlock[size.getBlockX()][size.getBlockY()][size.getBlockZ()];
        origin = new Vector();
        offset = new Vector();
    }

    /**
     * Constructs the clipboard.
     *
     * @param size
     * @param origin
     */
    public CuboidClipboard(Vector size, Vector origin) {
        this.size = size;
        data = new BaseBlock[size.getBlockX()][size.getBlockY()][size.getBlockZ()];
        this.origin = origin;
        offset = new Vector();
    }

    /**
     * Constructs the clipboard.
     *
     * @param size
     * @param origin
     * @param offset
     */
    public CuboidClipboard(Vector size, Vector origin, Vector offset) {
        this.size = size;
        data = new BaseBlock[size.getBlockX()][size.getBlockY()][size.getBlockZ()];
        this.origin = origin;
        this.offset = offset;
    }

    /**
     * Get the width (X-direction) of the clipboard.
     *
     * @return width
     */
    public int getWidth() {
        return size.getBlockX();
    }

    /**
     * Get the length (Z-direction) of the clipboard.
     *
     * @return length
     */
    public int getLength() {
        return size.getBlockZ();
    }

    /**
     * Get the height (Y-direction) of the clipboard.
     *
     * @return height
     */
    public int getHeight() {
        return size.getBlockY();
    }

    /**
     * Rotate the clipboard in 2D. It can only rotate by angles divisible by 90.
     *
     * @param angle in degrees
     */
    public void rotate2D(int angle) {
        angle = angle % 360;
        if (angle < 0 || angle % 90 != 0) { // Can only rotate 90 degrees at the moment
            return;
        }
        int numRotations = Math.abs((int) Math.floor(angle / 90.0));

        int width = getWidth();
        int length = getLength();
        int height = getHeight();
        Vector sizeRotated = size.transform2D(angle, 0, 0, 0, 0);
        int shiftX = sizeRotated.getX() < 0 ? -sizeRotated.getBlockX() - 1 : 0;
        int shiftZ = sizeRotated.getZ() < 0 ? -sizeRotated.getBlockZ() - 1 : 0;

        BaseBlock newData[][][] = new BaseBlock
                [Math.abs(sizeRotated.getBlockX())]
                [Math.abs(sizeRotated.getBlockY())]
                [Math.abs(sizeRotated.getBlockZ())];

        for (int x = 0; x < width; ++x) {
            for (int z = 0; z < length; ++z) {
                Vector v = (new Vector(x, 0, z)).transform2D(angle, 0, 0, 0, 0);
                int newX = v.getBlockX();
                int newZ = v.getBlockZ();
                for (int y = 0; y < height; ++y) {
                    BaseBlock block = data[x][y][z];
                    newData[shiftX + newX][y][shiftZ + newZ] = block;

                    for (int i = 0; i < numRotations; ++i) {
                        rotateBlock90(block);
                    }
                }
            }
        }

        data = newData;
        size = new Vector(Math.abs(sizeRotated.getBlockX()),
                          Math.abs(sizeRotated.getBlockY()),
                          Math.abs(sizeRotated.getBlockZ()));
        offset = offset.transform2D(angle, 0, 0, 0, 0)
                .subtract(shiftX, 0, shiftZ);
    }
    public static int rotate90(int type, int data) {
        switch (type) {
        case BlockID.TORCH:
        case BlockID.REDSTONE_TORCH_OFF:
        case BlockID.REDSTONE_TORCH_ON:
            switch (data) {
            case 1: return 3;
            case 2: return 4;
            case 3: return 2;
            case 4: return 1;
            }
            break;

        case BlockID.MINECART_TRACKS:
            switch (data) {
            case 6: return 7;
            case 7: return 8;
            case 8: return 9;
            case 9: return 6;
            }
            /* FALL-THROUGH */

        case BlockID.POWERED_RAIL:
        case BlockID.DETECTOR_RAIL:
        case BlockID.ACTIVATOR_RAIL:
            switch (data & 0x7) {
            case 0: return 1 | (data & ~0x7);
            case 1: return 0 | (data & ~0x7);
            case 2: return 5 | (data & ~0x7);
            case 3: return 4 | (data & ~0x7);
            case 4: return 2 | (data & ~0x7);
            case 5: return 3 | (data & ~0x7);
            }
            break;

        case BlockID.WOODEN_STAIRS:
        case BlockID.COBBLESTONE_STAIRS:
        case BlockID.BRICK_STAIRS:
        case BlockID.STONE_BRICK_STAIRS:
        case BlockID.NETHER_BRICK_STAIRS:
        case BlockID.SANDSTONE_STAIRS:
        case BlockID.SPRUCE_WOOD_STAIRS:
        case BlockID.BIRCH_WOOD_STAIRS:
        case BlockID.JUNGLE_WOOD_STAIRS:
        case BlockID.QUARTZ_STAIRS:
            switch (data) {
            case 0: return 2;
            case 1: return 3;
            case 2: return 1;
            case 3: return 0;
            case 4: return 6;
            case 5: return 7;
            case 6: return 5;
            case 7: return 4;
            }
            break;

        case BlockID.LEVER:
        case BlockID.STONE_BUTTON:
        case BlockID.WOODEN_BUTTON:
            int thrown = data & 0x8;
            int withoutThrown = data & ~0x8;
            switch (withoutThrown) {
            case 1: return 3 | thrown;
            case 2: return 4 | thrown;
            case 3: return 2 | thrown;
            case 4: return 1 | thrown;
            case 5: return 6 | thrown;
            case 6: return 5 | thrown;
            case 7: return 0 | thrown;
            case 0: return 7 | thrown;
            }
            break;

        case BlockID.WOODEN_DOOR:
        case BlockID.IRON_DOOR:
            if ((data & 0x8) == 8) {
                return data;
            }
            int doorExtra = data & ~0x3;
            int doorWithoutFlags = data & 0x3;
            switch (doorWithoutFlags) {
            case 0: return 1 | doorExtra;
            case 1: return 2 | doorExtra;
            case 2: return 3 | doorExtra;
            case 3: return 0 | doorExtra;
            }
            break;

        case BlockID.COCOA_PLANT:
        case BlockID.TRIPWIRE_HOOK:
            int extra = data & ~0x3;
            int withoutFlags = data & 0x3;
            switch (withoutFlags) {
            case 0: return 1 | extra;
            case 1: return 2 | extra;
            case 2: return 3 | extra;
            case 3: return 0 | extra;
            }
            break;

        case BlockID.SIGN_POST:
            return (data + 4) % 16;

        case BlockID.LADDER:
        case BlockID.WALL_SIGN:
        case BlockID.CHEST:
        case BlockID.FURNACE:
        case BlockID.BURNING_FURNACE:
        case BlockID.ENDER_CHEST:
        case BlockID.TRAPPED_CHEST:
        case BlockID.HOPPER:
            BuildInABox.getInstance().debug("hit furnace switch w/ data: " + data);
            switch (data) {
            case 2: return 5;
            case 3: return 4;
            case 4: return 2;
            case 5: return 3;
            }
            break;

        case BlockID.DISPENSER:
        case BlockID.DROPPER:
            int dispPower = data & 0x8;
            switch (data & ~0x8) {
            case 2: return 5 | dispPower;
            case 3: return 4 | dispPower;
            case 4: return 2 | dispPower;
            case 5: return 3 | dispPower;
            }
            break;

        case BlockID.PUMPKIN:
        case BlockID.JACKOLANTERN:
            switch (data) {
            case 0: return 1;
            case 1: return 2;
            case 2: return 3;
            case 3: return 0;
            }
            break;

        case BlockID.LOG:
            if (data >= 4 && data <= 11) data ^= 0xc;
            break;

        case BlockID.COMPARATOR_OFF:
        case BlockID.COMPARATOR_ON:
        case BlockID.REDSTONE_REPEATER_OFF:
        case BlockID.REDSTONE_REPEATER_ON:
            int dir = data & 0x03;
            int delay = data - dir;
            switch (dir) {
            case 0: return 1 | delay;
            case 1: return 2 | delay;
            case 2: return 3 | delay;
            case 3: return 0 | delay;
            }
            break;

        case BlockID.TRAP_DOOR:
            int withoutOrientation = data & ~0x3;
            int orientation = data & 0x3;
            switch (orientation) {
            case 0: return 3 | withoutOrientation;
            case 1: return 2 | withoutOrientation;
            case 2: return 0 | withoutOrientation;
            case 3: return 1 | withoutOrientation;
            }
            break;

        case BlockID.PISTON_BASE:
        case BlockID.PISTON_STICKY_BASE:
        case BlockID.PISTON_EXTENSION:
            final int rest = data & ~0x7;
            switch (data & 0x7) {
            case 2: return 5 | rest;
            case 3: return 4 | rest;
            case 4: return 2 | rest;
            case 5: return 3 | rest;
            }
            break;

        case BlockID.BROWN_MUSHROOM_CAP:
        case BlockID.RED_MUSHROOM_CAP:
            if (data >= 10) return data;
            return (data * 3) % 10;

        case BlockID.VINE:
            return ((data << 1) | (data >> 3)) & 0xf;

        case BlockID.FENCE_GATE:
            return ((data + 1) & 0x3) | (data & ~0x3);

        case BlockID.ANVIL:
            return data ^ 0x1;

        case BlockID.BED:
            if ((data & 7) < 4) {
                return data & ~3 | (data+1) & 3;
            }
            break;

        case BlockID.HEAD:
            switch (data) {
            case 2: return 5;
            case 3: return 4;
            case 4: return 2;
            case 5: return 3;
            }
        }

        return data;
    }

    private void rotateBlock90(BaseBlock block) {
        
        int id = block.getData();
        int data = rotate90(block.getType(), block.getData());
        block.setData(data);
        if (block.getType() == 61) {
            BuildInABox.getInstance().debug("Rotated Furnace from " + id + " to " + data + " and the blocks data is now " + block.getData() + "(" + block.getClass() + ")");
        } 
        
        if (block instanceof SkullBlock) {
            byte rot = ((SkullBlock) block).getRot();
            ((SkullBlock) block).setRot((byte) ((rot + 4) % 16));
        } if (block instanceof FurnaceBlock) {
            BuildInABox.getInstance().debug("Rotated Furnace from " + id + " to " + block.getData());
        }
    }

    /**
     * Copy to the clipboard.
     *
     * @param editSession
     */
    public void copy(EditSession editSession) {
        for (int x = 0; x < size.getBlockX(); ++x) {
            for (int y = 0; y < size.getBlockY(); ++y) {
                for (int z = 0; z < size.getBlockZ(); ++z) {
                    data[x][y][z] =
                            editSession.getBlock(new Vector(x, y, z).add(getOrigin()));
                }
            }
        }
    }


    public LocalEntity[] pasteEntities(Vector pos) {
        LocalEntity[] entities = new LocalEntity[this.entities.size()];
        for (int i = 0; i < this.entities.size(); ++i) {
            CopiedEntity copied = this.entities.get(i);
            if (copied.entity.spawn(copied.entity.getPosition().setPosition(copied.relativePosition.add(pos)))) {
                entities[i] = copied.entity;
            }
        }
        return entities;
    }

    public void storeEntity(LocalEntity entity) {
        this.entities.add(new CopiedEntity(entity));
    }

    /**
     * Get one point in the copy. The point is relative to the origin
     * of the copy (0, 0, 0) and not to the actual copy origin.
     *
     * @param pos
     * @return null
     * @throws ArrayIndexOutOfBoundsException
     */
    public BaseBlock getPoint(Vector pos) throws ArrayIndexOutOfBoundsException {
        return data[pos.getBlockX()][pos.getBlockY()][pos.getBlockZ()];
    }

    /**
     * Get one point in the copy. The point is relative to the origin
     * of the copy (0, 0, 0) and not to the actual copy origin.
     *
     * @param pos
     * @return null
     * @throws ArrayIndexOutOfBoundsException
     */
    public void setBlock(Vector pt, BaseBlock block) {
        data[pt.getBlockX()][pt.getBlockY()][pt.getBlockZ()] = block;
    }

    /**
     * Get the size of the copy.
     *
     * @return
     */
    public Vector getSize() {
        return size;
    }

    /**
     * @return the origin
     */
    public Vector getOrigin() {
        return origin;
    }

    /**
     * @param origin the origin to set
     */
    public void setOrigin(Vector origin) {
        this.origin = origin;
    }

    /**
     * @return the offset
     */
    public Vector getOffset() {
        return offset;
    }

    /**
     * @param offset
     */
    public void setOffset(Vector offset) {
        this.offset = offset;
    }

    private class CopiedEntity {
        private final LocalEntity entity;
        private final Vector relativePosition;

        public CopiedEntity(LocalEntity entity) {
            this.entity = entity;
            this.relativePosition = entity.getPosition().getPosition().subtract(getOrigin());
        }
    }

    @SuppressWarnings("incomplete-switch")
    public static int getRotationDegrees(BlockFace from, BlockFace to) {
        switch (from) {
        case NORTH:
            switch (to) {
            case NORTH:
                return 0;
            case EAST:
                return 90;
            case SOUTH:
                return 180;
            case WEST:
                return 270;
            }
            break;
        case EAST:
            switch (to) {
            case NORTH:
                return 270;
            case EAST:
                return 0;
            case SOUTH:
                return 90;
            case WEST:
                return 180;
            }
            break;
        case SOUTH:
            switch (to) {
            case NORTH:
                return 180;
            case EAST:
                return 270;
            case SOUTH:
                return 0;
            case WEST:
                return 90;
            }
            break;

        case WEST:
            switch (to) {
            case NORTH:
                return 90;
            case EAST:
                return 180;
            case SOUTH:
                return 270;
            case WEST:
                return 0;
            }
            break;
        default:
            return 0;
        }
        return 0;
    }

    public com.sk89q.worldedit.CuboidClipboard toWorldEditClipboard() {
        com.sk89q.worldedit.CuboidClipboard wecb = new com.sk89q.worldedit.CuboidClipboard(size, origin, offset);
        try {
            entitiesField.set(wecb, entities);
            dataField.set(wecb, data);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return wecb;
    }
}
