package com.sao.saomenu.client.runtime.forge;

import com.sao.saomenu.mixin.accessor.ClientAdvancementsAccessor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;

import java.util.Map;

/** Package and suffix follow Architectury's ExpectPlatform convention. */
public final class SAOClientPlatformImpl {
    private SAOClientPlatformImpl() {
    }

    public static Map<Advancement, AdvancementProgress> advancementProgress(ClientAdvancements advancements) {
        return ((ClientAdvancementsAccessor) advancements).saomenu$progress();
    }
}
