package com.antigravity.holoshop.config;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ShopItem {

  private final String id;
  private final Material material;
  private final int slot;
  private final double buyPrice;
  private final double sellPrice;
  private final int defaultAmount;

  public ShopItem(String id, Material material, int slot, double buyPrice, double sellPrice, int defaultAmount) {
    this.id = id;
    this.material = material;
    this.slot = slot;
    this.buyPrice = buyPrice;
    this.sellPrice = sellPrice;
    this.defaultAmount = defaultAmount;
  }

  public String getId() {
    return id;
  }

  public Material getMaterial() {
    return material;
  }

  public int getSlot() {
    return slot;
  }

  public double getBuyPrice() {
    return buyPrice;
  }

  public double getSellPrice() {
    return sellPrice;
  }

  public boolean canBuy() {
    return buyPrice > 0;
  }

  public boolean canSell() {
    return sellPrice > 0;
  }

  public int getDefaultAmount() {
    return defaultAmount;
  }

}
