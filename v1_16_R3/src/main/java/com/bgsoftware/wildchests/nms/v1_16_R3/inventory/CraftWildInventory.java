package com.bgsoftware.wildchests.nms.v1_16_R3.inventory;

import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import net.minecraft.server.v1_16_R3.IInventory;
import org.bukkit.craftbukkit.v1_16_R3.inventory.CraftInventory;

import java.util.List;

public class CraftWildInventory extends CraftInventory implements com.bgsoftware.wildchests.objects.inventory.CraftWildInventory {

    public CraftWildInventory(IInventory inventory) {
        super(inventory);
    }

    @Override
    public Chest getOwner() {
        return getInventory().chest;
    }

    @Override
    public WildContainerItemImpl getWildItem(int slot) {
        return getInventory().getWildItem(slot);
    }

    @Override
    public void setItem(int i, WildContainerItem itemStack) {
        getInventory().setItem(i, (WildContainerItemImpl) itemStack, true);
    }

    @Override
    public List<WildContainerItem> getWildContents() {
        return getInventory().items;
    }

    @Override
    public WildInventory getInventory() {
        return (WildInventory) super.getInventory();
    }

    @Override
    public String getTitle() {
        return getInventory().getTitle();
    }

    @Override
    public void setTitle(String title) {
        getInventory().setTitle(title);
    }

    @Override
    public boolean isFull() {
        return getInventory().isFull();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CraftWildInventory && getInventory() == ((CraftWildInventory) obj).getInventory();
    }

}
