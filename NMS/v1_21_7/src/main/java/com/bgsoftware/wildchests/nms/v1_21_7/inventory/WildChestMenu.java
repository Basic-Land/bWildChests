package com.bgsoftware.wildchests.nms.v1_21_7.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;

public class WildChestMenu extends ChestMenu {

    private final BaseNMSMenu base;

    private WildChestMenu(MenuType<?> menuType, int id, Inventory playerInventory, WildContainer inventory, int rows) {
        super(menuType, id, playerInventory, inventory, rows);
        this.base = new BaseNMSMenu(this, playerInventory, inventory);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        return this.base.getBukkitView();
    }

    @Override
    public void removed(Player player) {
        if (!InventoryListener.buyNewPage.containsKey(player.getUUID()))
            this.base.removed(player);
    }

    public static WildChestMenu of(int id, Inventory playerInventory, WildContainer inventory) {
        MenuType<?> menuType;
        int rows;

        switch (inventory.getContainerSize()) {
            case 9 -> {
                rows = 1;
                menuType = MenuType.GENERIC_9x1;
            }
            case 18 -> {
                rows = 2;
                menuType = MenuType.GENERIC_9x2;
            }
            case 27 -> {
                rows = 3;
                menuType = MenuType.GENERIC_9x3;
            }
            case 36 -> {
                rows = 4;
                menuType = MenuType.GENERIC_9x4;
            }
            case 45 -> {
                rows = 5;
                menuType = MenuType.GENERIC_9x5;
            }
            case 54 -> {
                rows = 6;
                menuType = MenuType.GENERIC_9x6;
            }
            default -> {
                throw new IllegalArgumentException("Invalid inventory size: " + inventory.getContainerSize());
            }
        }

        return new WildChestMenu(menuType, id, playerInventory, inventory, rows);
    }

}