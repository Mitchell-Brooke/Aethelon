package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoFishSettings;
import com.aethelon.mixin.FishingHookAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoFishModule extends Module {
    private enum Phase { IDLE, WAITING, REEL_DELAY, RECAST_DELAY }

    private final AutoFishSettings settings;
    private Phase phase = Phase.IDLE;
    private int delayRemaining = 0;
    private int ticksSinceCast = 0;
    private boolean lastBitten = false;

    public AutoFishModule(AutoFishSettings settings) {
        super("auto_fish", "Auto Fish", settings.enabled);
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
        if (player == null || mc.level == null || mc.screen != null || player.isSpectator()) {
            reset();
            return;
        }
        ItemStack held = player.getMainHandItem();
        if (!held.is(Items.FISHING_ROD)) {
            reset();
            return;
        }
        switch (phase) {
            case IDLE -> startCast(player);
            case WAITING -> tickWaiting(player);
            case REEL_DELAY -> tickReelDelay(player);
            case RECAST_DELAY -> tickRecastDelay(player);
        }
    }

    private void startCast(LocalPlayer player) {
        if (player.isUsingItem()) {
            return;
        }
        useItem(player);
        phase = Phase.WAITING;
        ticksSinceCast = 0;
        lastBitten = false;
    }

    private void tickWaiting(LocalPlayer player) {
        ticksSinceCast++;
        FishingHook hook = player.fishing;
        if (hook == null) {
            if (ticksSinceCast > 40) {
                phase = Phase.RECAST_DELAY;
                delayRemaining = randomDelay(settings.recastDelayMin, settings.recastDelayMax);
            }
            return;
        }
        boolean bitten = isBiting(hook);
        if (bitten && !lastBitten) {
            phase = Phase.REEL_DELAY;
            delayRemaining = randomDelay(settings.reelDelayMin, settings.reelDelayMax);
        }
        lastBitten = bitten;
        if (ticksSinceCast >= settings.noBiteTimeoutSec * 20) {
            phase = Phase.REEL_DELAY;
            delayRemaining = randomDelay(settings.reelDelayMin, settings.reelDelayMax);
        }
    }

    private void tickReelDelay(LocalPlayer player) {
        if (--delayRemaining <= 0) {
            useItem(player);
            phase = Phase.RECAST_DELAY;
            delayRemaining = randomDelay(settings.recastDelayMin, settings.recastDelayMax);
        }
    }

    private void tickRecastDelay(LocalPlayer player) {
        if (--delayRemaining <= 0) {
            startCast(player);
        }
    }

    private void useItem(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode != null && !player.isUsingItem()) {
            mc.gameMode.useItem(player, InteractionHand.MAIN_HAND);
        }
    }

    private boolean isBiting(FishingHook hook) {
        EntityDataAccessor<Boolean> key = FishingHookAccessor.aethelon$getDataBiting();
        return hook.getEntityData().get(key);
    }

    private int randomDelay(int min, int max) {
        int lo = Math.max(0, min);
        int hi = Math.max(lo, max);
        return lo + ThreadLocalRandom.current().nextInt(hi - lo + 1);
    }

    private void reset() {
        phase = Phase.IDLE;
        delayRemaining = 0;
        ticksSinceCast = 0;
        lastBitten = false;
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Reel delay (min ticks)", 1, 120,
                        () -> settings.reelDelayMin, v -> settings.reelDelayMin = v),
                new IntSetting("Reel delay (max ticks)", 1, 120,
                        () -> settings.reelDelayMax, v -> settings.reelDelayMax = v),
                new IntSetting("Recast delay (min ticks)", 0, 120,
                        () -> settings.recastDelayMin, v -> settings.recastDelayMin = v),
                new IntSetting("Recast delay (max ticks)", 0, 120,
                        () -> settings.recastDelayMax, v -> settings.recastDelayMax = v),
                new IntSetting("No-bite timeout (seconds)", 5, 600,
                        () -> settings.noBiteTimeoutSec, v -> settings.noBiteTimeoutSec = v)
        );
    }
}