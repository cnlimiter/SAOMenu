package com.sao.saomenu.api.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * Borrowed read-only frame view. Coordinates are screen GUI pixels, never menu-local pixels.
 * Do not retain this object after a renderer or visibility callback: the host reuses it.
 */
public interface HudRenderContext {
    /** Graphics for rendering; null while the editor checks visibility without drawing. */
    GuiGraphics graphics();

    Minecraft minecraft();

    int width();

    int height();

    /** Menu open/close alpha; one in the world pass. */
    float fade();

    /** Opaque player/team plate alpha, independent of the menu fade. */
    float plateAlpha();

    HudPass pass();

    int mouseX();

    int mouseY();

    default Player player() {
        Minecraft client = minecraft();
        return client == null ? null : client.player;
    }
}
