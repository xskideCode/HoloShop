package com.antigravity.holoshop.admin;

import org.bukkit.Location;

public class ShopAnchor {
    private final String id;
    private final Location location;
    private final String categoryId;

    public ShopAnchor(String id, Location location, String categoryId) {
        this.id = id;
        this.location = location;
        this.categoryId = categoryId;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public String getCategoryId() {
        return categoryId;
    }
}
