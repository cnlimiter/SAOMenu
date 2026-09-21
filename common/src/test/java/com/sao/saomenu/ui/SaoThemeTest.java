package com.sao.saomenu.ui;

import com.sao.saomenu.client.SAOConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题 token 的取值锁定。
 *
 * <p>菜单是叠在世界上画的,截图里粒子与实体位置每次运行都不同,所以那一侧没有
 * 可用的逐像素回归门(见 {@code tools/diff_screens.py} 里实测的噪声底噪)。
 * 主题抽取的"零观感变化"因此改由这些断言守住:每个 token 与主题色都钉死在
 * 重构前的字面量上,抄错一位就会在这里失败。</p>
 */
class SaoThemeTest {

    @BeforeEach
    void reset() {
        SAOConfig.reset();
        SaoTheme.resetForTest();
    }

    @Test
    void defaultPaletteMatchesHistoricalValues() {
        ThemeColors c = ThemeColors.sao();
        assertEquals(0xFF3C3C3D, c.textOnSurface(), "原 TEXT_DARK");
        assertEquals(0xFFF9F9F9, c.textOnAccent(), "原 TEXT_ON_ORANGE");
        assertEquals(0xFF9A9DA0, c.textMuted(), "原空装备占位灰");
        assertEquals(0xFFFFFFFF, c.highlight(), "原纯白高亮");
        assertEquals(0xFFA09FA0, c.divider(), "原 CARD_LINE");
        assertEquals(0x3A303030, c.shadow(), "原 SHADOW");
        assertEquals(0x52F9F9F9, c.surfaceSlot(), "原手持物品槽底");
        assertEquals(0xE6FFFFFF, c.dialogSurface(), "原弹窗内衬");
        assertEquals(0x6E303030, c.dialogShadow(), "原弹窗投影");
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
    void activeThemeExposesDerivedAccent() {
        SAOConfig.setAccentHue(202f);
        assertEquals(SaoTheme.accent(), SaoTheme.palette().accent(),
                "调色板的 accent 必须等于实时派生的主题色,否则界面会用上过期颜色");
        assertEquals(0xFF3C3C3D, SaoTheme.palette().textOnSurface(), "换色相不动其余 token");
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
        assertEquals(SaoTheme.SAO, SaoTheme.byId("no-such-theme").id(),
                "未知 id 回落到 SAO,手改坏的配置不至于崩");

        SaoTheme.select("no-such-theme");
        assertEquals(SaoTheme.SAO, SaoTheme.selectedId());
    }

    @Test
    void allPresetsCarryAFullPalette() {
        assertEquals(3, SaoTheme.presets().size());
        for (SaoTheme t : SaoTheme.presets()) {
            assertTrue(t.colors().textOnSurface() != 0, t.id() + " 缺调色板");
            assertTrue(t.defaultHue() >= 0f && t.defaultHue() <= 360f, t.id() + " 色相越界");
        }
    }
}
