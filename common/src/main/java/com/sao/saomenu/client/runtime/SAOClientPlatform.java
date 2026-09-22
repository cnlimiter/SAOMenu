package com.sao.saomenu.client.runtime;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;

import java.util.Map;

/** Client-only accessors must not appear in the common sound/particle platform bridge. */
public final class SAOClientPlatform {
    private SAOClientPlatform() {
    }

    @ExpectPlatform
    public static Map<Advancement, AdvancementProgress> advancementProgress(ClientAdvancements advancements) {
        throw new AssertionError();
    }
}
