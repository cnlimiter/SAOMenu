package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Boolean option. {@link #set(boolean)} writes the backing store; {@link #toggle()} flips it. */
public final class ToggleSetting implements Setting {
    private final ResourceLocation id;
    private final Component label;
    private final BooleanSupplier getter;
    private final Consumer<Boolean> setter;

    public ToggleSetting(ResourceLocation id, Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component label() {
        return label;
    }

    public boolean value() {
        return getter.getAsBoolean();
    }

    public void set(boolean value) {
        setter.accept(value);
    }

    public void toggle() {
        set(!value());
    }
}
