package com.sao.saomenu.dev.forge;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.dev.preview.SAOMenuPreview;
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
        event.enqueueWork(SAOMenuPreview::registerIfRequested);
    }
}
