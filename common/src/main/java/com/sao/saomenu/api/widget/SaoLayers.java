package com.sao.saomenu.api.widget;

import com.sao.saomenu.client.runtime.SAOClientPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Native Forge GUI layers preserve the parent mount and its unsaved native field state. */
final class SaoLayers {
    private SaoLayers() {
    }

    static boolean push(Screen parent, Screen layer) {
        Minecraft client = clientThread();
        if (client.screen != parent) {
            return false;
        }
        SAOClientPlatform.pushLayer(layer);
        return true;
    }

    static boolean pop(Screen current) {
        Minecraft client = clientThread();
        if (client.screen != current) {
            return false;
        }
        SAOClientPlatform.popLayer();
        return true;
    }

    private static Minecraft clientThread() {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) {
            throw new IllegalStateException("Dialogs must be opened and closed on the client thread");
        }
        return client;
    }
}
