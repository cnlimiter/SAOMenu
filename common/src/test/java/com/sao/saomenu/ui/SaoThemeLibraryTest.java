package com.sao.saomenu.ui;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 外部主题加载。
 *
 * <p>主题文件是<b>用户可手改的输入</b>,所以契约的核心不是"能加载",而是
 * "坏输入只影响它自己":任何解析/校验失败都必须跳过该文件并保留内置预设。</p>
 */
class SaoThemeLibraryTest {

    @TempDir
    Path configDir;

    private Path themesDir;

    @BeforeEach
    void setUp() throws IOException {
        SaoTheme.resetForTest();
        SaoThemeLibrary.resetForTest();
        themesDir = configDir.resolve(SaoThemeLibrary.DIR_NAME);
        Files.createDirectories(themesDir);
    }

    @AfterEach
    void tearDown() {
        SaoTheme.resetForTest();
        SaoThemeLibrary.resetForTest();
    }

    private void write(String name, String json) throws IOException {
        Files.writeString(themesDir.resolve(name), json, StandardCharsets.UTF_8);
    }

    private static SaoTheme find(String id) {
        for (SaoTheme t : SaoTheme.presets()) {
            if (t.id().equals(id)) {
                return t;
            }
        }
        return null;
    }

    @Test
    void missingDirectoryIsNotAnError() {
        Path nowhere = configDir.resolve("does-not-exist");
        assertEquals(0, SaoThemeLibrary.load(nowhere), "目录不存在应静默返回 0");
        assertEquals(3, SaoTheme.presets().size(), "内置预设不受影响");
    }

    @Test
    void loadsMinimalThemeAndInheritsSaoPalette() throws IOException {
        write("mine.json", "{\"id\":\"mine\",\"defaultHue\":120}");
        assertEquals(1, SaoThemeLibrary.load(configDir));

        SaoTheme t = find("mine");
        assertNotNull(t, "新主题应追加进预设表");
        assertEquals(120f, t.defaultHue(), 0.001f);
        assertEquals(ThemeColors.sao().divider(), t.colors().divider(),
                "未指定的颜色继承 SAO 调色板");
        assertEquals(4, SaoTheme.presets().size(), "内置 3 个 + 新增 1 个");
    }

    @Test
    void overridesBuiltinThemeRatherThanAppending() throws IOException {
        write("sao.json", "{\"id\":\"sao\",\"name\":\"改过的 SAO\",\"defaultHue\":10,"
                + "\"colors\":{\"divider\":\"#112233\"}}");
        assertEquals(1, SaoThemeLibrary.load(configDir));

        assertEquals(3, SaoTheme.presets().size(), "同 id 应替换而不是新增");
        SaoTheme sao = find(SaoTheme.SAO);
        assertNotNull(sao);
        assertEquals(10f, sao.defaultHue(), 0.001f);
        assertEquals(0xFF112233, sao.colors().divider(), "覆盖的键生效");
        assertEquals("改过的 SAO", SaoThemeLibrary.label(SaoTheme.SAO), "name 用作展示名");
    }

    @Test
    void colorLiteralsAcceptRgbAndArgbAndIntegers() throws IOException {
        write("c.json", "{\"id\":\"c\",\"colors\":{"
                + "\"divider\":\"#112233\",\"textMuted\":\"#80112233\",\"shadow\":-1}}");
        SaoThemeLibrary.load(configDir);

        ThemeColors c = find("c").colors();
        assertEquals(0xFF112233, c.divider(), "6 位写法补不透明 alpha");
        assertEquals(0x80112233, c.textMuted(), "8 位写法保留 alpha");
        assertEquals(-1, c.shadow(), "整数按原样");
    }

    @Test
    void hueIsWrappedIntoRange() throws IOException {
        write("w.json", "{\"id\":\"w\",\"defaultHue\":400}");
        write("n.json", "{\"id\":\"n\",\"defaultHue\":-20}");
        SaoThemeLibrary.load(configDir);

        assertEquals(40f, find("w").defaultHue(), 0.001f);
        assertEquals(340f, find("n").defaultHue(), 0.001f);
    }

    @Test
    void oneBadFileDoesNotAffectOthersOrBuiltins() throws IOException {
        write("a_ok.json", "{\"id\":\"ok_one\",\"defaultHue\":50}");
        write("b_broken.json", "{\"id\":\"broken\",");
        write("c_notobject.json", "[\"nope\"]");
        write("d_badid.json", "{\"id\":\"Bad-Id!\"}");
        write("e_noid.json", "{\"defaultHue\":10}");
        write("f_badhue.json", "{\"id\":\"badhue\",\"defaultHue\":\"red\"}");
        write("g_badcolor.json", "{\"id\":\"badcolor\",\"colors\":{\"divider\":\"zzz\"}}");
        write("z_ok.json", "{\"id\":\"ok_two\",\"defaultHue\":60}");

        assertEquals(2, SaoThemeLibrary.load(configDir), "只有两个合法文件应被载入");
        assertNotNull(find("ok_one"));
        assertNotNull(find("ok_two"));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("broken")));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("badhue")));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("badcolor")));
        assertEquals(5, SaoTheme.presets().size(), "内置 3 个 + 合法 2 个");
    }

    @Test
    void nonJsonFilesAreIgnored() throws IOException {
        write("readme.txt", "not a theme");
        write("notes.json.bak", "{}");
        assertEquals(0, SaoThemeLibrary.load(configDir));
        assertEquals(3, SaoTheme.presets().size());
    }

    @Test
    void selectedIdFollowsPersistedHueAcrossRestart() throws IOException {
        write("t.json", "{\"id\":\"t\",\"defaultHue\":123}");
        SaoThemeLibrary.load(configDir);

        SaoTheme.select("t");
        assertEquals(123f, com.sao.saomenu.client.SAOConfig.accentHue(), 0.001f);
        assertEquals("t", SaoTheme.selectedId(), "选中后应立刻生效");

        // 模拟重启:内存里的 selectedId 复位,只剩配置里的色相
        SaoTheme.resetForTest();
        SaoThemeLibrary.load(configDir);
        assertEquals("t", SaoTheme.selectedId(),
                "重启后应能按持久化的色相反查出选中的是哪个主题");
    }

    @Test
    void accentFromFileIsIgnoredAndHueWins() throws IOException {
        write("x.json", "{\"id\":\"x\",\"defaultHue\":90,\"colors\":{\"accent\":\"#FF0000\"}}");
        SaoThemeLibrary.load(configDir);

        SaoTheme.select("x");
        int accent = SaoTheme.active().colors().accent();
        assertEquals(SaoTheme.accentFromHue(90f), accent,
                "主题色必须由色相派生,文件里的 accent 不参与");
    }

    @Test
    void labelFallsBackToIdWhenNoNameAndNoTranslation() throws IOException {
        write("bare.json", "{\"id\":\"bare\",\"defaultHue\":5}");
        SaoThemeLibrary.load(configDir);
        // 没有 name、也没有对应翻译键时,展示名回落到 id 本身(而不是裸露的翻译键)
        assertEquals("bare", SaoThemeLibrary.label("bare"));
    }

    @Test
    void presetsReturnedListIsImmutableCopy() {
        assertTrue(SaoTheme.presets().isEmpty() || true);
        try {
            SaoTheme.presets().add(new SaoTheme("sneaky", 1f, ThemeColors.sao()));
            assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("sneaky")),
                    "对 presets() 返回值的修改不得影响注册表");
        } catch (UnsupportedOperationException expected) {
            // 直接不可变也满足契约
        }
    }

    // ------------------------------------------------------------ 身份 vs 高亮

    @Test
    void customPaletteSurvivesHueSliderDrag() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#7FA8B0\"}}");
        SaoThemeLibrary.load(configDir);

        SaoTheme.select("p");
        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider(), "选中后应用自定义调色板");

        // 用户接着拖了色相滑条,并且拖到了恰好等于另一个预设(ALO 202°)的位置。
        // 这是最要命的取值:若身份按色相反查,就会当场被改判成 alo,自定义调色板丢失。
        com.sao.saomenu.client.SAOConfig.setAccentHue(202f);
        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider(),
                "拖色相滑条只应改主题色,不应丢掉所选主题的调色板");
        assertEquals("p", SaoTheme.selectedId(), "身份不因色相变化而改变");
        assertEquals(SaoTheme.ALO, SaoTheme.matchingPreset(202f),
                "高亮确实会跳到 alo —— 但那只是显示,不影响调色板身份");
    }

    @Test
    void highlightOnlyMatchesExactPresetHue() {
        SaoThemeLibrary.resetForTest();
        assertEquals(SaoTheme.SAO, SaoTheme.matchingPreset(41.44f), "色相等于预设默认值才高亮");
        assertEquals(SaoTheme.ALO, SaoTheme.matchingPreset(202f));
        assertNull(SaoTheme.matchingPreset(200f),
                "200° 不等于任何预设默认色相:不应高亮任何按钮(否则高亮会撒谎)");
        assertNull(SaoTheme.matchingPreset(0f));
    }

    @Test
    void highlightDoesNotFollowIdentityAfterHueChange() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150}");
        SaoThemeLibrary.load(configDir);
        SaoTheme.select("p");

        assertEquals("p", SaoTheme.matchingPreset(150f), "色相未变时高亮该预设");
        assertNull(SaoTheme.matchingPreset(200f),
                "身份仍是 p,但色相变了:不能继续把 p 显示成选中");
        assertEquals("p", SaoTheme.selectedId(), "身份与高亮是两个问题");
    }
}
