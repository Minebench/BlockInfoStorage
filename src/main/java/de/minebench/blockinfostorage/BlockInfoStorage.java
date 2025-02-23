package de.minebench.blockinfostorage;

/*
 * BlockInfoStorage
 * Copyright (c) 2019 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class BlockInfoStorage extends JavaPlugin implements Listener {

    private static BlockInfoStorage instance;

    private final Map<UUID, Map<Integer, Map<Integer, Region.Location>>> regionLocs = new HashMap<>();
    private final Map<Region.Location, Region> dataMap = new ConcurrentHashMap<>();


    @Override
    public void onEnable() {
        instance = this;
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        for (Region region : dataMap.values()) {
            save(region);
        }
    }

    // --- "API" ---

    /**
     * Get the storage instance
     * @return The storage instance
     */
    public static BlockInfoStorage get() {
        return instance;
    }

    /**
     * Set block info
     * @param block The block to attach the info to
     * @param key   The key to set
     * @param value The value to set
     */
    public void setBlockInfo(Block block, NamespacedKey key, Object value) {
        getRegion(block).setInfo(block.getX(), block.getY(), block.getZ(), key, value);
    }

    /**
     * Set block info
     * @param location  The location of the block to attach the info to
     * @param key       The key to set
     * @param value     The value to set
     */
    public void setBlockInfo(Location location, NamespacedKey key, Object value) {
        getRegion(location).setInfo(location.getBlockX(), location.getBlockY(), location.getBlockZ(), key, value);
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param block     The block to attach the info to
     * @param plugin    The plugin for the info
     * @return All the info that the plugin assigned to the block or null if none was found
     */
    public ConfigurationSection getBlockInfo(Block block, Plugin plugin) {
        return getBlockInfo(block, plugin.getName());
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param location  The location of the block to get the info from
     * @param plugin    The plugin for the info
     * @return All the info that the plugin assigned to the block or null if none was found
     */
    public ConfigurationSection getBlockInfo(Location location, Plugin plugin) {
        return getBlockInfo(location, plugin.getName());
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param block The block to get the info from
     * @param type  The type of info to get
     * @return All the info of that type that is assigned to the block or null if none was found
     */
    public ConfigurationSection getBlockInfo(Block block, String type) {
        return getRegion(block).getInfo(block.getX(), block.getY(), block.getZ(), type.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param location  The location of the block to get the info from
     * @param type      The type of info to get
     * @return All the info of that type that is assigned to the block or null if none was found
     */
    public ConfigurationSection getBlockInfo(Location location, String type) {
        return getRegion(location).getInfo(location.getBlockX(), location.getBlockY(), location.getBlockZ(), type.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param block The block to get the info from
     * @param key   The namespaced key for the info to get
     * @return The info value assigned to the block or null if none was found
     */
    public Object getBlockInfoValue(Block block, NamespacedKey key) {
        return getRegion(block).getInfoValue(block.getX(), block.getY(), block.getZ(), key);
    }

    /**
     * Get information from a block. This is only for reading, use {@link #setBlockInfo(Block, NamespacedKey, Object)} for writing!
     * @param location  The location of the block to get the info from
     * @param key       The namespaced key for the info to get
     * @return The info value assigned to the block or null if none was found
     */
    public Object getBlockInfoValue(Location location, NamespacedKey key) {
        return getRegion(location).getInfoValue(location.getBlockX(), location.getBlockY(), location.getBlockZ(), key);
    }

    /**
     * Remove block info
     * @param block     The block the info is attached to
     * @param plugin    The plugin which's info to remove
     */
    public void removeBlockInfo(Block block, Plugin plugin) {
        removeBlockInfo(block, plugin.getName());
    }

    /**
     * Remove block info
     * @param location  The location of the block the info is attached to
     * @param plugin    The plugin which's info to remove
     */
    public void removeBlockInfo(Location location, Plugin plugin) {
        removeBlockInfo(location, plugin.getName());
    }

    /**
     * Remove block info
     * @param block The block the info is attached to
     * @param key   The key to remove
     */
    public void removeBlockInfo(Block block, NamespacedKey key) {
        getRegion(block).removeInfo(block.getX(), block.getZ(), block.getY(), key.getNamespace() + "." + key.getKey());
    }

    /**
     * Remove block info
     * @param location  The location of the block the info is attached to
     * @param key       The key to remove
     */
    public void removeBlockInfo(Location location, NamespacedKey key) {
        getRegion(location).removeInfo(location.getBlockX(), location.getBlockY(), location.getBlockZ(), key.getNamespace() + "." + key.getKey());
    }

    /**
     * Remove block info
     * @param block The block the info is attached to
     * @param type  The type to remove
     */
    public void removeBlockInfo(Block block, String type) {
        getRegion(block).removeInfo(block.getX(), block.getZ(), block.getY(), type.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Remove block info
     * @param location  The location of the block the info is attached to
     * @param type      The type to remove
     */
    public void removeBlockInfo(Location location, String type) {
        getRegion(location).removeInfo(location.getBlockX(), location.getBlockY(), location.getBlockZ(), type.toLowerCase(Locale.ENGLISH));
    }

    /**
     * Remove all block info (TODO: Decide whether or not this is too dangerous to expose)
     * @param block     The block info is attached to
     */
    public void removeBlockInfo(Block block) {
        getRegion(block).removeInfo(block.getX(), block.getY(), block.getZ());
    }

    /**
     * Remove all block info (TODO: Decide whether or not this is too dangerous to expose)
     * @param location  The location of the block info is attached to
     */
    public void removeBlockInfo(Location location) {
        getRegion(location).removeInfo(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    // --- Events ---

    /**
     * Cache chunk data
     */
    @EventHandler(priority = EventPriority.MONITOR)
    private void onChunkLoad(ChunkLoadEvent event) {
        Region.Location location = getRegionLocation(event.getChunk());
        synchronized (location) {
            if (!dataMap.containsKey(location)) {
                Region region = getOrCreateRegion(location);
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    synchronized (location) {
                        load(location, region).notifyLoad(event.getChunk().getX(), event.getChunk().getZ());
                    }
                });
            }
        }
    }

    /**
     * Remove cached chunk data
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onChunkUnload(ChunkUnloadEvent event) {
        Region.Location regionLocation = getRegionLocation(event.getChunk());
        synchronized (regionLocation) {
            Region region = dataMap.get(regionLocation);
            if (region != null) {
                region.notifyUnload(event.getChunk().getX(), event.getChunk().getZ());
                if (region.isUnloaded()) {
                    dataMap.remove(regionLocation);
                    save(region);
                }
            }
        }
    }

    /**
     * Save chunk data to disk to ensure being in-sync with the world
     */
    @EventHandler(priority = EventPriority.MONITOR)
    private void onWorldSave(WorldSaveEvent event) {
        Map<Integer, Map<Integer, Region.Location>> regions = regionLocs.get(event.getWorld().getUID());
        if (regions != null) {
            regions.values().stream().flatMap(m -> m.values().stream()).map(dataMap::get).filter(Objects::nonNull).forEach(this::save);
        }
    }

    /**
     * Save chunk data to disk and unload existing data
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldUnload(WorldUnloadEvent event) {
        Map<Integer, Map<Integer, Region.Location>> regions = regionLocs.get(event.getWorld().getUID());
        if (regions != null) {
            regions.values().stream().flatMap(m -> m.values().stream()).map(dataMap::remove).filter(Objects::nonNull).forEach(this::save);
            regionLocs.remove(event.getWorld().getUID());
        }
    }

    // --- Block update handling ---

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockBreakEvent event) {
        removeBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockPlaceEvent event) {
        removeBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockBurnEvent event) {
        removeBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockFadeEvent event) {
        removeBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockFromToEvent event) {
        removeBlockInfo(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockPistonExtendEvent event) {
        event.getBlocks().forEach(this::removeBlockInfo);
        // TODO: Block info moving?
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void on(BlockPistonRetractEvent event) {
        event.getBlocks().forEach(this::removeBlockInfo);
        // TODO: Block info moving?
    }

    // --- Internal utility methods ---

    private Region getOrCreateRegion(Region.Location location) {
        Region region = dataMap.get(location);
        if (region != null) {
            return region;
        }
        return new Region(location);
    }

    private Region load(Region.Location location) {
        return load(location, getOrCreateRegion(location));
    }

    private Region load(Region.Location location, Region region) {
        // load data from file
        region.load();

        dataMap.put(location, region);
        return region;
    }

    private void save(Region region) {
        try {
            region.save();
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Error while trying to save block info for region " + region.getLocation(), e);
        }
    }

    private Region getRegion(Block block) {
        Region.Location location = getRegionLocation(block);
        synchronized (location) {
            return load(location);
        }
    }

    private Region getRegion(Location loc) {
        Region.Location location = getRegionLocation(loc);
        synchronized (location) {
            return load(location);
        }
    }

    private Region.Location getRegionLocation(World world, int x, int z) {
        synchronized (regionLocs) {
            return regionLocs.computeIfAbsent(world.getUID(), k -> new HashMap<>())
                    .computeIfAbsent(x, k -> new HashMap<>())
                    .computeIfAbsent(z, k -> new Region.Location(world.getUID(), x, z));
        }
    }

    private Region.Location getRegionLocation(Chunk chunk) {
        return getRegionLocation(chunk.getWorld(), chunk.getX() >> 5, chunk.getZ() >> 5);
    }

    private Region.Location getRegionLocation(Block block) {
        return getRegionLocation(block.getWorld(), block.getX() >> 9, block.getZ() >> 9);
    }

    private Region.Location getRegionLocation(Location location) {
        return getRegionLocation(location.getWorld(), location.getBlockX() >> 9, location.getBlockZ() >> 9);
    }
}
