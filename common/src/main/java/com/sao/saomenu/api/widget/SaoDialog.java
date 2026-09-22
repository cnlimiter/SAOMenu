package com.sao.saomenu.api.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dimmed SAO card opened as a Forge GUI layer when the parent is the current screen.
 * {@link #finish()} pops only once and only while this dialog is current.
 */
public abstract class SaoDialog extends SaoScreen {
    private boolean finished;
    private boolean layered;

    public SaoDialog(Component title, Screen parent, int preferredWidth, int preferredHeight) {
        super(title, parent, preferredWidth, preferredHeight);
        setDimBackdrop(true);
    }

    public static boolean open(Screen parent, SaoDialog dialog) {
        if (dialog.parent != parent) {
            throw new IllegalArgumentException("Dialog return target differs from its layer parent");
        }
        if (!SaoLayers.push(parent, dialog)) {
            return false;
        }
        dialog.layered = true;
        return true;
    }

    @Override
    protected void init() {
        finished = false;
        layered = false;
        super.init();
    }

    @Override
    public void onClose() {
        finish();
    }

    @Override
    public void removed() {
        finished = true;
        super.removed();
    }

    /**
     * Pops this layer if it is still current. Returns {@code false} when already closed
     * or when another screen replaced it; callers must not run confirm callbacks then.
     */
    protected final boolean finish() {
        Minecraft client = Minecraft.getInstance();
        if (finished || client.screen != this) {
            return false;
        }
        finished = true;
        if (!layered || !SaoLayers.pop(this)) {
            client.setScreen(parent);
        }
        return true;
    }
}
