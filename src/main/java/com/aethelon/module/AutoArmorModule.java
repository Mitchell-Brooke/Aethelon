package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoArmorSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoArmorModule extends Module {
    private static final int STAGE_IDLE = 0;
    private static final int STAGE_QUICK_MOVE = 1;
    private static final int STAGE_PICKUP = 2;
    private static final int STAGE_PICKUP_ARMOR = 3;
    private static final int STAGE_PLACE_BACK = 4;

    private final AutoArmorSettings settings;
    private int timer = 0;
    private int stage = STAGE_IDLE;
    private int pendSlot = -1;
    private int pendArmorSlot = -1;
    private ItemStack pendItem = ItemStack.EMPTY;

    public AutoArmorModule(AutoArmorSettings settings) {
        super("auto_armor", "Auto Armor", settings.enabled);
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
        if (timer > 0) {
            timer--;
            return;
        }
        switch (stage) {
            case STAGE_IDLE -> {
                if (!player.inventoryMenu.getCarried().isEmpty()) {
                    stage = STAGE_PLACE_BACK;
                    timer = 1;
                } else {
                    scan(player);
                }
            }
            case STAGE_QUICK_MOVE -> doQuickMove(player, pendSlot);
            case STAGE_PICKUP -> doPickup(player, pendSlot, STAGE_PICKUP_ARMOR);
            case STAGE_PICKUP_ARMOR -> doPickup(player, pendArmorSlot, STAGE_PLACE_BACK);
            case STAGE_PLACE_BACK -> doPlaceBack(player);
        }
    }

    private void scan(LocalPlayer player) {
        InventoryMenu menu = player.inventoryMenu;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot type : slots) {
            if (!menu.getCarried().isEmpty()) {
                return;
            }
            int armorSlot = armorServerSlot(type);
            ItemStack worn = menu.getSlot(armorSlot).getItem();
            if (!worn.isEmpty() && armorDefense(worn, type) <= 0.0D) {
                continue;
            }
            int bestIndex = -1;
            double bestScore = Double.NEGATIVE_INFINITY;
            ItemStack bestStack = ItemStack.EMPTY;
            List<ItemStack> items = player.getInventory().getNonEquipmentItems();
            for (int i = 0; i < items.size(); i++) {
                ItemStack stack = items.get(i);
                if (stack.isEmpty()) {
                    continue;
                }
                if (player.getEquipmentSlotForItem(stack) != type) {
                    continue;
                }
                double defense = armorDefense(stack, type);
                if (defense <= 0.0D) {
                    continue;
                }
                if (!worn.isEmpty() && ItemStack.isSameItemSameComponents(stack, worn)) {
                    continue;
                }
                double score = score(stack, defense);
                if (score > bestScore) {
                    bestScore = score;
                    bestIndex = i;
                    bestStack = stack;
                }
            }
            if (bestIndex >= 0) {
                double wornScore = worn.isEmpty() ? 0.0D : score(worn, armorDefense(worn, type));
                if (bestScore >= wornScore + settings.upgradeThreshold) {
                    if (!worn.isEmpty() && countFreeInventorySlots(menu) < 1) {
                        continue;
                    }
                    startEquip(player, bestIndex, bestStack, worn.isEmpty());
                    return;
                }
            }
        }
    }

    private void startEquip(LocalPlayer player, int itemIndex, ItemStack stack, boolean quickMove) {
        int serverSlot = itemIndex < 9 ? 36 + itemIndex : itemIndex;
        int armorSlot = armorServerSlot(player.getEquipmentSlotForItem(stack));
        pendSlot = serverSlot;
        pendArmorSlot = armorSlot;
        pendItem = stack.copy();
        stage = quickMove ? STAGE_QUICK_MOVE : STAGE_PICKUP;
        timer = 0;
    }

    private void doQuickMove(LocalPlayer player, int serverSlot) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        if (mc.gameMode == null || serverSlot < 0 || serverSlot >= menu.slots.size()) {
            reset();
            return;
        }
        ItemStack atSlot = menu.getSlot(serverSlot).getItem();
        if (atSlot.isEmpty()) {
            reset();
            return;
        }
        int armorSlot = armorServerSlot(player.getEquipmentSlotForItem(atSlot));
        if (!menu.getSlot(armorSlot).getItem().isEmpty()) {
            reset();
            return;
        }
        mc.gameMode.handleInventoryMouseClick(0, serverSlot, 0, ClickType.QUICK_MOVE, player);
        pendSlot = -1;
        pendArmorSlot = -1;
        pendItem = ItemStack.EMPTY;
        stage = STAGE_IDLE;
        timer = randomDelay(settings.equipDelayMin, settings.equipDelayMax);
    }

    private void doPickup(LocalPlayer player, int serverSlot, int nextStage) {
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
        advance(nextStage, false);
    }

    private void doPlaceBack(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        if (mc.gameMode == null) {
            reset();
            return;
        }
        int target = pendSlot;
        if (target < 0 || !menu.getSlot(target).getItem().isEmpty()) {
            target = freeInventoryServerSlot(menu);
            if (target < 0) {
                boolean midSwap = pendArmorSlot >= 5 && pendArmorSlot <= 8 && pendSlot >= 9;
                if (!midSwap) {
                    timer = 2;
                    return;
                }
                mc.gameMode.handleInventoryMouseClick(0, pendArmorSlot, 0, ClickType.PICKUP, player);
                mc.gameMode.handleInventoryMouseClick(0, pendSlot, 0, ClickType.PICKUP, player);
                stage = STAGE_IDLE;
                timer = randomDelay(settings.equipDelayMin, settings.equipDelayMax);
                pendSlot = -1;
                pendArmorSlot = -1;
                pendItem = ItemStack.EMPTY;
                return;
            }
            pendSlot = target;
        }
        mc.gameMode.handleInventoryMouseClick(0, target, 0, ClickType.PICKUP, player);
        stage = STAGE_IDLE;
        timer = randomDelay(settings.equipDelayMin, settings.equipDelayMax);
        pendSlot = -1;
        pendArmorSlot = -1;
        pendItem = ItemStack.EMPTY;
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

    private int countFreeInventorySlots(InventoryMenu menu) {
        int count = 0;
        for (int i = 9; i <= 44; i++) {
            if (menu.getSlot(i).getItem().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private void advance(int nextStage, boolean finishDelay) {
        stage = nextStage;
        timer = finishDelay
                ? randomDelay(settings.equipDelayMin, settings.equipDelayMax)
                : randomClickGap();
    }

    private int armorServerSlot(EquipmentSlot type) {
        return switch (type) {
            case HEAD -> 5;
            case CHEST -> 6;
            case LEGS -> 7;
            case FEET -> 8;
            default -> -1;
        };
    }

    private double armorDefense(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0.0D};
        stack.forEachModifier(slot, (Holder<Attribute> attr, AttributeModifier mod) -> {
            if (attr.value().equals(Attributes.ARMOR.value())) {
                total[0] += mod.amount();
            }
        });
        return total[0];
    }

    private double score(ItemStack stack, double defense) {
        double score = defense;
        if ("enchants".equals(settings.scoreMode)) {
            score += protectionLevel(stack) * 3.0D;
        }
        return score;
    }

    private int protectionLevel(ItemStack stack) {
        int level = 0;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
            Holder<Enchantment> holder = entry.getKey();
            if (holder.is(Enchantments.PROTECTION)
                    || holder.is(Enchantments.FIRE_PROTECTION)
                    || holder.is(Enchantments.BLAST_PROTECTION)
                    || holder.is(Enchantments.PROJECTILE_PROTECTION)) {
                level += entry.getIntValue();
            }
        }
        return level;
    }

    private int randomDelay(int min, int max) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private int randomClickGap() {
        int lo = Math.max(1, settings.equipDelayMin);
        int hi = Math.max(lo, settings.equipDelayMax);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private void reset() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean midSwap = stage != STAGE_IDLE && !pendItem.isEmpty();
        boolean canClick = player != null && mc.gameMode != null
                && player.containerMenu instanceof InventoryMenu
                && player.containerMenu == player.inventoryMenu;
        if (midSwap && canClick) {
            pendSlot = -1;
            pendArmorSlot = -1;
            stage = STAGE_PLACE_BACK;
            timer = 1;
            return;
        }
        stage = STAGE_IDLE;
        timer = 0;
        pendSlot = -1;
        pendArmorSlot = -1;
        pendItem = ItemStack.EMPTY;
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Equip delay (min ticks)", 1, 60,
                        () -> settings.equipDelayMin, v -> settings.equipDelayMin = v),
                new IntSetting("Equip delay (max ticks)", 1, 120,
                        () -> settings.equipDelayMax, v -> settings.equipDelayMax = v),
                new IntSetting("Upgrade threshold (armor pts)", 0, 50,
                        () -> settings.upgradeThreshold, v -> settings.upgradeThreshold = v),
                new ChoiceSetting("Score mode",
                        new ChoiceSetting.Option[]{
                                new ChoiceSetting.Option("enchants", "Defense + Enchants"),
                                new ChoiceSetting.Option("defense", "Defense only")
                        },
                        () -> settings.scoreMode, v -> settings.scoreMode = v)
        );
    }
}