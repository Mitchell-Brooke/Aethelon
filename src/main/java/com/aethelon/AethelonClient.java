package com.aethelon;

import com.aethelon.config.AethelonConfig;
import com.aethelon.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;

public final class AethelonClient implements ClientModInitializer {
    public static AethelonClient INSTANCE;

    public final AethelonConfig config = new AethelonConfig();
    public final ModuleManager modules = new ModuleManager();

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        config.load();
        modules.registerAll(this);
    }

    public void onClientTick() {
        modules.tick();
    }
}