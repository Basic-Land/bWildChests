package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.command.ICommand;
import com.bgsoftware.wildchests.utils.ItemUtils;
import cz.devfire.bantidupe.AntiDupe;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import cz.basicland.bantidupe.bAntiDupe;

import java.util.ArrayList;
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
            ItemUtils.addItem(bAntiDupe.getApi() == null ? AntiDupe.getApi() == null ? chestItem : AntiDupe.getApi().saveItem(chestItem) : bAntiDupe.getApi().addUniqueId(chestItem), target.getInventory(), target.getLocation());
        } else {
            while (amount > 0) {
                ItemStack item = new ItemStack(chestItem);
                item.setAmount(1);
                ItemUtils.addItem(bAntiDupe.getApi() == null ? AntiDupe.getApi() == null ? item : AntiDupe.getApi().saveItem(item) : bAntiDupe.getApi().addUniqueId(item), target.getInventory(), target.getLocation());
                amount--;
            }
        }
        Locale.CHEST_GIVE_PLAYER.send(sender, target.getName(), chestItem.getAmount(), chestData.getName(), chestItem.getItemMeta().getDisplayName());
        Locale.CHEST_RECIEVE.send(target, chestItem.getAmount(), chestData.getName(), sender.getName(), chestItem.getItemMeta().getDisplayName());
    }

    @Override
    public List<String> tabComplete(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission()))
            return new ArrayList<>();

        if (args.length == 3) {
            List<String> list = new ArrayList<>();
            for (ChestData chestData : plugin.getChestsManager().getAllChestData())
                if (chestData.getName().startsWith(args[2]))
                    list.add(chestData.getName());
            return list;
        }

        if (args.length >= 4) {
            return new ArrayList<>();
        }

        return null;
    }

}
