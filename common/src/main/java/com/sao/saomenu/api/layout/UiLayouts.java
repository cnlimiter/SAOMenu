package com.sao.saomenu.api.layout;

/**
 * Small GUI-space helpers. This is not a layout engine: callers recompute rectangles
 * on resize and assign them to widgets.
 */
public final class UiLayouts {
    private UiLayouts() {
    }

    public static UiRect centered(int screenWidth, int screenHeight, int preferredWidth, int preferredHeight, int margin) {
        screenWidth = Math.max(0, screenWidth);
        screenHeight = Math.max(0, screenHeight);
        long doubleMargin = 2L * Math.max(0, margin);
        int width = Math.min(Math.max(0, preferredWidth), (int) Math.max(0L, screenWidth - doubleMargin));
        int height = Math.min(Math.max(0, preferredHeight), (int) Math.max(0L, screenHeight - doubleMargin));
        return new UiRect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    /**
     * Keeps {@code rect} inside the screen inset by {@code margin}. Oversized rectangles
     * shrink; position is clamped after shrinking.
     */
    public static UiRect clamped(UiRect rect, int screenWidth, int screenHeight, int margin) {
        screenWidth = Math.max(0, screenWidth);
        screenHeight = Math.max(0, screenHeight);
        int pad = Math.max(0, margin);
        int maxW = (int) Math.max(0L, screenWidth - 2L * pad);
        int maxH = (int) Math.max(0L, screenHeight - 2L * pad);
        int width = Math.min(rect.width(), maxW);
        int height = Math.min(rect.height(), maxH);
        int x = maxW == 0 ? screenWidth / 2 : Math.min(Math.max(rect.x(), pad), pad + maxW - width);
        int y = maxH == 0 ? screenHeight / 2 : Math.min(Math.max(rect.y(), pad), pad + maxH - height);
        return new UiRect(x, y, width, height);
    }

    /** Split {@code bounds} into {@code count} equal columns, left to right. */
    public static UiRect[] row(UiRect bounds, int count, int gap) {
        if (count <= 0) {
            return new UiRect[0];
        }
        int space = count == 1 ? 0 : Math.min(Math.max(0, gap), bounds.width() / (count - 1));
        int inner = bounds.width() - space * (count - 1);
        int cell = inner / count;
        int rem = inner % count;
        UiRect[] out = new UiRect[count];
        int x = bounds.x();
        for (int i = 0; i < count; i++) {
            int w = cell + (i == count - 1 ? rem : 0);
            out[i] = new UiRect(x, bounds.y(), w, bounds.height());
            x += w + space;
        }
        return out;
    }

    /** Split {@code bounds} into {@code count} equal rows, top to bottom. */
    public static UiRect[] column(UiRect bounds, int count, int gap) {
        if (count <= 0) {
            return new UiRect[0];
        }
        int space = count == 1 ? 0 : Math.min(Math.max(0, gap), bounds.height() / (count - 1));
        int inner = bounds.height() - space * (count - 1);
        int cell = inner / count;
        int rem = inner % count;
        UiRect[] out = new UiRect[count];
        int y = bounds.y();
        for (int i = 0; i < count; i++) {
            int h = cell + (i == count - 1 ? rem : 0);
            out[i] = new UiRect(bounds.x(), y, bounds.width(), h);
            y += h + space;
        }
        return out;
    }

    /**
     * Pack {@code count} cells into {@code columns} left-to-right, top-to-bottom.
     * The last row stretches across the full width when it has fewer cells.
     */
    public static UiRect[] grid(UiRect bounds, int columns, int count, int gap) {
        if (columns <= 0 || count <= 0) {
            return new UiRect[0];
        }
        int rows = (count - 1) / columns + 1;
        UiRect[] rowBounds = column(bounds, rows, gap);
        UiRect[] out = new UiRect[count];
        int i = 0;
        for (int r = 0; r < rows; r++) {
            int n = Math.min(columns, count - i);
            UiRect[] cells = row(rowBounds[r], n, gap);
            for (int c = 0; c < n; c++) {
                out[i++] = cells[c];
            }
        }
        return out;
    }

    public static int clampedScroll(int amount, int contentHeight, int viewportHeight) {
        int max = Math.max(0, contentHeight - Math.max(0, viewportHeight));
        if (amount < 0) {
            return 0;
        }
        return Math.min(amount, max);
    }
}
