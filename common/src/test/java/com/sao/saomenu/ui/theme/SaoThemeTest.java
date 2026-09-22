package com.sao.saomenu.ui.theme;

import com.sao.saomenu.config.SAOConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertEquals(SaoTheme.SAO, SaoTheme.byId("no-such-theme").id(),
                "未知 id 回落到 SAO,手改坏的配置不至于崩");

        SaoTheme.select("no-such-theme");
        assertEquals(SaoTheme.SAO, SaoTheme.selectedId());
    }

}
