package com.sao.saomenu.api.menu;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * One main-menu panel: a round main button, its first-level rows, and an optional side card.
 *
 * <p>Register through {@link com.sao.saomenu.api.SaoUiRegistry#menu} during the client
 * registration event. Duplicate {@link #id()} values throw. After freeze, consumers read
 * {@link com.sao.saomenu.api.SaoUi#panels()} — there is no mutable public registry.</p>
 *
 * <p>{@link #items()} is read every frame. Dynamic lists (inventory, online players) must
 * be cached by the supplier; the menu does not own that cache.</p>
 *
 * @param id       namespaced identity ({@code saomenu:profile} and siblings for builtins)
 * @param order    100/200/300/400 for builtins; lower draws higher in the main column
 * @param label    panel title
 * @param icons    main-button normal/hover art
 * @param items    first-level rows
 * @param sideCard left card, or {@code null}
 */
public record SaoPanel(
        ResourceLocation id,
        int order,
        Component label,
        MenuIcon icons,
        Supplier<List<MenuEntry>> items,
        SideCard sideCard
) implements UiContribution {

    public SaoPanel {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(icons, "icons");
        Objects.requireNonNull(items, "items");
    }

    /** Panel without a side card. */
    public static SaoPanel of(ResourceLocation id, int order, Component label, MenuIcon icons,
                              Supplier<List<MenuEntry>> items) {
        return new SaoPanel(id, order, label, icons, items, null);
    }

    /** Panel with a custom side card. */
    public static SaoPanel of(ResourceLocation id, int order, Component label, MenuIcon icons,
                              Supplier<List<MenuEntry>> items, SideCard sideCard) {
        return new SaoPanel(id, order, label, icons, items, sideCard);
    }
}
