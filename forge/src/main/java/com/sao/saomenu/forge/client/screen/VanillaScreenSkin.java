package com.sao.saomenu.forge.client.screen;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy;
import com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy.Mode;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;

/**
 * Visual adapters for named vanilla text/front-end/loading surfaces.
 * Native widgets, lists, packets, fade/reload and screen identities stay authoritative.
 */
public final class VanillaScreenSkin {
    private static final int WASH = 0xEE101218;
    private static final int DEATH_FROM = 0xA0281018;
    private static final int DEATH_TO = 0xD0121010;
    private static final int BOOK_WIDTH = 192;
    private static final int BOOK_HEIGHT = 192;
    private static final int ADVANCE_WINDOW_W = 252;
    private static final int ADVANCE_WINDOW_H = 140;

    private VanillaScreenSkin() {
    }

    public static boolean reskinBackground(Screen screen) {
        return VanillaUiPolicy.mode(screen) == Mode.RESKIN && !(screen instanceof AbstractContainerScreen<?>);
    }

    public static boolean decorate(Screen screen) {
        return VanillaUiPolicy.mode(screen) == Mode.DECORATE && !(screen instanceof AbstractContainerScreen<?>);
    }

    public static boolean skinDirectButton(AbstractButton button) {
        if (button == null) {
            return false;
        }
        Class<?> type = button.getClass();
        if (type != Button.class && type != CycleButton.class) {
            return false;
        }
        return VanillaUiPolicy.active(Minecraft.getInstance().screen);
    }

    public static boolean harmonizeLists() {
        return reskinBackground(Minecraft.getInstance().screen);
    }

    public static boolean overlayEnabled() {
        // Initial loading can render before client bootstrap reads the persisted master switch.
        return SAOConfig.path() != null && SaoUi.enabled();
    }

    public static void paintBackdrop(GuiGraphics graphics, Screen screen) {
        if (graphics == null || screen == null) {
            return;
        }
        int width = screen.width;
        int height = screen.height;
        if (width <= 0 || height <= 0) {
            return;
        }
        ThemeColors colors = SaoUi.theme().colors();
        graphics.fill(0, 0, width, height, WASH);
        paintRails(graphics, 0, 0, width, height, colors.accent());
        graphics.fill(0, 0, 6, 18, colors.accent());
    }


    public static void chatPlate(GuiGraphics graphics, int width, int height) {
        if (graphics == null || width <= 0 || height <= 0) {
            return;
        }
        ThemeColors colors = SaoUi.theme().colors();
        int top = height - 16;
        graphics.fill(0, top, width, height, 0xDA101218);
        graphics.fill(0, top, width, top + 2, colors.accent());
    }

    public static void titleChrome(GuiGraphics graphics, Screen screen) {
        if (graphics == null || screen == null) {
            return;
        }
        int accent = SaoUi.theme().colors().accent();
        int width = screen.width;
        graphics.fill(2, 2, 36, 4, accent);
        graphics.fill(2, 4, 4, 36, accent);
        graphics.fill(width - 36, 2, width - 2, 4, accent);
        graphics.fill(width - 4, 4, width - 2, 36, accent);
    }

    public static void bookHalo(GuiGraphics graphics, int screenWidth) {
        if (graphics == null) {
            return;
        }
        int left = (screenWidth - BOOK_WIDTH) / 2;
        paintFrame(graphics, left, 2, BOOK_WIDTH, BOOK_HEIGHT, 3, SaoUi.theme().colors().accent());
    }

    public static void advancementHalo(GuiGraphics graphics, Screen screen) {
        if (graphics == null || screen == null) {
            return;
        }
        int x = (screen.width - ADVANCE_WINDOW_W) / 2;
        int y = (screen.height - ADVANCE_WINDOW_H) / 2;
        paintFrame(graphics, x, y, ADVANCE_WINDOW_W, ADVANCE_WINDOW_H, 3, SaoUi.theme().colors().accent());
    }

    public static void topRail(GuiGraphics graphics, Screen screen) {
        if (graphics == null || screen == null || screen.width <= 0) {
            return;
        }
        graphics.fill(0, 0, screen.width, 2, SaoUi.theme().colors().accent());
    }

    public static void edgeFrame(GuiGraphics graphics, Screen screen) {
        if (graphics == null || screen == null) {
            return;
        }
        paintRails(graphics, 0, 0, screen.width, screen.height, SaoUi.theme().colors().accent());
    }

    public static int deathGradientFrom(Screen screen, int original) {
        return reskinBackground(screen) ? DEATH_FROM : original;
    }

    public static int deathGradientTo(Screen screen, int original) {
        return reskinBackground(screen) ? DEATH_TO : original;
    }

    public static void loadingProgress(GuiGraphics graphics, int minX, int minY, int maxX, int maxY,
                                       float progress, float fade) {
        if (graphics == null || maxX <= minX || maxY <= minY) {
            return;
        }
        float alpha = Mth.clamp(fade, 0f, 1f);
        ThemeColors colors = SaoUi.theme().colors();
        int border = SaoDraw.mulAlpha(colors.divider(), alpha);
        int track = SaoDraw.mulAlpha(0xFF202428, alpha);
        int fill = SaoDraw.mulAlpha(colors.accent(), alpha);
        graphics.fill(minX, minY, maxX, maxY, track);
        int innerLeft = minX + 1;
        int innerRight = maxX - 1;
        int bar = innerLeft + Math.round((innerRight - innerLeft) * Mth.clamp(progress, 0f, 1f));
        if (bar > innerLeft) {
            graphics.fill(innerLeft, minY + 1, bar, maxY - 1, fill);
        }
        graphics.fill(minX, minY, maxX, minY + 1, border);
        graphics.fill(minX, maxY - 1, maxX, maxY, border);
        graphics.fill(minX, minY, minX + 1, maxY, border);
        graphics.fill(maxX - 1, minY, maxX, maxY, border);
    }

    private static void paintRails(GuiGraphics graphics, int x, int y, int width, int height, int accent) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int right = x + width;
        int bottom = y + height;
        graphics.fill(x, y, right, y + 2, accent);
        graphics.fill(x, bottom - 2, right, bottom, accent);
        graphics.fill(x, y, x + 2, bottom, accent);
        graphics.fill(right - 2, y, right, bottom, accent);
    }

    private static void paintFrame(GuiGraphics graphics, int x, int y, int width, int height, int thickness, int accent) {
        int t = Math.max(1, thickness);
        graphics.fill(x - t, y - t, x + width + t, y, accent);
        graphics.fill(x - t, y + height, x + width + t, y + height + t, accent);
        graphics.fill(x - t, y, x, y + height, accent);
        graphics.fill(x + width, y, x + width + t, y + height, accent);
    }
}
