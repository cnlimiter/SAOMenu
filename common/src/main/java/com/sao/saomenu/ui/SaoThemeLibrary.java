package com.sao.saomenu.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 外部主题加载:扫描 {@code config/saomenu/themes/*.json} 并注册进 {@link SaoTheme}。
 *
 * <p><b>一个坏文件只影响它自己。</b>解析或校验失败时记录一条警告并跳过该文件,内置预设
 * 原样保留 —— 主题文件是用户可手改的输入,任何情况下都不该让游戏起不来或让菜单变空白。</p>
 *
 * <p>文件格式(除 {@code id} 外全部可选,缺省继承 SAO 配色,因此只写想改的那几个键也能用):</p>
 * <pre>
 * {
 *   "id": "my_theme",
 *   "name": "我的主题",
 *   "defaultHue": 120,
 *   "colors": {
 *     "textOnSurface": "#3C3C3D",
 *     "divider": "#A09FA0"
 *   }
 * }
 * </pre>
 *
 * <p>{@code accent} 不读:主题色永远由 {@code defaultHue} 经色相滑条实时派生
 * (见 {@link SaoTheme#active()}),文件里写它只会与滑条打架。</p>
 */
public final class SaoThemeLibrary {

    /** 目录名(相对 Minecraft 的 config 目录)。 */
    public static final String DIR_NAME = "themes";

    private static final Pattern ID_OK = Pattern.compile("[a-z0-9_]{1,32}");

    /** id → 展示名:只有外部文件会登记,内置预设走 {@code saomenu.theme.<id>} 翻译键。 */
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
            for (SaoTheme t : SaoTheme.presets()) {
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

        String id = str(obj, "id");
        if (id == null || !ID_OK.matcher(id).matches()) {
            warn(file.getFileName() + ": id 缺失或非法(只允许小写字母/数字/下划线,1-32 位)");
            return false;
        }

        float hue = SaoTheme.byId(SaoTheme.SAO).defaultHue();
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
            hue = ((hue % 360f) + 360f) % 360f;
        }

        ThemeColors base = ThemeColors.sao();
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
            for (int i = 0; i < keys.length; i++) {
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

        SaoTheme.register(new SaoTheme(id, hue, base));
        // nameKey 优先:主题包可以只给语言键,由资源包做本地化
        String labelKey = str(obj, "nameKey");
        String label = str(obj, "name");
        if (labelKey != null && !labelKey.isBlank()) {
            LABEL_KEYS.put(id, labelKey);
            LABELS.remove(id);
        } else if (label != null && !label.isBlank()) {
            LABELS.put(id, label);
            LABEL_KEYS.remove(id);
        }
        return true;
    }

    /** 主题的展示名:外部文件的 nameKey(已翻译)> 外部文件的 name > 翻译键 > id 本身。 */
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
            // 语言键没解析到(缺资源包):退回 id,而不是把裸键名显示在界面上
            return id;
        }
        String key = "saomenu.theme." + id;
        String resolved = SaoText.resolveLabel(key);
        return resolved == null || resolved.equals(key) ? id : resolved;
    }

    /** 测试用:清掉外部主题留下的展示名登记。 */
    static void resetForTest() {
        LABELS.clear();
        LABEL_KEYS.clear();
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

    /** 颜色字面量:{@code "#RGB"}、{@code "#RRGGBB"}、{@code "#AARRGGBB"} 或 32 位整数。 */
    private static Integer color(JsonElement e) {
        if (!e.isJsonPrimitive()) {
            return null;
        }
        try {
            if (e.getAsJsonPrimitive().isNumber()) {
                return e.getAsInt();
            }
            String s = e.getAsString().trim();
            if (s.startsWith("#")) {
                s = s.substring(1);
            }
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) {
                v |= 0xFF000000L; // 不带 alpha 的写法默认为不透明
            }
            if (s.length() > 8) {
                return null;
            }
            return (int) v;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static void warn(String msg) {
        com.sao.saomenu.SAOMenu.LOGGER.warn("[SAOMenu] 主题文件 {} 已跳过:{}", DIR_NAME, msg);
    }
}
