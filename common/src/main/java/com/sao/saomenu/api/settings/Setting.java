package com.sao.saomenu.api.settings;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * One namespaced row in a {@link SettingsGroup}. Addon code uses the typed implementations;
 * the settings screen switches on those kinds rather than string keys.
 */
public sealed interface Setting permits ToggleSetting, SliderSetting, ChoiceSetting, ActionSetting, NativeSetting {
    ResourceLocation id();

    Component label();
}
