package com.sao.saomenu.ui;

/**
 * 主题调色板。
 *
 * <p>按<b>角色</b>而不是按界面命名:同一个角色在不同界面漂移出过好几个值,
 * 那些漂移由各界面迁移时逐个显式处理,不在这个 record 里用"再加一个字段"掩盖过去。</p>
 *
 * <p>{@code accent} 是唯一由用户色相滑条实时派生的颜色,其余都是主题预设固定值;
 * 主题切换时整块替换,不需要逐个改调用点。</p>
 *
 * @param accent         主题色:按钮高亮、条目悬停、镶边、选中态
 * @param textOnSurface  浅色面板/条目上的正文
 * @param textOnAccent   主题色面上的文字(悬停反白)
 * @param textMuted      次要/占位文字
 * @param highlight      纯白强调:画在主题色或深色之上的高亮与说明文字
 * @param divider        面板内分隔线与下划线
 * @param shadow         浅色面板的投影
 * @param surfaceSlot    槽位/图片底衬(半透明白)
 * @param dialogSurface  弹窗内衬(近实心白,压住背后的菜单内容)
 * @param dialogShadow   弹窗投影
 */
public record ThemeColors(
        int accent,
        int textOnSurface,
        int textOnAccent,
        int textMuted,
        int highlight,
        int divider,
        int shadow,
        int surfaceSlot,
        int dialogSurface,
        int dialogShadow
) {

    /** 换一个主题色,其余不变。 */
    public ThemeColors withAccent(int newAccent) {
        return new ThemeColors(newAccent, textOnSurface, textOnAccent, textMuted, highlight,
                divider, shadow, surfaceSlot, dialogSurface, dialogShadow);
    }

    /**
     * SAO 原版配色:参考截图逐像素实测值。
     *
     * <p>这些数字就是重构前的原值,抽取只改了书写位置——任何一处对不上都会在
     * 截图像素比对里暴露出来。</p>
     */
    public static ThemeColors sao() {
        return new ThemeColors(
                0xFFEFA603,   // accent 占位,实际由色相派生
                0xFF3C3C3D,   // textOnSurface
                0xFFF9F9F9,   // textOnAccent
                0xFF9A9DA0,   // textMuted
                0xFFFFFFFF,   // highlight
                0xFFA09FA0,   // divider
                0x3A303030,   // shadow
                0x52F9F9F9,   // surfaceSlot
                0xE6FFFFFF,   // dialogSurface
                0x6E303030    // dialogShadow
        );
    }
}
