package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.SaoUiRegistry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;

/** Client-only accessors must not appear in the common sound/particle platform bridge. */
public final class SAOClientPlatform {
    private SAOClientPlatform() {
    }

    @ExpectPlatform
    public static Map<Advancement, AdvancementProgress> advancementProgress(ClientAdvancements advancements) {
        throw new AssertionError();
    }

    /** Dispatches the platform's client-only, synchronous UI registration event. */
    @ExpectPlatform
    public static void registerUi(SaoUiRegistry registry) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void pushLayer(Screen layer) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void popLayer() {
        throw new AssertionError();
    }
}
