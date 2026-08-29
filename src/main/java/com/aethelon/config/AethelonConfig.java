package com.aethelon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AethelonConfig {
    public static class AutoToolSettings {
        public boolean enabled = true;
        public int swapDelayMin = 1;
        public int swapDelayMax = 5;
        public String strategy = "fastest";
    }

    public static class AutoFishSettings {
        public boolean enabled = true;
        public int reelDelayMin = 4;
        public int reelDelayMax = 8;
        public int recastDelayMin = 4;
        public int recastDelayMax = 10;
        public int noBiteTimeoutSec = 60;
    }

    public final AutoToolSettings autoTool = new AutoToolSettings();
    public final AutoFishSettings autoFish = new AutoFishSettings();

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final transient Path path = FabricLoader.getInstance().getConfigDir().resolve("aethelon.json");

    public void load() {
        if (!Files.exists(path)) {
            save();
            return;
        }
        try {
            String json = Files.readString(path);
            AethelonConfig loaded = gson.fromJson(json, AethelonConfig.class);
            if (loaded != null) {
                autoTool.enabled = loaded.autoTool.enabled;
                autoTool.swapDelayMin = loaded.autoTool.swapDelayMin;
                autoTool.swapDelayMax = Math.max(loaded.autoTool.swapDelayMax, autoTool.swapDelayMin);
                autoTool.strategy = loaded.autoTool.strategy;
                if (autoTool.strategy == null || !("fastest".equals(autoTool.strategy) || "durability".equals(autoTool.strategy))) {
                    autoTool.strategy = "fastest";
                }
                if (loaded.autoFish != null) {
                    autoFish.enabled = loaded.autoFish.enabled;
                    autoFish.reelDelayMin = loaded.autoFish.reelDelayMin;
                    autoFish.reelDelayMax = Math.max(loaded.autoFish.reelDelayMax, autoFish.reelDelayMin);
                    autoFish.recastDelayMin = loaded.autoFish.recastDelayMin;
                    autoFish.recastDelayMax = Math.max(loaded.autoFish.recastDelayMax, autoFish.recastDelayMin);
                    autoFish.noBiteTimeoutSec = loaded.autoFish.noBiteTimeoutSec;
                }
            }
        } catch (Exception e) {
            System.err.println("[aethelon] Failed to load config: " + e.getMessage());
        }
    }

    public void save() {
        try {
            Files.writeString(path, gson.toJson(this));
        } catch (IOException e) {
            System.err.println("[aethelon] Failed to save config: " + e.getMessage());
        }
    }
}