package com.sao.saomenu.api.theme;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Public theme records: defaults, wrapping, and rejected inputs. */
class ThemeTokensTest {

    @Test
    void saoDefaultsMatchMenuMotionAndVanillaFont() {
        ThemeTokens sao = ThemeTokens.sao();
        assertSame(ThemeTokens.sao(), sao);
        assertEquals(ThemeColors.sao(), sao.colors());
        assertEquals(new ResourceLocation("minecraft", "default"), sao.bodyFont());
        assertEquals(new ResourceLocation("minecraft", "default"), sao.displayFont());
        assertEquals(260, sao.enterMillis());
        assertEquals(170, sao.exitMillis());
    }

    @Test
    void rejectsNullsAndNegativeDurations() {
        ThemeTokens sao = ThemeTokens.sao();
        assertThrows(NullPointerException.class,
                () -> new ThemeTokens(null, sao.bodyFont(), sao.displayFont(), 1, 1));
        assertThrows(NullPointerException.class,
                () -> new ThemeTokens(sao.colors(), null, sao.displayFont(), 1, 1));
        assertThrows(NullPointerException.class,
                () -> new ThemeTokens(sao.colors(), sao.bodyFont(), null, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new ThemeTokens(sao.colors(), sao.bodyFont(), sao.displayFont(), -1, 0));
        assertThrows(IllegalArgumentException.class,
                () -> new ThemeTokens(sao.colors(), sao.bodyFont(), sao.displayFont(), 0, -1));
    }

    @Test
    void definitionWrapsHueAndRejectsNonFinite() {
        ResourceLocation id = new ResourceLocation("addon", "night");
        Component label = Component.literal("Night");
        ThemeDefinition wrapped = new ThemeDefinition(id, 500, label, 400f, ThemeTokens.sao());
        assertEquals(40f, wrapped.defaultHue(), 0.001f);
        assertEquals(id, wrapped.id());
        assertEquals(500, wrapped.order());
        assertSame(ThemeTokens.sao(), wrapped.tokens());

        assertThrows(IllegalArgumentException.class,
                () -> new ThemeDefinition(id, 1, label, Float.NaN, ThemeTokens.sao()));
        assertThrows(IllegalArgumentException.class,
                () -> new ThemeDefinition(id, 1, label, Float.POSITIVE_INFINITY, ThemeTokens.sao()));
        assertThrows(NullPointerException.class,
                () -> new ThemeDefinition(null, 1, label, 0f, ThemeTokens.sao()));
        assertThrows(NullPointerException.class,
                () -> new ThemeDefinition(id, 1, null, 0f, ThemeTokens.sao()));
        assertThrows(NullPointerException.class,
                () -> new ThemeDefinition(id, 1, label, 0f, null));
    }
}
