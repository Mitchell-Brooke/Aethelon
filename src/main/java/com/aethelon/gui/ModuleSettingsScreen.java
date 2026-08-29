package com.aethelon.gui;

import com.aethelon.AethelonClient;
import com.aethelon.module.IntSetting;
import com.aethelon.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {
    private record Row(IntSetting setting, int baselineY) {
    }

    private final Module module;
    private final List<Row> rows = new ArrayList<>();

    public ModuleSettingsScreen(Module module) {
        super(Component.literal(module.getDisplayName() + " Settings"));
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        int width = 300;
        int cx = (this.width - width) / 2;
        int y = 80;
        for (IntSetting setting : module.settings()) {
            this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> change(setting, -1))
                    .bounds(cx + width - 100, y, 20, 20)
                    .build());
            this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> change(setting, 1))
                    .bounds(cx + width - 76, y, 20, 20)
                    .build());
            this.rows.add(new Row(setting, y));
            y += 26;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(cx, y + 8, width, 20)
                .build());
    }

    private void change(IntSetting setting, int delta) {
        setting.set(Math.max(setting.min(), Math.min(setting.max(), setting.value() + delta)));
        AethelonClient.INSTANCE.config.save();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.drawCenteredString(this.font, module.getDisplayName() + " Settings", this.width / 2, 40, 0xE0E0E0);
        int width = 300;
        int cx = (this.width - width) / 2;
        for (Row row : rows) {
            guiGraphics.drawString(this.font, row.setting.label(), cx + 24, row.baselineY() + 6, 0xC0C0C0);
            guiGraphics.drawCenteredString(this.font, String.valueOf(row.setting.value()),
                    cx + width - 46, row.baselineY() + 6, 0xFFFFFF);
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}