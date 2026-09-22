package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Exclusive choice among namespaced options. {@link #value()} may be {@code null} when the
 * backing store does not match any option (for example a custom theme hue).
 */
public final class ChoiceSetting implements Setting {
    private final ResourceLocation id;
    private final Component label;
    private final Supplier<List<Option>> options;
    private final Supplier<ResourceLocation> getter;
    private final Consumer<ResourceLocation> setter;
    private List<Option> snapshot;

    public ChoiceSetting(ResourceLocation id, Component label, List<Option> options,
                         Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        this(id, label, staticOptions(options), getter, setter);
    }

    public ChoiceSetting(ResourceLocation id, Component label, Supplier<List<Option>> options,
                         Supplier<ResourceLocation> getter, Consumer<ResourceLocation> setter) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.options = Objects.requireNonNull(options, "options");
        this.getter = Objects.requireNonNull(getter, "getter");
        this.setter = Objects.requireNonNull(setter, "setter");
        snapshot = copyOptions(options.get());
    }

    private static Supplier<List<Option>> staticOptions(List<Option> options) {
        List<Option> copy = List.copyOf(options);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("choice must expose at least one option");
        }
        return () -> copy;
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component label() {
        return label;
    }

    public List<Option> options() {
        List<Option> current = Objects.requireNonNull(options.get(), "options");
        if (!snapshot.equals(current)) {
            snapshot = copyOptions(current);
        }
        return snapshot;
    }

    /** Currently matching option id, or {@code null} if none of the options apply. */
    public ResourceLocation value() {
        return getter.get();
    }

    public void set(ResourceLocation optionId) {
        Objects.requireNonNull(optionId, "optionId");
        if (indexOf(optionId) < 0) {
            throw new IllegalArgumentException("Unknown choice: " + optionId);
        }
        setter.accept(optionId);
    }

    public void cycle(int delta) {
        List<Option> list = options();
        if (list.isEmpty()) {
            return;
        }
        int at = indexOf(list, value());
        if (at < 0) {
            at = 0;
        } else {
            int n = list.size();
            at = Math.floorMod(at + delta, n);
        }
        setter.accept(list.get(at).id());
    }

    private int indexOf(ResourceLocation optionId) {
        return indexOf(options(), optionId);
    }

    private static int indexOf(List<Option> list, ResourceLocation optionId) {
        if (optionId == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(optionId)) {
                return i;
            }
        }
        return -1;
    }

    private static List<Option> copyOptions(List<Option> options) {
        Objects.requireNonNull(options, "options");
        Set<ResourceLocation> seen = new HashSet<>();
        for (Option option : options) {
            Objects.requireNonNull(option, "option");
            if (!seen.add(option.id())) {
                throw new IllegalArgumentException("Duplicate choice: " + option.id());
            }
        }
        return List.copyOf(options);
    }

    /**
     * @param swatchHue hue used by the P5 theme chips; {@link Float#NaN} skips the color fill
     */
    public record Option(ResourceLocation id, Component label, float swatchHue) {
        public Option {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(label, "label");
        }

        public Option(ResourceLocation id, Component label) {
            this(id, label, Float.NaN);
        }

        public boolean hasSwatch() {
            return Float.isFinite(swatchHue);
        }
    }
}
