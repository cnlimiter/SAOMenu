package com.sao.saomenu.api.theme;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * A selectable theme contribution. Identity is a namespaced {@link ResourceLocation};
 * builtin SAO/ALO/GGO use {@code saomenu:sao}, {@code saomenu:alo}, {@code saomenu:ggo}.
 * Duplicate IDs are rejected at registration; a later user JSON file may overlay the
 * same id after freeze without going through the registry again.
 *
 * @param id          namespaced theme id
 * @param order       stable list order; builtins are 100 / 200 / 300
 * @param label       display name (translated for builtins, literal for most user files)
 * @param defaultHue  finite hue in degrees; stored wrapped into {@code [0, 360)}
 * @param tokens      fonts, motion, and the static palette (accent is derived at use)
 */
public record ThemeDefinition(
        ResourceLocation id,
        int order,
        Component label,
        float defaultHue,
        ThemeTokens tokens
) implements UiContribution {

    public ThemeDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(tokens, "tokens");
        if (!Float.isFinite(defaultHue)) {
            throw new IllegalArgumentException("defaultHue must be finite");
        }
        defaultHue = wrapHue(defaultHue);
    }

    /** Wraps a finite hue into {@code [0, 360)}. */
    public static float wrapHue(float hue) {
        return ((hue % 360f) + 360f) % 360f;
    }
}
