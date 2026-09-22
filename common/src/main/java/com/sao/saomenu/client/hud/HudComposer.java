package com.sao.saomenu.client.hud;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.api.hud.HudRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * 常驻 HUD 与菜单 HUD 共用的一层组合/可见性策略。
 *
 * <p>遍历 {@link SaoUi#hudElements()} 冻结快照,不复制列表。菜单 underlay 只画
 * 声明了该 pass 的元件(圆点物品栏);overlay 与 WORLD 按注册 order 绘制。
 * 血条板走 {@link HudRenderContext#plateAlpha()},其余淡出元件走 {@link HudRenderContext#fade()}。</p>
 */
final class HudComposer {
    private static final HudFrame FRAME = new HudFrame();


    private HudComposer() {
    }

    static void world(GuiGraphics g, Minecraft mc) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        int mx = (int) (mc.mouseHandler.xpos() * w / (double) mc.getWindow().getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * h / (double) mc.getWindow().getScreenHeight());
        compose(g, mc, w, h, 1f, 1f, HudPass.WORLD, mx, my);
    }

    static void gameOverlay(GuiGraphics g, Minecraft mc) {
        compose(g, mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight(),
                1f, 1f, HudPass.GAME_OVERLAY, 0, 0);
    }

    static void menuUnderlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha) {
        compose(g, mc, width, height, alpha, 1f, HudPass.MENU_UNDERLAY, 0, 0);
    }

    static void menuOverlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha,
                            int mouseX, int mouseY) {
        compose(g, mc, width, height, alpha, 1f, HudPass.MENU_OVERLAY, mouseX, mouseY);
    }

    private static void compose(GuiGraphics g, Minecraft mc, int w, int h,
                                float fade, float plateAlpha, HudPass pass, int mouseX, int mouseY) {
        List<HudElement> elements = SaoUi.hudElements();
        FRAME.begin(g, mc, w, h, fade, plateAlpha, pass, mouseX, mouseY);
        try {
            for (int i = 0, n = elements.size(); i < n; i++) {
                HudElement element = elements.get(i);
                if (element.inPass(pass) && element.visible(FRAME)) {
                    element.renderer().render(FRAME);
                }
            }
        } finally {
            FRAME.clear();
        }
    }
}
