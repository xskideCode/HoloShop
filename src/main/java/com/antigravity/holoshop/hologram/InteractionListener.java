package com.antigravity.holoshop.hologram;

import com.antigravity.holoshop.HoloShopPlugin;
import com.antigravity.holoshop.config.ShopCategory;
import com.antigravity.holoshop.config.ShopItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;

public class InteractionListener implements Listener {

  private final HoloShopPlugin plugin;
  private final LegacyComponentSerializer lcs = LegacyComponentSerializer.legacyAmpersand();

  public InteractionListener(HoloShopPlugin plugin) {
    this.plugin = plugin;
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    // Hide all existing holograms from the new player
    for (Entity entity : player.getWorld().getEntities()) {
      if (entity.getScoreboardTags().contains("holoshop_hologram")) {
        player.hideEntity(plugin, entity);
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    plugin.getHologramManager().closeHologram(event.getPlayer());
  }

  @EventHandler
  public void onInteract(PlayerInteractEntityEvent event) {
    if (!(event.getRightClicked() instanceof Interaction interaction))
      return;
      
    if (interaction.getScoreboardTags().contains("holoshop_anchor")) {
      event.setCancelled(true);
      String anchorId = interaction.getPersistentDataContainer().get(plugin.getNamespacedKey("anchor_id"), PersistentDataType.STRING);
      if (anchorId != null) {
          com.antigravity.holoshop.admin.ShopAnchor anchor = plugin.getAnchorManager().getAnchor(anchorId);
          if (anchor != null) {
              event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
              plugin.getHologramManager().openCategoryMenu(event.getPlayer(), anchor.getCategoryId(), anchor.getLocation());
          }
      }
      return;
    }

    if (!interaction.getScoreboardTags().contains("holoshop_hologram"))
      return;

    event.setCancelled(true);
    handleClick(event.getPlayer(), interaction.getPersistentDataContainer());
  }

  @EventHandler
  public void onDamage(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Interaction interaction))
      return;
      
    if (interaction.getScoreboardTags().contains("holoshop_anchor")) {
      event.setCancelled(true);
      if (event.getDamager() instanceof Player player) {
          if (player.hasPermission("HoloShop.admin") && player.isSneaking()) {
             String anchorId = interaction.getPersistentDataContainer().get(plugin.getNamespacedKey("anchor_id"), PersistentDataType.STRING);
             if (anchorId != null) {
                 boolean success = plugin.getAnchorManager().removeAnchor(anchorId, interaction.getLocation());
                 if (success) {
                     player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                     player.sendMessage(lcs.deserialize("&aPhysical shop anchor removed successfully!"));
                 } else {
                     player.sendMessage(lcs.deserialize("&eGhost anchor entities cleaned up."));
                 }
             }
          } else {
             String anchorId = interaction.getPersistentDataContainer().get(plugin.getNamespacedKey("anchor_id"), PersistentDataType.STRING);
             if (anchorId != null) {
                 com.antigravity.holoshop.admin.ShopAnchor anchor = plugin.getAnchorManager().getAnchor(anchorId);
                 if (anchor != null) {
                     player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
                     plugin.getHologramManager().openCategoryMenu(player, anchor.getCategoryId(), anchor.getLocation());
                 }
             }
          }
      }
      return;
    }

    if (!interaction.getScoreboardTags().contains("holoshop_hologram"))
      return;

    event.setCancelled(true);
    if (event.getDamager() instanceof Player player) {
      handleClick(player, interaction.getPersistentDataContainer());
    }
  }

  private void handleClick(Player player, PersistentDataContainer pdc) {
    String action = pdc.get(plugin.getNamespacedKey("action"), PersistentDataType.STRING);
    String categoryId = pdc.get(plugin.getNamespacedKey("category_id"), PersistentDataType.STRING);
    String itemIdStr = pdc.get(plugin.getNamespacedKey("item_id"), PersistentDataType.STRING);

    if (action != null) {
      handleActionClick(player, action, categoryId, itemIdStr, pdc);
      return;
    }

    if (itemIdStr != null && categoryId != null) {
      // Clicked an item entirely, open transaction menu
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
      plugin.getHologramManager().openTransactionMenu(player, categoryId, itemIdStr);
      return;
    }

    if (categoryId != null) {
      // Clicked a category
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
      plugin.getHologramManager().openCategoryMenu(player, categoryId);
    }
  }

  private void handleActionClick(Player player, String action, String categoryId, String itemIdStr,
      PersistentDataContainer pdc) {
    if ("back".equals(action)) {
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
      plugin.getHologramManager().navigateToMainMenu(player); // Anchor-safe
      return;
    }

    if ("close".equals(action)) {
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.8f);
      plugin.getHologramManager().closeHologram(player);
      return;
    }

    if ("next_page".equals(action)) {
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
      int currentPage = pdc.getOrDefault(plugin.getNamespacedKey("amount"), PersistentDataType.INTEGER, 0);
      plugin.getHologramManager().openCategoryMenu(player, categoryId, currentPage + 1);
      return;
    }

    if ("prev_page".equals(action)) {
      player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
      int currentPage = pdc.getOrDefault(plugin.getNamespacedKey("amount"), PersistentDataType.INTEGER, 0);
      plugin.getHologramManager().openCategoryMenu(player, categoryId, currentPage - 1);
      return;
    }

    Integer amount = pdc.get(plugin.getNamespacedKey("amount"), PersistentDataType.INTEGER);
    if (amount == null || categoryId == null || itemIdStr == null)
      return;

    ShopCategory category = plugin.getShopConfig().getCategories().get(categoryId);
    if (category == null)
      return;
    ShopItem shopItem = category.getItems().get(Integer.parseInt(itemIdStr));
    if (shopItem == null)
      return;

    if ("buy".equals(action)) {
      double totalCost = shopItem.getBuyPrice() * amount;
      if (plugin.getVaultManager().hasEnough(player, totalCost)) {
        ItemStack toGive = new ItemStack(shopItem.getMaterial(), amount);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(toGive);

        if (!leftover.isEmpty()) {
          player.sendMessage(
              lcs.deserialize(plugin.getConfig().getString("messages.inventory-full", "&cInventory full!")));
          toGive.setAmount(amount - leftover.values().stream().mapToInt(ItemStack::getAmount).sum());
          player.getInventory().removeItem(toGive);
          player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
          return;
        }

        plugin.getVaultManager().withdraw(player, totalCost);
        player.sendMessage(lcs
            .deserialize(plugin.getConfig().getString("messages.buy-success", "&aBought {amount}x {item} for ${price}")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", shopItem.getMaterial().name())
                .replace("{price}", plugin.getVaultManager().format(totalCost))));
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.5f, 2.0f);
        plugin.getHologramManager().openTransactionMenu(player, categoryId, itemIdStr); // Refresh balance
      } else {
        player.sendMessage(
            lcs.deserialize(plugin.getConfig().getString("messages.not-enough-money", "&cNot enough money!")));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
    } else if ("sell".equals(action)) {
      int playerHas = 0;
      for (ItemStack content : player.getInventory().getContents()) {
        if (content != null && content.getType() == shopItem.getMaterial()) {
          playerHas += content.getAmount();
        }
      }

      if (playerHas >= amount) {
        player.getInventory().removeItem(new ItemStack(shopItem.getMaterial(), amount));
        double totalEarned = shopItem.getSellPrice() * amount;
        plugin.getVaultManager().deposit(player, totalEarned);

        player.sendMessage(lcs
            .deserialize(plugin.getConfig().getString("messages.sell-success", "&aSold {amount}x {item} for ${price}")
                .replace("{amount}", String.valueOf(amount))
                .replace("{item}", shopItem.getMaterial().name())
                .replace("{price}", plugin.getVaultManager().format(totalEarned))));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
        plugin.getHologramManager().openTransactionMenu(player, categoryId, itemIdStr); // Refresh balance
      } else {
        player.sendMessage(lcs.deserialize(
            plugin.getConfig().getString("messages.not-enough-items", "&cYou don't have enough items to sell!")));
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
      }
    }
  }
}
