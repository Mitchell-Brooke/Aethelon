package com.aethelon.gui;

import com.aethelon.AethelonClient;
import com.aethelon.module.Module;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class AethelonMainScreen extends Screen {
    private record ModuleRow(Module module, Button button) {
    }

    private final List<ModuleRow> rows = new ArrayList<>();

    public AethelonMainScreen() {
        super(Component.literal("Aethelon"));
    }

    @Override
    protected void init() {
        super.init();
        int width = 240;
        int cx = (this.width - width) / 2;
        int y = 80;
        for (Module module : AethelonClient.INSTANCE.modules.getModules()) {
            Button button = Button.builder(stateText(module), btn -> toggle(module, btn))
                    .bounds(cx, y, width, 20)
                    .build();
            this.addRenderableWidget(button);
            this.rows.add(new ModuleRow(module, button));
            y += 26;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(cx, y + 8, width, 20)
                .build());
    }

    private static Component stateText(Module module) {
        return Component.literal(module.getDisplayName() + "  [" + (module.isEnabled() ? "ON" : "OFF") + "]");
    }

    private void toggle(Module module, Button button) {
        module.toggle();
        AethelonClient.INSTANCE.config.save();
        button.setMessage(stateText(module));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isPressed) {
        if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            for (ModuleRow row : this.rows) {
                if (row.button.isMouseOver(event.x(), event.y())) {
                    this.minecraft.setScreen(new ModuleSettingsScreen(row.module));
                    return true;
                }
            }
        }
        return super.mouseClicked(event, isPressed);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.drawCenteredString(this.font, "Aethelon", this.width / 2, 40, 0xFFE0E0E0);
        guiGraphics.drawCenteredString(this.font, "Left click to toggle - Right click for settings",
                this.width / 2, this.height - 30, 0xFF888888);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}