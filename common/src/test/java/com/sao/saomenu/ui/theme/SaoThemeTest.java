package com.sao.saomenu.ui.theme;

import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 主题切换、色相派生与无效预设的用户可见行为。 */
class SaoThemeTest {

    @BeforeEach
    void reset() {
        SAOConfig.reset();
        SaoTheme.resetForTest();
    }

    /**
     * 主题色的三个基准点。明度固定 0.937,所以最亮的通道是 239(0xEF)而不是 255;
     * 另外两个通道是 3(0x03)。这三个值任一改动都会立刻改变全模组的主题观感。
     */
    @Test
    void accentDerivesFromHue() {
        SAOConfig.setAccentHue(SAOConfig.DEF_ACCENT_HUE);
        assertEquals(0xFFEFA603, SaoTheme.accent(), "默认色相即 SAO 橙 #EFA603");

        SAOConfig.setAccentHue(0f);
        assertEquals(0xFFEF0303, SaoTheme.accent(), "0° 纯红");

        SAOConfig.setAccentHue(120f);
        assertEquals(0xFF03EF03, SaoTheme.accent(), "120° 纯绿");
    }

    @Test
    void selectingPresetWritesItsDefaultHue() {
        SaoTheme.select(SaoTheme.ALO);
        assertEquals(202f, SAOConfig.accentHue(), 0.001f);
        assertEquals(SaoTheme.ALO, SaoTheme.selectedId());

        SaoTheme.select(SaoTheme.GGO);
        assertEquals(355f, SAOConfig.accentHue(), 0.001f);

        SaoTheme.select(SaoTheme.SAO);
        assertEquals(41.44f, SAOConfig.accentHue(), 0.001f);
    }

    @Test
    void unknownPresetFallsBackToDefault() {
        List<ThemeDefinition> withEarlierAddon = new ArrayList<>(SaoTheme.definitions());
        withEarlierAddon.add(0, new ThemeDefinition(new ResourceLocation("addon", "first"), -100,
                Component.literal("First"), 120f, ThemeTokens.sao()));
        SaoTheme.installRegistered(withEarlierAddon);
        assertEquals(SaoTheme.SAO, SaoTheme.byId("no-such-theme").id(),
                "未知 id 回落到 SAO,手改坏的配置不至于崩");

        SaoTheme.select("no-such-theme");
        assertEquals(SaoTheme.SAO, SaoTheme.selectedId());
    }


    @Test
    void legacyStoredShortIdSelectsBuiltin() {
        SAOConfig.setThemeId("ggo");
        SaoTheme.resetForTest();
        assertEquals(SaoTheme.GGO, SaoTheme.selectedId());
    }

    @Test
    void tokensAreCachedUntilHueOrIdentityChanges() {
        ThemeTokens a = SaoTheme.tokens();
        ThemeTokens b = SaoTheme.tokens();
        assertSame(a, b, "同一选择与色相下不得每帧重建");
        assertEquals(SaoTheme.accentFromHue(SAOConfig.accentHue()), a.colors().accent());

        SAOConfig.setAccentHue(120f);
        ThemeTokens c = SaoTheme.tokens();
        assertNotSame(a, c);
        assertEquals(SaoTheme.accentFromHue(120f), c.colors().accent());
        assertEquals(a.bodyFont(), c.bodyFont());
        assertSame(c, SaoTheme.tokens());
    }

    @Test
    void addonDefinitionPreservesAllTokenFields() {
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

        SaoTheme.select("addon:night");
        ThemeTokens active = SaoTheme.tokens();
        assertEquals(new ResourceLocation("minecraft", "alt"), active.bodyFont());
        assertEquals(new ResourceLocation("minecraft", "uniform"), active.displayFont());
        assertEquals(400, active.enterMillis());
        assertEquals(80, active.exitMillis());
        assertEquals(SaoTheme.accentFromHue(210f), active.colors().accent());
        assertEquals("addon:night", SaoTheme.selectedId());
        assertSame(SaoTheme.definitions(), SaoTheme.definitions());
    }
}
