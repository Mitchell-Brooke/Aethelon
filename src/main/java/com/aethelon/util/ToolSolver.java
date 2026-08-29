package com.aethelon.util;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

public final class ToolSolver {
    private ToolSolver() {
    }

    public static int bestSlot(Player player, BlockState state, int minImprovementPct, String strategy) {
        Holder<Enchantment> efficiency = getEfficiencyHolder();
        Inventory inv = player.getInventory();
        int current = inv.getSelectedSlot();
        ItemStack currentStack = inv.getItem(current);
        float currentScore = score(currentStack, state, efficiency);
        float threshold = currentScore * (1 + Math.max(0, minImprovementPct) / 100f);

        boolean preferDurability = "durability".equals(strategy);
        int best = -1;
        float bestScore = -1f;
        int bestRemaining = -1;
        for (int i = 0; i < 9; i++) {
            if (i == current) {
                continue;
            }
            ItemStack stack = inv.getItem(i);
            float s = score(stack, state, efficiency);
            if (preferDurability) {
                if (s < threshold) {
                    continue;
                }
                int remaining = remainingDurability(stack);
                if (remaining > bestRemaining || (remaining == bestRemaining && s > bestScore)) {
                    bestRemaining = remaining;
                    bestScore = s;
                    best = i;
                }
            } else if (s > bestScore) {
                bestScore = s;
                best = i;
            }
        }
        if (best < 0) {
            return -1;
        }
        return preferDurability || bestScore >= threshold ? best : -1;
    }

    private static int remainingDurability(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max <= 0 ? Integer.MAX_VALUE : max - stack.getDamageValue();
    }

    private static Holder<Enchantment> getEfficiencyHolder() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return null;
        }
        return mc.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.EFFICIENCY);
    }

    private static float score(ItemStack stack, BlockState state, Holder<Enchantment> efficiency) {
        if (stack.isEmpty()) {
            return -1f;
        }
        Tool tool = stack.get(DataComponents.TOOL);
        float speed = tool == null ? 1.0f : tool.getMiningSpeed(state);
        float score = speed;
        if (efficiency != null) {
            int eff = EnchantmentHelper.getItemEnchantmentLevel(efficiency, stack);
            if (eff > 0) {
                score += eff * eff;
            }
        }
        if (tool != null && tool.isCorrectForDrops(state)) {
            score += 10.0f;
        }
        return score;
    }
}