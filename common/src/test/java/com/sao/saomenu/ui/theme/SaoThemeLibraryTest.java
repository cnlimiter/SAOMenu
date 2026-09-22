package com.sao.saomenu.ui.theme;

import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        SAOConfig.setThemeId(SaoTheme.SAO);
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

    private static String id(String shortId) {
        return shortId.indexOf(':') < 0 ? "saomenu:" + shortId : shortId;
    }

    private static SaoTheme find(String rawId) {
        String canonical = id(rawId);
        for (SaoTheme t : SaoTheme.presets()) {
            if (t.id().equals(canonical)) {
                return t;
            }
        }
        return null;
    }

    private static ThemeDefinition def(String rawId) {
        return SaoTheme.definitionOrNull(id(rawId));
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
        assertEquals(id("mine"), t.id());
        assertEquals(120f, t.defaultHue(), 0.001f);
        assertEquals(ThemeColors.sao().divider(), t.colors().divider(),
                "未指定的颜色继承 SAO 调色板");
        assertEquals(4, SaoTheme.presets().size(), "内置 3 个 + 新增 1 个");
        ThemeTokens tokens = def("mine").tokens();
        assertEquals(ThemeTokens.sao(), tokens, "未指定的字体与动效继承基础主题,而不是另设一套缺省值");
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
        write("h_badfont.json", "{\"id\":\"badfont\",\"bodyFont\":\"Not A Font\"}");
        write("i_baddur.json", "{\"id\":\"baddur\",\"enterMillis\":-4}");
        write("z_ok.json", "{\"id\":\"ok_two\",\"defaultHue\":60}");

        assertEquals(2, SaoThemeLibrary.load(configDir), "只有两个合法文件应被载入");
        assertNotNull(find("ok_one"));
        assertNotNull(find("ok_two"));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("broken")));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals(id("badhue"))));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals(id("badcolor"))));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals(id("badfont"))));
        assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals(id("baddur"))));
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

        SaoTheme.select(id("t"));
        assertEquals(123f, SAOConfig.accentHue(), 0.001f);
        assertEquals(id("t"), SaoTheme.selectedId(), "选中后应立刻生效");

        // 模拟重启:内存里的 selectedId 复位,只剩配置里的色相
        SaoTheme.resetForTest();
        SaoThemeLibrary.load(configDir);
        assertEquals(id("t"), SaoTheme.selectedId(),
                "重启后应能按持久化的色相反查出选中的是哪个主题");
    }

    @Test
    void accentFromFileIsIgnoredAndHueWins() throws IOException {
        write("x.json", "{\"id\":\"x\",\"defaultHue\":90,\"colors\":{\"accent\":\"#FF0000\"}}");
        SaoThemeLibrary.load(configDir);

        SaoTheme.select(id("x"));
        int accent = SaoTheme.active().colors().accent();
        assertEquals(SaoTheme.accentFromHue(90f), accent,
                "主题色必须由色相派生,文件里的 accent 不参与");
        assertEquals(SaoTheme.accentFromHue(90f), SaoTheme.tokens().colors().accent());
    }

    @Test
    void labelFallsBackToIdWhenNoNameAndNoTranslation() throws IOException {
        write("bare.json", "{\"id\":\"bare\",\"defaultHue\":5}");
        SaoThemeLibrary.load(configDir);
        // 没有 name、也没有对应翻译键时,展示名回落到 path 本身(而不是裸露的翻译键)
        assertEquals("bare", SaoThemeLibrary.label(id("bare")));
    }

    @Test
    void customPaletteSurvivesHueDragThenRestart() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#7FA8B0\"}}");
        SaoThemeLibrary.load(configDir);
        SaoTheme.select(id("p"));

        // 用户接着把色相拖到恰好等于另一个预设(ALO 202°)的位置
        SAOConfig.setAccentHue(202f);
        // 模拟重启:主题层内存状态清零,配置保留
        SaoTheme.resetForTest();
        SaoThemeLibrary.load(configDir);

        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider(),
                "重启后自定义调色板不得丢失 —— 这正是只按色相反查做不到的那一步");
        assertEquals(id("p"), SaoTheme.selectedId());
    }

    @Test
    void unknownThemeIdFallsBackToHueLookup() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#7FA8B0\"}}");
        SaoThemeLibrary.load(configDir);

        // 模拟旧配置(没有 themeId)或主题文件已被删除:配置里指向一个不存在的主题
        SAOConfig.setThemeId("gone");
        SAOConfig.setAccentHue(150f);
        SaoTheme.resetForTest();
        SaoThemeLibrary.load(configDir);

        assertEquals(id("p"), SaoTheme.selectedId(), "themeId 无效时应按色相反查兜底");
        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider());
    }

    @Test
    void selectPersistsThemeId() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150}");
        SaoThemeLibrary.load(configDir);
        SaoTheme.select(id("p"));
        assertEquals(id("p"), SAOConfig.themeId(),
                "选择主题必须把 namespaced id 写进配置,否则拖过色相后无法恢复");
    }

    @Test
    void resettingConfigurationAlsoResetsRenderedPaletteIdentity() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#112233\"}}");
        SaoThemeLibrary.load(configDir);
        SaoTheme.select(id("p"));
        assertEquals(0xFF112233, SaoTheme.palette().divider());

        SAOConfig.reset();
        assertEquals(SaoTheme.SAO, SaoTheme.active().id());
        assertEquals(SaoTheme.byId(SaoTheme.SAO).colors().divider(), SaoTheme.palette().divider());
        assertEquals(SaoTheme.accentFromHue(SAOConfig.DEF_ACCENT_HUE), SaoTheme.tokens().colors().accent());
    }

    @Test
    void presetsReturnedListIsImmutableCopy() {
        try {
            SaoTheme.presets().add(new SaoTheme("sneaky", 1f, ThemeColors.sao()));
            assertFalse(SaoTheme.presets().stream().anyMatch(t -> t.id().equals("sneaky")),
                    "对 presets() 返回值的修改不得影响注册表");
        } catch (UnsupportedOperationException expected) {
            // 直接不可变也满足契约
        }
        try {
            SaoTheme.definitions().add(new ThemeDefinition(
                    new ResourceLocation("addon", "sneaky"), 1, Component.literal("x"),
                    1f, ThemeTokens.sao()));
            assertFalse(SaoTheme.definitions().stream().anyMatch(t -> t.id().getPath().equals("sneaky")));
        } catch (UnsupportedOperationException expected) {
            // 直接不可变也满足契约
        }
    }

    // ------------------------------------------------------------ 身份 vs 高亮

    @Test
    void customPaletteSurvivesHueSliderDrag() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#7FA8B0\"}}");
        SaoThemeLibrary.load(configDir);

        SaoTheme.select(id("p"));
        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider(), "选中后应用自定义调色板");

        // 用户接着拖了色相滑条,并且拖到了恰好等于另一个预设(ALO 202°)的位置。
        // 这是最要命的取值:若身份按色相反查,就会当场被改判成 alo,自定义调色板丢失。
        SAOConfig.setAccentHue(202f);
        assertEquals(0xFF7FA8B0, SaoTheme.active().colors().divider(),
                "拖色相滑条只应改主题色,不应丢掉所选主题的调色板");
        assertEquals(id("p"), SaoTheme.selectedId(), "身份不因色相变化而改变");
        assertEquals(SaoTheme.ALO, SaoTheme.matchingPreset(202f),
                "高亮确实会跳到 alo —— 但那只是显示,不影响调色板身份");
    }

    @Test
    void replacingSelectedPaletteAndSwitchingAtSameHueRefreshesRendering() throws IOException {
        write("p.json", "{\"id\":\"p\",\"defaultHue\":150,\"colors\":{\"divider\":\"#112233\"}}");
        SaoThemeLibrary.load(configDir);
        SaoTheme.select(id("p"));
        ThemeTokens before = SaoTheme.tokens();
        assertEquals(0xFF112233, SaoTheme.palette().divider());
        SAOConfig.setAccentHue(202f);
        assertEquals(SaoTheme.accentFromHue(202f), SaoTheme.accent());

        write("p.json", "{\"id\":\"p\",\"defaultHue\":80,\"colors\":{\"divider\":\"#445566\"}}");
        SaoThemeLibrary.load(configDir);
        ThemeTokens after = SaoTheme.tokens();
        assertNotSame(before, after, "同 id 再载入必须失效缓存");
        assertEquals(0xFF445566, SaoTheme.palette().divider(), "Replacing the selected id must refresh its colors");
        assertEquals(SaoTheme.accentFromHue(202f), SaoTheme.accent(), "Reload must preserve the user's hue");

        SaoTheme.select(SaoTheme.ALO);
        assertEquals(SaoTheme.byId(SaoTheme.ALO).colors().divider(), SaoTheme.palette().divider(),
                "Equal hue does not imply equal palette identity");
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
        SaoTheme.select(id("p"));

        assertEquals(id("p"), SaoTheme.matchingPreset(150f), "色相未变时高亮该预设");
        assertNull(SaoTheme.matchingPreset(200f),
                "身份仍是 p,但色相变了:不能继续把 p 显示成选中");
        assertEquals(id("p"), SaoTheme.selectedId(), "身份与高亮是两个问题");
    }

    @Test
    void userFileMaySetFontsAndDurations() throws IOException {
        write("motion.json", "{\"id\":\"motion\",\"defaultHue\":12,"
                + "\"bodyFont\":\"minecraft:alt\",\"displayFont\":\"minecraft:uniform\","
                + "\"enterMillis\":40,\"exitMillis\":90}");
        assertEquals(1, SaoThemeLibrary.load(configDir));
        SaoTheme.select(id("motion"));
        ThemeTokens tokens = SaoTheme.tokens();
        assertEquals(new ResourceLocation("minecraft", "alt"), tokens.bodyFont());
        assertEquals(new ResourceLocation("minecraft", "uniform"), tokens.displayFont());
        assertEquals(40, tokens.enterMillis());
        assertEquals(90, tokens.exitMillis());
    }

    @Test
    void jsonOverlayOfAddonKeepsUnspecifiedTokenFields() throws IOException {
        ThemeTokens custom = new ThemeTokens(
                ThemeColors.sao(),
                new ResourceLocation("minecraft", "alt"),
                new ResourceLocation("minecraft", "uniform"),
                400,
                80);
        ThemeDefinition addon = new ThemeDefinition(
                new ResourceLocation("addon", "night"),
                500,
                Component.literal("Night"),
                210f,
                custom);
        List<ThemeDefinition> merged = new ArrayList<>(SaoTheme.definitions());
        merged.add(addon);
        SaoTheme.installRegistered(merged);

        write("night.json", "{\"id\":\"addon:night\",\"colors\":{\"divider\":\"#112233\"}}");
        assertEquals(1, SaoThemeLibrary.load(configDir));

        SaoTheme.select("addon:night");
        ThemeTokens tokens = SaoTheme.tokens();
        assertEquals(0xFF112233, tokens.colors().divider());
        assertEquals(new ResourceLocation("minecraft", "alt"), tokens.bodyFont(),
                "未写的字体必须保留代码主题的值");
        assertEquals(new ResourceLocation("minecraft", "uniform"), tokens.displayFont());
        assertEquals(400, tokens.enterMillis());
        assertEquals(80, tokens.exitMillis());
        assertEquals(SaoTheme.accentFromHue(210f), tokens.colors().accent());
        assertEquals("Night", SaoThemeLibrary.label("addon:night"));
    }

    @Test
    void definitionsSnapshotIsCachedUntilOverlay() throws IOException {
        List<ThemeDefinition> a = SaoTheme.definitions();
        assertSame(a, SaoTheme.definitions());
        write("z.json", "{\"id\":\"z\",\"defaultHue\":1}");
        SaoThemeLibrary.load(configDir);
        List<ThemeDefinition> b = SaoTheme.definitions();
        assertNotSame(a, b);
        assertSame(b, SaoTheme.definitions());
        assertEquals(4, b.size());
    }
}
