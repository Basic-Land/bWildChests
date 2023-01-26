package com.bgsoftware.wildchests.objects.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import org.bukkit.inventory.Inventory;

import java.util.List;

public interface CraftWildInventory extends Inventory {

    Chest getOwner();

    WildContainerItem getWildItem(int slot);

    void setItem(int i, WildContainerItem itemStack);

    List<WildContainerItem> getWildContents();

    String getTitle();

    void setTitle(String title);

    boolean isFull();

}
