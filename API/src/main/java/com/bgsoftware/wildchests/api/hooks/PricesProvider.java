package com.bgsoftware.wildchests.api.hooks;

import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;

public interface PricesProvider {

    double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack);

}
