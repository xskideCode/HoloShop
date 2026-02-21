package com.antigravity.holoshop.admin;

import com.antigravity.holoshop.HoloShopPlugin;
import com.antigravity.holoshop.config.ShopCategory;
import com.antigravity.holoshop.config.ShopItem;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminGUI implements Listener {

    private final HoloShopPlugin plugin;
    private final LegacyComponentSerializer lcs = LegacyComponentSerializer.legacyAmpersand();

    public AdminGUI(HoloShopPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player player) {
        Inventory inv = Bukkit.createInventory(null, 54, lcs.deserialize("&8Admin: Categories"));

        // Load Categories
        int slot = 0;
        for (Map.Entry<String, ShopCategory> entry : plugin.getShopConfig().getCategories().entrySet()) {
            ShopCategory cat = entry.getValue();
            ItemStack item = new ItemStack(cat.getDisplayMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(lcs.deserialize("&e" + cat.getTitle()));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(lcs.deserialize("&7ID: " + entry.getKey()));
            lore.add(lcs.deserialize("&7Items: " + cat.getItems().size()));
            lore.add(lcs.deserialize(""));
            lore.add(lcs.deserialize("&eLeft-Click &7to edit items"));
            meta.lore(lore);
            item.setItemMeta(meta);
            
            // Encode category ID in the item for the click event
            ItemMeta persistentMeta = item.getItemMeta();
            persistentMeta.getPersistentDataContainer().set(plugin.getNamespacedKey("admin_cat"), org.bukkit.persistence.PersistentDataType.STRING, entry.getKey());
            item.setItemMeta(persistentMeta);

            inv.setItem(slot++, item);
        }

        player.openInventory(inv);
    }

    public void openCategoryMenu(Player player, String categoryId) {
        ShopCategory category = plugin.getShopConfig().getCategories().get(categoryId);
        if (category == null) return;

        Inventory inv = Bukkit.createInventory(null, 54, lcs.deserialize("&8Admin: " + category.getTitle()));

        // Display Items
        for (ShopItem item : category.getItems().values()) {
            ItemStack stack = new ItemStack(item.getMaterial());
            ItemMeta meta = stack.getItemMeta();
            meta.displayName(lcs.deserialize("&f" + item.getMaterial().name()));
            
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(lcs.deserialize("&7Slot: &e" + item.getSlot()));
            lore.add(lcs.deserialize("&7Buy Price: &a$" + item.getBuyPrice()));
            lore.add(lcs.deserialize("&7Sell Price: &c$" + item.getSellPrice()));
            lore.add(lcs.deserialize(""));
            lore.add(lcs.deserialize("&cRight-Click to delete."));
            lore.add(lcs.deserialize("&eLeft-Click to edit prices. (WIP)"));
            meta.lore(lore);
            
            meta.getPersistentDataContainer().set(plugin.getNamespacedKey("admin_item_cat"), org.bukkit.persistence.PersistentDataType.STRING, categoryId);
            meta.getPersistentDataContainer().set(plugin.getNamespacedKey("admin_item_slot"), org.bukkit.persistence.PersistentDataType.INTEGER, item.getSlot());
            stack.setItemMeta(meta);

            inv.setItem(item.getSlot(), stack);
        }

        // Add Item Button
        ItemStack addButton = new ItemStack(Material.EMERALD_BLOCK);
        ItemMeta addMeta = addButton.getItemMeta();
        addMeta.displayName(lcs.deserialize("&a&lAdd Item"));
        addMeta.getPersistentDataContainer().set(plugin.getNamespacedKey("admin_add_to_cat"), org.bukkit.persistence.PersistentDataType.STRING, categoryId);
        addButton.setItemMeta(addMeta);
        inv.setItem(53, addButton);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.getView().title());
        
        if (title.startsWith("Admin: ")) {
            event.setCancelled(true);
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            // Clicked a category to open it
            if (meta.getPersistentDataContainer().has(plugin.getNamespacedKey("admin_cat"), org.bukkit.persistence.PersistentDataType.STRING)) {
                String catId = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("admin_cat"), org.bukkit.persistence.PersistentDataType.STRING);
                openCategoryMenu(player, catId);
                return;
            }

            // Clicked 'Add Item'
            if (meta.getPersistentDataContainer().has(plugin.getNamespacedKey("admin_add_to_cat"), org.bukkit.persistence.PersistentDataType.STRING)) {
                String catId = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("admin_add_to_cat"), org.bukkit.persistence.PersistentDataType.STRING);
                player.closeInventory();
                
                ItemStack held = player.getInventory().getItemInMainHand();
                if (held.getType() == Material.AIR) {
                    player.sendMessage("§cYou must hold the item you want to add in your main hand!");
                    return;
                }
                
                ChatPromptManager.startAddItemPrompt(plugin, player, catId, held);
                return;
            }

            // Clicked an item to edit/delete
            if (meta.getPersistentDataContainer().has(plugin.getNamespacedKey("admin_item_cat"), org.bukkit.persistence.PersistentDataType.STRING)) {
                String catId = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("admin_item_cat"), org.bukkit.persistence.PersistentDataType.STRING);
                Integer slot = meta.getPersistentDataContainer().get(plugin.getNamespacedKey("admin_item_slot"), org.bukkit.persistence.PersistentDataType.INTEGER);
                
                if (event.isRightClick()) {
                    // Delete
                    plugin.getShopConfig().getCategories().get(catId).getItems().remove(slot);
                    plugin.getShopConfig().saveShops();
                    openCategoryMenu(player, catId);
                    player.sendMessage("§aItem deleted.");
                } else {
                    player.sendMessage("§ePrice editing GUI is WIP. Right click to delete for now.");
                }
                return;
            }
        }
    }
}
