package com.bgsoftware.wildchests.nms.v1_12_R1.inventory;

import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.nms.v1_12_R1.NMSInventory;
import com.bgsoftware.wildchests.objects.chests.WChest;
import net.minecraft.server.v1_12_R1.Container;
import net.minecraft.server.v1_12_R1.ContainerChest;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.PlayerInventory;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftInventoryView;

public class WildContainerChest extends ContainerChest {

    private final PlayerInventory playerInventory;
    private final WildInventory inventory;
    private CraftInventoryView bukkitEntity;

    private WildContainerChest(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory){
        super(playerInventory, inventory, entityHuman);
        this.playerInventory = playerInventory;
        this.inventory = inventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if(bukkitEntity == null) {
            CraftWildInventory inventory = new CraftWildInventory(this.inventory);
            bukkitEntity = new CraftInventoryView(playerInventory.player.getBukkitEntity(), inventory, this);
        }

        return bukkitEntity;
    }

    @Override
    public void b(EntityHuman entityhuman) {
        if(!InventoryListener.buyNewPage.containsKey(entityhuman.getUniqueID()))
            ((TileEntityWildChest) ((WChest) inventory.chest).getTileEntityContainer()).closeContainer(entityhuman);
    }

    public static Container of(PlayerInventory playerInventory, EntityHuman entityHuman, WildInventory inventory){
        return new WildContainerChest(playerInventory, entityHuman, inventory);
    }

}
