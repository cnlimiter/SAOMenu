package com.sao.saomenu.api.menu;

import com.sao.saomenu.api.layout.UiRect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Optional left-hand card for a {@link SaoPanel}.
 *
 * <h2>Coordinates</h2>
 * {@code bounds} are <b>menu-local</b> GUI pixels and already include the slide-in offset.
 * Convert to screen space with {@link MenuHost#localBoxToScreen} before scissor, tooltips
 * or widgets that live outside the menu pose. {@code mouseX}/{@code mouseY} are <b>screen</b>
 * GUI pixels (the same numbers {@code Screen#render} receives).
 *
 * <h2>Lifecycle</h2>
 * Called every frame the panel is open, after layout and inside the menu group transform.
 * {@link MenuContext#entry()} is {@code null}. Do not retain {@code graphics}, {@code ctx}
 * or {@code bounds} past return. The host owns open/close timing; this callback only paints.
 */
@FunctionalInterface
public interface SideCard {

    void render(GuiGraphics graphics, MenuContext ctx, UiRect bounds, float eased, float alpha,
                int mouseX, int mouseY);
}
