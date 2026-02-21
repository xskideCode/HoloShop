# Physical Shop Anchors Tutorial

By default, typing `/shop` opens the 3D Holographic Menu directly in front of the player's face. 

However, you can also place **Physical Anchors** in your world. When a player walks up to an Anchor and clicks it, the shop menu wraps around the physical block, creating a highly immersive experience similar to Shopkeepers or Citizens!

## How to Create an Anchor
1. Stand exactly where you want the shop menu to appear.
2. Look at the block you want the anchor to sit above.
3. Type `/shop createanchor <category_id>`. (e.g., `/shop createanchor blocks`).
4. A floating visual indicator will spawn with an invisible, grief-proof hit box.

## How it Works
* **Players:** When a player Left-Clicks or Right-Clicks the anchor, the Hologram menu opens around the anchor.
* **Performance:** Anchors use 1.21 Display Entities. They do not lag the server like ArmorStands, and cannot be exploited with hoppers because they are completely visual.
* **Dynamic Tracking:** The visual beacon entity utilizes a `BukkitRunnable` to natively track and rotate towards the nearest player as they walk past it. 
* **Auto-Close:** If a player walks more than 8 blocks away from an active Anchor they clicked, the menu automatically closes to save memory.
* **Saving:** Anchors survive restarts and are saved cleanly to `plugins/HoloShop/anchors.yml`.

## Removing an Anchor
To permanently delete an anchor and its shop interface from the world:
1. Ensure you have the `HoloShop.admin` permission.
2. Slowly approach the visual anchor block.
3. Hold `Shift` (Sneak) and **Left-Click** (Punch) the anchor.
4. The anchor will shatter, its item drop will disappear, and it will be permanently cleanly un-registered from the database!
