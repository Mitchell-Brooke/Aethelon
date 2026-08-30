package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoRefillSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoRefillModule extends Module {
    private static final int VERIFY_MAX = 20;

    private final AutoRefillSettings settings;
    private int coolDown = 0;
    private int verifyTicks = 0;
    private ItemStack pendingExpected = ItemStack.EMPTY;
    private ItemStack lastSeen = ItemStack.EMPTY;

    public AutoRefillModule(AutoRefillSettings settings) {
        super("auto_refill", "Auto Refill", settings.enabled);
        this.settings = settings;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        settings.enabled = enabled;
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator()
                || player.isDeadOrDying() || !(player.containerMenu instanceof InventoryMenu)
                || player.containerMenu != player.inventoryMenu) {
            reset();
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        int selected = player.getInventory().getSelectedSlot();
        if (selected < 0 || selected > 8) {
            lastSeen = ItemStack.EMPTY;
            return;
        }
        ItemStack now = menu.getSlot(36 + selected).getItem();
        if (verifyTicks > 0) {
            verifyTicks--;
            if (!now.isEmpty() && ItemStack.isSameItemSameComponents(now, pendingExpected)) {
                verifyTicks = 0;
                pendingExpected = ItemStack.EMPTY;
                coolDown = randomDelay();
            } else if (verifyTicks == 0) {
                pendingExpected = ItemStack.EMPTY;
                coolDown = 2;
            }
            lastSeen = now;
            return;
        }
        if (coolDown > 0) {
            coolDown--;
            lastSeen = now;
            return;
        }
        boolean edgeEmpty = !lastSeen.isEmpty() && now.isEmpty();
        boolean drained = !now.isEmpty()
                && lastSeen.getCount() > now.getCount()
                && now.getCount() < settings.minCount;
        if (edgeEmpty || drained) {
            attemptRefill(player, menu, now, selected, lastSeen);
        }
        lastSeen = now;
    }

    private void attemptRefill(LocalPlayer player, InventoryMenu menu, ItemStack current, int selected, ItemStack desired) {
        if (!menu.getCarried().isEmpty()) {
            return;
        }
        if (desired.isEmpty() || desired.getMaxStackSize() <= 1) {
            return;
        }
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        int bestIndex = -1;
        int bestCount = -1;
        for (int i = 9; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty() || !ItemStack.isSameItemSameComponents(stack, desired)) {
                continue;
            }
            if (stack.getCount() > bestCount) {
                bestCount = stack.getCount();
                bestIndex = i;
            }
        }
        if (bestIndex < 0) {
            return;
        }
        if (!current.isEmpty() && bestCount <= current.getCount()) {
            return;
        }
        if (bestIndex >= menu.slots.size() || Minecraft.getInstance().gameMode == null) {
            return;
        }
        Minecraft.getInstance().gameMode.handleInventoryMouseClick(0, bestIndex, selected, ClickType.SWAP, player);
        pendingExpected = desired;
        verifyTicks = VERIFY_MAX;
    }

    private int randomDelay() {
        int lo = Math.max(1, settings.clickDelayMin);
        int hi = Math.max(lo, settings.clickDelayMax);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private void reset() {
        coolDown = 0;
        verifyTicks = 0;
        pendingExpected = ItemStack.EMPTY;
        lastSeen = ItemStack.EMPTY;
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Refill below count", 1, 64,
                        () -> settings.minCount, v -> settings.minCount = v),
                new IntSetting("Click delay (min ticks)", 1, 20,
                        () -> settings.clickDelayMin, v -> settings.clickDelayMin = v),
                new IntSetting("Click delay (max ticks)", 1, 40,
                        () -> settings.clickDelayMax, v -> settings.clickDelayMax = v)
        );
    }
}