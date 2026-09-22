package com.sao.saomenu.ui.theme;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 主题:一套调色板 + 它的默认色相。
 *
 * <p>主题是全局的(不是每个界面一份),所以用静态入口 {@link #active()} / {@link #tokens()} 取用。
 * 给上百个绘制调用点穿一个 context 参数换来的只是"纯度",却让每次读主题都要
 * 往上找参数;这里选择先简单。</p>
 *
 * <p>{@code accent} 由色相实时派生,因此主题切换 = 换预设 + 换色相;
 * 色相滑条是用户对主题的个人覆盖,预设只提供默认值。</p>
 *
 * <p>代码预设经 {@link #registerBuiltins} 进入公开注册表,冻结后
 * {@link #installRegistered} 成为稳定底表,用户 JSON 再按同 id 覆盖。
 * {@link #definitions()} 是合并后的不可变快照,不是每次映射。</p>
 */
public record SaoTheme(String id, float defaultHue, ThemeColors colors) {

    /** SAO:原作橙。 */
    public static final String SAO = SAOMenu.MOD_ID + ":sao";
    /** ALO:妖精之舞蓝。 */
    public static final String ALO = SAOMenu.MOD_ID + ":alo";
    /** GGO:枪界红。 */
    public static final String GGO = SAOMenu.MOD_ID + ":ggo";

    static final Pattern SHORT_ID = Pattern.compile("[a-z0-9_]{1,32}");

    private static final List<ThemeDefinition> BUILTIN = List.of(
            builtin("sao", 100, 41.44f),
            builtin("alo", 200, 202f),
            builtin("ggo", 300, 355f));
    private static final Comparator<ThemeDefinition> ORDER = Comparator.comparingInt(ThemeDefinition::order)
            .thenComparing(theme -> theme.id().toString());

    /** 当前可用预设 = 冻结的代码定义 + {@code config/saomenu/themes/*.json} 覆盖。 */
    private static final List<ThemeDefinition> PRESETS = new ArrayList<>(BUILTIN);
    private static List<ThemeDefinition> definitionSnapshot = List.copyOf(PRESETS);
    private static List<SaoTheme> presetSnapshot = views(PRESETS);

    private static ThemeTokens cachedTokens;
    private static String cachedTokenId;
    private static float cachedHue;

    private static String selectedId = SAO;

    private static ThemeDefinition builtin(String path, int order, float hue) {
        return new ThemeDefinition(
                new ResourceLocation(SAOMenu.MOD_ID, path),
                order,
                Component.translatable("saomenu.theme." + path),
                hue,
                ThemeTokens.sao());
    }

    private static List<SaoTheme> views(List<ThemeDefinition> defs) {
        List<SaoTheme> views = new ArrayList<>(defs.size());
        for (ThemeDefinition def : defs) {
            views.add(view(def));
        }
        return List.copyOf(views);
    }

    private static SaoTheme view(ThemeDefinition def) {
        return new SaoTheme(def.id().toString(), def.defaultHue(), def.tokens().colors());
    }

    /** Register the three builtin SAO / ALO / GGO definitions. */
    public static void registerBuiltins(SaoUiRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        for (ThemeDefinition def : BUILTIN) {
            registry.theme(def);
        }
    }

    /**
     * Install the frozen code (and addon) definitions as the stable preset table.
     * User JSON overlays are applied afterwards and are not passed through this method.
     */
    public static void installRegistered(List<ThemeDefinition> definitions) {
        PRESETS.clear();
        if (definitions != null) {
            for (ThemeDefinition def : definitions) {
                if (def != null) {
                    PRESETS.add(def);
                }
            }
        }
        if (PRESETS.isEmpty()) {
            PRESETS.addAll(BUILTIN);
        }
        resolvedFromConfig = false;
        refreshSnapshots();
    }

    /**
     * Merged selectable themes: frozen code definitions plus user-file overlays.
     * Cached immutable snapshot; callers must not expect a defensive copy per call.
     */
    public static List<ThemeDefinition> definitions() {
        return definitionSnapshot;
    }

    /** 按 id 取预设;不存在返回 null(与 {@link #byId} 不同,这里不做回落)。 */
    private static ThemeDefinition findDefinition(String id) {
        for (ThemeDefinition def : PRESETS) {
            if (def.id().toString().equals(id)) {
                return def;
            }
        }
        return null;
    }

    static ThemeDefinition definitionOrNull(String id) {
        return findDefinition(id);
    }

    /** 按 id 取预设;未知 id 回落到 SAO(配置文件被手改坏了也不至于崩)。 */
    public static SaoTheme byId(String id) {
        ThemeDefinition found = findDefinition(id);
        return view(found != null ? found : defaultDefinition());
    }

    private static ThemeDefinition defaultDefinition() {
        ThemeDefinition sao = findDefinition(SAO);
        return sao != null ? sao : BUILTIN.get(0);
    }

    /** 全部可用预设(内置 + 外部主题文件)。返回不可变副本,避免调用点改到注册表。 */
    public static List<SaoTheme> presets() {
        return presetSnapshot;
    }

    /**
     * Overlay or append a definition after freeze (user JSON / same-id reload).
     * Same id replaces its definition; the merged view remains ordered by order then full id.
     */
    static void overlay(ThemeDefinition theme) {
        Objects.requireNonNull(theme, "theme");
        int index = 0;
        String id = theme.id().toString();
        while (index < PRESETS.size() && !PRESETS.get(index).id().toString().equals(id)) {
            index++;
        }
        if (index < PRESETS.size()) {
            PRESETS.set(index, theme);
        } else {
            PRESETS.add(theme);
        }
        refreshSnapshots();
    }

    static int nextOrder() {
        int max = 0;
        for (ThemeDefinition def : PRESETS) {
            max = Math.max(max, def.order());
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) max + 100);
    }

    private static void refreshSnapshots() {
        PRESETS.sort(ORDER);
        definitionSnapshot = List.copyOf(PRESETS);
        presetSnapshot = views(PRESETS);
        cachedTokens = null;
        cachedTokenId = null;
    }

    /** 配置身份变化时重新解析;只拖动色相不改变调色板身份。 */
    private static boolean resolvedFromConfig = false;
    private static String resolvedConfigId;

    /**
     * Legacy config / JSON ids were un-namespaced ({@code sao}). Missing {@code :}
     * is migrated to {@code saomenu:<id>} explicitly; already-namespaced ids stay.
     */
    public static String canonicalizeId(String raw) {
        ResourceLocation loc = parseThemeId(raw);
        return loc != null ? loc.toString() : SAO;
    }

    /**
     * Parse a theme identity. Short ids matching {@link #SHORT_ID} become
     * {@code saomenu:<id>}; namespaced ids go through {@link ResourceLocation#tryParse}.
     */
    public static ResourceLocation parseThemeId(String raw) {
        if (raw == null) {
            return null;
        }
        String id = raw.trim();
        if (id.isEmpty()) {
            return null;
        }
        if (id.indexOf(':') < 0) {
            if (!SHORT_ID.matcher(id).matches()) {
                return null;
            }
            return new ResourceLocation(SAOMenu.MOD_ID, id);
        }
        return ResourceLocation.tryParse(id);
    }

    /** Restore selection after loading/resetting config, never by following every hue-slider tick. */
    private static void resolveConfiguredSelection() {
        String stored = SAOConfig.themeId();
        if (resolvedFromConfig && Objects.equals(stored, resolvedConfigId)) {
            return;
        }
        resolvedFromConfig = true;
        resolvedConfigId = stored;
        selectedId = SAO;
        if (findDefinition(stored) != null) {
            selectedId = stored;
            return;
        }
        // Legacy/missing themes resolve by hue once for this configured identity.
        int hue = Math.round(SAOConfig.accentHue());
        for (ThemeDefinition theme : PRESETS) {
            if (Math.round(theme.defaultHue()) == hue) {
                selectedId = theme.id().toString();
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
        for (ThemeDefinition t : PRESETS) {
            if (Math.round(t.defaultHue()) == want) {
                return t.id().toString();
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
        resolveConfiguredSelection();
        return selectedId;
    }

    /** 选择完整 namespaced id;把 id 与默认色相一并写入配置。 */
    public static void select(String id) {
        SaoTheme t = byId(id);
        selectedId = t.id;
        resolvedFromConfig = true; // 显式选择优先于冷启动反查
        SAOConfig.setThemeId(t.id);
        resolvedConfigId = t.id;
        SAOConfig.setAccentHue(t.defaultHue);
        cachedTokens = null;
        cachedTokenId = null;
    }

    /**
     * Active tokens: registered fonts / durations plus the live hue-derived accent.
     * Rebuilt only when selection, hue, or the underlying definition changes.
     */
    public static ThemeTokens tokens() {
        String id = selectedId();
        float hue = SAOConfig.accentHue();
        if (cachedTokens == null || !id.equals(cachedTokenId) || Float.compare(cachedHue, hue) != 0) {
            ThemeDefinition preset = findDefinition(id);
            if (preset == null) {
                preset = defaultDefinition();
            }
            ThemeTokens base = preset.tokens();
            cachedTokens = base.withColors(base.colors().withAccent(accentFromHue(hue)));
            cachedTokenId = id;
            cachedHue = hue;
        }
        return cachedTokens;
    }

    /**
     * 当前生效主题:预设调色板 + 用户色相实时派生的主题色。
     *
     * <p>仅在选择、色相或预设内容变化时重建不可变调色板;
     * 每帧多次读取不会复制预设列表或重复派生颜色。</p>
     */
    public static SaoTheme active() {
        ThemeTokens tokens = tokens();
        ThemeDefinition preset = findDefinition(selectedId());
        if (preset == null) {
            preset = defaultDefinition();
        }
        return new SaoTheme(preset.id().toString(), preset.defaultHue(), tokens.colors());
    }

    /** 当前生效调色板(最常用的一层快捷方式)。 */
    public static ThemeColors palette() {
        return tokens().colors();
    }

    /** 当前主题色。 */
    public static int accent() {
        return palette().accent();
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
        resolvedConfigId = null;
        PRESETS.clear();
        PRESETS.addAll(BUILTIN);
        cachedTokens = null;
        cachedTokenId = null;
        refreshSnapshots();
    }
}
