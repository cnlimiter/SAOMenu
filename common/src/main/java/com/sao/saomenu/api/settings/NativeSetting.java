package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * String option hosted as a vanilla {@code EditBox} so IME, focus and narration stay native.
 * The settings screen mounts the widget; this object only owns the backing value.
 */
public final class NativeSetting implements Setting {
    public static final int DEFAULT_MAX_LENGTH = 32;

    private final ResourceLocation id;
    private final Component label;
    private final Supplier<String> getter;
    private final Consumer<String> setter;
    private final int maxLength;

    public NativeSetting(ResourceLocation id, Component label, Supplier<String> getter, Consumer<String> setter) {
        this(id, label, getter, setter, DEFAULT_MAX_LENGTH);
    }

    public NativeSetting(ResourceLocation id, Component label, Supplier<String> getter, Consumer<String> setter,
                         int maxLength) {
        if (maxLength <= 0) {
            throw new IllegalArgumentException("maxLength must be positive");
        }
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
        this.maxLength = maxLength;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component label() {
        return label;
    }

    public int maxLength() {
        return maxLength;
    }

    public String value() {
        String value = getter.get();
        return value == null ? "" : value;
    }

    public void set(String value) {
        String next = value == null ? "" : value;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        setter.accept(next);
    }
}
