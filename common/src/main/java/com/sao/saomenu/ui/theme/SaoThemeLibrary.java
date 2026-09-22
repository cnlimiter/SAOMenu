package com.sao.saomenu.ui.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.ui.text.SaoText;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 外部主题加载:扫描 {@code config/saomenu/themes/*.json} 并覆盖进 {@link SaoTheme}。
 *
 * <p><b>一个坏文件只影响它自己。</b>解析或校验失败时记录一条警告并跳过该文件,内置预设
 * 原样保留 —— 主题文件是用户可手改的输入,任何情况下都不该让游戏起不来或让菜单变空白。</p>
 *
 * <p>文件格式(除 {@code id} 外全部可选;缺省继承同 id 已注册定义,否则继承 SAO 令牌):</p>
 * <pre>
 * {
 *   "id": "my_theme",
 *   "name": "我的主题",
 *   "defaultHue": 120,
 *   "bodyFont": "minecraft:default",
 *   "displayFont": "minecraft:alt",
 *   "enterMillis": 260,
 *   "exitMillis": 170,
 *   "colors": {
 *     "textOnSurface": "#3C3C3D",
 *     "divider": "#A09FA0"
 *   }
 * }
 * </pre>
 *
 * <p>短 id(无 {@code :})在加载时显式迁到 {@code saomenu:<id>}。{@code accent} 不读:
 * 主题色永远由色相滑条实时派生,文件里写它只会与滑条打架。</p>
 */
public final class SaoThemeLibrary {

    /** 目录名(相对 Minecraft 的 config 目录)。 */
    public static final String DIR_NAME = "themes";

    /** id → 展示名:只有外部文件会登记,内置预设走 {@code saomenu.theme.<path>} 翻译键。 */
    private static final Map<String, String> LABELS = new LinkedHashMap<>();

    /** id → 语言键:外部文件写 {@code nameKey} 时登记,优先于字面 {@code name}。 */
    private static final Map<String, String> LABEL_KEYS = new LinkedHashMap<>();

    private SaoThemeLibrary() {
    }

    /**
     * 加载目录下全部 {@code .json}。目录不存在时静默返回 0。
     *
     * @return 成功载入的主题数(含替换内置的)
     */
    public static int load(Path configDir) {
        Path dir = configDir.resolve(DIR_NAME);
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        int ok = 0;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.sorted().toList()) {
                String name = f.getFileName().toString();
                if (!name.endsWith(".json") || !Files.isRegularFile(f)) {
                    continue;
                }
                if (loadOne(f)) {
                    ok++;
                }
            }
        } catch (IOException e) {
            warn("无法读取主题目录 " + dir + ": " + e.getMessage());
        }
        if (ok > 0) {
            StringBuilder sb = new StringBuilder();
            for (ThemeDefinition t : SaoTheme.definitions()) {
                sb.append(t.id()).append(' ');
            }
            com.sao.saomenu.SAOMenu.LOGGER.info("[SAOMenu] 外部主题载入 {} 个,可用预设:{}",
                    ok, sb.toString().trim());
        }
        return ok;
    }

    /** 单个文件:任何一步失败都返回 false 并留下原因,绝不抛出。 */
    private static boolean loadOne(Path file) {
        String text;
        try {
            text = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            warn(file.getFileName() + ": 读取失败 " + e.getMessage());
            return false;
        }

        JsonObject obj;
        try {
            JsonElement root = JsonParser.parseString(text);
            if (!root.isJsonObject()) {
                warn(file.getFileName() + ": 顶层必须是 JSON 对象");
                return false;
            }
            obj = root.getAsJsonObject();
        } catch (RuntimeException e) {
            warn(file.getFileName() + ": JSON 解析失败 " + e.getMessage());
            return false;
        }

        String rawId = str(obj, "id");
        ResourceLocation id = SaoTheme.parseThemeId(rawId);
        if (id == null) {
            warn(file.getFileName() + ": id 缺失或非法(短 id 只允许小写字母/数字/下划线 1-32 位,或合法 ResourceLocation)");
            return false;
        }

        ThemeDefinition previous = SaoTheme.definitionOrNull(id.toString());
        ThemeTokens baseTokens = previous != null ? previous.tokens() : ThemeTokens.sao();
        float hue = previous != null ? previous.defaultHue() : SaoTheme.byId(SaoTheme.SAO).defaultHue();
        if (obj.has("defaultHue")) {
            try {
                hue = obj.get("defaultHue").getAsFloat();
            } catch (RuntimeException e) {
                warn(file.getFileName() + ": defaultHue 不是数字");
                return false;
            }
            if (!Float.isFinite(hue)) {
                warn(file.getFileName() + ": defaultHue 不是有限数");
                return false;
            }
            hue = ThemeDefinition.wrapHue(hue);
        }

        ThemeColors base = baseTokens.colors();
        if (obj.has("colors")) {
            JsonElement ce = obj.get("colors");
            if (!ce.isJsonObject()) {
                warn(file.getFileName() + ": colors 必须是对象");
                return false;
            }
            JsonObject co = ce.getAsJsonObject();
            if (co.has("accent")) {
                warn(file.getFileName() + ": colors.accent 被忽略(主题色由 defaultHue 派生)");
            }
            int[] v = {base.accent(), base.textOnSurface(), base.textOnAccent(), base.textMuted(),
                    base.highlight(), base.divider(), base.shadow(), base.surfaceSlot(),
                    base.dialogSurface(), base.dialogShadow()};
            String[] keys = {"accent", "textOnSurface", "textOnAccent", "textMuted", "highlight",
                    "divider", "shadow", "surfaceSlot", "dialogSurface", "dialogShadow"};
            for (int i = 1; i < keys.length; i++) {
                if (!co.has(keys[i])) {
                    continue;
                }
                Integer parsed = color(co.get(keys[i]));
                if (parsed == null) {
                    warn(file.getFileName() + ": colors." + keys[i]
                            + " 不是颜色(用 \"#RRGGBB\"/\"#AARRGGBB\" 或整数)");
                    return false;
                }
                v[i] = parsed;
            }
            base = new ThemeColors(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7], v[8], v[9]);
        }

        ResourceLocation bodyFont = baseTokens.bodyFont();
        ResourceLocation displayFont = baseTokens.displayFont();
        if (obj.has("bodyFont")) {
            ResourceLocation parsed = font(obj, "bodyFont");
            if (parsed == null) {
                warn(file.getFileName() + ": bodyFont 不是合法资源名");
                return false;
            }
            bodyFont = parsed;
        }
        if (obj.has("displayFont")) {
            ResourceLocation parsed = font(obj, "displayFont");
            if (parsed == null) {
                warn(file.getFileName() + ": displayFont 不是合法资源名");
                return false;
            }
            displayFont = parsed;
        }

        int enter = baseTokens.enterMillis();
        int exit = baseTokens.exitMillis();
        Integer parsedEnter = millis(obj, "enterMillis", file);
        if (obj.has("enterMillis") && parsedEnter == null) {
            return false;
        }
        if (parsedEnter != null) {
            enter = parsedEnter;
        }
        Integer parsedExit = millis(obj, "exitMillis", file);
        if (obj.has("exitMillis") && parsedExit == null) {
            return false;
        }
        if (parsedExit != null) {
            exit = parsedExit;
        }

        ThemeTokens tokens;
        try {
            tokens = new ThemeTokens(base, bodyFont, displayFont, enter, exit);
        } catch (RuntimeException e) {
            warn(file.getFileName() + ": tokens 非法 " + e.getMessage());
            return false;
        }

        int order = previous != null ? previous.order() : SaoTheme.nextOrder();
        if (obj.has("order")) {
            try {
                order = Math.toIntExact(integer(obj.get("order")));
            } catch (RuntimeException e) {
                warn(file.getFileName() + ": order 不是整数");
                return false;
            }
        }

        String labelKey = str(obj, "nameKey");
        String label = str(obj, "name");
        Component component;
        String canonical = id.toString();
        if (labelKey != null && !labelKey.isBlank()) {
            LABEL_KEYS.put(canonical, labelKey);
            LABELS.remove(canonical);
            component = Component.translatable(labelKey);
        } else if (label != null && !label.isBlank()) {
            LABELS.put(canonical, label);
            LABEL_KEYS.remove(canonical);
            component = Component.literal(label);
        } else if (previous != null) {
            component = previous.label();
        } else {
            component = Component.literal(id.getPath());
        }

        try {
            SaoTheme.overlay(new ThemeDefinition(id, order, component, hue, tokens));
        } catch (RuntimeException e) {
            warn(file.getFileName() + ": 主题定义非法 " + e.getMessage());
            return false;
        }
        return true;
    }

    /** 主题的展示名:外部文件的 nameKey(已翻译)> 外部文件的 name > 定义标签 > 翻译键 > path。 */
    public static String label(String id) {
        String literal = LABELS.get(id);
        if (literal != null) {
            return literal;
        }
        String customKey = LABEL_KEYS.get(id);
        if (customKey != null) {
            String translated = SaoText.resolveLabel(customKey);
            if (translated != null && !translated.equals(customKey)) {
                return translated;
            }
            return pathOf(id);
        }
        ThemeDefinition def = SaoTheme.definitionOrNull(id);
        if (def != null) {
            if (def.label().getContents() instanceof TranslatableContents tc) {
                String translated = SaoText.resolveLabel(tc.getKey());
                if (translated != null && !translated.equals(tc.getKey())) {
                    return translated;
                }
            } else {
                String text = def.label().getString();
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        String path = pathOf(id);
        String key = "saomenu.theme." + path;
        String resolved = SaoText.resolveLabel(key);
        return resolved == null || resolved.equals(key) ? path : resolved;
    }

    /** 测试用:清掉外部主题留下的展示名登记。 */
    static void resetForTest() {
        LABELS.clear();
        LABEL_KEYS.clear();
    }

    private static String pathOf(String canonical) {
        int colon = canonical.indexOf(':');
        return colon < 0 ? canonical : canonical.substring(colon + 1);
    }

    private static String str(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) {
            return null;
        }
        try {
            return o.get(key).getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static ResourceLocation font(JsonObject o, String key) {
        String raw = str(o, key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return ResourceLocation.tryParse(raw.trim());
    }

    private static Integer millis(JsonObject o, String key, Path file) {
        if (!o.has(key)) {
            return null;
        }
        try {
            int value = Math.toIntExact(integer(o.get(key)));
            if (value < 0) {
                warn(file.getFileName() + ": " + key + " 不能为负");
                return null;
            }
            return value;
        } catch (RuntimeException e) {
            warn(file.getFileName() + ": " + key + " 不是整数");
            return null;
        }
    }

    private static long integer(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Not a JSON integer");
        }
        return value.getAsBigDecimal().longValueExact();
    }

    /** 颜色字面量:{@code "#RRGGBB"}、{@code "#AARRGGBB"} 或有符号/无符号 32 位整数。 */
    private static Integer color(JsonElement e) {
        if (!e.isJsonPrimitive()) {
            return null;
        }
        try {
            if (e.getAsJsonPrimitive().isNumber()) {
                long value = integer(e);
                return value >= Integer.MIN_VALUE && value <= 0xFFFFFFFFL ? (int) value : null;
            }
            String s = e.getAsString().trim();
            if ((s.length() != 7 && s.length() != 9) || s.charAt(0) != '#') {
                return null;
            }
            int value = HexFormat.fromHexDigits(s, 1, s.length());
            return s.length() == 7 ? value | 0xFF000000 : value;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void warn(String msg) {
        com.sao.saomenu.SAOMenu.LOGGER.warn("[SAOMenu] 主题文件 {} 已跳过:{}", DIR_NAME, msg);
    }
}
