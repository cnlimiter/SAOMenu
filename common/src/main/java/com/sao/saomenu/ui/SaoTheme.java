package com.sao.saomenu.ui;

import com.sao.saomenu.client.SAOConfig;

import java.util.List;

/**
 * 主题:一套调色板 + 它的默认色相。
 *
 * <p>主题是全局的(不是每个界面一份),所以用静态入口 {@link #active()} 取用。
 * 给上百个绘制调用点穿一个 context 参数换来的只是"纯度",却让每次读主题都要
 * 往上找参数;这里选择先简单。</p>
 *
 * <p>{@code accent} 由色相实时派生,因此主题切换 = 换预设 + 换色相;
 * 色相滑条是用户对主题的个人覆盖,预设只提供默认值。</p>
 */
public record SaoTheme(String id, float defaultHue, ThemeColors colors) {

    /** SAO:原作橙。 */
    public static final String SAO = "sao";
    /** ALO:妖精之舞蓝。 */
    public static final String ALO = "alo";
    /** GGO:枪界红。 */
    public static final String GGO = "ggo";

    private static final List<SaoTheme> PRESETS = List.of(
            new SaoTheme(SAO, 41.44f, ThemeColors.sao()),
            new SaoTheme(ALO, 202f, ThemeColors.sao()),
            new SaoTheme(GGO, 355f, ThemeColors.sao()));

    private static String selectedId = SAO;

    /** 按 id 取预设;未知 id 回落到 SAO(配置文件被手改坏了也不至于崩)。 */
    public static SaoTheme byId(String id) {
        for (SaoTheme t : PRESETS) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return PRESETS.get(0);
    }

    /** 全部内置预设。 */
    public static List<SaoTheme> presets() {
        return PRESETS;
    }

    /** 当前选中的预设 id。 */
    public static String selectedId() {
        return selectedId;
    }

    /** 选择预设;同时把该预设的默认色相写入配置。 */
    public static void select(String id) {
        SaoTheme t = byId(id);
        selectedId = t.id;
        SAOConfig.setAccentHue(t.defaultHue);
    }

    /**
     * 当前生效主题:预设调色板 + 用户色相实时派生的主题色。
     *
     * <p>每次取用都会重算 accent,所以色相滑条拖动时全部界面立即跟随,
     * 不需要缓存失效逻辑。</p>
     */
    public static SaoTheme active() {
        SaoTheme preset = byId(selectedId);
        return new SaoTheme(preset.id, preset.defaultHue,
                preset.colors.withAccent(accentFromHue(SAOConfig.accentHue())));
    }

    /** 当前生效调色板(最常用的一层快捷方式)。 */
    public static ThemeColors palette() {
        return active().colors;
    }

    /** 当前主题色。 */
    public static int accent() {
        return accentFromHue(SAOConfig.accentHue());
    }

    /** 由色相派生的主题色(饱和 0.987、明度 0.937,即 SAO 橙 #EFA603 的取值)。 */
    public static int accentFromHue(float hue) {
        return hsvToRgb(hue, 0.987f, 0.937f);
    }

    /** HSV(H,S,V)→ 不透明 ARGB。色相按 60° 分段落入 RGB 立方体的六个面。 */
    public static int hsvToRgb(float hue, float s, float v) {
        float c = v * s;
        float hp = (hue % 360f) / 60f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r = 0f;
        float g = 0f;
        float b = 0f;
        switch ((int) hp) {
            case 0 -> {
                r = c;
                g = x;
            }
            case 1 -> {
                r = x;
                g = c;
            }
            case 2 -> {
                g = c;
                b = x;
            }
            case 3 -> {
                g = x;
                b = c;
            }
            case 4 -> {
                r = x;
                b = c;
            }
            default -> {
                r = c;
                b = x;
            }
        }
        float m = v - c;
        return 0xFF000000
                | (Math.round((r + m) * 255f) << 16)
                | (Math.round((g + m) * 255f) << 8)
                | Math.round((b + m) * 255f);
    }

    /** 测试用:复位到默认预设。 */
    static void resetForTest() {
        selectedId = SAO;
    }
}
