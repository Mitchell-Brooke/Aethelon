package com.aethelon.gui;

import com.aethelon.AethelonClient;
import com.aethelon.module.ChoiceSetting;
import com.aethelon.module.Module;
import com.aethelon.module.Setting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ModuleSettingsScreen extends Screen {
    private record Row(Setting setting, int baselineY, Button toggle) {
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
        for (Setting setting : module.settings()) {
            Button toggle = null;
            if (setting instanceof ChoiceSetting choice) {
                toggle = Button.builder(Component.literal(choice.valueText()), btn -> this.change(choice, btn))
                        .bounds(cx + width - 150, y, 130, 20)
                        .build();
            } else {
                this.addRenderableWidget(Button.builder(Component.literal("-"), btn -> this.change(setting, -1, null))
                        .bounds(cx + width - 100, y, 20, 20)
                        .build());
                this.addRenderableWidget(Button.builder(Component.literal("+"), btn -> this.change(setting, 1, null))
                        .bounds(cx + width - 76, y, 20, 20)
                        .build());
            }
            if (toggle != null) {
                this.addRenderableWidget(toggle);
            }
            this.rows.add(new Row(setting, y, toggle));
            y += 26;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(cx, y + 8, width, 20)
                .build());
    }

    private void change(Setting setting, int delta, Button refreshButton) {
        setting.applyDelta(delta);
        AethelonClient.INSTANCE.config.save();
        if (refreshButton != null) {
            refreshButton.setMessage(Component.literal(setting.valueText()));
        }
    }

    private void change(ChoiceSetting choice, Button toggle) {
        change(choice, 1, toggle);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawCenteredString(this.font, module.getDisplayName() + " Settings", this.width / 2, 40, 0xFFE0E0E0);
        int width = 300;
        int cx = (this.width - width) / 2;
        for (Row row : rows) {
            guiGraphics.drawString(this.font, row.setting.label(), cx + 24, row.baselineY() + 6, 0xFFC0C0C0);
            if (row.toggle == null) {
                guiGraphics.drawCenteredString(this.font, row.setting.valueText(),
                        cx + width - 46, row.baselineY() + 6, 0xFFFFFFFF);
            }
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}