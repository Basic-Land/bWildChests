package com.bgsoftware.wildchests.nms.v1_21_7;

import com.bgsoftware.common.reflection.ReflectMethod;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.nms.NMSInventory;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.CraftWildInventoryImpl;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.WildChestBlockEntity;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.WildChestMenu;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.WildContainer;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.WildContainerItemImpl;
import com.bgsoftware.wildchests.nms.v1_21_7.inventory.WildHopperMenu;
import com.bgsoftware.wildchests.objects.chests.WChest;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.TickingBlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.util.CraftChatMessage;
import org.bukkit.entity.Player;

public final class NMSInventoryImpl implements NMSInventory {

    private static final ReflectMethod<TickingBlockEntity> CREATE_TICKING_BLOCK = new ReflectMethod<>(
            LevelChunk.class, "a", BlockEntity.class, BlockEntityTicker.class);

    @Override
    public void updateTileEntity(Chest chest) {
        Location location = chest.getLocation();
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            throw new IllegalArgumentException("Cannot update tile entity of chests in null world.");

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);

        if (blockEntity instanceof WildChestBlockEntity wildChestBlockEntity) {
            ((WChest) chest).setTileEntityContainer(wildChestBlockEntity);
        } else {
            WildChestBlockEntity wildChestBlockEntity = new WildChestBlockEntity(chest, serverLevel, blockPos);
            serverLevel.removeBlockEntity(blockPos);
            serverLevel.setBlockEntity(wildChestBlockEntity);
            LevelChunk levelChunk = serverLevel.getChunkAt(blockPos);
            serverLevel.addBlockEntityTicker(CREATE_TICKING_BLOCK.invoke(levelChunk, wildChestBlockEntity, wildChestBlockEntity));
        }
    }

    @Override
    public void removeTileEntity(Chest chest) {
        Location location = chest.getLocation();
        World bukkitWorld = location.getWorld();

        if (bukkitWorld == null)
            throw new IllegalArgumentException("Cannot update tile entity of chests in null world.");

        ServerLevel serverLevel = ((CraftWorld) bukkitWorld).getHandle();
        BlockPos blockPos = new BlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        BlockEntity blockEntity = serverLevel.getBlockEntity(blockPos);
        if (blockEntity instanceof WildChestBlockEntity)
            serverLevel.removeBlockEntity(blockPos);
    }

    @Override
    public WildContainerItemImpl createItemStack(org.bukkit.inventory.ItemStack bukkitItem) {
        return new WildContainerItemImpl(CraftItemStack.asNMSCopy(bukkitItem));
    }

    @Override
    public CraftWildInventory createInventory(Chest chest, int size, String title, int index) {
        WildContainer wildContainer = new WildContainer(size, title, chest, index);

        if (chest instanceof StorageChest)
            wildContainer.setItemFunction = (slot, itemStack) -> chest.setItem(slot, CraftItemStack.asCraftMirror(itemStack));

        return new CraftWildInventoryImpl(wildContainer);
    }

    @Override
    public void openPage(Player player, CraftWildInventory inventory) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        String title = inventory.getTitle();

        AbstractContainerMenu containerMenu = createMenu(serverPlayer.nextContainerCounter(), serverPlayer.getInventory(), inventory);
        containerMenu.setTitle(CraftChatMessage.fromStringOrNull(title));

        // Cursor item is not updated, so we need to update it manually
        org.bukkit.inventory.ItemStack cursorItem = player.getItemOnCursor();

        ClientboundOpenScreenPacket openScreenPacket = new ClientboundOpenScreenPacket(containerMenu.containerId,
                containerMenu.getType(), containerMenu.getTitle());

        serverPlayer.connection.send(openScreenPacket);
        serverPlayer.containerMenu = containerMenu;
        serverPlayer.initMenu(containerMenu);

        player.setItemOnCursor(cursorItem);
    }

    @Override
    public void createDesignItem(CraftWildInventory craftWildInventory,
                                 org.bukkit.inventory.ItemStack itemStack) {
        ItemStack designItem = CraftItemStack.asNMSCopy(itemStack == null || itemStack.getType() == Material.AIR ?
                new org.bukkit.inventory.ItemStack(Material.BLACK_STAINED_GLASS_PANE) : itemStack.clone());

        designItem.setCount(1);

        CustomData customData = designItem.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        customData = customData.update(compoundTag -> compoundTag.putBoolean("DesignItem", true));
        designItem.set(DataComponents.CUSTOM_DATA, customData);

        WildContainer container = ((CraftWildInventoryImpl) craftWildInventory).getInventory();
        container.setItem(0, designItem, false);
        container.setItem(1, designItem, false);
        container.setItem(3, designItem, false);
        container.setItem(4, designItem, false);
    }

    public static AbstractContainerMenu createMenu(int id, Inventory playerInventory,
                                                   CraftWildInventory craftWildInventory) {
        WildContainer container = ((CraftWildInventoryImpl) craftWildInventory).getInventory();
        return container.getContainerSize() == 5 ? WildHopperMenu.of(id, playerInventory, container) :
                WildChestMenu.of(id, playerInventory, container);
    }

}
