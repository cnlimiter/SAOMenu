package com.sao.saomenu.api.theme;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * Resolved theme tokens for drawing and motion: palette, font resources, and enter/exit
 * durations in milliseconds. {@link #colors()} {@code accent} is the live hue-derived value
 * on the active snapshot returned by {@code SaoUi.theme()}.
 *
 * @param colors       role-based palette; {@code accent} is a placeholder on registered
 *                     definitions and the live hue color on the active snapshot
 * @param bodyFont     namespaced font for body copy ({@code minecraft:default} unless overridden)
 * @param displayFont  namespaced font for titles and display copy
 * @param enterMillis  enter / open duration in milliseconds, {@code >= 0}
 * @param exitMillis   exit / close duration in milliseconds, {@code >= 0}
 */
public record ThemeTokens(
        ThemeColors colors,
        ResourceLocation bodyFont,
        ResourceLocation displayFont,
        int enterMillis,
        int exitMillis
) {

    /** Vanilla default bitmap font. */
    public static final ResourceLocation DEFAULT_FONT = new ResourceLocation("minecraft", "default");

    /** Menu group open duration used by the SAO builtin. */
    public static final int SAO_ENTER_MILLIS = 260;

    /** Menu group close duration used by the SAO builtin. */
    public static final int SAO_EXIT_MILLIS = 170;

    private static final ThemeTokens SAO = new ThemeTokens(
            ThemeColors.sao(), DEFAULT_FONT, DEFAULT_FONT, SAO_ENTER_MILLIS, SAO_EXIT_MILLIS);

    public ThemeTokens {
        Objects.requireNonNull(colors, "colors");
        Objects.requireNonNull(bodyFont, "bodyFont");
        Objects.requireNonNull(displayFont, "displayFont");
        if (enterMillis < 0) {
            throw new IllegalArgumentException("enterMillis must be >= 0");
        }
        if (exitMillis < 0) {
            throw new IllegalArgumentException("exitMillis must be >= 0");
        }
    }

    /**
     * Builtin SAO tokens: measured palette, {@code minecraft:default} for both fonts,
     * 260 ms enter and 170 ms exit.
     */
    public static ThemeTokens sao() {
        return SAO;
    }

    /** Palette with a replacement accent; fonts and durations unchanged. */
    public ThemeTokens withColors(ThemeColors newColors) {
        return new ThemeTokens(newColors, bodyFont, displayFont, enterMillis, exitMillis);
    }
}
