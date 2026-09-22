package com.sao.saomenu.client.runtime.forge;

import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.forge.SaoUiRegisterEvent;
import com.sao.saomenu.mixin.accessor.ClientAdvancementsAccessor;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.Map;

/** Package and suffix follow Architectury's ExpectPlatform convention. */
public final class SAOClientPlatformImpl {
    private SAOClientPlatformImpl() {
    }

    public static Map<Advancement, AdvancementProgress> advancementProgress(ClientAdvancements advancements) {
        return ((ClientAdvancementsAccessor) advancements).saomenu$progress();
    }

    public static void registerUi(SaoUiRegistry registry) {
        MinecraftForge.EVENT_BUS.post(new SaoUiRegisterEvent(registry));
    }

    public static void pushLayer(Screen layer) {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), layer);
    }

    public static void popLayer() {
        ForgeHooksClient.popGuiLayer(Minecraft.getInstance());
    }
}
