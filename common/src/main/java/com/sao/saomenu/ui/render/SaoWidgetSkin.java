package com.sao.saomenu.ui.render;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.ui.text.SAOScrollText;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/** One visual implementation for native and SDK buttons; their controllers remain native. */
public final class SaoWidgetSkin {
    private SaoWidgetSkin() {
    }

    public static void button(GuiGraphics graphics, int x, int y, int width, int height,
                              Component label, boolean active, boolean highlighted, float alpha) {
        if (width <= 0 || height <= 0 || alpha <= 0) {
            return;
        }
        ThemeColors colors = SaoUi.theme().colors();
        boolean lit = highlighted && active;
        int background = lit ? colors.accent() : colors.surfaceSlot();
        int foreground = !active ? colors.textMuted()
                : lit ? colors.textOnAccent() : colors.textOnSurface();
        SaoDraw.roundedRect(graphics, x, y, width, height, 3, SaoDraw.mulAlpha(background, alpha));
        var font = SaoUi.bodyFont();
        int available = Math.max(0, width - 8);
        int textWidth = font.width(label);
        int color = SaoDraw.mulAlpha(foreground, alpha);
        int textY = y + (height - font.lineHeight) / 2;
        if (textWidth <= available) {
            graphics.drawString(font, label, x + (width - textWidth) / 2, textY, color, false);
        } else if (available > 0) {
            int textX = x + 4 - SAOScrollText.offset(textWidth, available, Util.getMillis(), y);
            graphics.enableScissor(x + 4, y, x + width - 4, y + height);
            try {
                graphics.drawString(font, label, textX, textY, color, false);
            } finally {
                graphics.disableScissor();
            }
        }
    }
}
