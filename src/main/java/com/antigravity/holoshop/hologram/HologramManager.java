package com.antigravity.holoshop.hologram;

import com.antigravity.holoshop.HoloShopPlugin;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HologramManager {

  private final HoloShopPlugin plugin;
  private final Map<UUID, HologramMenu> activeHolograms = new HashMap<>();

  public HologramManager(HoloShopPlugin plugin) {
    this.plugin = plugin;
  }

  public void openMainMenu(Player player) {
    Location anchor = calculateNewAnchor(player);
    closeHologram(player);
    HologramMenu menu = new HologramMenu(plugin, player, anchor);
    menu.spawnMainMenu();
    activeHolograms.put(player.getUniqueId(), menu);
  }

  public void navigateToMainMenu(Player player) {
    Location anchor = getExistingOrNewAnchor(player);
    closeHologram(player);
    HologramMenu menu = new HologramMenu(plugin, player, anchor);
    menu.spawnMainMenu();
    activeHolograms.put(player.getUniqueId(), menu);
  }

  public void openCategoryMenu(Player player, String categoryId, Location forcedAnchor, int page) {
    Location anchor;
    if (forcedAnchor != null) {
        anchor = forcedAnchor;
    } else {
        anchor = getExistingOrNewAnchor(player);
    }
    closeHologram(player);
    HologramMenu menu = new HologramMenu(plugin, player, anchor);
    menu.spawnCategoryMenu(categoryId, page);
    activeHolograms.put(player.getUniqueId(), menu);
  }

  public void openCategoryMenu(Player player, String categoryId, int page) {
      openCategoryMenu(player, categoryId, null, page);
  }

  public void openCategoryMenu(Player player, String categoryId, Location forcedAnchor) {
      openCategoryMenu(player, categoryId, forcedAnchor, 0);
  }

  public void openCategoryMenu(Player player, String categoryId) {
      openCategoryMenu(player, categoryId, null, 0);
  }

  public void openTransactionMenu(Player player, String categoryId, String itemId) {
    Location anchor = getExistingOrNewAnchor(player);
    closeHologram(player);
    HologramMenu menu = new HologramMenu(plugin, player, anchor);
    menu.spawnTransactionMenu(categoryId, itemId);
    activeHolograms.put(player.getUniqueId(), menu);
  }

  private Location calculateNewAnchor(Player player) {
    Location eyeLoc = player.getEyeLocation();
    // Force pitch to 0 so the hologram spawns horizontally relative to the player, never into the ground or sky
    Vector horizDir = eyeLoc.getDirection().setY(0);
    if (horizDir.lengthSquared() < 0.001) {
        horizDir = player.getLocation().getDirection().setY(0);
    }
    horizDir.normalize();
    Location anchor = eyeLoc.add(horizDir.multiply(3.5));
    // Face the player's head
    anchor.setDirection(player.getEyeLocation().toVector().subtract(anchor.toVector()));
    return anchor;
  }

  private Location getExistingOrNewAnchor(Player player) {
    HologramMenu existing = activeHolograms.get(player.getUniqueId());
    if (existing != null) {
      return existing.getAnchor();
    }
    return calculateNewAnchor(player);
  }

  public void closeHologram(Player player) {
    HologramMenu menu = activeHolograms.remove(player.getUniqueId());
    if (menu != null) {
      menu.despawnAll();
    }
  }

  public void cleanupAll() {
    for (HologramMenu menu : activeHolograms.values()) {
      menu.despawnAll();
    }
    activeHolograms.clear();

    plugin.getServer().getWorlds().forEach(world -> {
      for (Entity entity : world.getEntities()) {
        if (entity.getScoreboardTags().contains("holoshop_hologram")) {
          entity.remove();
        }
      }
    });
  }

  public HologramMenu getActiveHologram(Player player) {
    return activeHolograms.get(player.getUniqueId());
  }
}
