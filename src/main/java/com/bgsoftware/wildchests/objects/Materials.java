package com.bgsoftware.wildchests.objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Materials {

    GREEN_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 13),
    RED_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 14),
    BLACK_STAINED_GLASS_PANE("STAINED_GLASS_PANE", 15);

    private static final boolean isLegacy = isLegacy();
    private final String legacyType;
    private final byte legacyData;

    Materials(String legacyType, int legacyData) {
        this.legacyType = legacyType;
        this.legacyData = (byte) legacyData;
    }

    private static boolean isLegacy() {
        try {
            Material.valueOf("STAINED_GLASS_PANE");
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public ItemStack toBukkitItem() {
        return !isLegacy ? new ItemStack(Material.matchMaterial(name())) : new ItemStack(Material.matchMaterial(legacyType), 1, legacyData);
    }

}
