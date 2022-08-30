package com.bgsoftware.wildchests.command.commands;

import com.bgsoftware.wildchests.Locale;
import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.objects.chests.StorageChest;
import com.bgsoftware.wildchests.api.objects.data.ChestData;
import com.bgsoftware.wildchests.command.ICommand;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public final class CommandSet implements ICommand {

    @Override
    public String getLabel() {
        return "set";
    }

    @Override
    public String getUsage() {
        return "chests set <amount>";
    }

    @Override
    public String getPermission() {
        return "wildchests.set";
    }

    @Override
    public String getDescription() {
        return "Set a storage chest content size.";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void perform(WildChestsPlugin plugin, CommandSender sender, String[] args) {
        Player target = (Player) sender;
        Block block = target.getLocation().getBlock();

        if (block == null) {
            Locale.INVALID_CHEST.send(sender);
            return;
        }

        StorageChest storageChest = (StorageChest) plugin.getChestsManager().getChest(block.getLocation());
        if (storageChest == null) return;
        storageChest.setAmount(BigInteger.valueOf(Long.parseLong(args[1])));
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