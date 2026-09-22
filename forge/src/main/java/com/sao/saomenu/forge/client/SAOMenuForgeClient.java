package com.sao.saomenu.forge.client;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.client.input.SAOKeybinds;
import com.sao.saomenu.client.render.SAOMenu3DPanel;
import com.sao.saomenu.client.runtime.SaoClientRuntime;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge 客户端初始化:按键必须在 RegisterKeyMappingsEvent 注册。
 */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class SAOMenuForgeClient {

    private SAOMenuForgeClient() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SAOKeybinds.OPEN_MENU);
        event.register(SAOKeybinds.TOGGLE_UI);
        for (net.minecraft.client.KeyMapping km : SAOKeybinds.SKILL_KEYS) {
            event.register(km);
        }
    }


    /** 菜单屏渲染完成后:把主帧缓冲 blit 到世界菜单板的备用纹理。 */
    @Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT)
    public static final class ScreenHooks {
        private static boolean runtimeStarted;

        @SubscribeEvent(priority = EventPriority.HIGHEST)
        public static void onClientTick(TickEvent.ClientTickEvent event) {
            if (!runtimeStarted && event.phase == TickEvent.Phase.START) {
                // Client setup/load-complete still run with the Forge bus stopped.
                // Its first tick guarantees addon subscribers can receive registration.
                SaoClientRuntime.initialize();
                runtimeStarted = true;
            }
        }
        private ScreenHooks() {
        }

        @SubscribeEvent
        public static void onScreenPostRender(ScreenEvent.Render.Post event) {
            if (event.getScreen() instanceof com.sao.saomenu.client.menu.SAOMenuScreen) {
                SAOMenu3DPanel.onMenuScreenRendered(Minecraft.getInstance());
            }
        }
    }
}
