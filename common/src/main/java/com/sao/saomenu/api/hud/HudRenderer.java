package com.sao.saomenu.api.hud;

/**
 * Draws one HUD element in a single compose pass. Do not retain {@code context}
 * or {@link net.minecraft.client.gui.GuiGraphics} past return.
 */
@FunctionalInterface
public interface HudRenderer {
    void render(HudRenderContext context);
}
