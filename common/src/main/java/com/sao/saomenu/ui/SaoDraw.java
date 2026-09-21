package com.sao.saomenu.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

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

    /** 复位染色,避免影响后续图元。 */
    public static void resetTint() {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
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
     * 圆角矩形填充:两条直条相交,四角各留 r×r 缺口(背景透出即圆角)。
     *
     * <p>不用着色器画圆角——SAO 界面在多处叠加半透明,自建图元在 Iris/Oculus
     * 下会被当成不透明几何,反而不如两次 {@code fill} 稳。</p>
     */
    public static void roundedRect(GuiGraphics g, int x, int y, int w, int h, int r, int color) {
        g.fill(x + r, y, x + w - r, y + h, color);
        g.fill(x, y + r, x + w, y + h - r, color);
    }
}
