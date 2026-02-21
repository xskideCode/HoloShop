package com.antigravity.holoshop;

import com.antigravity.holoshop.command.ShopCommand;
import com.antigravity.holoshop.config.ShopConfig;
import com.antigravity.holoshop.economy.VaultManager;
import com.antigravity.holoshop.hologram.HologramManager;
import com.antigravity.holoshop.hologram.InteractionListener;
import com.antigravity.holoshop.admin.AnchorManager;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class HoloShopPlugin extends JavaPlugin {

  private VaultManager vaultManager;
  private ShopConfig shopConfig;
  private HologramManager hologramManager;
  private AnchorManager anchorManager;
  private com.antigravity.holoshop.admin.AdminGUI adminGUI;

  @Override
  public void onEnable() {
    saveDefaultConfig();

    // Initialize Vault
    this.vaultManager = new VaultManager(this);
    if (!vaultManager.setupEconomy()) {
      getLogger().severe("Disabled due to no Vault dependency found!");
      getServer().getPluginManager().disablePlugin(this);
      return;
    }

    // Initialize Config
    this.shopConfig = new ShopConfig(this);
    shopConfig.loadConfig();

    // Initialize Hologram Manager
    this.hologramManager = new HologramManager(this);

    // Initialize Anchor Manager
    this.anchorManager = new AnchorManager(this);

    // Initialize Admin Modules
    this.adminGUI = new com.antigravity.holoshop.admin.AdminGUI(this);
    new com.antigravity.holoshop.admin.ChatPromptManager(this);

    // Register Listeners
    getServer().getPluginManager().registerEvents(new InteractionListener(this), this);

    // Register Commands
    getCommand("shop").setExecutor(new ShopCommand(this));

    getLogger().info("HoloShop (Hologram Edition) enabled successfully!");
  }

  @Override
  public void onDisable() {
    if (hologramManager != null) {
      hologramManager.cleanupAll();
    }
    if (anchorManager != null) {
      anchorManager.despawnAll();
    }
    getLogger().info("HoloShop disabled.");
  }

  public VaultManager getVaultManager() {
    return vaultManager;
  }

  public ShopConfig getShopConfig() {
    return shopConfig;
  }

  public HologramManager getHologramManager() {
    return hologramManager;
  }

  public AnchorManager getAnchorManager() {
    return anchorManager;
  }

  public com.antigravity.holoshop.admin.AdminGUI getAdminGUI() {
    return adminGUI;
  }

  public NamespacedKey getNamespacedKey(String key) {
    return new NamespacedKey(this, key);
  }
}
