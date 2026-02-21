package com.antigravity.holoshop.config;

import com.antigravity.holoshop.HoloShopPlugin;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

public class ShopConfig {

  private final HoloShopPlugin plugin;
  private final Map<String, ShopCategory> categories = new HashMap<>();
  private File file;
  private FileConfiguration config;

  public ShopConfig(HoloShopPlugin plugin) {
    this.plugin = plugin;
  }

  public void loadConfig() {
    if (file == null) {
      file = new File(plugin.getDataFolder(), "shops.yml");
    }
    if (!file.exists()) {
      plugin.saveResource("shops.yml", false);
    }

    config = YamlConfiguration.loadConfiguration(file);
    categories.clear();

    ConfigurationSection catsSection = config.getConfigurationSection("categories");
    if (catsSection == null) {
      plugin.getLogger().warning("No 'categories' section found in shops.yml!");
      return;
    }

    for (String catKey : catsSection.getKeys(false)) {
      ConfigurationSection sec = catsSection.getConfigurationSection(catKey);
      if (sec == null)
        continue;

      String title = sec.getString("title", "Shop");
      String matName = sec.getString("material", "CHEST");
      Material catMat = Material.matchMaterial(matName);
      if (catMat == null)
        catMat = Material.CHEST;
      int displaySlot = sec.getInt("slot", 0);

      ShopCategory category = new ShopCategory(catKey, title, catMat, displaySlot);

      // Load Items for this Category
      ConfigurationSection itemsSection = sec.getConfigurationSection("items");
      if (itemsSection != null) {
        for (String itemKey : itemsSection.getKeys(false)) {
          ConfigurationSection itemSec = itemsSection.getConfigurationSection(itemKey);
          if (itemSec == null)
            continue;

          String iMatName = itemSec.getString("material", "DIRT");
          Material iMat = Material.matchMaterial(iMatName);
          if (iMat == null) {
            plugin.getLogger().warning("Invalid material '" + iMatName + "' in shop item '" + itemKey + "'");
            iMat = Material.STONE;
          }

          int slot = itemSec.getInt("slot", 0);
          double buyPrice = itemSec.getDouble("buy-price", -1.0);
          double sellPrice = itemSec.getDouble("sell-price", -1.0);
          int defaultAmount = itemSec.getInt("default-amount", 1);

          ShopItem shopItem = new ShopItem(itemKey, iMat, slot, buyPrice, sellPrice, defaultAmount);
          category.addItem(shopItem);
        }
      }

      categories.put(catKey, category);
      plugin.getLogger().info("Loaded category '" + catKey + "' with " + category.getItems().size() + " items.");
    }
  }

  public Map<String, ShopCategory> getCategories() {
    return categories;
  }

  public ShopCategory getCategory(String id) {
    return categories.get(id);
  }

  public void saveShops() {
    for (Map.Entry<String, ShopCategory> entry : categories.entrySet()) {
      String catKey = entry.getKey();
      ShopCategory category = entry.getValue();
      
      String basePath = "categories." + catKey;
      config.set(basePath + ".title", category.getTitle());
      config.set(basePath + ".material", category.getDisplayMaterial().name());
      config.set(basePath + ".slot", category.getDisplaySlot());
      
      // Clear old items just to be safe
      config.set(basePath + ".items", null);
      
      for (ShopItem item : category.getItems().values()) {
        String itemPath = basePath + ".items." + item.getId();
        config.set(itemPath + ".material", item.getMaterial().name());
        config.set(itemPath + ".slot", item.getSlot());
        config.set(itemPath + ".buy-price", item.getBuyPrice());
        config.set(itemPath + ".sell-price", item.getSellPrice());
        config.set(itemPath + ".default-amount", item.getDefaultAmount());
      }
    }
    
    try {
      config.save(file);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Could not save shops.yml", e);
    }
  }
}
