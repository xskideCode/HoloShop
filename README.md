# HoloShop

HoloShop is a premium, frictionless visual economy shop for Minecraft 1.21+. It eliminates clunky chat menus and traditional chest GUIs, replacing them with a gorgeous, zero-lag holographic interface.

## 📚 Documentation
This repository contains a full **GitHub Wiki** explaining how to install, configure, and manage HoloShop. 

To learn how to use the plugin, please view the Markdown files located in the `docs/wiki/` folder:
1. `docs/wiki/Home.md` - Start Here (Overview & Key Features)
2. `docs/wiki/Commands-and-Permissions.md` - Full reference of all commands
3. `docs/wiki/Admin-Guide.md` - Learn how to add, price, and manage items securely in-game
4. `docs/wiki/Physical-Anchors.md` - Learn how to create dynamic, player-tracking physical shop anchors in your world

*(Note: We recommend uploading the `docs/wiki/` files to your GitHub Wiki page for the best formatting experience).*

## Development
HoloShop is built using the Spigot API and depends on Vault for economy hooks. The plugin uses vector mathematics continuously update `ItemDisplay`, `TextDisplay`, and `Interaction` entities to create a mathematically perfect, rotating 2D flat plane without the server lag of standard Entity ArmorStands.
