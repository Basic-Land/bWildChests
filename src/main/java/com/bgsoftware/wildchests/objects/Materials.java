package com.bgsoftware.wildchests.objects;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum Materials {

    GREEN_STAINED_GLASS_PANE,
    RED_STAINED_GLASS_PANE,
    BLACK_STAINED_GLASS_PANE;

    public ItemStack toBukkitItem(){
        return new ItemStack(Material.matchMaterial(name()));
    }
}
