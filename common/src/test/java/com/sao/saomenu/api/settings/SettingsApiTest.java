package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Public settings types: validation, clamp, choice membership, native truncation, group isolation. */
class SettingsApiTest {

    @Test
    void toggleFlipsBackingStore() {
        AtomicBoolean value = new AtomicBoolean(false);
        ToggleSetting setting = new ToggleSetting(id("toggle"), Component.literal("T"),
                () -> value.get(), value::set);
        setting.toggle();
        assertTrue(setting.value());
        setting.set(false);
        assertFalse(value.get());
    }

    @Test
    void sliderClampsAndFormats() {
        AtomicReference<Float> value = new AtomicReference<>(0.5f);
        SliderSetting setting = new SliderSetting(id("slider"), Component.literal("S"),
                () -> value.get(), v -> value.set(v), 0f, 1f, SliderSetting.Format.PERCENT);
        setting.set(4f);
        assertEquals(1f, setting.value(), 0.0001f);
        setting.setFraction(0.25f);
        assertEquals(0.25f, value.get(), 0.0001f);
        assertEquals("25%", setting.formatValue());
        setting.nudge(-100);
        assertEquals(0f, setting.value(), 0.0001f);
    }

    @Test
    void sliderRejectsEmptyRange() {
        assertThrows(IllegalArgumentException.class, () ->
                new SliderSetting(id("bad"), Component.literal("S"), () -> 0f, v -> {}, 1f, 1f));
    }

    @Test
    void choiceRejectsUnknownAndCycles() {
        List<ChoiceSetting.Option> options = List.of(
                new ChoiceSetting.Option(id("a"), Component.literal("A")),
                new ChoiceSetting.Option(id("b"), Component.literal("B")));
        AtomicReference<ResourceLocation> value = new AtomicReference<>(id("a"));
        ChoiceSetting setting = new ChoiceSetting(id("choice"), Component.literal("C"),
                options, value::get, value::set);
        assertThrows(IllegalArgumentException.class, () -> setting.set(id("missing")));
        setting.cycle(1);
        assertEquals(id("b"), setting.value());
        setting.cycle(1);
        assertEquals(id("a"), setting.value());
        value.set(null);
        assertNull(setting.value());
        setting.cycle(1);
        assertEquals(id("a"), setting.value());
    }

    @Test
    void staticChoiceRequiresOptions() {
        assertThrows(IllegalArgumentException.class, () ->
                new ChoiceSetting(id("empty"), Component.literal("C"), List.of(),
                        () -> null, v -> {}));
    }

    @Test
    void actionRunsOncePerActivate() {
        AtomicInteger n = new AtomicInteger();
        ActionSetting setting = new ActionSetting(id("act"), Component.literal("Do"),
                Component.literal("Go"), n::incrementAndGet);
        setting.run();
        setting.run();
        assertEquals(2, n.get());
        assertEquals("Go", setting.actionLabel().getString());
    }

    @Test
    void nativeTruncatesAndTreatsNullAsEmpty() {
        AtomicReference<String> value = new AtomicReference<>("hi");
        NativeSetting setting = new NativeSetting(id("name"), Component.literal("N"),
                value::get, value::set, 4);
        setting.set("abcdef");
        assertEquals("abcd", setting.value());
        setting.set(null);
        assertEquals("", value.get());
    }

    @Test
    void groupCopiesOptionsAndRejectsDuplicateIds() {
        ToggleSetting a = new ToggleSetting(id("a"), Component.literal("A"), () -> true, v -> {});
        List<Setting> mutable = new ArrayList<>();
        mutable.add(a);
        SettingsGroup group = new SettingsGroup(id("g"), 1, Component.literal("G"),
                mutable, () -> {}, () -> {});
        mutable.add(new ToggleSetting(id("b"), Component.literal("B"), () -> false, v -> {}));
        assertEquals(1, group.options().size());
        assertThrows(UnsupportedOperationException.class, () -> group.options().add(a));
        assertThrows(IllegalArgumentException.class, () ->
                new SettingsGroup(id("g2"), 1, Component.literal("G"),
                        List.of(a, new ToggleSetting(id("a"), Component.literal("A2"), () -> false, v -> {})),
                        () -> {}, () -> {}));
    }

    @Test
    void groupSaveAndResetStayOnOwnerCallbacks() {
        AtomicInteger saved = new AtomicInteger();
        AtomicInteger reset = new AtomicInteger();
        AtomicInteger other = new AtomicInteger();
        SettingsGroup group = SettingsGroup.of(id("addon"), 500, Component.literal("Addon"),
                List.of(new ToggleSetting(id("only"), Component.literal("Only"), () -> true, v -> {})),
                saved::incrementAndGet, reset::incrementAndGet);
        group.save();
        group.reset();
        assertEquals(1, saved.get());
        assertEquals(1, reset.get());
        assertEquals(0, other.get());
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("test", path);
    }
}
