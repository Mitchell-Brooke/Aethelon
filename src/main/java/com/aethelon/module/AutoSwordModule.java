package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoSwordSettings;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

public class AutoSwordModule extends Module {
    private static final int VERIFY_MAX = 20;
    private static final int STAGE_IDLE = 0;
    private static final int STAGE_ARM = 1;
    private static final int STAGE_COMBAT = 2;
    private static final int STAGE_DISARM_SWAP = 3;

    private final AutoSwordSettings settings;
    private int stage = STAGE_IDLE;
    private int verifyTicks = 0;
    private int linger = 0;
    private int attempt = 0;
    private int armedSlot = -1;
    private int priorSlot = -1;
    private int sourceMain = -1;
    private ItemStack preSwapItem = ItemStack.EMPTY;

    public AutoSwordModule(AutoSwordSettings settings) {
        super("auto_sword", "Auto Sword", settings.enabled);
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
        switch (stage) {
            case STAGE_IDLE -> {
                if (hasTarget(player)) {
                    beginCombat(player, menu);
                }
            }
            case STAGE_ARM -> {
                verifyTicks--;
                if (isSword(menu.getSlot(36 + armedSlot).getItem())) {
                    stage = STAGE_COMBAT;
                    linger = 0;
                } else if (verifyTicks <= 0) {
                    ItemStack nowA = menu.getSlot(36 + armedSlot).getItem();
                    if (attempt < 2 && ItemStack.isSameItemSameComponents(nowA, preSwapItem)) {
                        attempt++;
                        mc.gameMode.handleInventoryMouseClick(0, sourceMain, armedSlot, ClickType.SWAP, player);
                        verifyTicks = VERIFY_MAX;
                    } else {
                        reverseArm(mc, player);
                    }
                }
            }
            case STAGE_COMBAT -> {
                if (hasTarget(player)) {
                    linger = 0;
                } else {
                    linger++;
                    if (linger >= settings.releaseDelayTicks) {
                        beginDisarm(mc, player, menu);
                    }
                }
            }
            case STAGE_DISARM_SWAP -> {
                verifyTicks--;
                if (!isSword(menu.getSlot(36 + armedSlot).getItem()) || verifyTicks <= 0) {
                    boolean userChanged = player.getInventory().getSelectedSlot() != armedSlot;
                    if (!userChanged && priorSlot >= 0) {
                        selectSlot(player, priorSlot);
                    }
                    reset();
                }
            }
        }
    }

    private void beginCombat(LocalPlayer player, InventoryMenu menu) {
        int currentSelected = player.getInventory().getSelectedSlot();
        if (currentSelected < 0 || currentSelected > 8) {
            return;
        }
        int bestHotbar = -1;
        int bestMain = -1;
        double bestScore = 0.0D;
        List<ItemStack> items = player.getInventory().getNonEquipmentItems();
        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            double score = swordScore(stack);
            if (score > bestScore) {
                bestScore = score;
                bestHotbar = i < 9 ? i : bestHotbar;
                bestMain = i >= 9 ? i : bestMain;
            }
        }
        if (bestScore <= 0.0D) {
            return;
        }
        priorSlot = currentSelected;
        sourceMain = -1;
        attempt = 0;
        Minecraft mc = Minecraft.getInstance();
        if (bestHotbar >= 0) {
            armedSlot = bestHotbar;
            if (armedSlot != currentSelected) {
                selectSlot(player, armedSlot);
            }
            stage = STAGE_COMBAT;
            linger = 0;
            return;
        }
        int mainSlot = bestMain;
        if (mainSlot >= menu.slots.size() || mc.gameMode == null) {
            return;
        }
        preSwapItem = menu.getSlot(36 + currentSelected).getItem().copy();
        mc.gameMode.handleInventoryMouseClick(0, mainSlot, currentSelected, ClickType.SWAP, player);
        armedSlot = currentSelected;
        sourceMain = mainSlot;
        stage = STAGE_ARM;
        verifyTicks = VERIFY_MAX;
    }

    private void beginDisarm(Minecraft mc, LocalPlayer player, InventoryMenu menu) {
        if (sourceMain < 0) {
            boolean userChanged = player.getInventory().getSelectedSlot() != armedSlot;
            if (!userChanged && priorSlot >= 0) {
                selectSlot(player, priorSlot);
            }
            reset();
            return;
        }
        if (armedSlot < 0 || armedSlot > 8 || mc.gameMode == null) {
            reset();
            return;
        }
        mc.gameMode.handleInventoryMouseClick(0, sourceMain, armedSlot, ClickType.SWAP, player);
        stage = STAGE_DISARM_SWAP;
        verifyTicks = VERIFY_MAX;
    }

    private void reverseArm(Minecraft mc, LocalPlayer player) {
        if (sourceMain >= 0 && armedSlot >= 0 && armedSlot <= 8 && mc.gameMode != null
                && ItemStack.isSameItemSameComponents(
                        player.inventoryMenu.getSlot(36 + armedSlot).getItem(), preSwapItem)) {
            mc.gameMode.handleInventoryMouseClick(0, sourceMain, armedSlot, ClickType.SWAP, player);
        }
        boolean userChanged = player.getInventory().getSelectedSlot() != armedSlot;
        if (!userChanged && priorSlot >= 0) {
            selectSlot(player, priorSlot);
        }
        reset();
    }

    private boolean hasTarget(LocalPlayer player) {
        double radius = settings.range;
        List<Monster> mobs = player.level().getEntitiesOfClass(Monster.class,
                player.getBoundingBox().inflate(radius));
        for (Monster mob : mobs) {
            if (mob.isAlive() && !mob.isRemoved()
                    && mob.distanceToSqr(player) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private double swordScore(ItemStack stack) {
        if (!stack.is(ItemTags.SWORDS)) {
            return 0.0D;
        }
        double[] damage = {0.0D};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (Holder<Attribute> attr, AttributeModifier mod) -> {
            if (attr.value().equals(Attributes.ATTACK_DAMAGE.value())) {
                damage[0] += mod.amount();
            }
        });
        double score = damage[0];
        if (settings.considerEnchants) {
            for (Object2IntMap.Entry<Holder<Enchantment>> entry : stack.getEnchantments().entrySet()) {
                if (entry.getKey().is(Enchantments.SHARPNESS)) {
                    score += 1.0D + 0.5D * entry.getIntValue();
                }
            }
        }
        return score;
    }

    private boolean isSword(ItemStack stack) {
        return !stack.isEmpty() && stack.is(ItemTags.SWORDS);
    }

    private void selectSlot(LocalPlayer player, int slot) {
        if (slot < 0 || slot > 8) {
            return;
        }
        player.getInventory().setSelectedSlot(slot);
        ClientPacketListener connection = player.connection;
        if (connection != null) {
            connection.send(new ServerboundSetCarriedItemPacket(slot));
        }
    }

    private void reset() {
        stage = STAGE_IDLE;
        verifyTicks = 0;
        linger = 0;
        attempt = 0;
        armedSlot = -1;
        priorSlot = -1;
        sourceMain = -1;
        preSwapItem = ItemStack.EMPTY;
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Target radius (blocks)", 2, 16,
                        () -> settings.range, v -> settings.range = v),
                new BoolSetting("Count sharpness",
                        () -> settings.considerEnchants, v -> settings.considerEnchants = v),
                new IntSetting("Release delay (ticks)", 5, 120,
                        () -> settings.releaseDelayTicks, v -> settings.releaseDelayTicks = v)
        );
    }
}