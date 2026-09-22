package com.sao.saomenu.client.render.target;

import net.minecraft.util.Mth;

/**
 * SAO 目标血条的造型与配色模型(纯函数,只依赖 {@link Mth})。
 *
 * <p>血量色标、菱形(风筝形)轮廓、受击闪白强度与浮动位移都在这里;
 * 实际绘制由 {@link SAOTargetBar3D} 在世界空间提交几何时调用。
 * 类内不持有任何状态,可直接单元测试。</p>
 *
 * <p>历史上的 2D 斜切玻璃血条绘制路径(逐行 {@code fill} 拼平行四边形)
 * 已在 3D 环绕血条取代后删除——那条路径在 Iris/Oculus 下不可用,
 * 是当初改画世界空间几何的原因。</p>
 */
public final class SAOTargetBar {

    // ------------------------------------------------------------ 造型常量

    /** 平行四边形每行的横向偏移量(相对条高的比例):顶边比底边更靠右。 */
    public static final float SKEW = 1.15f;
    /** 右侧分离尾块宽度占主条宽的比例。 */
    public static final float TAIL_FRAC = 0.13f;
    /** 受击闪白时长。 */
    public static final long FLASH_MS = 320;

    // 血量色标:红分量单调递增、绿分量单调递减(见 hpColor)
    private static final int HP_GREEN = 0xFF7CE04A;
    private static final int HP_YELLOW = 0xFFF2E23A;
    private static final int HP_ORANGE = 0xFFF57F22;
    private static final int HP_RED = 0xFFF53A26;
    private static final int HP_CRIT = 0xFFFF1520;

    private SAOTargetBar() {
    }

    // ------------------------------------------------------------ 纯函数(可测)

    /** 主条宽度:随 GUI 高度缩放,钳制到手感区间。 */
    public static int barWidth(int screenH) {
        return Mth.clamp(Math.round(screenH * 0.16f), 56, 150);
    }

    /** 主条高度。参考图里条身比原版血条厚不少,约为宽的 1/7。 */
    public static int barHeight(int screenH) {
        return Mth.clamp(Math.round(screenH * 0.026f), 7, 16);
    }

    /** 顶边相对底边的总偏移(像素);平行四边形的斜度。 */
    public static int skewOffset(int barH) {
        return Math.round(barH * SKEW);
    }

    /** 右侧尾块宽度。 */
    public static int tailWidth(int barW) {
        return Math.max(5, Math.round(barW * TAIL_FRAC));
    }

    /**
     * 血量颜色:满血亮绿 → 60% 黄 → 30% 橙 → 15% 以下正红。
     *
     * <p>分段线性插值。色标刻意选成「红分量随血量下降单调不减、绿分量单调不增」,
     * 这样任何一次掉血都只会让颜色朝更危险的方向走,不会出现回弹。</p>
     */
    public static int hpColor(float frac) {
        float f = Mth.clamp(frac, 0f, 1f);
        if (f >= 0.6f) {
            return lerpColor(HP_YELLOW, HP_GREEN, (f - 0.6f) / 0.4f);
        }
        if (f >= 0.3f) {
            return lerpColor(HP_ORANGE, HP_YELLOW, (f - 0.3f) / 0.3f);
        }
        if (f >= 0.15f) {
            return lerpColor(HP_RED, HP_ORANGE, (f - 0.15f) / 0.15f);
        }
        return lerpColor(HP_CRIT, HP_RED, f / 0.15f);
    }

    /**
     * 头顶菱形(风筝形)在给定纵向进度处的半宽比例。
     *
     * @param t 0 为顶点、1 为底尖
     * @return 半宽相对最大半宽的比例 0..1
     */
    public static float kiteHalfWidth(float t) {
        float u = Mth.clamp(t, 0f, 1f);
        // 上段 32% 迅速张开到最宽,下段收成长尖
        return u <= 0.32f ? u / 0.32f : 1f - (u - 0.32f) / 0.68f;
    }

    /** 受击闪白强度:0 表示无闪。 */
    public static float flashStrength(long now, long hurtAt) {
        if (hurtAt <= 0) {
            return 0f;
        }
        long age = now - hurtAt;
        if (age < 0 || age >= FLASH_MS) {
            return 0f;
        }
        float t = age / (float) FLASH_MS;
        return (1f - t) * (1f - t);
    }

    /** 菱形浮动位移(像素):缓慢上下呼吸。 */
    public static float kiteBob(long now) {
        return Mth.sin(now / 420f) * 1.6f;
    }

    // ------------------------------------------------------------ 小工具

    /** 线性插值两个 ARGB(alpha 也参与)。 */
    public static int lerpColor(int from, int to, float t) {
        float f = Mth.clamp(t, 0f, 1f);
        int a = Math.round(((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int gg = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (gg << 8) | b;
    }
}
