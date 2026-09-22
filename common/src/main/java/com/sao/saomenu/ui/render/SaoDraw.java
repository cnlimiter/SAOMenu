package com.sao.saomenu.ui.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;

/**
 * 共用绘制笔刷。
 *
 * <p>SAO 界面的图元大多是「半透明填充 + 叠加贴图」,于是每个界面都各写了一份
 * {@code mulAlpha} / {@code setTint} / {@code blitBlended} / 圆角矩形。集中到这里,
 * 以后要改混合策略(例如统一给贴图加描边)只有一处。</p>
 */
public final class SaoDraw {

    private SaoDraw() {
    }

    // ---------------------------------------------------------------- 颜色

    /** 把基础色(含 alpha)整体乘一个透明度系数。 */
    public static int mulAlpha(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int rgb = argb & 0xFFFFFF;
        int na = Math.round(a * Mth.clamp(factor, 0f, 1f));
        return (na << 24) | rgb;
    }

    /** 让后续 blit 整体带一个透明度(白贴图 → 原色淡出)。 */
    public static void shaderAlpha(float a) {
        RenderSystem.setShaderColor(1f, 1f, 1f, Mth.clamp(a, 0f, 1f));
    }

    /** 按给定 ARGB 给后续 blit 染色(把白色贴图染成主题色)。 */
    public static void tint(int argb, float alpha) {
        RenderSystem.setShaderColor(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                Mth.clamp(alpha, 0f, 1f));
    }

    // ---------------------------------------------------------------- 图元

    /**
     * 开混合后贴整张 GUI 贴图。
     *
     * <p>{@code GuiGraphics.fill()} 收尾会把混合关掉,而 {@code blit()} 不管理混合状态;
     * 「fill 阴影 → blit 面板」这种顺序会让贴图边缘的低 alpha 像素(柔和阴影)
     * 以实心纯黑画出,表现为面板四周一圈黑框。</p>
     */
    public static void blendedBlit(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h) {
        RenderSystem.enableBlend();
        g.blit(tex, x, y, 0, 0, w, h, w, h);
    }

    /**
     * 圆角矩形填充:三条不重叠直条,四角各留 r×r 缺口,半透明中心不会重复叠色。
     *
     * <p>不用着色器画圆角——SAO 界面在多处叠加半透明,自建图元在 Iris/Oculus
     * 下会被当成不透明几何,因此使用普通 {@code fill}。</p>
     */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        if (w <= 0 || h <= 0) {
            return;
        }
        r = Math.min(Math.max(0, r), Math.min(w, h) / 2);
        if (r == 0) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + r, y, x + w - r, y + r, color);
        g.fill(x, y + r, x + w, y + h - r, color);
        g.fill(x + r, y + h - r, x + w - r, y + h, color);
    }

    // ---------------------------------------------------------------- 文字

    /**
     * 在 (x, y) 起笔,按 {@code scale} 放大字号。缩放后的坐标系里 (0,0) 是字形左上角。
     *
     * <p>组件本身按屏幕比例缩放时,必须把字也乘同一个系数,否则 GUI Scale /
     * 窗口大小一变,9px 字就会在格子里上下漂。</p>
     */
    public static void drawScaled(GuiGraphics g, Font font, String text,
                                  float x, float y, float scale, int argb, boolean shadow) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        pose.scale(scale, scale, 1f);
        g.drawString(font, text, 0, 0, argb, shadow);
        pose.popPose();
    }

    public static void drawScaled(GuiGraphics g, Font font, FormattedCharSequence text,
                                  float x, float y, float scale, int argb, boolean shadow) {
        var pose = g.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 0f);
            pose.scale(scale, scale, 1f);
            g.drawString(font, text, 0, 0, argb, shadow);
        } finally {
            pose.popPose();
        }
    }

    /**
     * 以 (cx, cy) 为视觉中心画字,字号随 {@code scale} 变。
     * 垂直方向按 {@link Font#lineHeight} 居中,不再写死 8px。
     */
    public static void drawCentered(GuiGraphics g, Font font, String text,
                                    float cx, float cy, float scale, int argb, boolean shadow) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0f);
        pose.scale(scale, scale, 1f);
        g.drawString(font, text, -font.width(text) / 2, -font.lineHeight / 2, argb, shadow);
        pose.popPose();
    }

    /**
     * 让字形视觉高度占 {@code boxH} 的 {@code frac}。
     * GUI 空间里字体是固定像素,组件却按屏高比例缩,必须把字也乘这个系数,
     * 否则最小窗口下 9px 字会顶破 13px 的菜单行。
     */
    public static float fitScale(int lineHeight, float boxH, float frac) {
        if (boxH <= 0f) {
            return 0.30f;
        }
        return Mth.clamp((boxH * frac) / Math.max(1f, lineHeight), 0.30f, 4.0f);
    }

    public static float fitScale(Font font, float boxH) {
        return fitScale(font.lineHeight, boxH, 0.62f);
    }

    public static float fitScale(Font font, float boxH, float frac) {
        return fitScale(font.lineHeight, boxH, frac);
    }

    /**
     * 先按全部 {@code wanted} 行分。只有行高低于 {@code minLinePx} 才从尾部减行,
     * 下限 2 行。正常尺寸(例如 6 行摊在 57px ≈ 9.5px)一行不丢。
     */
    public static int rowsKept(int boxH, int wanted, int minLinePx) {
        int rows = Math.max(1, wanted);
        int min = Math.max(1, minLinePx);
        while (rows > 2 && boxH / (float) rows < min) {
            rows--;
        }
        return rows;
    }

    /** 按未缩放字体宽度截断,供 {@link #drawScaled} 使用。 */
    public static String clipTo(Font font, String s, int maxUnscaled) {
        if (s == null || s.isEmpty() || maxUnscaled <= 0) {
            return "";
        }
        if (font.width(s) <= maxUnscaled) {
            return s;
        }
        int dots = font.width("…");
        if (maxUnscaled <= dots) {
            return "";
        }
        return font.plainSubstrByWidth(s, maxUnscaled - dots) + "…";
    }

    /** Clips without flattening translated siblings, colors, fonts or emphasis to a String. */
    public static FormattedCharSequence clipTo(Font font, Component text, int maxUnscaled) {
        if (maxUnscaled <= 0) {
            return FormattedCharSequence.EMPTY;
        }
        if (font.width(text) <= maxUnscaled) {
            return text.getVisualOrderText();
        }
        FormattedText dots = FormattedText.of("…", text.getStyle());
        int remaining = maxUnscaled - font.width(dots);
        if (remaining <= 0) {
            return FormattedCharSequence.EMPTY;
        }
        return Language.getInstance().getVisualOrder(
                FormattedText.composite(font.substrByWidth(text, remaining), dots));
    }

    /**
     * 行内左对齐:字号缩进行高,垂直居中。{@code maxW} 是屏幕像素宽。
     */
    public static void drawInRow(GuiGraphics g, Font font, String text,
                                 float x, float rowY, float rowH, float maxW,
                                 int argb, boolean shadow) {
        if (text == null || text.isEmpty() || maxW <= 0f || rowH <= 0f) {
            return;
        }
        float s = fitScale(font, rowH);
        String shown = clipTo(font, text, Math.max(0, Math.round(maxW / s)));
        if (shown.isEmpty()) {
            return;
        }
        float th = font.lineHeight * s;
        drawScaled(g, font, shown, x, rowY + (rowH - th) / 2f, s, argb, shadow);
    }

    public static void drawInRow(GuiGraphics g, Font font, Component text,
                                 float x, float rowY, float rowH, float maxW,
                                 int argb, boolean shadow) {
        if (maxW <= 0f || rowH <= 0f) {
            return;
        }
        float scale = fitScale(font, rowH);
        FormattedCharSequence shown = clipTo(font, text, Math.max(0, Math.round(maxW / scale)));
        drawScaled(g, font, shown, x, rowY + (rowH - font.lineHeight * scale) / 2f, scale, argb, shadow);
    }

    /** 在高 {@code boxH} 的行里垂直居中原字号文字的 Y(顶边)。 */
    public static int textY(Font font, int boxY, int boxH) {
        return boxY + (boxH - font.lineHeight) / 2;
    }
}
