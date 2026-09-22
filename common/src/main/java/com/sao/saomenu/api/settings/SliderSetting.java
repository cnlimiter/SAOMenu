package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.Locale;
import java.util.Objects;

/** Bounded float option. {@link #set(float)} clamps to {@code [min, max]}. */
public final class SliderSetting implements Setting {
    private final ResourceLocation id;
    private final Component label;
    private final FloatGetter getter;
    private final FloatSetter setter;
    private final float min;
    private final float max;
    private final Format format;

    public SliderSetting(ResourceLocation id, Component label, FloatGetter getter, FloatSetter setter,
                         float min, float max) {
        this(id, label, getter, setter, min, max, Format.RAW);
    }

    public SliderSetting(ResourceLocation id, Component label, FloatGetter getter, FloatSetter setter,
                         float min, float max, Format format) {
        if (!(min < max) || !Float.isFinite(min) || !Float.isFinite(max)) {
            throw new IllegalArgumentException("slider range must be finite with min < max");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
        this.min = min;
        this.max = max;
        this.format = Objects.requireNonNull(format, "format");
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component label() {
        return label;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public Format format() {
        return format;
    }

    public float value() {
        return Mth.clamp(getter.get(), min, max);
    }

    public void set(float value) {
        setter.set(Mth.clamp(value, min, max));
    }

    public float fraction() {
        return Mth.clamp((value() - min) / (max - min), 0f, 1f);
    }

    public void setFraction(float fraction) {
        set(min + Mth.clamp(fraction, 0f, 1f) * (max - min));
    }

    public void nudge(int steps) {
        float span = max - min;
        set(value() + steps * (span / 20f));
    }

    public String formatValue() {
        return format.apply(value());
    }

    @FunctionalInterface
    public interface FloatGetter {
        float get();
    }

    @FunctionalInterface
    public interface FloatSetter {
        void set(float value);
    }

    public enum Format {
        RAW,
        PERCENT,
        MULTIPLIER_1,
        MULTIPLIER_2,
        DEGREES;

        public String apply(float value) {
            return switch (this) {
                case PERCENT -> String.format(Locale.ROOT, "%.0f%%", value * 100f);
                case MULTIPLIER_2 -> String.format(Locale.ROOT, "%.2fx", value);
                case MULTIPLIER_1 -> String.format(Locale.ROOT, "%.1fx", value);
                case DEGREES -> Math.round(value) + "°";
                case RAW -> String.format(Locale.ROOT, "%.2f", value);
            };
        }
    }
}
