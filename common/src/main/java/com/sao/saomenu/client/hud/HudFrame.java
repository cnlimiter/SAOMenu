package com.sao.saomenu.client.hud;

import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.api.hud.HudRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Mutability stays behind the public read-only frame interface. Client thread only. */
final class HudFrame implements HudRenderContext {
    private GuiGraphics graphics;
    private Minecraft minecraft;
    private int width;
    private int height;
    private float fade;
    private float plateAlpha;
    private HudPass pass;
    private int mouseX;
    private int mouseY;

    void begin(GuiGraphics graphics, Minecraft minecraft, int width, int height,
               float fade, float plateAlpha, HudPass pass, int mouseX, int mouseY) {
        this.graphics = graphics;
        this.minecraft = minecraft;
        this.width = width;
        this.height = height;
        this.fade = fade;
        this.plateAlpha = plateAlpha;
        this.pass = pass;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
    }

    void clear() {
        graphics = null;
        minecraft = null;
    }

    @Override public GuiGraphics graphics() { return graphics; }
    @Override public Minecraft minecraft() { return minecraft; }
    @Override public int width() { return width; }
    @Override public int height() { return height; }
    @Override public float fade() { return fade; }
    @Override public float plateAlpha() { return plateAlpha; }
    @Override public HudPass pass() { return pass; }
    @Override public int mouseX() { return mouseX; }
    @Override public int mouseY() { return mouseY; }
}
