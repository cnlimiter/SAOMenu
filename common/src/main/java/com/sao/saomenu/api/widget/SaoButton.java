package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.ui.text.SAOScrollText;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Vanilla button events, focus, narration and click sound with SAO fill/label colors. */
public class SaoButton extends Button {
    private Component lastMessage;
    private ResourceLocation lastFont;
    private Component styledMessage;

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
        ThemeTokens tokens = SaoUi.theme();
        ThemeColors colors = tokens.colors();
        boolean lit = isHoveredOrFocused() && active;
        int background = lit ? colors.accent() : colors.surfaceSlot();
        int foreground = lit ? colors.textOnAccent() : colors.textOnSurface();
        if (!active) {
            foreground = colors.textMuted();
        }
        UiPaint.fillRounded(graphics, getX(), getY(), getWidth(), getHeight(), 3,
                UiPaint.mulAlpha(background, this.alpha));
        var font = Minecraft.getInstance().font;
        if (getMessage() != lastMessage || !tokens.bodyFont().equals(lastFont)) {
            lastMessage = getMessage();
            lastFont = tokens.bodyFont();
            styledMessage = lastMessage.copy().withStyle(style -> style.withFont(lastFont));
        }
        int available = Math.max(0, getWidth() - 8);
        int textWidth = font.width(styledMessage);
        int color = UiPaint.mulAlpha(foreground, this.alpha);
        int y = getY() + (getHeight() - font.lineHeight) / 2;
        if (textWidth <= available) {
            graphics.drawString(font, styledMessage, getX() + (getWidth() - textWidth) / 2, y, color, false);
        } else if (available > 0 && getHeight() > 0) {
            int x = getX() + 4 - SAOScrollText.offset(textWidth, available, Util.getMillis(), getY());
            graphics.enableScissor(getX() + 4, getY(), getX() + getWidth() - 4, getY() + getHeight());
            try {
                graphics.drawString(font, styledMessage, x, y, color, false);
            } finally {
                graphics.disableScissor();
            }
        }
    }
}
