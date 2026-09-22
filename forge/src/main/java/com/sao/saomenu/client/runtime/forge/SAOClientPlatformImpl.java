package com.sao.saomenu.client.runtime.forge;

import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.forge.SaoUiRegisterEvent;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.mixin.accessor.FontAccessor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Supplier;

/** Package and suffix follow Architectury's ExpectPlatform convention. */
public final class SAOClientPlatformImpl {
    private SAOClientPlatformImpl() {
    }

    public static void registerUi(SaoUiRegistry registry) {
        MinecraftForge.EVENT_BUS.post(new SaoUiRegisterEvent(registry));
    }

    public static Font createThemedFont(Supplier<ResourceLocation> primary) {
        Minecraft client = Minecraft.getInstance();
        return new Font(id -> ((FontAccessor) client.font).saomenu$fonts().apply(
                ThemeTokens.DEFAULT_FONT.equals(id) ? primary.get() : id), false);
    }

    public static void pushLayer(Screen layer) {
        ForgeHooksClient.pushGuiLayer(Minecraft.getInstance(), layer);
    }

    public static void popLayer() {
        ForgeHooksClient.popGuiLayer(Minecraft.getInstance());
    }
}
