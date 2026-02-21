package com.antigravity.holoshop.admin;

import com.antigravity.holoshop.HoloShopPlugin;
import com.antigravity.holoshop.config.ShopCategory;
import com.antigravity.holoshop.config.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatPromptManager implements Listener {

    private final HoloShopPlugin plugin;
    private static final Map<UUID, PromptState> activePrompts = new HashMap<>();

    public ChatPromptManager(HoloShopPlugin plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public static void startAddItemPrompt(HoloShopPlugin plugin, Player player, String categoryId, ItemStack item) {
        PromptState state = new PromptState(categoryId, item);
        activePrompts.put(player.getUniqueId(), state);
        player.sendMessage("§a[HoloShop Admin] §fType the §abuy price§f for this item in chat (or type 'cancel'):");
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activePrompts.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        String message = event.getMessage();

        if (message.equalsIgnoreCase("cancel")) {
            activePrompts.remove(player.getUniqueId());
            player.sendMessage("§cItem addition cancelled.");
            return;
        }

        PromptState state = activePrompts.get(player.getUniqueId());

        try {
            double price = Double.parseDouble(message);

            if (!state.hasBuyPrice) {
                state.buyPrice = price;
                state.hasBuyPrice = true;
                player.sendMessage("§aBuy price set to §e" + price);
                player.sendMessage("§a[HoloShop Admin] §fType the §csell price§f for this item in chat (or type -1 to disable):");
            } else {
                state.sellPrice = price;
                activePrompts.remove(player.getUniqueId());

                // Run on main thread to modify config
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ShopCategory category = plugin.getShopConfig().getCategories().get(state.categoryId);
                    if (category != null) {
                        int nextSlot = 0;
                        for (Integer s : category.getItems().keySet()) {
                            if (s >= nextSlot) nextSlot = s + 1;
                        }
                        
                        String newItemId = "item_" + nextSlot; // Generate an ID for the new item
                        ShopItem newItem = new ShopItem(
                                newItemId,
                                state.item.getType(),
                                nextSlot,
                                state.buyPrice,
                                state.sellPrice,
                                state.item.getAmount() // Store amount natively
                        );

                        category.getItems().put(nextSlot, newItem);
                        plugin.getShopConfig().saveShops();
                        player.sendMessage("§aSuccessfully added §e" + state.item.getType().name() + " §ato category §e" + state.categoryId + "§a.");
                        
                        // Re-open category GUI
                        // Hacky way to get the admin GUI instance
                        new AdminGUI(plugin).openCategoryMenu(player, state.categoryId);
                    }
                });
            }

        } catch (NumberFormatException e) {
            player.sendMessage("§cInvalid number. Please type a valid price (e.g. 5.5) or type 'cancel'.");
        }
    }

    private static class PromptState {
        String categoryId;
        ItemStack item;
        double buyPrice = 0;
        double sellPrice = 0;
        boolean hasBuyPrice = false;

        PromptState(String categoryId, ItemStack item) {
            this.categoryId = categoryId;
            this.item = item;
        }
    }
}
