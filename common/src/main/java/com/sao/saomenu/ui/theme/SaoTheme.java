package com.sao.saomenu.ui.theme;

import com.sao.saomenu.config.SAOConfig;

import java.util.ArrayList;
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

    /** 内置预设:永远在,且顺序固定(决定设置页按钮顺序)。 */
    private static final List<SaoTheme> BUILTIN = List.of(
            new SaoTheme(SAO, 41.44f, ThemeColors.sao()),
            new SaoTheme(ALO, 202f, ThemeColors.sao()),
            new SaoTheme(GGO, 355f, ThemeColors.sao()));

    /** 当前可用预设 = 内置 + {@code config/saomenu/themes/*.json} 载入的。 */
    private static final List<SaoTheme> PRESETS = new ArrayList<>(BUILTIN);

    private static String selectedId = SAO;

    /** 按 id 取预设;不存在返回 null(与 {@link #byId} 不同,这里不做回落)。 */
    private static SaoTheme findById(String id) {
        if (id == null) {
            return null;
        }
        for (SaoTheme t : PRESETS) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** 按 id 取预设;未知 id 回落到 SAO(配置文件被手改坏了也不至于崩)。 */
    public static SaoTheme byId(String id) {
        SaoTheme found = findById(id);
        return found != null ? found : PRESETS.get(0);
    }

    /** 全部可用预设(内置 + 外部主题文件)。返回不可变副本,避免调用点改到注册表。 */
    public static List<SaoTheme> presets() {
        return List.copyOf(PRESETS);
    }

    /**
     * 注册一个外部主题(来自 {@code config/saomenu/themes/*.json})。
     *
     * <p>id 与已有预设相同则<b>替换</b>它(主题包改写内置预设),否则追加到末尾。
     * 不需要处理"当前选中项被替换":{@link #active()} 每次都按 id 重新查。</p>
     */
    public static void register(SaoTheme theme) {
        for (int i = 0; i < PRESETS.size(); i++) {
            if (PRESETS.get(i).id.equals(theme.id)) {
                PRESETS.set(i, theme);
                return;
            }
        }
        PRESETS.add(theme);
    }

    /** 是否已按持久化色相恢复过选择(冷启动只做一次)。 */
    private static boolean resolvedFromConfig = false;

    /**
     * 冷启动恢复:配置里没有 themeId 字段,只能按色相反查一次。
     *
     * <p>只恢复<b>一次</b>:之后以内存选择为准,否则用户拖色相滑条时身份会被反查改掉,
     * JSON 主题的自定义调色板会当场丢失。</p>
     */
    private static void resolveFromConfigOnce() {
        if (resolvedFromConfig) {
            return;
        }
        resolvedFromConfig = true;
        // 首选配置里的 themeId:它不受色相滑条影响,所以"选中 JSON 主题 → 拖色相 → 重启"
        // 也能保住自定义调色板(按色相反查做不到这一点)。
        String stored = SAOConfig.themeId();
        if (findById(stored) != null) {
            selectedId = stored;
            return;
        }
        // 兜底:旧配置没有 themeId,或该主题文件已被删除 —— 按色相反查
        int hue = Math.round(SAOConfig.accentHue());
        for (SaoTheme t : PRESETS) {
            if (Math.round(t.defaultHue) == hue) {
                selectedId = t.id;
                return;
            }
        }
    }

    /**
     * 当前色相下与之匹配的预设 id;色相被自定义时不匹配任何预设(返回 null)。
     *
     * <p><b>这是"哪个按钮该高亮"的唯一判定</b>,与 {@link #selectedId()} 是两个问题:
     * 用户选了主题再拖色相滑条时,身份(selectedId)要保持以留住自定义调色板,
     * 但配色已不再是那个预设,按钮不该继续高亮。设置界面与自检共用本方法,避免各写一遍。</p>
     */
    public static String matchingPreset(float hue) {
        int want = Math.round(hue);
        for (SaoTheme t : PRESETS) {
            if (Math.round(t.defaultHue) == want) {
                return t.id;
            }
        }
        return null;
    }

    /** 当前选中预设的 id —— 这是<b>调色板身份</b>,不是"哪个按钮该高亮"。
     *
     * <p>两者是不同的问题:用户选了主题再拖色相滑条时,身份要保持(否则自定义调色板丢失),
     * 但配色已经不再是那个预设了,按钮不该继续高亮。高亮判定见 {@link #matchingPreset(float)}。</p>
     */
    public static String selectedId() {
        resolveFromConfigOnce();
        return selectedId;
    }

    /** 选择预设;把 id 与默认色相一并写入配置。 */
    public static void select(String id) {
        SaoTheme t = byId(id);
        selectedId = t.id;
        resolvedFromConfig = true; // 显式选择优先于冷启动反查
        SAOConfig.setThemeId(t.id);
        SAOConfig.setAccentHue(t.defaultHue);
    }

    /**
     * 当前生效主题:预设调色板 + 用户色相实时派生的主题色。
     *
     * <p>每次取用都会重算 accent,所以色相滑条拖动时全部界面立即跟随,
     * 不需要缓存失效逻辑。</p>
     */
    public static SaoTheme active() {
        SaoTheme preset = byId(selectedId());
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

    /** 测试用:复位到默认预设,并丢掉外部主题文件载入的预设。 */
    static void resetForTest() {
        selectedId = SAO;
        resolvedFromConfig = false;
        PRESETS.clear();
        PRESETS.addAll(BUILTIN);
    }
}
