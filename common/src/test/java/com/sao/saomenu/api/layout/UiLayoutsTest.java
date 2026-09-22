package com.sao.saomenu.api.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiLayoutsTest {
    @Test
    void centeredShrinksOnSmallScreen() {
        UiRect card = UiLayouts.centered(80, 50, 280, 180, 8);
        assertEquals(new UiRect(8, 8, 64, 34), card);
        assertTrue(card.x() >= 8);
        assertTrue(card.right() <= 72);
        assertTrue(card.bottom() <= 42);
    }

    @Test
    void centeredKeepsPreferredWhenItFits() {
        assertEquals(new UiRect(60, 30, 200, 100), UiLayouts.centered(320, 160, 200, 100, 8));
    }

    @Test
    void clampedShrinksAndStaysInsideMargin() {
        UiRect oversized = new UiRect(-20, -4, 400, 20);
        UiRect fit = UiLayouts.clamped(oversized, 100, 40, 4);
        assertEquals(4, fit.x());
        assertEquals(4, fit.y());
        assertEquals(92, fit.width());
        assertEquals(20, fit.height());
        assertTrue(fit.right() <= 96);
        assertTrue(fit.bottom() <= 36);
    }

    @Test
    void rowSplitsWidthAndKeepsRemainderOnLast() {
        UiRect[] cells = UiLayouts.row(new UiRect(10, 5, 10, 8), 3, 1);
        assertEquals(3, cells.length);
        assertEquals(new UiRect(10, 5, 2, 8), cells[0]);
        assertEquals(new UiRect(13, 5, 2, 8), cells[1]);
        assertEquals(new UiRect(16, 5, 4, 8), cells[2]);
        assertEquals(20, cells[2].right());
    }

    @Test
    void columnSplitsHeight() {
        UiRect[] rows = UiLayouts.column(new UiRect(0, 0, 6, 10), 2, 2);
        assertEquals(new UiRect(0, 0, 6, 4), rows[0]);
        assertEquals(new UiRect(0, 6, 6, 4), rows[1]);
    }

    @Test
    void gridPacksLeftToRightTopToBottom() {
        UiRect[] cells = UiLayouts.grid(new UiRect(0, 0, 10, 10), 2, 3, 2);
        assertEquals(3, cells.length);
        assertEquals(0, cells[0].x());
        assertEquals(0, cells[0].y());
        assertEquals(cells[0].right() + 2, cells[1].x());
        assertEquals(cells[0].y(), cells[1].y());
        assertEquals(cells[0].x(), cells[2].x());
        assertTrue(cells[2].y() >= cells[0].bottom());
        assertEquals(10, cells[2].right());
    }

    @Test
    void clampedScrollNeverLeavesContent() {
        assertEquals(0, UiLayouts.clampedScroll(-4, 50, 20));
        assertEquals(30, UiLayouts.clampedScroll(999, 50, 20));
        assertEquals(0, UiLayouts.clampedScroll(10, 10, 20));
        assertEquals(8, UiLayouts.clampedScroll(8, 50, 20));
    }

    @Test
    void emptyCountsYieldEmptyArrays() {
        assertEquals(0, UiLayouts.row(new UiRect(0, 0, 10, 10), 0, 2).length);
        assertEquals(0, UiLayouts.grid(new UiRect(0, 0, 10, 10), 0, 4, 1).length);
        assertEquals(0, UiLayouts.grid(new UiRect(0, 0, 10, 10), 2, 0, 1).length);
    }
}
