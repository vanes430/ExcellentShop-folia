package su.nightexpress.nexshop.util;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import su.nightexpress.economybridge.api.Currency;
import su.nightexpress.nexshop.api.shop.Shop;
import su.nightexpress.nexshop.api.shop.product.Product;
import su.nightexpress.nexshop.api.shop.product.typing.CommandTyping;
import su.nightexpress.nexshop.api.shop.product.typing.PluginTyping;
import su.nightexpress.nexshop.api.shop.product.typing.ProductTyping;
import su.nightexpress.nexshop.api.shop.product.typing.VanillaTyping;
import su.nightexpress.nexshop.api.shop.type.ShopClickAction;
import su.nightexpress.nexshop.api.shop.type.TradeType;
import su.nightexpress.nexshop.config.Config;
import su.nightexpress.nexshop.config.Perms;
import su.nightexpress.nexshop.shop.chest.config.ChestPerms;
import su.nightexpress.nexshop.shop.virtual.impl.VirtualProduct;
import su.nightexpress.nexshop.shop.virtual.impl.VirtualShop;
import su.nightexpress.nightcore.core.config.CoreLang;
import su.nightexpress.nightcore.util.*;
import su.nightexpress.nightcore.util.text.NightMessage;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ShopUtils {

    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME;

    private static DateTimeFormatter dateFormatter;

    public static void setDateFormatter(@NotNull String pattern) {
        ShopUtils.dateFormatter = DateTimeFormatter.ofPattern(pattern);
    }

    @NotNull
    public static DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    public static boolean hasCurrencyPermission(@NotNull Player player, @NotNull Currency currency) {
        boolean hasOldPerm = player.hasPermission(ChestPerms.PREFIX + "currency." + currency.getInternalId());

        return player.hasPermission(Perms.CURRENCY) || player.hasPermission(Perms.PREFIX_CURRENCY + currency.getInternalId()) || hasOldPerm;
    }

    @NotNull
    public static String generateProductId(@NotNull VirtualShop shop, @NotNull ProductTyping typing) {
        String id = switch (typing) {
            case VanillaTyping vanilla -> {
                ItemStack item = vanilla.getItem();
                String name = StringUtil.transformForID(NightMessage.stripTags(ItemUtil.getNameSerialized(item)).toLowerCase()); // Remove all non-latins from item display name.

                yield name.isBlank() ? BukkitThing.getValue(item.getType()) : name;
            }
            case PluginTyping pluginTyping -> (pluginTyping.getHandler().getName() + "_" + pluginTyping.getItemId()).toLowerCase();
            case CommandTyping ignored -> "command_item";
            default -> UUID.randomUUID().toString().substring(0, 8);
        };

        int count = 0;
        while (shop.getProductById(addCount(id, count)) != null) {
            count++;
        }

        return addCount(id, count);
    }

    @NotNull
    public static String getProductLogName(@NotNull Product product) {
        ItemStack preview = product.getPreview();
        String name = ItemUtil.getCustomNameSerialized(preview);
        if (name == null) name = ItemUtil.getItemNameSerialized(preview);
        if (name == null) name = StringUtil.capitalizeUnderscored(BukkitThing.getValue(preview.getType()));

        return name;
    }

    @NotNull
    private static String addCount(@NotNull String id, int count) {
        return count == 0 ? id : id + "_" + count;
    }

    @Nullable
    public static VirtualProduct getBestProduct(@NotNull Collection<VirtualProduct> products, @NotNull TradeType tradeType, int stackSize, @Nullable Player player) {
        Comparator<VirtualProduct> comparator = Comparator.comparingDouble(product -> product.getPrice(tradeType, player) * UnitUtils.amountToUnits(product, stackSize));
        Stream<VirtualProduct> stream = products.stream();

        return (tradeType == TradeType.BUY ? stream.min(comparator) : stream.max(comparator)).orElse(null);
    }

    @NotNull
    public static ShopClickAction getClickAction(@NotNull Player player, @NotNull ClickType click, @NotNull Shop shop, @NotNull Product product) {
        boolean isBuyable = product.isBuyable();
        boolean isSellable = product.isSellable();
        if (!isBuyable && !isSellable) return ShopClickAction.UNDEFINED;

        if (Players.isBedrock(player)) {
            if (isBuyable && isSellable) return ShopClickAction.PURCHASE_OPTION;

            return !isSellable ? ShopClickAction.BUY_SELECTION : ShopClickAction.SELL_SELECTION;
        }

        ShopClickAction action = Config.GUI_CLICK_ACTIONS.get().get(click);
        return action == null ? ShopClickAction.UNDEFINED : action;
    }

    @NotNull
    public static Set<LocalTime> parseTimes(@NotNull List<String> list) {
        return list.stream().map(timeRaw -> LocalTime.parse(timeRaw, TIME_FORMATTER)).collect(Collectors.toSet());
    }

    @NotNull
    public static Set<DayOfWeek> parseDays(@NotNull String str) {
        return Stream.of(str.split(","))
            .map(raw -> StringUtil.getEnum(raw.trim(), DayOfWeek.class).orElse(null))
            .filter(Objects::nonNull).collect(Collectors.toSet());
    }

    public static boolean canUseDialogs() {
        return Version.isAtLeast(Version.MC_1_21_7);
    }
}
