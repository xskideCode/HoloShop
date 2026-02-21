package com.antigravity.holoshop.config;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

public class ShopCategory {

  private final String id;
  private final String title;
  private final Material displayMaterial;
  private final int displaySlot;
  private final Map<Integer, ShopItem> items = new HashMap<>(); // Indexed by slot for fast GUI placement

  public ShopCategory(String id, String title, Material displayMaterial, int displaySlot) {
    this.id = id;
    this.title = title;
    this.displayMaterial = displayMaterial;
    this.displaySlot = displaySlot;
  }

  public String getId() {
    return id;
  }

  public String getTitle() {
    return title;
  }

  public Material getDisplayMaterial() {
    return displayMaterial;
  }

  public int getDisplaySlot() {
    return displaySlot;
  }

  public void addItem(ShopItem item) {
    items.put(item.getSlot(), item);
  }

  public Map<Integer, ShopItem> getItems() {
    return items;
  }

  public ShopItem getItemAtSlot(int slot) {
    return items.get(slot);
  }
}
