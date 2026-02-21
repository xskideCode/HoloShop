package com.antigravity.holoshop.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class VaultManager {

  private final JavaPlugin plugin;
  private Economy econ = null;

  public VaultManager(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public boolean setupEconomy() {
    if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
      return false;
    }
    RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
    if (rsp == null) {
      return false;
    }
    econ = rsp.getProvider();
    return econ != null;
  }

  public Economy getEconomy() {
    return econ;
  }

  public boolean hasEnough(OfflinePlayer player, double amount) {
    if (econ == null)
      return false;
    return econ.has(player, amount);
  }

  public double getBalance(OfflinePlayer player) {
    if (econ == null)
      return 0.0;
    return econ.getBalance(player);
  }

  public boolean withdraw(OfflinePlayer player, double amount) {
    if (econ == null)
      return false;
    return econ.withdrawPlayer(player, amount).transactionSuccess();
  }

  public boolean deposit(OfflinePlayer player, double amount) {
    if (econ == null)
      return false;
    return econ.depositPlayer(player, amount).transactionSuccess();
  }

  public String format(double amount) {
    if (econ == null)
      return "$" + amount;
    return econ.format(amount);
  }
}
