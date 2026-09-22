package com.sao.saomenu.ui.animation;

/**
 * 全模组共用的一套动画语汇:缓动曲线与时长。
 *
 * <p>此前每个界面各写一份 {@code easeOutCubic} / {@code easeOutBack} /
 * {@code clamp01}(最多时同时存在 4 份拷贝),数值一旦想调整就要满仓库找。
 * 这里集中一次,数值与原实现逐位一致——抽取只为了让它们只有一处定义,
 * 不改变任何观感。</p>
 *
 * <p>纯函数,不依赖 Minecraft,可直接单元测试。</p>
 */
public final class SaoMotion {

    // ---------------------------------------------------------------- 时长(ms)

    /** 菜单整组弹出。 */
    public static final long OPEN_MS = 260;
    /** 左侧面板滑入。 */
    public static final long PANEL_MS = 200;
    /** 菜单项逐条错峰间隔。 */
    public static final long ITEM_STAGGER_MS = 45;
    /** 单条菜单项滑入。 */
    public static final long ITEM_MS = 180;
    /** 关闭缩回。 */
    public static final long CLOSE_MS = 170;
    /** 按压反馈高亮持续。 */
    public static final long PRESS_MS = 120;
    /** 主按钮堆叠展开:单按钮时长。 */
    public static final long UNFOLD_MS = 240;
    /** 主按钮堆叠展开:相邻错峰。 */
    public static final long UNFOLD_STAGGER_MS = 45;
    /** 整组上下浮动周期。 */
    public static final long BOB_PERIOD_MS = 2800;

    private SaoMotion() {
    }

    // ---------------------------------------------------------------- 缓动

    /** 夹到 0..1。与旧实现的短路写法保持一致(NaN 会原样穿过)。 */
    public static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    /**
     * 三次缓出:开头快、结尾稳。
     *
     * <p>内部先 {@code clamp01}。所有现存调用点本来就先夹过一遍,所以这层夹紧
     * 不改变任何观感,只是让越界输入不再可能画出离谱的缩放值。</p>
     */
    public static float easeOutCubic(float t) {
        float u = 1f - clamp01(t);
        return 1f - u * u * u;
    }

    /** 三次缓入。 */
    public static float easeInCubic(float t) {
        float c = clamp01(t);
        return c * c * c;
    }

    /** 三次缓入缓出。 */
    public static float easeInOutCubic(float t) {
        float c = clamp01(t);
        return c < 0.5f ? 4f * c * c * c : 1f - (float) Math.pow(-2f * c + 2f, 3) / 2f;
    }

    /**
     * 三次回弹缓出:略微冲过头再回落,用于弹出与展开。
     *
     * <p>回弹系数取菜单侧历史值({@code 1.70158 / 2.70158})。</p>
     *
     * <p><b>已知漂移:</b>{@code SAOSettingsScreen} 里另有一份回弹系数为
     * {@code 1.70158 × 1.25} 的版本,幅度略大。两者不合并是因为合并会改变
     * 设置界面的转场观感,属于需要单独确认的变更;设置界面重写时再统一。</p>
     */
    public static float easeOutBack(float t) {
        float u = clamp01(t) - 1f;
        return 1f + 2.70158f * u * u * u + 1.70158f * u * u;
    }
}
