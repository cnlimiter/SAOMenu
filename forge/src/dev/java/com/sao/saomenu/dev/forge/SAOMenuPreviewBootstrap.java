package com.sao.saomenu.dev.forge;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.dev.preview.SAOMenuPreview;
import com.sao.saomenu.api.forge.SaoUiRegisterEvent;
import com.sao.saomenu.dev.preview.FrameworkApiFixtures;
import com.sao.saomenu.dev.preview.FrameworkRecoveryPreview;
import com.sao.saomenu.dev.preview.ContainerNativePreview;
import com.sao.saomenu.dev.preview.FrontendNativePreview;
import com.sao.saomenu.dev.preview.network.MultiplayerNativePreview;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Only present on the preview run's development classpath. */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SAOMenuPreviewBootstrap {
    private SAOMenuPreviewBootstrap() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (FrameworkRecoveryPreview.requested()) FrameworkRecoveryPreview.register();
            else if (ContainerNativePreview.requested()) ContainerNativePreview.register();
            else if (FrontendNativePreview.requested()) FrontendNativePreview.register();
            else if (MultiplayerNativePreview.requested()) MultiplayerNativePreview.register();
            else SAOMenuPreview.registerIfRequested();
        });
    }

    @Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT)
    public static final class ApiRegistration {
        @SubscribeEvent
        public static void register(SaoUiRegisterEvent event) {
            FrameworkApiFixtures.register(event.registry());
            FrameworkRecoveryPreview.registerUi(event.registry());
            MultiplayerNativePreview.registerUi(event.registry());
        }
    }
}
