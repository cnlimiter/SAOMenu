package com.sao.saomenu.api.layout;

/**
 * Axis-aligned GUI-pixel rectangle. Mouse hit tests keep the fractional coordinate
 * and exclude the right and bottom edges. Dimensions must be nonnegative; a zero
 * size is empty and contains nothing.
 */
public record UiRect(int x, int y, int width, int height) {
    public UiRect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("UiRect width and height must be nonnegative");
        }
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public int centerX() {
        return x + width / 2;
    }

    public int centerY() {
        return y + height / 2;
    }

    /** Inclusive on the origin, exclusive on {@link #right()} and {@link #bottom()}. */
    public boolean contains(double px, double py) {
        return width > 0 && height > 0
                && px >= x && px < right()
                && py >= y && py < bottom();
    }

    public UiRect inset(int amount) {
        return inset(amount, amount, amount, amount);
    }

    /** Insets stay inside this rectangle; crossing edges collapse at their midpoint. Negative values mean zero. */
    public UiRect inset(int left, int top, int right, int bottom) {
        int l = Math.min(width, Math.max(0, left));
        int t = Math.min(height, Math.max(0, top));
        int r = Math.max(0, width - Math.max(0, right));
        int b = Math.max(0, height - Math.max(0, bottom));
        int w = Math.max(0, r - l);
        int h = Math.max(0, b - t);
        if (l > r) {
            l = r + (l - r) / 2;
        }
        if (t > b) {
            t = b + (t - b) / 2;
        }
        return new UiRect(x + l, y + t, w, h);
    }

    public UiRect translate(int dx, int dy) {
        return new UiRect(x + dx, y + dy, width, height);
    }

    public boolean intersects(UiRect other) {
        return width > 0 && height > 0 && other.width > 0 && other.height > 0
                && x < other.right() && right() > other.x
                && y < other.bottom() && bottom() > other.y;
    }
}
