package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.SaoUiRegistry;
import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Client-only accessors must not appear in the common sound/particle platform bridge. */
public final class SAOClientPlatform {
    private SAOClientPlatform() {
    }

    /** Dispatches the platform's client-only, synchronous UI registration event. */
    @ExpectPlatform
    public static void registerUi(SaoUiRegistry registry) {
        throw new AssertionError();
    }

    /** A stable Font whose default family follows the supplier, retaining explicit styled fonts. */
    @ExpectPlatform
    public static Font createThemedFont(Supplier<ResourceLocation> primary) {
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
