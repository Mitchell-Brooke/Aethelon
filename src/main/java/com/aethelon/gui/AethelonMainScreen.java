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

    private static final int ROW_HEIGHT = 26;
    private static final int CONTENT_TOP = 80;
    private static final int CONTENT_BOTTOM_MARGIN = 55;

    private final List<ModuleRow> rows = new ArrayList<>();
    private int scrollOffset = 0;

    public AethelonMainScreen() {
        super(Component.literal("Aethelon"));
    }

    @Override
    protected void init() {
        super.init();
        int width = 240;
        int cx = (this.width - width) / 2;
        int y = CONTENT_TOP;
        for (Module module : AethelonClient.INSTANCE.modules.getModules()) {
            Button button = Button.builder(stateText(module), btn -> toggle(module, btn))
                    .bounds(cx, y, width, 20)
                    .build();
            this.addRenderableWidget(button);
            this.rows.add(new ModuleRow(module, button));
            y += ROW_HEIGHT;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Done"), btn -> this.onClose())
                .bounds(cx, this.height - 50, width, 20)
                .build());
        this.scrollOffset = 0;
    }

    private int viewportBottom() {
        return this.height - CONTENT_BOTTOM_MARGIN;
    }

    private int contentHeight() {
        return this.rows.size() * ROW_HEIGHT;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - (viewportBottom() - CONTENT_TOP));
    }

    private void refreshPositions() {
        int cx = (this.width - 240) / 2;
        for (int i = 0; i < this.rows.size(); i++) {
            this.rows.get(i).button.setY(CONTENT_TOP + i * ROW_HEIGHT - this.scrollOffset);
            this.rows.get(i).button.setX(cx);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int before = this.scrollOffset;
        this.scrollOffset = Math.max(0, Math.min(maxScroll(), this.scrollOffset - (int) (verticalAmount * ROW_HEIGHT)));
        if (this.scrollOffset != before) {
            refreshPositions();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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