# Commands & Permissions

HoloShop is designed to be lightweight and simple, minimizing the number of commands required for daily use.

## Player Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/shop` | None | Opens the main HoloShop 3D holographic menu in front of the player's face. |
| `/shop close` | None | Manually closes the active holographic menu. (Distance and looking away also auto-closes). |

## Admin Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/shop admin` | `HoloShop.admin` | Opens the Admin In-Game Editor (Chest GUI) to manage categories and items. |
| `/shop createanchor <category_id>`| `HoloShop.admin` | Drops a physical visual anchor into the world to bind a shop menu to a specific location. |
| `/shop reload` | `HoloShop.admin` | Safely reloads `config.yml` and `shops.yml` from disk, closing all active menus for online players. |
