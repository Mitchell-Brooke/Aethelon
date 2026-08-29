package com.aethelon.module;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.List;

public abstract class Module {
    private final String id;
    private final String displayName;
    private boolean enabled;

    protected Module(String id, String displayName, boolean enabled) {
        this.id = id;
        this.displayName = displayName;
        this.enabled = enabled;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void toggle() {
        setEnabled(!isEnabled());
    }

    public void tick() {
    }

    public void onBlockBreakStart(BlockPos pos, Direction direction) {
    }

    public List<IntSetting> settings() {
        return List.of();
    }
}