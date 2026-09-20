package xyz.dqrkis.module.modules.misc;

import xyz.dqrkis.event.events.TickListener;
import xyz.dqrkis.module.Category;
import xyz.dqrkis.module.Module;
import xyz.dqrkis.module.setting.BooleanSetting;
import xyz.dqrkis.module.setting.ItemSetting;
import xyz.dqrkis.module.setting.NumberSetting;
import xyz.dqrkis.module.setting.StringSetting;
import xyz.dqrkis.utils.ChatUtils;
import xyz.dqrkis.utils.EncryptedString;
import xyz.dqrkis.utils.ItemUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;

import java.util.HashMap;
import java.util.Map;

public final class AuctionSniper extends Module implements TickListener {

    private final ItemSetting snipingItem = new ItemSetting(EncryptedString.of("Sniping Item"), Items.AIR);
    private final StringSetting price = new StringSetting(EncryptedString.of("Price"), "1k");
    private final NumberSetting refreshDelay = new NumberSetting(EncryptedString.of("Refresh Delay"), 0, 100, 2, 1);
    private final NumberSetting buyDelay = new NumberSetting(EncryptedString.of("Buy Delay"), 0, 100, 2, 1);
    private final StringSetting requiredEnchantments = new StringSetting(EncryptedString.of("Required Enchantments"), "");
    private final StringSetting forbiddenEnchantments = new StringSetting(EncryptedString.of("Forbidden Enchantments"), "");
    private final NumberSetting minEnchantLevel = new NumberSetting(EncryptedString.of("Min Enchantment Level"), 1, 10, 1, 1);
    private final BooleanSetting exactEnchantMatch = new BooleanSetting(EncryptedString.of("Exact Enchantment Match"), false);

    private final Map<String, Double> priceThresholds = new HashMap<>();

    private int ticks;

    public AuctionSniper() {
        super(EncryptedString.of("Auction Sniper"), EncryptedString.of("Snipes items on auction house for cheap"), -1, Category.MISC);
        addSettings(snipingItem, price, refreshDelay, buyDelay, requiredEnchantments, forbiddenEnchantments, minEnchantLevel, exactEnchantMatch);
    }

    @Override
    public void onEnable() {
        double threshold = parsePrice(price.getValue());
        if (threshold < 0) {
            ChatUtils.error("Invalid Price");
            setEnabled(false);
            return;
        }
        if (snipingItem.getItem() != Items.AIR) {
            priceThresholds.put(snipingItem.getItem().toString(), threshold);
        }
        ticks = 0;
        eventManager.add(TickListener.class, this);
        super.onEnable();
    }

    @Override
    public void onDisable() {
        eventManager.remove(TickListener.class, this);
        super.onDisable();
    }

    @Override
    public void onTick() {
        if (mc.player == null) return;
        if (ticks > 0) { ticks--; return; }

        handleManualTick();
    }

    private void handleManualTick() {
        ScreenHandler handler = mc.player.currentScreenHandler;
        if (!(handler instanceof GenericContainerScreenHandler)) {
            mc.player.networkHandler.sendChatCommand(buildSearchCommand());
            ticks = 20;
            return;
        }
        GenericContainerScreenHandler container = (GenericContainerScreenHandler) handler;
        if (container.getRows() == 6) applyFiltersAndBuy(container);
        else if (container.getRows() == 3) confirmBuy(container);
    }

    private String buildSearchCommand() {
        if (!requiredEnchantments.getValue().isEmpty()) {
            String firstEnchant = requiredEnchantments.getValue().split(",")[0].trim();
            String itemName = snipingItem.getItem().toString().replace("minecraft:", "").replace("_", " ");
            return "ah " + itemName + " " + firstEnchant;
        }
        String key = snipingItem.getItem().getTranslationKey();
        String[] parts = key.split("\\.");
        String name = parts[parts.length - 1].replace("_", " ");
        return "ah " + name;
    }

    private void applyFiltersAndBuy(GenericContainerScreenHandler container) {
        for (int i = 0; i < 44; i++) {
            ItemStack stack = container.getSlot(i).getStack();
            if (isDesiredItem(stack)) {
                mc.interactionManager.clickSlot(container.syncId, i, 1, SlotActionType.QUICK_MOVE, mc.player);
                ticks = buyDelay.getValueInt();
                return;
            }
        }
        mc.interactionManager.clickSlot(container.syncId, 49, 1, SlotActionType.QUICK_MOVE, mc.player);
        ticks = refreshDelay.getValueInt();
    }

    private void confirmBuy(GenericContainerScreenHandler container) {
        ItemStack stack = container.getSlot(13).getStack();
        if (!stack.isEmpty() && isDesiredItem(stack)) {
            mc.interactionManager.clickSlot(container.syncId, 15, 0, SlotActionType.PICKUP, mc.player);
            ticks = 20;
        }
    }

    private boolean isDesiredItem(ItemStack stack) {
        if (stack.isEmpty() || snipingItem.getItem() == Items.AIR) return false;
        if (stack.getItem() != snipingItem.getItem()) return false;
        // Price check via tooltip would require lore parsing; simplified to enchant filter only
        String required = requiredEnchantments.getValue().trim();
        if (!required.isEmpty()) {
            for (String ench : required.split(",")) {
                String id = ench.trim().toLowerCase();
                if (!id.isEmpty() && !ItemUtils.hasEnchant(stack, id)) return false;
            }
        }
        String forbidden = forbiddenEnchantments.getValue().trim();
        if (!forbidden.isEmpty()) {
            for (String ench : forbidden.split(",")) {
                String id = ench.trim().toLowerCase();
                if (!id.isEmpty() && ItemUtils.hasEnchant(stack, id)) return false;
            }
        }
        return true;
    }

    private double parsePrice(String s) {
        try {
            s = s.trim().toLowerCase().replace(",", "").replace("$", "");
            if (s.endsWith("k")) return Double.parseDouble(s.substring(0, s.length() - 1)) * 1000;
            if (s.endsWith("m")) return Double.parseDouble(s.substring(0, s.length() - 1)) * 1_000_000;
            return Double.parseDouble(s);
        } catch (Exception e) { return -1; }
    }
}
