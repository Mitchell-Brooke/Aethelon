package com.aethelon.module;

import com.aethelon.AethelonClient;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();

    public void registerAll(AethelonClient client) {
        modules.add(new AutoToolModule(client.config.autoTool));
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public void tick() {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.tick();
            }
        }
    }

    public void onBlockBreakStart(BlockPos pos, Direction direction) {
        for (Module module : modules) {
            if (module.isEnabled()) {
                module.onBlockBreakStart(pos, direction);
            }
        }
    }
}