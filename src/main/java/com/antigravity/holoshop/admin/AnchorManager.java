package com.antigravity.holoshop.admin;

import com.antigravity.holoshop.HoloShopPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AnchorManager {

    private final HoloShopPlugin plugin;
    private final File anchorsFile;
    private FileConfiguration anchorsConfig;
    private final Map<String, ShopAnchor> anchors = new HashMap<>();
    private final Map<String, Entity[]> spawnedAnchorEntities = new HashMap<>();

    public AnchorManager(HoloShopPlugin plugin) {
        this.plugin = plugin;
        this.anchorsFile = new File(plugin.getDataFolder(), "anchors.yml");
        loadAnchors();
        startTrackingTask();
    }

    private void startTrackingTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Entity[] pair : spawnedAnchorEntities.values()) {
                if (pair == null || pair.length == 0 || pair[0] == null || !pair[0].isValid()) continue;
                ItemDisplay visual = (ItemDisplay) pair[0];
                
                Player nearest = null;
                double minD = 225;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld().equals(visual.getWorld())) {
                        double d = p.getLocation().distanceSquared(visual.getLocation());
                        if (d < minD) {
                            minD = d;
                            nearest = p;
                        }
                    }
                }
                
                if (nearest != null) {
                    Vector dir = nearest.getEyeLocation().toVector().subtract(visual.getLocation().toVector());
                    dir.setY(0);
                    if (dir.lengthSquared() > 0.001) {
                        float yaw = new Location(null, 0,0,0).setDirection(dir).getYaw();
                        Location target = visual.getLocation();
                        target.setYaw(yaw);
                        target.setPitch(0);
                        visual.setTeleportDuration(3);
                        visual.teleport(target);
                    }
                }
            }
        }, 20L, 3L);
    }

    public void loadAnchors() {
        anchors.clear();
        despawnAll();

        if (!anchorsFile.exists()) {
            plugin.saveResource("anchors.yml", false);
        }
        
        // Ensure file exists even if saveResource doesn't create it (e.g. no default in jar)
        if (!anchorsFile.exists()) {
            try {
                anchorsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create anchors.yml!");
            }
        }

        anchorsConfig = YamlConfiguration.loadConfiguration(anchorsFile);
        ConfigurationSection section = anchorsConfig.getConfigurationSection("anchors");

        if (section == null) return;

        for (String id : section.getKeys(false)) {
            String worldName = section.getString(id + ".world");
            double x = section.getDouble(id + ".x");
            double y = section.getDouble(id + ".y");
            double z = section.getDouble(id + ".z");
            float yaw = (float) section.getDouble(id + ".yaw");
            float pitch = (float) section.getDouble(id + ".pitch");
            String categoryId = section.getString(id + ".category");

            World world = Bukkit.getWorld(worldName);
            if (world != null) {
                Location loc = new Location(world, x, y, z, yaw, pitch);
                ShopAnchor anchor = new ShopAnchor(id, loc, categoryId);
                anchors.put(id, anchor);
                spawnAnchorVisuals(anchor);
            }
        }
        plugin.getLogger().info("Loaded " + anchors.size() + " shop anchors.");
    }

    public void saveAnchors() {
        for (ShopAnchor anchor : anchors.values()) {
            String path = "anchors." + anchor.getId();
            Location loc = anchor.getLocation();
            anchorsConfig.set(path + ".world", loc.getWorld().getName());
            anchorsConfig.set(path + ".x", loc.getX());
            anchorsConfig.set(path + ".y", loc.getY());
            anchorsConfig.set(path + ".z", loc.getZ());
            anchorsConfig.set(path + ".yaw", loc.getYaw());
            anchorsConfig.set(path + ".pitch", loc.getPitch());
            anchorsConfig.set(path + ".category", anchor.getCategoryId());
        }
        try {
            anchorsConfig.save(anchorsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save anchors.yml");
        }
    }

    public void createAnchor(Location loc, String categoryId) {
        String id = UUID.randomUUID().toString();
        // Center the location block
        Location centered = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 0.5, loc.getBlockZ() + 0.5, loc.getYaw(), loc.getPitch());
        ShopAnchor anchor = new ShopAnchor(id, centered, categoryId);
        anchors.put(id, anchor);
        spawnAnchorVisuals(anchor);
        saveAnchors();
    }

    public boolean removeAnchor(String id, Location clickedLoc) {
        ShopAnchor anchor = anchors.remove(id);
        Location searchLoc = anchor != null ? anchor.getLocation() : clickedLoc;
        
        if (searchLoc != null) {
            for (Entity e : searchLoc.getWorld().getNearbyEntities(searchLoc, 2.0, 2.0, 2.0)) {
                if (e.getScoreboardTags().contains("holoshop_anchor")) {
                    e.remove();
                }
            }
        }

        if (anchor != null) {
            // Remove entities
            Entity[] entityPair = spawnedAnchorEntities.remove(id);
            if (entityPair != null) {
                for (Entity e : entityPair) {
                    if (e != null && e.isValid()) {
                        e.remove();
                    }
                }
            }
            // Remove from config and save
            anchorsConfig.set("anchors." + id, null);
            try {
                anchorsConfig.save(anchorsFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to save anchors.yml after deleting anchor: " + id);
            }
            return true;
        }
        return false;
    }

    private void spawnAnchorVisuals(ShopAnchor anchor) {
        Location loc = anchor.getLocation();
        
        // The visible floating item (acting as the anchor graphic)
        ItemDisplay visual = loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
            display.setPersistent(false);
            display.setItemStack(new ItemStack(Material.BEACON)); // Default visual
            display.setBillboard(Display.Billboard.FIXED);
            Transformation transform = display.getTransformation();
            transform.getScale().set(0.75f);
            display.setTransformation(transform);
            display.addScoreboardTag("holoshop_anchor");
        });

        // The invisible interaction box
        Interaction interaction = loc.getWorld().spawn(loc, Interaction.class, entity -> {
            entity.setPersistent(false);
            entity.setInteractionWidth(1.0f);
            entity.setInteractionHeight(1.0f);
            entity.addScoreboardTag("holoshop_anchor");
            entity.getPersistentDataContainer().set(plugin.getNamespacedKey("anchor_id"), PersistentDataType.STRING, anchor.getId());
            entity.getPersistentDataContainer().set(plugin.getNamespacedKey("category_id"), PersistentDataType.STRING, anchor.getCategoryId());
        });

        spawnedAnchorEntities.put(anchor.getId(), new Entity[]{visual, interaction});
    }

    public void despawnAll() {
        for (Entity[] entityPair : spawnedAnchorEntities.values()) {
            for (Entity e : entityPair) {
                if (e != null && e.isValid()) {
                    e.remove();
                }
            }
        }
        spawnedAnchorEntities.clear();
        
        // Failsafe cleanup in case of improper shutdowns
        plugin.getServer().getWorlds().forEach(w -> {
            for (Entity e : w.getEntities()) {
                if (e.getScoreboardTags().contains("holoshop_anchor")) {
                    e.remove();
                }
            }
        });
    }

    public ShopAnchor getAnchor(String id) {
        return anchors.get(id);
    }
}
