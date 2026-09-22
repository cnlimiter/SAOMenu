package com.sao.saomenu.ui.render;

import org.junit.jupiter.api.Test;
import com.sao.saomenu.client.menu.MenuLayout;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字号必须跟行高走:小窗口行更矮,scale 必须更小,否则 9px 字会顶破组件。
 */
class SaoDrawTest {

    @Test
    void smallerRowYieldsSmallerFontScale() {
        float at18 = SaoDraw.fitScale(9, 18, 0.62f);
        float at13 = SaoDraw.fitScale(9, 13, 0.62f);
        float at54 = SaoDraw.fitScale(9, 54, 0.62f);
        assertTrue(at13 < at18, "最小窗口行高 13 应比设计行 18 字更小");
        assertTrue(at18 < at54, "大 GUI 下行更高,字应跟着变大");
        assertEquals(18 * 0.62f / 9f, at18, 0.001f);
        assertTrue(at13 < 1f, "13px 行里字号必须小于原 9px");
    }

    @Test
    void clampsSaneRange() {
        assertEquals(0.30f, SaoDraw.fitScale(9, 1, 0.62f), 0.001f);
        assertEquals(4.0f, SaoDraw.fitScale(9, 1000, 0.62f), 0.001f);
        assertEquals(0.30f, SaoDraw.fitScale(9, 0, 0.62f), 0.001f);
    }

    @Test
    void scaleTableKeepsFontInsideRowsAndReadableOnTinyCards() {
        final int line = 9;
        final float frac = 0.62f;
        float physSmall = itemVisAt(256, line, frac) * 2f;
        float physLarge = itemVisAt(512, line, frac) * 1f;
        assertEquals(physSmall, physLarge, 0.5f,
                "同一窗口 GUI scale 1 与 2 的菜单字物理像素应相等");

        int statsSmall = MenuLayout.cardH(256) - MenuLayout.cardH(256) / 2 - 2;
        int statsLarge = MenuLayout.cardH(512) - MenuLayout.cardH(512) / 2 - 2;
        int rows = 6;
        int minLine = 8;
        assertEquals(6, SaoDraw.rowsKept(statsSmall, rows, minLine),
                "856×512 / scale 2 的 6 行约 9.7px,不该砍护甲");
        float cardPhysSmall = line * SaoDraw.fitScale(line, statsSmall / (float) rows, frac) * 2f;
        float cardPhysLarge = line * SaoDraw.fitScale(line, statsLarge / (float) rows, frac) * 1f;
        assertEquals(cardPhysSmall, cardPhysLarge, 0.6f,
                "行数不变时玩家卡物理字号应跟窗口走");

        int tinyStats = MenuLayout.cardH(120) - MenuLayout.cardH(120) / 2 - 2;
        int tinyRows = SaoDraw.rowsKept(tinyStats, rows, minLine);
        assertTrue(tinyRows < rows && tinyRows >= 2, "极小卡才减行");
        assertTrue(tinyStats / (float) tinyRows >= minLine, "减行后每行仍可读");
    }

    private static float itemVisAt(int guiH, int line, float frac) {
        return line * SaoDraw.fitScale(line, MenuLayout.itemH(guiH), frac);
    }
}
