package com.bgsoftware.wildchests.objects.chests;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.ChestType;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.api.objects.data.InventoryData;
import com.bgsoftware.wildchests.database.DatabaseObject;
import com.bgsoftware.wildchests.handlers.ChestsHandler;
import com.bgsoftware.wildchests.listeners.InventoryListener;
import com.bgsoftware.wildchests.objects.containers.TileEntityContainer;
import com.bgsoftware.wildchests.objects.inventory.CraftWildInventory;
import com.bgsoftware.wildchests.objects.inventory.InventoryHolder;
import com.bgsoftware.wildchests.objects.inventory.WildContainerItem;
import com.bgsoftware.wildchests.utils.BlockPosition;
import com.bgsoftware.wildchests.utils.ItemUtils;
import com.google.common.collect.Maps;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

public abstract class WChest extends DatabaseObject implements Chest {

    public static final Map<UUID, Chest> viewers = Maps.newHashMap();
    protected static final WildChestsPlugin plugin = WildChestsPlugin.getPlugin();
    public static Inventory guiConfirm;
    public static String guiConfirmTitle;

    protected final UUID placer;
    protected final BlockPosition blockPosition;
    protected final ChestData chestData;

    protected TileEntityContainer tileEntityContainer;
    protected boolean removed = false;

    protected WChest(UUID placer, Location location, ChestData chestData) {
        this.placer = placer;
        this.blockPosition = new BlockPosition(location.getWorld().getName(),
                location.getBlockX(), location.getBlockY(), location.getBlockZ());
        this.chestData = chestData;
    }

    /* CHEST RELATED METHODS */

    @Override
    public UUID getPlacer() {
        return placer;
    }

    @Override
    public Location getLocation() {
        return new Location(Bukkit.getWorld(this.blockPosition.getWorldName()),
                this.blockPosition.getX(), this.blockPosition.getY(), this.blockPosition.getZ());
    }

    @Override
    public ChestData getData() {
        return chestData;
    }

    @Override
    public ChestType getChestType() {
        return getData().getChestType();
    }

    @Override
    public void remove() {
        plugin.getChestsManager().removeChest(this);

        new HashSet<>(WChest.viewers.entrySet()).stream().filter(entry -> entry.getValue().equals(this)).forEach(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());

            if (player != null)
                player.closeInventory();

            WChest.viewers.remove(entry.getKey());
        });
    }

    public boolean isRemoved() {
        return removed;
    }

    public void markAsRemoved() {
        removed = true;
    }

    /* INVENTORIES / PAGES RELATED METHODS */

    @Override
    public ItemStack[] getContents() {
        List<WildContainerItem> originalContents = getWildContents();
        ItemStack[] contents = new ItemStack[originalContents.size()];

        for (int i = 0; i < contents.length; i++)
            contents[i] = originalContents.get(i).getBukkitItem();

        return contents;
    }

    public abstract List<WildContainerItem> getWildContents();

    @Override
    public Map<Integer, ItemStack> addItems(ItemStack... itemStacks) {
        Map<Integer, ItemStack> additionalItems = new HashMap<>();
        Map<Integer, ItemStack> itemAdditionalItems = new HashMap<>();

        for (ItemStack itemStack : itemStacks) {
            if (itemStack != null) {
                int currentInventory = 0;

                do {
                    Inventory inventory = getPage(currentInventory);
                    if (inventory != null) {
                        itemAdditionalItems = inventory.addItem(itemStack);
                    }
                    currentInventory++;
                } while (!itemAdditionalItems.isEmpty() && currentInventory < getPagesAmount());

                additionalItems.putAll(itemAdditionalItems);
            }
        }

        return additionalItems;
    }

    @Override
    public Map<Integer, ItemStack> addRawItems(ItemStack... itemStacks) {
        return addItems(itemStacks);
    }

    @Override
    public void removeItem(int amountToRemove, ItemStack itemStack) {
        Inventory[] pages = getPages();

        int itemsRemoved = 0;

        for (int i = 0; i < pages.length && itemsRemoved < amountToRemove; i++) {
            Inventory page = pages[i];
            int toRemove = Math.min(amountToRemove - itemsRemoved, ItemUtils.countItems(itemStack, page));
            ItemStack cloned = itemStack.clone();
            cloned.setAmount(toRemove);
            ItemStack leftOver = page.removeItem(cloned).get(0);

            if (leftOver != null)
                toRemove -= leftOver.getAmount();

            itemsRemoved += toRemove;
        }
    }

    @Override
    public ItemStack getItem(int i) {
        return getWildItem(i).getBukkitItem();
    }

    public abstract WildContainerItem getWildItem(int i);

    @Override
    public void setItem(int i, ItemStack itemStack) {
        setItem(i, plugin.getNMSInventory().createItemStack(itemStack));
    }

    public abstract void setItem(int i, WildContainerItem itemStack);

    @Override
    public abstract Inventory getPage(int page);

    @Override
    public abstract Inventory[] getPages();

    @Override
    public void setPage(int page, Inventory inventory) {
        setPage(page, new InventoryHolder(null, inventory.getContents()));
    }

    @Override
    public abstract Inventory setPage(int page, int size, String title);

    public void setPage(int page, InventoryHolder inventoryHolder) {
        Inventory inventory = setPage(page, inventoryHolder.getSize(), inventoryHolder.getTitle());
        inventory.setContents(inventoryHolder.getContents());
    }

    @Override
    public void openPage(Player player, int page) {
        viewers.put(player.getUniqueId(), this);
        plugin.getNMSInventory().openPage(player, (CraftWildInventory) getPage(page));
    }

    @Override
    public void closePage(Player player) {
        viewers.remove(player.getUniqueId());
    }

    @Override
    public abstract int getPagesAmount();

    @Override
    public abstract int getPageIndex(Inventory inventory);

    /* BLOCK-ACTIONS RELATED METHODS */

    @Override
    public boolean onBreak(BlockBreakEvent event) {
        List<ItemStack> chestContents = new LinkedList<>();
        for (int page = 0; page < getPagesAmount(); page++) {
            Inventory inventory = getPage(page);
            Collections.addAll(chestContents, inventory.getContents());
            inventory.clear();
        }

        ItemUtils.dropOrCollect(event.getPlayer(), chestContents, getData().isAutoCollect(),
                getLocation(), false);

        return true;
    }

    @Override
    public boolean onOpen(PlayerInteractEvent event) {
        viewers.put(event.getPlayer().getUniqueId(), this);
        return true;
    }

    @Override
    public boolean onClose(InventoryCloseEvent event) {
        HumanEntity player = event.getPlayer();

        //Checking if player is buying new page
        if (InventoryListener.buyNewPage.containsKey(player.getUniqueId()))
            return false;

        viewers.remove(player.getUniqueId());

        // Remove item in cursor and drop it on ground
        ItemStack itemCursor = player.getItemOnCursor();
        if (itemCursor != null && itemCursor.getType() != Material.AIR) {
            player.setItemOnCursor(new ItemStack(Material.AIR));
            ItemStack leftOvers = player.getInventory().addItem(itemCursor).get(0);
            if (leftOvers != null)
                plugin.getNMSAdapter().dropItemAsPlayer(player, leftOvers);
        }

        //Checking that it's the last player that views the inventory
        return getViewersAmount(event.getViewers()) <= 1;
    }

    @Override
    public boolean onInteract(InventoryClickEvent event) {
        if (event.getSlotType() != InventoryType.SlotType.OUTSIDE)
            return false;

        ChestData chestData = getData();
        int index = getPageIndex(event.getWhoClicked().getOpenInventory().getTopInventory());

        if (event.getClick() == ClickType.LEFT) {
            //Making sure he's not in the first page
            if (index != 0) {
                //movingBetweenPages.add(event.getWhoClicked().getUniqueId());
                openPage((Player) event.getWhoClicked(), index - 1);
                //movingBetweenPages.remove(event.getWhoClicked().getUniqueId());
            }
        } else if (event.getClick() == ClickType.RIGHT) {
            //Making sure it's not the last page
            if (index + 1 < getPagesAmount()) {
                //movingBetweenPages.add(event.getWhoClicked().getUniqueId());
                openPage((Player) event.getWhoClicked(), index + 1);
            }

            //Making sure next page is purchasble
            else if (chestData.getPagesData().containsKey(++index + 1)) {
                InventoryData inventoryData = chestData.getPagesData().get(index + 1);
                InventoryListener.buyNewPage.put(event.getWhoClicked().getUniqueId(), inventoryData);

                if (plugin.getSettings().confirmGUI) {
                    event.getWhoClicked().openInventory(guiConfirm);
                } else {
                    Locale.EXPAND_CHEST.send(event.getWhoClicked(), inventoryData.getPrice());
                    event.getWhoClicked().closeInventory();
                }
            }
        }

        return true;
    }

    @Override
    public boolean onPlace(BlockPlaceEvent event) {
        throw new UnsupportedOperationException("onPlace for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperMove(InventoryMoveItemEvent event) {
        throw new UnsupportedOperationException("onHopperMove for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperMove(ItemStack itemStack, Hopper hopper) {
        throw new UnsupportedOperationException("onHopperMove for chests is not supported anymore.");
    }

    @Override
    public boolean onHopperItemTake(Inventory hopperInventory) {
        throw new UnsupportedOperationException("onHopperItemTake for chests is not supported anymore.");
    }

    public void onChunkLoad() {
        plugin.getNMSInventory().updateTileEntity(this);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack itemStack) {
        ChestData chestData = getData();
        return !chestData.isAutoCrafter() || !chestData.isHopperFilter() ||
                chestData.containsRecipe(itemStack);
    }

    @Override
    public boolean canPlaceItemThroughFace(ItemStack itemStack) {
        return true;
    }

    @Override
    public int[] getSlotsForFace() {
        return IntStream.range(0, getPage(0).getSize() * getPagesAmount()).toArray();
    }

    /* CONTAINER RELATED METHODS */

    public TileEntityContainer getTileEntityContainer() {
        return tileEntityContainer;
    }

    public void setTileEntityContainer(TileEntityContainer tileEntityContainer) {
        this.tileEntityContainer = tileEntityContainer;
    }

    /* DATA RELATED METHODS */

    public abstract void loadFromData(ChestsHandler.UnloadedChest unloadedChest);

    public void loadFromFile(YamlConfiguration cfg) {
        if (cfg.contains("inventory")) {
            ChestData chestData = getData();
            for (String inventoryIndex : cfg.getConfigurationSection("inventory").getKeys(false)) {
                Inventory inventory = setPage(Integer.parseInt(inventoryIndex), chestData.getDefaultSize(), chestData.getTitle(Integer.parseInt(inventoryIndex) + 1));
                if (cfg.isConfigurationSection("inventory." + inventoryIndex)) {
                    for (String slot : cfg.getConfigurationSection("inventory." + inventoryIndex).getKeys(false)) {
                        try {
                            inventory.setItem(Integer.parseInt(slot), cfg.getItemStack("inventory." + inventoryIndex + "." + slot));
                        } catch (Exception ex) {
                            break;
                        }
                    }
                }
            }
        }
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WChest wChest = (WChest) o;
        return this.blockPosition.equals(wChest.blockPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.blockPosition);
    }

    private int getViewersAmount(List<HumanEntity> viewersList) {
        int viewers = 0;

        for (HumanEntity viewer : viewersList) {
            if (viewer.getGameMode() != GameMode.SPECTATOR)
                viewers++;
        }

        return viewers;
    }

}
