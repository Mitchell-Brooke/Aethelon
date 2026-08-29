package com.aethelon.module;

import com.aethelon.config.AethelonConfig.AutoToolSettings;
import com.aethelon.util.ToolSolver;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoToolModule extends Module {
    private final AutoToolSettings settings;
    private int pendingSlot = -1;
    private int pendingTicks = 0;

    public AutoToolModule(AutoToolSettings settings) {
        super("auto_tool", "Auto Tool Swap", settings.enabled);
        this.settings = settings;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        settings.enabled = enabled;
    }

    @Override
    public void onBlockBreakStart(BlockPos pos, Direction direction) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null) {
            return;
        }
        LocalPlayer player = mc.player;
        BlockState state = mc.level.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        int slot = ToolSolver.bestSlot(player, state, settings.minSpeedImprovementPct, settings.strategy);
        if (slot < 0 || player.getInventory().getSelectedSlot() == slot || this.pendingSlot >= 0) {
            return;
        }
        this.pendingSlot = slot;
        int min = Math.max(1, settings.swapDelayMin);
        int max = Math.max(min, settings.swapDelayMax);
        this.pendingTicks = min + ThreadLocalRandom.current().nextInt(max - min + 1);
    }

    @Override
    public void tick() {
        if (this.pendingSlot < 0) {
            return;
        }
        this.pendingTicks--;
        if (this.pendingTicks > 0) {
            return;
        }
        int slot = this.pendingSlot;
        this.pendingSlot = -1;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isSpectator() || Minecraft.getInstance().screen != null) {
            return;
        }
        player.getInventory().setSelectedSlot(slot);
        ClientPacketListener connection = player.connection;
        connection.send(new ServerboundSetCarriedItemPacket(slot));
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Min. speed improvement %", 0, 100,
                        () -> settings.minSpeedImprovementPct, v -> settings.minSpeedImprovementPct = v),
                new IntSetting("Swap delay (min ticks)", 1, 20,
                        () -> settings.swapDelayMin, v -> settings.swapDelayMin = v),
                new IntSetting("Swap delay (max ticks)", 1, 20,
                        () -> settings.swapDelayMax, v -> settings.swapDelayMax = v),
                new ChoiceSetting("Tool pick strategy",
                        new ChoiceSetting.Option[]{
                                new ChoiceSetting.Option("fastest", "Fastest"),
                                new ChoiceSetting.Option("durability", "Most durability")
                        },
                        () -> settings.strategy, v -> settings.strategy = v)
        );
    }
}