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

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class Region {

    private final Location location;
    private Map<Integer, Integer> loaded = new HashMap<>();

    private boolean modified = false;

    private FileConfiguration config = null;

    Region(Location location) {
        this.location = location;
    }

    /**
     * Notify this holder that a certain chunk got loaded
     * @param x The X coordinate of the chunk
     * @param z The Z coordinate of the chunk
     */
    void notifyLoad(int x, int z) {
        loaded.put(x, z);
    }

    /**
     * Notify this holder that a certain chunk got unloaded
     * @param x The X coordinate of the chunk
     * @param z The Z coordinate of the chunk
     */
    void notifyUnload(int x, int z) {
        loaded.remove(x, z);
    }

    /**
     * Check whether or not a region is fully unloaded
     * @return true if no chunk is loaded anymore, false if there stilla re some
     */
    boolean isUnloaded() {
        return loaded.isEmpty();
    }

    /**
     * Load the data from the disk
     */
    void load() {
        if (config != null) {
            throw new IllegalStateException("Config is already loaded!");
        }
        File configFile = getConfigFile();
        if (configFile.exists()) {
            config = YamlConfiguration.loadConfiguration(getConfigFile());
        } else {
            config = new YamlConfiguration();
        }
    }

    private File getConfigFile() {
        World world = Bukkit.getWorld(location.worldId);
        if (world == null) {
            throw new IllegalStateException("Could not find world with ID " + location.worldId + "?");
        }
        return new File(new File(world.getWorldFolder(), "blockinfo"), "bi." + location.x + "." + location.z + ".yml");
    }

    synchronized void save() throws IOException {
        if (!modified) {
            return;
        }

        File configFile = getConfigFile();
        configFile.getParentFile().mkdirs();
        config.save(configFile);
        modified = false;
    }

    private void ensureLoaded() {
        if (config == null) {
            throw new IllegalStateException("Config isn't loaded yet!");
        }
    }

    void setInfo(int x, int y, int z, NamespacedKey key, Object value) {
        ensureLoaded();
        config.set(x + "." + y + "." + z + "." + key.getNamespace() + "." + key.getKey(), value);
        modified = true;
    }

    Object getInfoValue(int x, int y, int z, NamespacedKey key) {
        ensureLoaded();
        return config.get(x + "." + y + "." + z + "." + key.getNamespace() + "." + key.getKey());
    }

    ConfigurationSection getInfo(int x, int y, int z, String key) {
        ensureLoaded();
        return config.getConfigurationSection(x + "." + y + "." + z + "." + key);
    }

    void removeInfo(int x, int y, int z) {
        ensureLoaded();
        config.set(x + "." + y + "." + z, null);
        ConfigurationSection section = config.getConfigurationSection(x + "." + y);
        if (section == null) {
            return;
        }
        if (section.getKeys(false).isEmpty()) {
            config.set(x + "." + y, null);
            section = config.getConfigurationSection(String.valueOf(x));
            if (section != null && section.getKeys(false).isEmpty()) {
                config.set(String.valueOf(x), null);
            }
        }
        modified = true;
    }

    void removeInfo(int x, int z, int y, String key) {
        ensureLoaded();
        ConfigurationSection section = config.getConfigurationSection(x + "." + y + "." + z);
        if (section == null) {
            return;
        }
        section.set(key, null);
        if (section.getKeys(false).isEmpty()) {
            removeInfo(x, y, z);
        }
        modified = true;
    }

    Location getLocation() {
        return location;
    }

    static class Location {
        private final UUID worldId;
        private final int x;
        private final int z;

        Location(UUID worldId, int x, int z) {
            this.worldId = worldId;
            this.x = x;
            this.z = z;
        }

        @Override
        public int hashCode() {
            int result = worldId.hashCode();
            result = 31 * result + x;
            result = 31 * result + z;
            return result;
        }
    }
}
