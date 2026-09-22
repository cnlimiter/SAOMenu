package com.sao.saomenu.api.settings;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One settings category. Options are an immutable snapshot taken at construction; live choice
 * contents (for example registered themes) stay inside the {@link ChoiceSetting} supplier.
 * {@link #save()} and {@link #reset()} are owned by the contributing mod and must not write
 * another group's store.
 */
public final class SettingsGroup implements UiContribution {
    private final ResourceLocation id;
    private final int order;
    private final Component label;
    private final Component description;
    private final List<Setting> options;
    private final Runnable save;
    private final Runnable reset;

    public SettingsGroup(ResourceLocation id, int order, Component label, List<? extends Setting> options,
                         Runnable save, Runnable reset) {
        this(id, order, label, Component.empty(), options, save, reset);
    }

    public SettingsGroup(ResourceLocation id, int order, Component label, Component description,
                         List<? extends Setting> options, Runnable save, Runnable reset) {
        this.id = Objects.requireNonNull(id, "id");
        this.order = order;
        this.label = Objects.requireNonNull(label, "label");
        this.description = Objects.requireNonNull(description, "description");
        this.save = Objects.requireNonNull(save, "save");
        this.reset = Objects.requireNonNull(reset, "reset");
        this.options = copyOptions(options);
    }

    public static SettingsGroup of(ResourceLocation id, int order, Component label, List<? extends Setting> options,
                                   Runnable save, Runnable reset) {
        return new SettingsGroup(id, order, label, options, save, reset);
    }

    private static List<Setting> copyOptions(List<? extends Setting> options) {
        Objects.requireNonNull(options, "options");
        List<Setting> copy = new ArrayList<>(options.size());
        Set<ResourceLocation> seen = new HashSet<>();
        for (Setting option : options) {
            Objects.requireNonNull(option, "option");
            if (!seen.add(option.id())) {
                throw new IllegalArgumentException("Duplicate setting: " + option.id());
            }
            copy.add(option);
        }
        return List.copyOf(copy);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int order() {
        return order;
    }

    public Component label() {
        return label;
    }

    /** Secondary P5 subtitle; {@link Component#empty()} when the addon does not supply one. */
    public Component description() {
        return description;
    }

    public List<Setting> options() {
        return options;
    }

    public void save() {
        save.run();
    }

    public void reset() {
        reset.run();
    }
}
