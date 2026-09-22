package com.sao.saomenu.api.menu;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Capabilities the open menu exposes to panels and side cards.
 *
 * <h2>Coordinates</h2>
 * Menu chrome is laid out in <b>menu-local</b> GUI pixels (the untransformed layout
 * space of {@code MenuLayout}). The whole group is then scaled, sheared, rotated and
 * shifted. {@link #screenPointOf} and {@link #localBoxToScreen} convert that local space
 * to <b>screen</b> GUI pixels for scissor boxes, tooltips and overlay widgets.
 * Mouse coordinates passed into {@link SideCard#render} are screen GUI pixels.
 * Do not treat local x/y as screen x/y.
 */
public interface MenuHost {

    /** Host screen, used as the return target for child screens. */
    Screen screen();

    /**
     * Select the panel at {@code index} in {@link SaoUi#panels()}. Pass {@code -1} to
     * collapse the open panel.
     */
    void selectMain(int index);

    /** Select by namespaced id; unknown ids throw. */
    default void selectPanel(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        List<SaoPanel> panels = SaoUi.panels();
        for (int i = 0; i < panels.size(); i++) {
            if (id.equals(panels.get(i).id())) {
                selectMain(i);
                return;
            }
        }
        throw new IllegalArgumentException("Unknown panel: " + id);
    }

    /** Confirm-close dialog. */
    void openCloseConfirm();

    void toggleMap();

    void playClick();

    void playPanel();

    void playAlert();

    /**
     * Menu-local point → screen GUI pixels after this frame's group transform.
     *
     * @return {@code {screenX, screenY}}
     */
    float[] screenPointOf(float localX, float localY);

    /**
     * Axis-aligned screen-space box of a menu-local rectangle, for {@code GuiGraphics}
     * scissor. A rotated/sheared local rect is not axis-aligned on screen; this is the
     * bounding box of the four projected corners.
     */
    UiRect localBoxToScreen(int localX, int localY, int width, int height);
}
