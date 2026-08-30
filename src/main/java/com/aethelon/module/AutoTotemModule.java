package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoTotemSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    private static final int STAGE_PICKUP_TOTEM = 1;
    private static final int STAGE_PLACE_OFFHAND = 2;
    private static final int STAGE_PLACE_BACK = 3;
    private static final int OFFHAND_SLOT = 45;
    private static final int VERIFY_MAX = 15;

    private final AutoTotemSettings settings;
    private int stage = STAGE_IDLE;
    private int totemSlot = -1;
    private int timer = 0;
    private boolean screenOwned = false;
    private int verifyTicks = 0;
    private int nextStage = STAGE_IDLE;
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
        if (mc.screen != null && !(mc.screen instanceof InventoryScreen)) {
            reset();
            return;
        }
        InventoryMenu menu = player.inventoryMenu;
        if (verifyTicks > 0) {
            verifyTicks--;
            if (verifyTarget.getAsBoolean()) {
                stage = nextStage;
                nextStage = STAGE_IDLE;
                verifyTicks = 0;
                timer = randomClickGap();
            } else if (verifyTicks == 0) {
                reset();
            }
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        if (!menu.getSlot(OFFHAND_SLOT).getItem().is(Items.TOTEM_OF_UNDYING)) {
            if (stage == STAGE_IDLE) {
                if (!menu.getCarried().isEmpty()) {
                    return;
                }
                int slot = findTotemServerSlot(player);
                if (slot < 0) {
                    return;
                }
                if (!menu.getSlot(OFFHAND_SLOT).getItem().isEmpty() && !settings.replaceOccupied) {
                    return;
                }
                if (mc.screen == null) {
                    mc.setScreen(new InventoryScreen(player));
                    screenOwned = true;
                    timer = 2;
                    return;
                }
                totemSlot = slot;
                stage = STAGE_PICKUP_TOTEM;
            }
        } else {
            reset();
            return;
        }
        switch (stage) {
            case STAGE_PICKUP_TOTEM -> doPickup(player, totemSlot, STAGE_PLACE_OFFHAND,
                    () -> !menu.getCarried().isEmpty());
            case STAGE_PLACE_OFFHAND -> doPlaceOffhand(player);
            case STAGE_PLACE_BACK -> doPlaceBack(player);
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

    private void doPickup(LocalPlayer player, int serverSlot, int nextStage, BooleanSupplier target) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        if (mc.gameMode == null || serverSlot < 0 || serverSlot >= menu.slots.size()) {
            reset();
            return;
        }
        if (menu.getSlot(serverSlot).getItem().isEmpty()) {
            reset();
            return;
        }
        mc.gameMode.handleInventoryMouseClick(0, serverSlot, 0, ClickType.PICKUP, player);
        this.nextStage = nextStage;
        this.verifyTarget = target;
        this.verifyTicks = VERIFY_MAX;
    }

    private void doPlaceOffhand(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        if (mc.gameMode == null) {
            reset();
            return;
        }
        if (menu.getCarried().isEmpty()) {
            finish(player);
            return;
        }
        if (menu.getSlot(OFFHAND_SLOT).getItem().is(Items.TOTEM_OF_UNDYING)) {
            finish(player);
            return;
        }
        mc.gameMode.handleInventoryMouseClick(0, OFFHAND_SLOT, 0, ClickType.PICKUP, player);
        this.nextStage = STAGE_PLACE_BACK;
        this.verifyTarget = () -> menu.getSlot(OFFHAND_SLOT).getItem().is(Items.TOTEM_OF_UNDYING);
        this.verifyTicks = VERIFY_MAX;
    }

    private void doPlaceBack(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        if (mc.gameMode == null) {
            reset();
            return;
        }
        if (menu.getCarried().isEmpty()) {
            finish(player);
            return;
        }
        int target = totemSlot;
        if (target < 0 || !menu.getSlot(target).getItem().isEmpty()) {
            target = freeInventoryServerSlot(menu);
            if (target < 0) {
                timer = 2;
                return;
            }
        }
        mc.gameMode.handleInventoryMouseClick(0, target, 0, ClickType.PICKUP, player);
        this.nextStage = STAGE_IDLE;
        this.verifyTarget = () -> menu.getCarried().isEmpty();
        this.verifyTicks = VERIFY_MAX;
    }

    private void finish(LocalPlayer player) {
        if (player.inventoryMenu.getSlot(OFFHAND_SLOT).getItem().is(Items.TOTEM_OF_UNDYING)) {
            closeOwnedScreen(player);
            stage = STAGE_IDLE;
            totemSlot = -1;
            timer = randomDelay(settings.swapDelayMin, settings.swapDelayMax);
        } else {
            closeOwnedScreen(player);
            stage = STAGE_IDLE;
            totemSlot = -1;
            timer = 2;
        }
    }

    private void closeOwnedScreen(LocalPlayer player) {
        if (screenOwned && Minecraft.getInstance().screen instanceof InventoryScreen) {
            player.closeContainer();
        }
        screenOwned = false;
    }

    private int freeInventoryServerSlot(InventoryMenu menu) {
        for (int i = 36; i <= 44; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                return i;
            }
        }
        for (int i = 9; i <= 35; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    private int randomDelay(int min, int max) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private int randomClickGap() {
        int lo = Math.max(1, settings.swapDelayMin);
        int hi = Math.max(lo, settings.swapDelayMax);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private void reset() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (screenOwned && player != null && mc.screen instanceof InventoryScreen) {
            player.closeContainer();
        }
        screenOwned = false;
        stage = STAGE_IDLE;
        totemSlot = -1;
        timer = 0;
        verifyTicks = 0;
        verifyTarget = () -> true;
        nextStage = STAGE_IDLE;
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