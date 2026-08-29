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
import net.minecraft.world.inventory.AbstractContainerMenu;
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
        if (!valid(player)) {
            reset();
            return;
        }
        if (player.containerMenu != player.inventoryMenu) {
            reset();
            return;
        }
        if (timer > 0) {
            timer--;
            return;
        }
        switch (stage) {
            case STAGE_IDLE -> scan(player);
            case STAGE_QUICK_MOVE -> doQuickMove(player, pendSlot);
            case STAGE_PICKUP -> doPickup(player, pendSlot);
            case STAGE_PICKUP_ARMOR -> doPickup(player, pendArmorSlot);
            case STAGE_PLACE_BACK -> doPickup(player, pendSlot);
        }
    }

    private boolean valid(LocalPlayer player) {
        if (player == null || Minecraft.getInstance().level == null) {
            return false;
        }
        if (Minecraft.getInstance().screen != null || player.isSpectator() || player.isDeadOrDying()) {
            return false;
        }
        AbstractContainerMenu menu = player.containerMenu;
        return menu instanceof InventoryMenu && menu.getCarried().isEmpty();
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
                }
            }
            if (bestIndex >= 0) {
                double wornScore = worn.isEmpty() ? 0.0D : score(worn, armorDefense(worn, type));
                if (bestScore >= wornScore + settings.upgradeThreshold) {
                    startEquip(player, bestIndex, worn.isEmpty());
                    return;
                }
            }
        }
    }

    private void startEquip(LocalPlayer player, int itemIndex, boolean quickMove) {
        Minecraft mc = Minecraft.getInstance();
        InventoryMenu menu = player.inventoryMenu;
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        ItemStack stack = items.get(itemIndex);
        int serverSlot = itemIndex < 9 ? 36 + itemIndex : itemIndex;
        int armorSlot = armorServerSlot(player.getEquipmentSlotForItem(stack));
        pendSlot = serverSlot;
        pendArmorSlot = armorSlot;
        if (quickMove) {
            stage = STAGE_QUICK_MOVE;
            timer = 0;
        } else {
            stage = STAGE_PICKUP;
            timer = 0;
        }
        if (mc.gameMode != null && System.getProperty("aethelon.selfTest") != null) {
            System.out.println("[aethelon] selfTest: auto-armor selected " + stack + " at slot " + serverSlot
                    + " -> armor slot " + armorSlot + (quickMove ? " (quick move)" : " (swap)"));
        }
    }

    private void doQuickMove(LocalPlayer player, int serverSlot) {
        if (!clickAllowed(player)) {
            reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && !player.inventoryMenu.getSlot(serverSlot).getItem().isEmpty()
                && player.inventoryMenu.getSlot(armorServerSlot(player.getEquipmentSlotForItem(
                player.inventoryMenu.getSlot(serverSlot).getItem()))).getItem().isEmpty()) {
            mc.gameMode.handleInventoryMouseClick(0, serverSlot, 0, ClickType.QUICK_MOVE, player);
        }
        stage = STAGE_IDLE;
        timer = randomDelay(settings.equipDelayMin, settings.equipDelayMax);
    }

    private void doPickup(LocalPlayer player, int serverSlot) {
        if (!clickAllowed(player)) {
            reset();
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && serverSlot >= 0 && serverSlot < player.inventoryMenu.slots.size()) {
            mc.gameMode.handleInventoryMouseClick(0, serverSlot, 0, ClickType.PICKUP, player);
        }
        stage = nextStage(stage);
        timer = randomClickGap();
    }

    private int nextStage(int current) {
        return switch (current) {
            case STAGE_PICKUP -> STAGE_PICKUP_ARMOR;
            default -> STAGE_PLACE_BACK;
        };
    }

    private boolean clickAllowed(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return mc.screen == null && !player.isSpectator() && !player.isDeadOrDying()
                && player.containerMenu == player.inventoryMenu
                && mc.gameMode != null;
    }

    private int armorServerSlot(EquipmentSlot type) {
        return 8 - type.getIndex();
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
        stage = STAGE_IDLE;
        timer = 0;
        pendSlot = -1;
        pendArmorSlot = -1;
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