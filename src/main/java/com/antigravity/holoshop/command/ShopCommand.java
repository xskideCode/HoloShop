package com.antigravity.holoshop.command;

import com.antigravity.holoshop.HoloShopPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ShopCommand implements CommandExecutor {

  private final HoloShopPlugin plugin;

  public ShopCommand(HoloShopPlugin plugin) {
    this.plugin = plugin;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage("Only players can use the shop.");
      return true;
    }

    Player player = (Player) sender;

    if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
      if (player.hasPermission("HoloShop.admin")) {
        plugin.reloadConfig();
        plugin.getShopConfig().loadConfig();
        player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
            .deserialize(plugin.getConfig().getString("prefix", "&8[&eHoloShop&8] ") + "&aConfiguration reloaded!"));
        return true;
      }
    }

    if (args.length == 1 && args[0].equalsIgnoreCase("close")) {
      plugin.getHologramManager().closeHologram(player);
      player.sendMessage(LegacyComponentSerializer.legacyAmpersand()
          .deserialize(plugin.getConfig().getString("prefix", "&8[&eHoloShop&8] ") + "&7Hologram closed."));
      return true;
    }

    if (args.length >= 1 && args[0].equalsIgnoreCase("admin")) {
        if (player.hasPermission("HoloShop.admin")) {
            plugin.getAdminGUI().openMainMenu(player);
        } else {
            player.sendMessage("§cYou do not have permission to use the admin shop.");
        }
        return true;
    }

    if (args.length == 2 && args[0].equalsIgnoreCase("createanchor")) {
        if (player.hasPermission("HoloShop.admin")) {
            String categoryId = args[1];
            if (!plugin.getShopConfig().getCategories().containsKey(categoryId)) {
                player.sendMessage("§cCategory '" + categoryId + "' does not exist!");
                return true;
            }
            // Spawn the anchor at the block the player is looking at, or their foot location if they aren't looking at a block
            org.bukkit.block.Block targetBlock = player.getTargetBlockExact(5);
            org.bukkit.Location spawnLoc = (targetBlock != null) ? targetBlock.getLocation().add(0, 1, 0) : player.getLocation();
            plugin.getAnchorManager().createAnchor(spawnLoc, categoryId);
            player.sendMessage("§aCreated shop anchor for category: §e" + categoryId);
        } else {
            player.sendMessage("§cYou do not have permission to create anchors.");
        }
        return true;
    }

    // Open main menu
    plugin.getHologramManager().openMainMenu(player);
    plugin.getServer().getScheduler().runTask(plugin, () -> player.playSound(player.getLocation(),
        org.bukkit.Sound.valueOf(plugin.getConfig().getString("sounds.open-menu", "BLOCK_BEACON_ACTIVATE")), 1f, 1f));

    return true;
  }
}
