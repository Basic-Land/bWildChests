package com.bgsoftware.wildchests.hooks;

import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import com.bgsoftware.wildstacker.WildStackerPlugin;
import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.bgsoftware.wildstacker.api.objects.StackedItem;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public final class StackerProvider_WildStacker implements StackerProvider {
    @Override
    public int getItemAmount(Item item) {
        return WildStackerAPI.getItemAmount(item);
    }

    @Override
    public void setItemAmount(Item item, int amount) {
        WildStackerAPI.getStackedItem(item).setStackAmount(amount, true);
    }

    @Override
    public boolean dropItem(Location location, ItemStack itemStack, int amount) {
        StackedItem item = WildStackerPlugin.getPlugin().getSystemManager().spawnItemWithAmount(location, itemStack, 1);
        item.getItem().setPickupDelay(20);
        item.setStackAmount(amount, true);
        item.updateName();
        return true;
    }

}
