package com.sao.saomenu.api.menu;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Namespaced normal/hover textures for a main-menu panel button.
 *
 * <p>Paths are {@link ResourceLocation}s in the contributing mod's namespace
 * (for example {@code mymod:textures/gui/symbol_foo_normal.png}). Builtin panels
 * use {@code saomenu:textures/gui/symbol_*_{normal,hover}.png}.</p>
 */
public record MenuIcon(ResourceLocation normal, ResourceLocation hover) {

    public MenuIcon {
        Objects.requireNonNull(normal, "normal");
        Objects.requireNonNull(hover, "hover");
    }

    /** Same texture for rest and hover. */
    public static MenuIcon of(ResourceLocation icon) {
        Objects.requireNonNull(icon, "icon");
        return new MenuIcon(icon, icon);
    }

    public ResourceLocation pick(boolean hovered) {
        return hovered ? hover : normal;
    }
}
