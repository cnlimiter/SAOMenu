package com.sao.saomenu.api.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiRectTest {
    @Test
    void containsIsHalfOpenOnRightAndBottom() {
        UiRect rect = new UiRect(10, 20, 30, 40);
        assertTrue(rect.contains(10, 20));
        assertTrue(rect.contains(10.0, 20.0));
        assertTrue(rect.contains(39.9, 59.9));
        assertFalse(rect.contains(40, 20));
        assertFalse(rect.contains(10, 60));
        assertFalse(rect.contains(9.9, 20));
        assertFalse(rect.contains(10, 19.9));
    }

    @Test
    void emptyRectContainsNothing() {
        UiRect empty = new UiRect(5, 5, 0, 10);
        assertFalse(empty.contains(5, 5));
        assertFalse(empty.contains(5.5, 8));
        assertEquals(5, empty.right());
        assertEquals(15, empty.bottom());
    }

    @Test
    void rejectsNegativeDimensions() {
        assertThrows(IllegalArgumentException.class, () -> new UiRect(0, 0, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new UiRect(0, 0, 1, -1));
    }

    @Test
    void insetClampsToNonnegativeSize() {
        UiRect rect = new UiRect(8, 8, 20, 12);
        assertEquals(new UiRect(12, 12, 12, 4), rect.inset(4));
        assertEquals(new UiRect(18, 14, 0, 0), rect.inset(10));
        assertEquals(new UiRect(10, 9, 16, 8), rect.inset(2, 1, 2, 3));
    }

    @Test
    void translateAndCenter() {
        UiRect rect = new UiRect(10, 20, 8, 6);
        assertEquals(new UiRect(12, 17, 8, 6), rect.translate(2, -3));
        assertEquals(14, rect.centerX());
        assertEquals(23, rect.centerY());
    }

    @Test
    void intersectsRequiresOverlapNotTouchingEdge() {
        UiRect a = new UiRect(0, 0, 10, 10);
        assertTrue(a.intersects(new UiRect(9, 9, 2, 2)));
        assertFalse(a.intersects(new UiRect(10, 0, 4, 4)));
        assertFalse(a.intersects(new UiRect(0, 10, 4, 4)));
        assertFalse(a.intersects(new UiRect(2, 2, 0, 4)));
    }
}
