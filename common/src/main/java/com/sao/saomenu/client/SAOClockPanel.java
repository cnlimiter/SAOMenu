package com.sao.saomenu.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sao.saomenu.SAOMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.time.LocalTime;

/**
 * SAO 时钟:半透明板上用方块数字显示当前时间(HH:MM)。
 *
 * <p>表盘/指针已去掉。几何仍按原数字贴图参考帧(高 222 / 字高 160)等比缩放。
 * 菜单打开时按住可拖动,位置与 {@code clockScale} 持久化。</p>
 */
public final class SAOClockPanel {

    private static final ResourceLocation TEX_DIGITS =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/clock_digits.png");

    /** 组件 GUI 高度;缩放系数 = H / 222(参考帧整块高度)。 */
    private static final int H = 36;

    private static float getScaleFactor() {
        return (H / 222f) * SAOConfig.clockScale();
    }

    // ---- 参考帧坐标系(高 222):只保留数字板 ----
    private static final int REF_PAD = 28;          // 板左右内边距
    private static final int REF_DIGIT_H = 160;     // 数字字高
    private static final int REF_DIGIT_Y = 31;      // (222-160)/2
    private static final int REF_CELL = 68;         // 贴图集中单个字形格宽
    private static final int REF_ADV = 84;          // 数字步进(字形 68 + 间隙 16)
    private static final int REF_ATLAS_W = 748;     // 贴图集总宽(11 格 * 68)

    /** 玻璃板:纯白 alpha≈0.35。 */
    private static final int PANEL_BG = 0x59FFFFFF;

    private static int refWidth(String text) {
        int n = Math.max(1, text.length());
        return 2 * REF_PAD + (n - 1) * REF_ADV + REF_CELL;
    }

    /** 组件总宽(GUI 像素,用于命中/拖动/锚点)。 */
    private static int getScaledW() {
        return Math.round(refWidth("00:00") * getScaleFactor());
    }

    /** 组件总高(GUI 像素,用于命中/拖动/锚点)。 */
    private static int getScaledH() {
        return Math.round(H * SAOConfig.clockScale());
    }

    private static boolean dragging;
    private static float grabFx;
    private static float grabFy;
    private static boolean draggedSinceDown;

    private SAOClockPanel() {
    }

    public static int panelW() {
        return getScaledW();
    }

    public static int panelH() {
        return getScaledH();
    }

    private static int originX(int screenW) {
        int w = getScaledW();
        float fx = Mth.clamp(SAOConfig.clockPanelX(), 0f, 1f);
        return Math.max(2, Math.min(Math.round(fx * (screenW - w)), screenW - w - 2));
    }

    private static int originY(int screenH) {
        int h = getScaledH();
        float fy = Mth.clamp(SAOConfig.clockPanelY(), 0f, 1f);
        return Math.max(2, Math.min(Math.round(fy * (screenH - h)), screenH - h - 2));
    }

    private static MenuLayout.Rect rect(int screenW, int screenH) {
        return new MenuLayout.Rect(originX(screenW), originY(screenH), getScaledW(), getScaledH());
    }

    // ------------------------------------------------------------ 渲染

    public static void render(GuiGraphics g, Minecraft mc, int screenW, int screenH, float alpha) {
        if (!SAOConfig.showClock()) {
            return;
        }
        // 「仅菜单内显示」:菜单关闭(或打开的是其他界面)时 HUD 层不再画时钟;
        // 菜单屏路径不受影响——SAOMenuScreen 本身就是 mc.screen
        if (SAOConfig.clockOnlyInMenu() && !(mc.screen instanceof SAOMenuScreen)) {
            return;
        }
        int x = originX(screenW);
        int y = originY(screenH);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1f, 1f, 1f, Mth.clamp(alpha, 0f, 1f));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        float k = getScaleFactor();
        pose.scale(k, k, 1f);

        String text = timeText();
        int boardW = refWidth(text);
        g.fill(0, 0, boardW, 222, PANEL_BG);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int idx = (c == ':') ? 10 : (c - '0');
            if (idx < 0 || idx > 10) {
                continue;
            }
            g.blit(TEX_DIGITS, REF_PAD + i * REF_ADV, REF_DIGIT_Y,
                    REF_CELL, REF_DIGIT_H, idx * REF_CELL, 0, REF_CELL, REF_DIGIT_H,
                    REF_ATLAS_W, REF_DIGIT_H);
        }

        pose.popPose();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** 当前时间文本(12/24 小时制跟随配置,冒号取贴图集第 11 格)。 */
    private static String timeText() {
        LocalTime t = LocalTime.now();
        int hour = t.getHour();
        if (!SAOConfig.clock24h()) {
            hour = hour % 12;
            if (hour == 0) {
                hour = 12;
            }
        }
        return String.format("%02d:%02d", hour, t.getMinute());
    }

    // ------------------------------------------------------------ 拖动

    public static boolean hitCard(int screenW, int screenH, int mx, int my) {
        return SAOConfig.showClock() && rect(screenW, screenH).contains(mx, my);
    }

    public static void beginDrag(int screenW, int screenH, int mx, int my) {
        MenuLayout.Rect r = rect(screenW, screenH);
        grabFx = (mx - r.x()) / (float) r.w();
        grabFy = (my - r.y()) / (float) r.h();
        dragging = true;
        draggedSinceDown = false;
    }

    public static void dragTo(int screenW, int screenH, int mx, int my) {
        if (!dragging) {
            return;
        }
        int w = getScaledW();
        int h = getScaledH();
        float fx = (mx - grabFx * w) / (float) Math.max(1, screenW - w);
        float fy = (my - grabFy * h) / (float) Math.max(1, screenH - h);
        SAOConfig.setClockPanelX(fx);
        SAOConfig.setClockPanelY(fy);
        draggedSinceDown = true;
    }

    public static void endDragAndSave() {
        if (dragging && draggedSinceDown) {
            java.nio.file.Path p = SAOConfig.path();
            if (p == null) {
                p = Minecraft.getInstance().gameDirectory.toPath()
                        .resolve("config").resolve("saomenu.json");
            }
            SAOConfig.save(p);
        }
        dragging = false;
    }
}
