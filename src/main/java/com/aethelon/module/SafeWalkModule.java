package com.aethelon.module;

import com.aethelon.config.AethelonConfig.SafeWalkSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SafeWalkModule extends Module {
    private final SafeWalkSettings settings;

    public SafeWalkModule(SafeWalkSettings settings) {
        super("safe_walk", "Safe Walk", settings.enabled);
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
                || player.getAbilities().flying || player.isFallFlying() || !player.onGround()) {
            releaseSneak(mc);
            return;
        }
        if (edgeAhead(player, mc.level)) {
            mc.options.keyShift.setDown(true);
        } else {
            releaseSneak(mc);
        }
    }

    private void releaseSneak(Minecraft mc) {
        if (mc.options.keyShift.isDown()) {
            mc.options.keyShift.setDown(false);
        }
    }

    private boolean edgeAhead(LocalPlayer player, Level level) {
        var mv = player.input.getMoveVector();
        float forward = mv.y;
        float strafe = mv.x;
        if (forward == 0.0F && strafe == 0.0F) {
            return false;
        }
        double yaw = Math.toRadians(player.getYRot());
        double fX = -Math.sin(yaw);
        double fZ = Math.cos(yaw);
        double sX = -Math.cos(yaw);
        double sZ = -Math.sin(yaw);
        double dx = fX * forward + sX * strafe;
        double dz = fZ * forward + sZ * strafe;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len == 0.0D) {
            return false;
        }
        double probeX = player.getX() + dx / len * 0.6D;
        double probeZ = player.getZ() + dz / len * 0.6D;
        double feetY = player.getBoundingBox().minY;
        int feetBlock = BlockPos.containing(player.getX(), feetY, player.getZ()).getY();
        int probeBlockX = BlockPos.containing(probeX, feetY, probeZ).getX();
        int probeBlockZ = BlockPos.containing(probeX, feetY, probeZ).getZ();
        int depth = Math.max(1, settings.maxFallBlocks);
        for (int y = feetBlock; y > feetBlock - 1 - depth; y--) {
            BlockPos pos = new BlockPos(probeBlockX, y, probeBlockZ);
            if (!level.isLoaded(pos)) {
                return false;
            }
            if (isSolid(level, pos)) {
                return (feetY - (y + 1.0D)) > depth;
            }
        }
        return true;
    }

    private boolean isSolid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(level, pos).isEmpty();
    }

    @Override
    public List<Setting> settings() {
        return List.of(
                new IntSetting("Sneak before falling past (blocks)", 1, 10,
                        () -> settings.maxFallBlocks, v -> settings.maxFallBlocks = v)
        );
    }
}