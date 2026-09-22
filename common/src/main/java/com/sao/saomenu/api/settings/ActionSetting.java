package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Fire-and-forget row. The settings screen never writes a stored value for this kind. */
public final class ActionSetting implements Setting {
    private final ResourceLocation id;
    private final Component label;
    private final Component actionLabel;
    private final Runnable action;

    public ActionSetting(ResourceLocation id, Component label, Runnable action) {
        this(id, label, label, action);
    }

    public ActionSetting(ResourceLocation id, Component label, Component actionLabel, Runnable action) {
        this.id = Objects.requireNonNull(id, "id");
        this.label = Objects.requireNonNull(label, "label");
        this.actionLabel = Objects.requireNonNull(actionLabel, "actionLabel");
        this.action = Objects.requireNonNull(action, "action");
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public Component label() {
        return label;
    }

    public Component actionLabel() {
        return actionLabel;
    }

    public void run() {
        action.run();
    }
}
