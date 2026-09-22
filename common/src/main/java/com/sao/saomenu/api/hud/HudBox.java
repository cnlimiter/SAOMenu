package com.sao.saomenu.api.hud;

/**
 * Axis-aligned HUD box in screen GUI pixels.
 */
public record HudBox(int x, int y, int width, int height) {
    public HudBox {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("HUD box dimensions must be nonnegative");
        }
    }

    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
    }
}
