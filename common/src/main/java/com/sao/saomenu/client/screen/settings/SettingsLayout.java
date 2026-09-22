package com.sao.saomenu.client.screen.settings;

import net.minecraft.util.Mth;

/** Bounded category/option viewport math. Independent of P5 skin constants. */
final class SettingsLayout {
    static final int VISIBLE_CATS = 4;
    static final int ROW_MIN = 16;
    static final int ROW_MAX = 34;

    private SettingsLayout() {
    }

    static int visibleRows(int avail, int count) {
        int maxVis = Math.max(1, avail / ROW_MIN);
        return Math.max(0, Math.min(count, maxVis));
    }

    static int rowH(int avail, int count) {
        int vis = Math.max(1, visibleRows(avail, count));
        return Mth.clamp(avail / vis, ROW_MIN, ROW_MAX);
    }

    static int clampScroll(int scroll, int count, int visible) {
        return Mth.clamp(scroll, 0, Math.max(0, count - Math.max(0, visible)));
    }

    static int optionViewport(int rowsTop, int footerTop) {
        return Math.max(ROW_MIN, footerTop - rowsTop);
    }

    static int visibleSlots(int count, int scroll, int visible) {
        if (count <= 0 || visible <= 0) {
            return 0;
        }
        int start = Math.max(0, Math.min(scroll, Math.max(0, count - 1)));
        return Math.max(0, Math.min(visible, count - start));
    }

    static boolean indexInViewport(int index, int scroll, int visible, int count) {
        return index >= 0 && index < count && index >= scroll && index < scroll + Math.max(0, visible);
    }
}
