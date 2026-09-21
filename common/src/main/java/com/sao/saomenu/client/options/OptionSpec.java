package com.sao.saomenu.client.options;

import java.util.Locale;

/**
 * 设置界面的一行:一个滑块、一个开关,或一行预设按钮。
 *
 * <p>此前设置界面用 7 个并联的 {@code switch (Page)} 描述同一批行
 * ({@code rowCount} / {@code rowIsSlider} / {@code rowLabel} / {@code sliderGet} /
 * {@code sliderSet} / {@code sliderMin} / {@code sliderMax} / {@code toggleGet} /
 * {@code toggleFlip})。新增一项设置要同时改 7 处并保持下标一致,漏一处或错一位
 * 就是静默的错值/错标签。改成声明式行表后,加一项只是追加一条。</p>
 *
 * @param labelKey 语言键
 * @param kind     行类型
 * @param get      滑块读数;非滑块为 null
 * @param set      滑块写值;非滑块为 null
 * @param boolGet  开关读数;非开关为 null
 * @param boolFlip 开关翻转;非开关为 null
 * @param min      滑块下限
 * @param max      滑块上限
 * @param fmt      数值显示格式
 */
public record OptionSpec(
        String labelKey,
        Kind kind,
        Get get,
        Set set,
        BoolGet boolGet,
        BoolFlip boolFlip,
        float min,
        float max,
        Fmt fmt
) {

    public enum Kind { SLIDER, TOGGLE, PRESET }

    /** 数值显示格式。 */
    public enum Fmt { PERCENT, MULT2, MULT1, DEG }

    @FunctionalInterface
    public interface Get {
        float get();
    }

    @FunctionalInterface
    public interface Set {
        void set(float v);
    }

    @FunctionalInterface
    public interface BoolGet {
        boolean get();
    }

    @FunctionalInterface
    public interface BoolFlip {
        void flip();
    }

    /** 滑块行。 */
    public static OptionSpec slider(String labelKey, Get get, Set set, float min, float max, Fmt fmt) {
        return new OptionSpec(labelKey, Kind.SLIDER, get, set, null, null, min, max, fmt);
    }

    /** 开关行。 */
    public static OptionSpec toggle(String labelKey, BoolGet get, BoolFlip flip) {
        return new OptionSpec(labelKey, Kind.TOGGLE, null, null, get, flip, 0f, 0f, Fmt.MULT1);
    }

    /**
     * 预设按钮行(主题三选一)。
     *
     * <p>仍带上一组滑块访问器:该行在界面上不是滑块,但历史代码在若干分支里
     * 会按"默认分支"读到色相,给它同样的取值可以保证改造前后逐像素一致。</p>
     */
    public static OptionSpec preset(String labelKey, Get hueGet, Set hueSet) {
        return new OptionSpec(labelKey, Kind.PRESET, hueGet, hueSet, null, null, 0f, 360f, Fmt.DEG);
    }

    public boolean isSlider() {
        return kind == Kind.SLIDER;
    }

    /** 按行格式渲染数值文本。 */
    public String format(float v) {
        return switch (fmt) {
            case PERCENT -> String.format(Locale.ROOT, "%.0f%%", v * 100f);
            case MULT2 -> String.format(Locale.ROOT, "%.2fx", v);
            case MULT1 -> String.format(Locale.ROOT, "%.1fx", v);
            case DEG -> Math.round(v) + "°";
        };
    }
}
