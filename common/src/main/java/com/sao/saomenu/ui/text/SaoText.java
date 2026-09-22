package com.sao.saomenu.ui.text;

import net.minecraft.network.chat.Component;

/**
 * 文案工具。
 *
 * <p>此前每个界面都各写一份 {@code tr}(手写 {@code {n}} 替换,为的是绕开
 * Forge/Fabric 对 {@code MessageFormat} 行为不一致)与一份 {@code resolveLabel}
 * (区分"语言键"和"已经是最终文本的动态名")。集中到这里。</p>
 */
public final class SaoText {

    private SaoText() {
    }

    /**
     * 取翻译并手动替换 {@code {0}}、{@code {1}}…
     *
     * <p>不用 {@code Component.translatable(key, args)} 的 MessageFormat 替换:
     * 两个平台对单引号与数字格式的处理不一致,而菜单里大量出现玩家名与物品名,
     * 一旦被当成格式串就会吃掉字符。</p>
     */
    public static String tr(String key, Object... args) {
        String s = Component.translatable(key).getString();
        for (int i = 0; i < args.length; i++) {
            s = s.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return s;
    }

    /**
     * 菜单项标签:语言键 → 翻译;未知键(在线玩家名、物品名这类动态文本)→ 原样返回。
     *
     * <p>判据是"像不像一个键":含点号或带 {@code saomenu.} 前缀的才查翻译表,
     * 其余当作已经是最终文本。原版对缺失翻译返回键本身,所以动态名也能原样显示。</p>
     */
    public static String resolveLabel(String key) {
        if (!key.startsWith("saomenu.") && !key.contains(".")) {
            return key;
        }
        return Component.translatable(key).getString();
    }
}
