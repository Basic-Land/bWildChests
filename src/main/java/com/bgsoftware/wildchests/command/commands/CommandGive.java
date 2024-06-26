package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.utils.ItemUtils;
import cz.basicland.blibs.spigot.hooks.Check;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class CommandGive implements ICommand {

    @Override
    public String getLabel() {
        return "give";
    }

    @Override
    public String getUsage() {
        return "chests give <player-name> <chest-name> [amount]";
    }

    @Override
    public String getPermission() {
        return "wildchests.give";
    }

    @Override
    public String getDescription() {
        return "Give a custom chest to a player.";
    }

    @Override
    public int getMinArgs() {
        return 3;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        Player target = Bukkit.getPlayer(args[1]);

        if (target == null) {
            Locale.INVALID_PLAYER.send(sender, args[1]);
            return;
        }

        ChestData chestData = plugin.getChestsManager().getChestData(args[2]);

        if (chestData == null) {
            Locale.INVALID_CHEST.send(sender, args[2]);
            return;
        }

        ItemStack chestItem = chestData.getItemStack();

        if (args.length == 4) {
            try {
                chestItem.setAmount(Integer.valueOf(args[3]));
            } catch (IllegalArgumentException ex) {
                Locale.INVALID_AMOUNT.send(sender);
                return;
            }
        }

        int amount = chestItem.getAmount();
        if (amount == 1) {
            Check.add(chestItem);
            ItemUtils.addItem(chestItem, target.getInventory(), target.getLocation());
        } else {
            while (amount > 0) {
                ItemStack item = new ItemStack(chestItem);
                item.setAmount(1);
                Check.add(item);
                ItemUtils.addItem(item, target.getInventory(), target.getLocation());
                amount--;
            }
        }
        Locale.CHEST_GIVE_PLAYER.send(sender, target.getName(), chestItem.getAmount(), chestData.getName(), chestItem.getItemMeta().getDisplayName());
        Locale.CHEST_RECIEVE.send(target, chestItem.getAmount(), chestData.getName(), sender.getName(), chestItem.getItemMeta().getDisplayName());
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission()))
            return Collections.emptyList();

        if (args.length == 3) {
            List<String> list = new LinkedList<>();
            for (ChestData chestData : plugin.getChestsManager().getAllChestData())
                if (chestData.getName().startsWith(args[2]))
                    list.add(chestData.getName());
            return list;
        }

        if (args.length >= 4) {
            return Collections.emptyList();
        }

        return null;
    }

}
