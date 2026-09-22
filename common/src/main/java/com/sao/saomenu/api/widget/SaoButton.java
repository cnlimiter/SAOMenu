package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.ui.render.SaoWidgetSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Vanilla button events, focus, narration and click sound with SAO fill/label colors. */
public class SaoButton extends Button {

    public SaoButton(int x, int y, int width, int height, Component label, OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
    }

    public SaoButton(UiRect bounds, Component label, OnPress onPress) {
        this(bounds.x(), bounds.y(), bounds.width(), bounds.height(), label, onPress);
    }

    public SaoButton(UiRect bounds, Component label, Runnable onPress) {
        this(bounds, label, button -> onPress.run());
    }

    public void setBounds(UiRect bounds) {
        setX(bounds.x());
        setY(bounds.y());
        setWidth(bounds.width());
        this.height = bounds.height();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!SaoUi.enabled()) {
            super.renderWidget(graphics, mouseX, mouseY, partialTick);
            return;
        }
        SaoWidgetSkin.button(graphics, getX(), getY(), getWidth(), getHeight(), getMessage(),
                active, isHoveredOrFocused(), this.alpha);
    }
}
