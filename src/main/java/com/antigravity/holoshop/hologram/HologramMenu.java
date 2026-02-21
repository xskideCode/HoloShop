package com.antigravity.holoshop.hologram;

import com.antigravity.holoshop.HoloShopPlugin;
import com.antigravity.holoshop.config.ShopCategory;
import com.antigravity.holoshop.config.ShopItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HologramMenu {

  private final HoloShopPlugin plugin;
  private final Player player;
  private final Location anchor;
  private final Map<Entity, Vector> localOffsets = new HashMap<>();
  private final List<Entity> spawnedEntities = new ArrayList<>();
  private final LegacyComponentSerializer lcs = LegacyComponentSerializer.legacyAmpersand();
  private BukkitTask trackingTask;

  public HologramMenu(HoloShopPlugin plugin, Player player, Location anchor) {
    this.plugin = plugin;
    this.player = player;
    this.anchor = anchor.clone();
  }

  public Location getAnchor() {
    return anchor.clone();
  }

  private void trackEntity(Entity entity, float rx, float uy, float fz) {
    localOffsets.put(entity, new Vector(rx, uy, fz));
    spawnedEntities.add(entity);
    hideFromOthers(entity);
  }

  private Location calculateWorldLoc(float rx, float uy, float fz) {
    Vector forward = anchor.getDirection().normalize();
    Vector up = new Vector(0, 1, 0);
    Vector right = forward.clone().crossProduct(up).normalize().multiply(-1);
    return anchor.clone().add(right.multiply(rx)).add(up.multiply(uy)).add(forward.multiply(fz));
  }

  private void spawnBackdrop(boolean showClose) {
    Location bgLoc = calculateWorldLoc(0, 2.5f, -0.15f);
    TextDisplay bgDisplay = bgLoc.getWorld().spawn(bgLoc, TextDisplay.class, display -> {
      display.setPersistent(false);
      display.text(net.kyori.adventure.text.Component.text("\uE001"));
      display.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0)); 
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(0.85f);
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(bgDisplay, 0, 2.5f, -0.15f);

    double balance = plugin.getVaultManager().getBalance(player);
    spawnText(-1.15f, -1.0f, 0.05f, "&bBalance: &f$" + plugin.getVaultManager().format(balance), 0.4f);

    if (showClose) {
      spawnButton(1.0f, -0.85f, 0f, Material.BARRIER, "&c&l[ CLOSE ]", "close", 0, null, null);
    }
  }

  public void spawnMainMenu() {
    spawnBackdrop(true);
    Map<String, ShopCategory> categories = plugin.getShopConfig().getCategories();
    if (categories.isEmpty()) return;

    spawnTitle("§b✦ §lShop Categories §b✦", 0.9f);

    int i = 0;
    float startY = 0.5f;
    float gapY = 0.5f;
    for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
      float offsetY = startY - (i * gapY);
      spawnCategoryRow(0f, offsetY, 0f, entry.getValue().getDisplayMaterial(), "&e" + entry.getValue().getTitle(), entry.getKey());
      i++;
    }
    startTracking();
  }

  public void spawnCategoryMenu(String categoryId, int page) {
    spawnBackdrop(false);
    ShopCategory category = plugin.getShopConfig().getCategories().get(categoryId);
    if (category == null) return;

    spawnTitle("§b✦ §l" + category.getTitle() + " §b✦", 1.0f);
    spawnIcon(-1.0f, 0.25f, 0f, category.getDisplayMaterial(), "&e" + category.getTitle(), null, null);

    List<ShopItem> allItems = new ArrayList<>(category.getItems().values());
    int itemsPerPage = 6;
    int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
    if (totalPages == 0) totalPages = 1;
    if (page < 0) page = 0;
    if (page >= totalPages) page = totalPages - 1;

    int startIndex = page * itemsPerPage;
    int endIndex = Math.min(startIndex + itemsPerPage, allItems.size());
    List<ShopItem> items = allItems.subList(startIndex, endIndex);

    int columns = 3;
    float paddingX = 0.75f; 
    float paddingY = 0.8f; 

    for (int i = 0; i < items.size(); i++) {
        int row = i / columns;
        int col = i % columns;
        ShopItem shopItem = items.get(i);

        float offsetX = (col - (columns - 1) / 4.7f) * paddingX;
        float offsetY = -(row - 0.9f) * paddingY;
        
        String priceText = "";
        if (shopItem.getBuyPrice() > 0) priceText += "&a$" + plugin.getVaultManager().format(shopItem.getBuyPrice()) + "\n";
        if (shopItem.getSellPrice() > 0) priceText += "&c$" + plugin.getVaultManager().format(shopItem.getSellPrice());

        spawnIcon(offsetX, offsetY, 0f, shopItem.getMaterial(), priceText.trim(), "item_id", String.valueOf(shopItem.getSlot()));
        
        Entity lastSpawned = spawnedEntities.get(spawnedEntities.size() - 1);
        if (lastSpawned instanceof Interaction) {
            lastSpawned.getPersistentDataContainer().set(plugin.getNamespacedKey("category_id"), PersistentDataType.STRING, categoryId);
        }
    }

    spawnButton(-0.1f, -0.85f, 0f, Material.BARRIER, "&c[ Back ]", "back", 0, null, null);

    if (page > 0) {
        spawnButton(0.7f, -0.85f, 0f, Material.ARROW, "&e[ Prev Page ]", "prev_page", page, null, categoryId);
    }
    if (page < totalPages - 1) {
        spawnButton(1.3f, -0.85f, 0f, Material.ARROW, "&e[ Next Page ]", "next_page", page, null, categoryId);
    }
    startTracking();
  }

  public void spawnTransactionMenu(String categoryId, String itemId) {
    spawnBackdrop(false);
    ShopCategory category = plugin.getShopConfig().getCategories().get(categoryId);
    if (category == null) return;
    ShopItem shopItem = category.getItems().get(Integer.parseInt(itemId));
    if (shopItem == null) return;

    Location centerLoc = calculateWorldLoc(-0.75f, 0.25f, 0f);
    ItemDisplay centerDisplay = centerLoc.getWorld().spawn(centerLoc, ItemDisplay.class, display -> {
      display.setPersistent(false);
      display.setItemStack(new ItemStack(shopItem.getMaterial()));
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(0.75f);
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(centerDisplay, -0.75f, 0.25f, 0f);

    spawnText(-0.75f, 0.25f - 0.6f, 0.05f, "&6" + shopItem.getMaterial().name().replace("_", " "), 0.5f);

    if (shopItem.getBuyPrice() > 0) {
      spawnButton(0.5f, 0.85f, 0f, Material.LIME_STAINED_GLASS, "&a&lBUY 64x", "buy", 64, shopItem, categoryId);
      spawnButton(0.5f, 0.25f, 0f, Material.LIME_STAINED_GLASS, "&a&lBUY 16x", "buy", 16, shopItem, categoryId);
      spawnButton(0.5f, -0.35f, 0f, Material.LIME_STAINED_GLASS, "&a&lBUY 1x", "buy", 1, shopItem, categoryId);
    }

    if (shopItem.getSellPrice() > 0) {
      spawnButton(1.25f, 0.85f, 0f, Material.RED_STAINED_GLASS, "&c&lSELL 64x", "sell", 64, shopItem, categoryId);
      spawnButton(1.25f, 0.25f, 0f, Material.RED_STAINED_GLASS, "&c&lSELL 16x", "sell", 16, shopItem, categoryId);
      spawnButton(1.25f, -0.35f, 0f, Material.RED_STAINED_GLASS, "&c&lSELL 1x", "sell", 1, shopItem, categoryId);
    }

    spawnButton(1.0f, -0.85f, 0f, Material.ARROW, "&7[ Back ]", "back", 0, null, null);
    startTracking();
  }

  private void spawnButton(float rx, float uy, float fz, Material mat, String title, String action, int amount, ShopItem item, String categoryId) {
    Location loc = calculateWorldLoc(rx, uy, fz);
    ItemDisplay icon = loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
      display.setPersistent(false);
      display.setItemStack(new ItemStack(mat));
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(0.3f); 
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(icon, rx, uy, fz);

    String finalLabel = title;
    if (item != null) {
        double price = action.equals("buy") ? item.getBuyPrice() * amount : item.getSellPrice() * amount;
        finalLabel += "\n&e$" + plugin.getVaultManager().format(price);
    }
    spawnText(rx, uy - 0.35f, fz + 0.05f, finalLabel, 0.4f);

    float interactionH = 0.6f;
    Location interactionLoc = calculateWorldLoc(rx, uy - interactionH / 2.0f, fz);
    Interaction interaction = interactionLoc.getWorld().spawn(interactionLoc, Interaction.class, entity -> {
      entity.setPersistent(false);
      entity.setInteractionWidth(0.6f);
      entity.setInteractionHeight(interactionH);
      entity.addScoreboardTag("holoshop_hologram");
      entity.getPersistentDataContainer().set(plugin.getNamespacedKey("action"), PersistentDataType.STRING, action);
      if (amount >= 0) {
        entity.getPersistentDataContainer().set(plugin.getNamespacedKey("amount"), PersistentDataType.INTEGER, amount);
      }
      if (item != null) {
        entity.getPersistentDataContainer().set(plugin.getNamespacedKey("item_id"), PersistentDataType.STRING, String.valueOf(item.getSlot()));
      }
      if (categoryId != null) {
        entity.getPersistentDataContainer().set(plugin.getNamespacedKey("category_id"), PersistentDataType.STRING, categoryId);
      }
    });
    trackEntity(interaction, rx, uy - interactionH / 2.0f, fz);
  }
  
  private void spawnIcon(float rx, float uy, float fz, Material mat, String label, String tagKey, String tagValue) {
    Location loc = calculateWorldLoc(rx, uy, fz);
    ItemDisplay icon = loc.getWorld().spawn(loc, ItemDisplay.class, display -> {
      display.setPersistent(false);
      display.setItemStack(new ItemStack(mat));
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(0.4f); 
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(icon, rx, uy, fz);

    spawnText(rx, uy - 0.55f, fz + 0.05f, label, 0.4f); 

    float interactionH = 0.6f;
    Location interactionLoc = calculateWorldLoc(rx, uy - interactionH / 2.0f, fz);
    Interaction interaction = interactionLoc.getWorld().spawn(interactionLoc, Interaction.class, entity -> {
      entity.setPersistent(false);
      entity.setInteractionWidth(0.6f);
      entity.setInteractionHeight(interactionH);
      entity.addScoreboardTag("holoshop_hologram");
      if (tagKey != null && tagValue != null) {
          entity.getPersistentDataContainer().set(plugin.getNamespacedKey(tagKey), PersistentDataType.STRING, tagValue);
      }
    });
    trackEntity(interaction, rx, uy - interactionH / 2.0f, fz);
  }

  private void spawnCategoryRow(float rx, float uy, float fz, Material mat, String label, String categoryId) {
    Location iconLoc = calculateWorldLoc(rx - 0.75f, uy, fz);
    ItemDisplay icon = iconLoc.getWorld().spawn(iconLoc, ItemDisplay.class, display -> {
      display.setPersistent(false);
      display.setItemStack(new ItemStack(mat));
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(0.3f); 
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(icon, rx - 0.75f, uy, fz);

    spawnText(rx + 0.4f, uy, fz + 0.05f, label, 0.6f); 

    float interactionH = 0.4f;
    Location interactionLoc = calculateWorldLoc(rx, uy - interactionH / 2.0f, fz);
    Interaction interaction = interactionLoc.getWorld().spawn(interactionLoc, Interaction.class, entity -> {
      entity.setPersistent(false);
      entity.setInteractionWidth(1.75f);
      entity.setInteractionHeight(interactionH);
      entity.addScoreboardTag("holoshop_hologram");
      entity.getPersistentDataContainer().set(plugin.getNamespacedKey("category_id"), PersistentDataType.STRING, categoryId);
    });
    trackEntity(interaction, rx, uy - interactionH / 2.0f, fz);
  }

  private void spawnText(float rx, float uy, float fz, String text, float scale) {
    Location loc = calculateWorldLoc(rx, uy, fz);
    TextDisplay titleDisplay = loc.getWorld().spawn(loc, TextDisplay.class, display -> {
      display.setPersistent(false);
      display.text(lcs.deserialize(text));
      display.setBillboard(Display.Billboard.FIXED);
      display.setRotation(anchor.getYaw(), 0);
      Transformation transform = display.getTransformation();
      transform.getScale().set(scale);
      display.setTransformation(transform);
      display.addScoreboardTag("holoshop_hologram");
    });
    trackEntity(titleDisplay, rx, uy, fz);
  }

  private void spawnTitle(String text, float heightOffset) {
    spawnText(0f, heightOffset, 0.075f, text, 0.9f);
  }

  private void hideFromOthers(Entity entity) {
    for (Player p : Bukkit.getOnlinePlayers()) {
      if (!p.equals(player)) {
        p.hideEntity(plugin, entity);
      }
    }
  }

  public void startTracking() {
      if (trackingTask != null) return;
      trackingTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
          if (!player.isOnline() || !player.isValid()) {
              plugin.getHologramManager().closeHologram(player);
              return;
          }
          
          if (anchor == null || player.getLocation().getWorld() != anchor.getWorld() || 
              player.getLocation().distanceSquared(anchor) > 64) {
              plugin.getHologramManager().closeHologram(player);
              return;
          }

          Vector dir = player.getEyeLocation().toVector().subtract(anchor.toVector());
          dir.setY(0);
          if (dir.lengthSquared() > 0.001) {
              dir.normalize();
          } else {
              dir = anchor.getDirection();
          }

          anchor.setDirection(dir);
          float yaw = anchor.getYaw();

          Vector forward = dir;
          Vector up = new Vector(0, 1, 0);
          Vector right = forward.clone().crossProduct(up).normalize().multiply(-1);

          for (Map.Entry<Entity, Vector> entry : localOffsets.entrySet()) {
              Entity e = entry.getKey();
              if (e == null || !e.isValid()) continue;
              Vector local = entry.getValue();

              Location target = anchor.clone()
                  .add(right.clone().multiply(local.getX()))
                  .add(up.clone().multiply(local.getY()))
                  .add(forward.clone().multiply(local.getZ()));
              
              target.setYaw(yaw);
              target.setPitch(0);

              if (e instanceof Display display) {
                  display.setTeleportDuration(3);
              }
              e.teleport(target);
          }
      }, 0L, 3L);
  }

  public void despawnAll() {
    if (trackingTask != null) {
        trackingTask.cancel();
        trackingTask = null;
    }
    for (Entity e : spawnedEntities) {
      if (e != null && e.isValid()) {
          e.remove();
      }
    }
    spawnedEntities.clear();
    localOffsets.clear();
  }
}