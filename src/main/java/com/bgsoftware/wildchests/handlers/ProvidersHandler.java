package com.bgsoftware.wildchests.handlers;

import com.bgsoftware.wildchests.WildChestsPlugin;
import com.bgsoftware.wildchests.api.handlers.ProvidersManager;
import com.bgsoftware.wildchests.api.hooks.BankProvider;
import com.bgsoftware.wildchests.api.hooks.PricesProvider;
import com.bgsoftware.wildchests.api.hooks.StackerProvider;
import com.bgsoftware.wildchests.api.objects.DepositMethod;
import com.bgsoftware.wildchests.api.objects.chests.Chest;
import com.bgsoftware.wildchests.hooks.PricesProvider_Default;
import com.bgsoftware.wildchests.hooks.StackerProvider_Default;
import com.bgsoftware.wildchests.hooks.listener.IChestBreakListener;
import com.bgsoftware.wildchests.hooks.listener.IChestPlaceListener;
import com.bgsoftware.wildchests.utils.Executor;
import com.google.common.base.Preconditions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

@SuppressWarnings({"WeakerAccess", "unused"})
public final class ProvidersHandler implements ProvidersManager {

    private final WildChestsPlugin plugin;

    private final Map<DepositMethod, BankProvider> bankProviderMap = new EnumMap<>(DepositMethod.class);
    private final Map<UUID, PendingTransaction> pendingTransactions = new HashMap<>();
    private final List<IChestPlaceListener> chestPlaceListeners = new ArrayList<>();
    private final List<IChestBreakListener> chestBreakListeners = new ArrayList<>();
    private PricesProvider pricesProvider = new PricesProvider_Default();
    private StackerProvider stackerProvider = new StackerProvider_Default();

    public ProvidersHandler(WildChestsPlugin plugin) {
        this.plugin = plugin;

        Executor.sync(() -> {
            registerPricesProvider(plugin);
            registerStackersProvider();
            registerBanksProvider();
            registerGeneralHooks();

            if (bankProviderMap.isEmpty()) {
                WildChestsPlugin.log("");
                WildChestsPlugin.log("If you want sell-chests to be enabled, please install Vault & Economy plugin.");
                WildChestsPlugin.log("");
            }
        });
    }

    @Override
    public void setPricesProvider(PricesProvider pricesProvider) {
        this.pricesProvider = pricesProvider;
    }

    @Override
    public void setStackerProvider(StackerProvider stackerProvider) {
        this.stackerProvider = stackerProvider;
    }

    @Override
    public void setBanksProvider(BankProvider banksProvider) {
        Preconditions.checkNotNull(banksProvider, "bankProvider parameter cannot be null.");
        bankProviderMap.put(DepositMethod.CUSTOM, banksProvider);
    }

    /*
     * Hooks' methods
     */

    public double getPrice(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        return pricesProvider.getPrice(offlinePlayer, itemStack);
    }

    public int getItemAmount(Item item) {
        return stackerProvider.getItemAmount(item);
    }

    public void setItemAmount(Item item, int amount) {
        stackerProvider.setItemAmount(item, amount);
    }

    public boolean dropItem(Location location, ItemStack itemStack, int amount) {
        return stackerProvider.dropItem(location, itemStack, amount);
    }

    /*
     * Handler's methods
     */

    public TransactionResult<Double> canSellItem(OfflinePlayer offlinePlayer, ItemStack itemStack) {
        double price = itemStack == null ? 0 : getPrice(offlinePlayer, itemStack);
        return TransactionResult.of(price, _price -> _price > 0);
    }

    public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double money) {
        BankProvider vaultProvider = bankProviderMap.get(DepositMethod.VAULT);

        if (vaultProvider == null)
            return false;

        return vaultProvider.withdrawPlayer(offlinePlayer, money);
    }

    public void startSellingTask(OfflinePlayer offlinePlayer) {
        if (!pendingTransactions.containsKey(offlinePlayer.getUniqueId()))
            pendingTransactions.put(offlinePlayer.getUniqueId(), new PendingTransaction());
    }

    public void stopSellingTask(OfflinePlayer offlinePlayer) {
        PendingTransaction pendingTransaction = pendingTransactions.remove(offlinePlayer.getUniqueId());
        if (pendingTransaction != null)
            pendingTransaction.forEach(((depositMethod, value) -> depositPlayer(offlinePlayer, depositMethod, value)));
    }

    public boolean depositPlayer(OfflinePlayer offlinePlayer, DepositMethod depositMethod, double money) {
        BankProvider bankProvider = bankProviderMap.get(depositMethod);

        if (bankProvider == null)
            return false;

        PendingTransaction pendingTransaction = pendingTransactions.get(offlinePlayer.getUniqueId());

        if (pendingTransaction != null) {
            pendingTransaction.depositMoney(depositMethod, money);
            return true;
        }

        return bankProvider.depositMoney(offlinePlayer, BigDecimal.valueOf(money));
    }

    public void depositAllPending() {
        pendingTransactions.forEach((uuid, pendingTransaction) -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            pendingTransaction.forEach((depositMethod, value) -> depositPlayer(offlinePlayer, depositMethod, value));
        });
        pendingTransactions.clear();
    }

    public void registerChestPlaceListener(IChestPlaceListener chestPlaceListener) {
        this.chestPlaceListeners.add(chestPlaceListener);
    }

    public void notifyChestPlaceListeners(Chest chest) {
        this.chestPlaceListeners.forEach(chestPlaceListener -> chestPlaceListener.placeChest(chest));
    }

    public void registerChestBreakListener(IChestBreakListener chestBreakListener) {
        this.chestBreakListeners.add(chestBreakListener);
    }

    public void notifyChestBreakListeners(@Nullable OfflinePlayer offlinePlayer, Chest chest) {
        this.chestBreakListeners.forEach(chestBreakListener -> chestBreakListener.breakChest(offlinePlayer, chest));
    }

    private void registerPricesProvider(WildChestsPlugin plugin) {
        if (!(pricesProvider instanceof PricesProvider_Default))
            return;

        Optional<PricesProvider> pricesProvider = Optional.empty();

        switch (plugin.getSettings().pricesProvider.toUpperCase()) {
            case "CMI":
                if (Bukkit.getPluginManager().isPluginEnabled("CMI")) {
                    pricesProvider = createInstance("PricesProvider_CMI");
                }
                break;
            case "SHOPGUIPLUS":
                if (Bukkit.getPluginManager().isPluginEnabled("ShopGUIPlus")) {
                    Plugin shopGUIPlus = Bukkit.getPluginManager().getPlugin("ShopGUIPlus");
                    if (shopGUIPlus.getDescription().getVersion().startsWith("1.2")) {
                        pricesProvider = createInstance("PricesProvider_ShopGUIPlus12");
                    } else {
                        pricesProvider = createInstance("PricesProvider_ShopGUIPlus14");
                    }
                }
                break;
            case "QUANTUMSHOP":
                if (Bukkit.getPluginManager().isPluginEnabled("QuantumShop")) {
                    pricesProvider = createInstance("PricesProvider_QuantumShop");
                }
                break;
            case "ESSENTIALS":
                if (Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
                    pricesProvider = createInstance("PricesProvider_Essentials");
                }
                break;
            case "ZSHOP":
                if (Bukkit.getPluginManager().isPluginEnabled("zShop")) {
                    pricesProvider = createInstance("PricesProvider_zShop");
                }
                break;
            case "ECONOMYSHOPGUI":
                if (Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI") ||
                        Bukkit.getPluginManager().isPluginEnabled("EconomyShopGUI-Premium")) {
                    pricesProvider = createInstance("PricesProvider_EconomyShopGUI");
                }
                break;
        }

        if (!pricesProvider.isPresent()) {
            WildChestsPlugin.log("- Couldn''t find any prices providers, using default one");
            return;
        }

        this.pricesProvider = pricesProvider.get();
    }

    private void registerStackersProvider() {
        if (!(stackerProvider instanceof StackerProvider_Default))
            return;

        if (Bukkit.getPluginManager().isPluginEnabled("WildStacker")) {
            Optional<StackerProvider> stackerProvider = createInstance("StackerProvider_WildStacker");
            stackerProvider.ifPresent(this::setStackerProvider);
        }
    }

    private void registerBanksProvider() {
        if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
            Optional<BankProvider> bankProvider = createInstance("BankProvider_Vault");
            bankProvider.ifPresent(provider -> bankProviderMap.put(DepositMethod.VAULT, provider));
        }

        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2")) {
            Optional<BankProvider> bankProvider = createInstance("BankProvider_SuperiorSkyblock");
            bankProvider.ifPresent(provider -> bankProviderMap.put(DepositMethod.SUPERIORSKYBLOCK2, provider));
        }
    }

    private void registerGeneralHooks() {
        if (Bukkit.getPluginManager().isPluginEnabled("SuperiorSkyblock2"))
            registerHook("SuperiorSkyblockHook");

        if (Bukkit.getPluginManager().isPluginEnabled("ChestShop"))
            registerHook("ChestShopHook");

        if (Bukkit.getPluginManager().isPluginEnabled("TransportPipes"))
            registerHook("TransportPipesHook");

        if (Bukkit.getPluginManager().isPluginEnabled("CoreProtect"))
            registerHook("CoreProtectHook");
    }

    private void registerHook(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.wildchests.hooks." + className);
            Method registerMethod = clazz.getMethod("register", WildChestsPlugin.class);
            registerMethod.invoke(null, plugin);
        } catch (Exception ignored) {
        }
    }

    private <T> Optional<T> createInstance(String className) {
        try {
            Class<?> clazz = Class.forName("com.bgsoftware.wildchests.hooks." + className);
            try {
                Method compatibleMethod = clazz.getDeclaredMethod("isCompatible");
                if (!(boolean) compatibleMethod.invoke(null))
                    return Optional.empty();
            } catch (Exception ignored) {
            }

            try {
                Constructor<?> constructor = clazz.getConstructor(WildChestsPlugin.class);
                // noinspection unchecked
                return Optional.of((T) constructor.newInstance(plugin));
            } catch (Exception error) {
                // noinspection unchecked
                return Optional.of((T) clazz.newInstance());
            }
        } catch (ClassNotFoundException ignored) {
            return Optional.empty();
        } catch (Exception error) {
            error.printStackTrace();
            return Optional.empty();
        }
    }

    public static final class TransactionResult<T> {

        private final T data;
        private final Predicate<T> success;

        private TransactionResult(T data, Predicate<T> success) {
            this.data = data;
            this.success = success;
        }

        public static <T> TransactionResult<T> of(T data, Predicate<T> success) {
            return new TransactionResult<>(data, success);
        }

        public boolean isSuccess() {
            return success == null || success.test(data);
        }

        public T getData() {
            return data;
        }

    }

    private static final class PendingTransaction {

        private final Map<DepositMethod, MutableDouble> pendingDeposits = new EnumMap<>(DepositMethod.class);

        void depositMoney(DepositMethod depositMethod, double money) {
            pendingDeposits.computeIfAbsent(depositMethod, d -> new MutableDouble()).value += money;
        }

        void forEach(BiConsumer<DepositMethod, Double> consumer) {
            for (Map.Entry<DepositMethod, MutableDouble> entry : pendingDeposits.entrySet())
                consumer.accept(entry.getKey(), entry.getValue().value);
        }

        private static final class MutableDouble {

            private double value = 0;

        }

    }

}
