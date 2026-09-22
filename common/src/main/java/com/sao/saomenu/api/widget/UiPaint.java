package com.sao.saomenu.api.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

final class UiPaint {
    private UiPaint() {
    }

    static int mulAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0x00FFFFFF;
        int na = Math.round(a * Mth.clamp(factor, 0f, 1f));
        return (na << 24) | rgb;
    }

    static float easeOutCubic(float t) {
        float c = t < 0f ? 0f : Math.min(t, 1f);
        float u = 1f - c;
        return 1f - u * u * u;
    }

    /**
     * Two-strip rounded fill used by SAO panels: corners stay transparent instead of
     * drawing a shader disc that Iris would treat as opaque.
     */
    static void fillRounded(GuiGraphics graphics, int x, int y, int width, int height, int radius, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int r = Math.min(Math.max(0, radius), Math.min(width, height) / 2);
        graphics.fill(x + r, y, x + width - r, y + height, color);
        graphics.fill(x, y + r, x + width, y + height - r, color);
    }
}
