package com.bgsoftware.wildchests.objects.data;

import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import org.bukkit.ChatColor;

public class WInventoryData implements InventoryData {

    private String title;
    private double price;

    public WInventoryData(String title, double price) {
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.price = price;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public double getPrice() {
        return price;
    }
}
