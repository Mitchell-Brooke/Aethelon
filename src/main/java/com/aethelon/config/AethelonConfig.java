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
        public int minSpeedImprovementPct = 10;
        public int swapDelayMin = 1;
        public int swapDelayMax = 5;
        public String strategy = "fastest";
    }

    public final AutoToolSettings autoTool = new AutoToolSettings();

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
                autoTool.minSpeedImprovementPct = loaded.autoTool.minSpeedImprovementPct;
                autoTool.swapDelayMin = loaded.autoTool.swapDelayMin;
                autoTool.swapDelayMax = Math.max(loaded.autoTool.swapDelayMax, autoTool.swapDelayMin);
                autoTool.strategy = loaded.autoTool.strategy;
                if (autoTool.strategy == null || !("fastest".equals(autoTool.strategy) || "durability".equals(autoTool.strategy))) {
                    autoTool.strategy = "fastest";
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