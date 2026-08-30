package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoTotemSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BooleanSupplier;

public class AutoTotemModule extends Module {
    private static final int STAGE_IDLE = 0;
    private static final int STAGE_SWAP = 1;
    private static final int VERIFY_MAX = 20;

    private final AutoTotemSettings settings;
    private int stage = STAGE_IDLE;
    private int totemSlot = -1;
    private int timer = 0;
    private int verifyTicks = 0;
    private BooleanSupplier verifyTarget = () -> true;

    public AutoTotemModule(AutoTotemSettings settings) {
        super("auto_totem", "Auto Totem", settings.enabled);
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
        if (player == null || mc.level == null || player.isSpectator()
                || player.isDeadOrDying() || !(player.containerMenu instanceof InventoryMenu)
                || player.containerMenu != player.inventoryMenu) {
            reset();
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        if (verifyTicks > 0) {
            verifyTicks--;
            if (verifyTarget.getAsBoolean()) {
                verifyTicks = 0;
                finish(true);
            } else if (verifyTicks == 0) {
                verifyTicks = 0;
                finish(false);
            }
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        if (menu.getSlot(45).getItem().is(Items.TOTEM_OF_UNDYING)) {
            reset();
            return;
        }
        if (stage == STAGE_IDLE) {
            if (!menu.getCarried().isEmpty()) {
                return;
            }
            int slot = findTotemServerSlot(player);
            if (slot < 0) {
                return;
            }
            if (!menu.getSlot(45).getItem().isEmpty() && !settings.replaceOccupied) {
                return;
            }
            totemSlot = slot;
            stage = STAGE_SWAP;
        }
        if (stage == STAGE_SWAP) {
            if (mc.gameMode == null || totemSlot < 0 || totemSlot >= menu.slots.size()) {
                reset();
                return;
            }
            mc.gameMode.handleInventoryMouseClick(0, totemSlot, 40, ClickType.SWAP, player);
            stage = STAGE_IDLE;
            verifyTarget = () -> menu.getSlot(45).getItem().is(Items.TOTEM_OF_UNDYING);
            verifyTicks = VERIFY_MAX;
        }
    }

    private int findTotemServerSlot(LocalPlayer player) {
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).is(Items.TOTEM_OF_UNDYING)) {
                return i < 9 ? 36 + i : i;
            }
        }
        return -1;
    }

    private void finish(boolean success) {
        if (!success) {
            stage = STAGE_IDLE;
            totemSlot = -1;
            timer = 2;
            return;
        }
        stage = STAGE_IDLE;
        totemSlot = -1;
        timer = randomDelay(settings.swapDelayMin, settings.swapDelayMax);
    }

    private int randomDelay(int min, int max) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private void reset() {
        stage = STAGE_IDLE;
        totemSlot = -1;
        timer = 0;
        verifyTicks = 0;
        verifyTarget = () -> true;
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new BoolSetting("Replace other offhand item",
                        () -> settings.replaceOccupied, v -> settings.replaceOccupied = v),
                new IntSetting("Swap delay (min ticks)", 1, 60,
                        () -> settings.swapDelayMin, v -> settings.swapDelayMin = v),
                new IntSetting("Swap delay (max ticks)", 1, 120,
                        () -> settings.swapDelayMax, v -> settings.swapDelayMax = v)
        );
    }
}