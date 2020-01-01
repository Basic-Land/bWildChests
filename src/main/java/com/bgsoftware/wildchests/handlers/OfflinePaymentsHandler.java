package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.utils.Executor;
import com.bgsoftware.wildchests.utils.Pair;
import com.google.common.collect.Sets;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class OfflinePaymentsHandler {

    private final WildChestsPlugin plugin;
    private final Map<UUID, Set<Pair<ItemStack, Double>>> awaitingItems = new ConcurrentHashMap<>();

    public OfflinePaymentsHandler(WildChestsPlugin plugin){
        this.plugin = plugin;

        File file = new File(plugin.getDataFolder(), "offline_payments");

        if(file.exists()){
            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);

            for(String uuidKey : cfg.getConfigurationSection("").getKeys(false)) {
                Set<Pair<ItemStack, Double>> awaitingItems = Sets.newConcurrentHashSet();

                UUID uuid = UUID.fromString(uuidKey);
                String payment = cfg.getString(uuidKey);

                for (String pair : payment.split(";")) {
                    String[] pairSections = pair.split("=");
                    awaitingItems.add(new Pair<>(plugin.getNMSAdapter().deserialzeItem(pairSections[0]), Double.valueOf(pairSections[1])));
                }

                this.awaitingItems.put(uuid, awaitingItems);
            }
        }

        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> saveItems(false), 6000, 6000);
    }

    public void tryDepositItems(Player player, Consumer<Double> moneyEarned){
        if(!awaitingItems.containsKey(player.getUniqueId())) {
            moneyEarned.accept(0D);
            return;
        }

        Set<Pair<ItemStack, Double>> awaitingItems = this.awaitingItems.get(player.getUniqueId());

        if(!plugin.getProviders().isVaultEnabled()){
            moneyEarned.accept(0D);
            return;
        }

        BigDecimal totalPrice = BigDecimal.ZERO;

        for(Pair<ItemStack, Double> pair : awaitingItems) {
            totalPrice = totalPrice.add(BigDecimal.valueOf(plugin.getProviders().getPrice(player, pair.getKey(), pair.getValue())));
        }

        final BigDecimal TOTAL_PRICE = totalPrice;

        Executor.sync(() -> {
            boolean removeFromMap = true;

            if(plugin.getSettings().sellCommand.isEmpty()) {
                if(!plugin.getProviders().depositPlayer(player, TOTAL_PRICE.doubleValue())){
                    WildChestsPlugin.log("&cCouldn't deposit offline payment for " + player.getName() + "...");
                    removeFromMap = false;
                }
            }
            else{
                Executor.sync(() ->
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), plugin.getSettings().sellCommand
                                .replace("{player-name}", player.getName())
                                .replace("{price}", String.valueOf(TOTAL_PRICE))));
            }

            if(removeFromMap) {
                this.awaitingItems.remove(player.getUniqueId());
                moneyEarned.accept(TOTAL_PRICE.doubleValue());
            }

            else{
                moneyEarned.accept(0D);
            }
        });
    }

    public void addItem(UUID uuid, ItemStack itemStack, double multiplier){
        awaitingItems.computeIfAbsent(uuid, set -> Sets.newConcurrentHashSet()).add(new Pair<>(itemStack, multiplier));
    }

    public void loadItems(UUID uuid, String payment){
        Set<Pair<ItemStack, Double>> awaitingItems = Sets.newConcurrentHashSet();

        for(String pair : payment.split(";")) {
            String[] pairSections = pair.split("=");
            awaitingItems.add(new Pair<>(plugin.getNMSAdapter().deserialzeItem(pairSections[0]), Double.valueOf(pairSections[1])));
        }

        this.awaitingItems.put(uuid, awaitingItems);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void saveItems(boolean async){
        if(async && Bukkit.isPrimaryThread()){
            Executor.async(() -> saveItems(false));
            return;
        }

        File file = new File(plugin.getDataFolder(), "offline_payments");
        if(file.exists())
            file.delete();

        if(!awaitingItems.isEmpty()) {
            YamlConfiguration cfg = new YamlConfiguration();

            for (Map.Entry<UUID, Set<Pair<ItemStack, Double>>> entry : awaitingItems.entrySet()) {
                StringBuilder payment = new StringBuilder();

                for (Pair<ItemStack, Double> pair : entry.getValue()) {
                    payment.append(";").append(plugin.getNMSAdapter().serialize(pair.getKey())).append("=").append(pair.getValue());
                }

                cfg.set(entry.getKey() + "", payment.substring(1));
            }

            try{
                file.createNewFile();
                cfg.save(file);
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }
    }

}