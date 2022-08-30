package com.bgsoftware.wildchests.listeners;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.utils.ItemUtils;
import cz.basicland.bantidupe.bAntiDupe;
import cz.basicland.bantidupe.bAntiDupeAPI;
import cz.devfire.bantidupe.AntiDupe;
import cz.devfire.bantidupe.AntiDupeAPI;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("unused")
public final class BlockListener implements Listener {

    private final WildChestsPlugin plugin;
    private final BlockFace[] blockFaces = new BlockFace[]{BlockFace.WEST, BlockFace.EAST, BlockFace.NORTH, BlockFace.SOUTH};

    public BlockListener(WildChestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType() != Material.CHEST && e.getBlockPlaced().getType() != Material.TRAPPED_CHEST)
            return;

        boolean hasNearbyChest = false;

        for (BlockFace blockFace : blockFaces) {
            Block block = e.getBlockPlaced().getRelative(blockFace);
            Material blockMaterial = block.getType();
            if (blockMaterial == Material.CHEST || blockMaterial == Material.TRAPPED_CHEST) {
                hasNearbyChest = true;
                if (plugin.getChestsManager().getChest(block.getLocation()) != null) {
                    e.setCancelled(true);
                    return;
                }
            }
        }

        ChestData chestData = plugin.getChestsManager().getChestData(e.getItemInHand());

        if (chestData == null)
            return;

        if (hasNearbyChest) {
            e.setCancelled(true);
            return;
        }
        if (Bukkit.getPluginManager().isPluginEnabled("bAntiDupe")) {
            ItemStack itemStack = e.getItemInHand();
            Player player = e.getPlayer();
            try {
                bAntiDupeAPI api = bAntiDupe.getApi();
                if (api.isDuped(itemStack)) {
                    e.setCancelled(true);
                    api.warnPlayer(player);
                    api.removeUniqueId(itemStack, false);
                    ItemStack itemStack1 = player.getInventory().getItemInMainHand();
                    if (player.getGameMode() != GameMode.CREATIVE) itemStack1.setAmount(itemStack1.getAmount() + 1);

                    if (itemStack.equals(player.getInventory().getItemInMainHand())) {
                        api.logRemoval(player.getName(), player.getInventory().getItemInMainHand());
                        player.getInventory().remove(player.getInventory().getItemInMainHand());
                    }

                    itemStack1 = player.getInventory().getItemInOffHand();
                    if (player.getGameMode() != GameMode.CREATIVE) itemStack1.setAmount(itemStack1.getAmount() + 1);
                    if (itemStack.equals(player.getInventory().getItemInOffHand())) {
                        api.logRemoval(player.getName(), player.getInventory().getItemInOffHand());
                        player.getInventory().setItemInOffHand(null);
                    }
                    return;
                }

                api.removeUniqueId(itemStack, false);

            } catch (Exception ex) {
                AntiDupeAPI api = AntiDupe.getApi();

                if (api.isDuped(itemStack)) {
                    e.setCancelled(true);
                    api.warnPlayer(player);
                    api.removeItem(itemStack);
                    ItemStack itemStack1 = player.getInventory().getItemInMainHand();
                    if (player.getGameMode() != GameMode.CREATIVE) itemStack1.setAmount(itemStack1.getAmount() + 1);

                    if (itemStack.equals(player.getInventory().getItemInMainHand())) {
                        player.getInventory().remove(player.getInventory().getItemInMainHand());
                    }

                    itemStack1 = player.getInventory().getItemInOffHand();
                    if (player.getGameMode() != GameMode.CREATIVE) itemStack1.setAmount(itemStack1.getAmount() + 1);
                    if (itemStack.equals(player.getInventory().getItemInOffHand())) {
                        player.getInventory().setItemInOffHand(null);
                    }
                    return;
                }

                api.removeItem(itemStack);
            }
        }
        Chest chest = plugin.getChestsManager().addChest(e.getPlayer().getUniqueId(), e.getBlockPlaced().getLocation(), chestData);

        plugin.getProviders().notifyChestPlaceListeners(chest);

        //chest.onPlace(e);

        Locale.CHEST_PLACED.send(e.getPlayer(), chestData.getName(), e.getItemInHand().getItemMeta().getDisplayName());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestBreak(BlockBreakEvent e) {
        Chest chest = plugin.getChestsManager().getChest(e.getBlock().getLocation());

        if (chest == null)
            return;

        e.setCancelled(true);

        if (e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            ChestData chestData = chest.getData();
            ItemStack chestItem = chestData.getItemStack();
            ItemUtils.dropOrCollect(e.getPlayer(), bAntiDupe.getApi() == null ? AntiDupe.getApi() == null ? chestItem : AntiDupe.getApi().saveItem(chestItem) : bAntiDupe.getApi().addUniqueId(chestItem), chestData.isAutoCollect(), chest.getLocation());
        }

        chest.onBreak(e);

        plugin.getProviders().notifyChestBreakListeners(e.getPlayer(), chest);

        chest.remove();
        e.getBlock().setType(Material.AIR);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChestExplode(EntityExplodeEvent e) {
        List<Block> blockList = new ArrayList<>(e.blockList());

        Player sourcePlayer = null;

        if (e.getEntity() instanceof TNTPrimed && ((TNTPrimed) e.getEntity()).getSource() instanceof Player) {
            sourcePlayer = (Player) ((TNTPrimed) e.getEntity()).getSource();
        }

        for (Block block : blockList) {
            Chest chest = plugin.getChestsManager().getChest(block.getLocation());

            if (chest == null)
                continue;

            e.blockList().remove(block);

            if (plugin.getSettings().explodeDropChance > 0 && (plugin.getSettings().explodeDropChance == 100 ||
                    ThreadLocalRandom.current().nextInt(101) <= plugin.getSettings().explodeDropChance)) {
                ChestData chestData = chest.getData();
                ItemUtils.dropOrCollect(null, chestData.getItemStack(), false, chest.getLocation());
            }

            chest.onBreak(new BlockBreakEvent(block, sourcePlayer));

            plugin.getProviders().notifyChestBreakListeners(sourcePlayer, chest);

            chest.remove();
            block.setType(Material.AIR);
        }
    }

}
