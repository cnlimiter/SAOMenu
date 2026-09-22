package com.sao.saomenu.client.hud;

import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/**
 * 常驻 HUD 与菜单 HUD 共用的一层组合/可见性策略。
 *
 * <p>菜单 underlay 只画圆点物品栏;overlay 画地图、状态板(不随菜单 alpha 淡出)
 * 以及时钟/技能条/战斗反馈。{@link SAOConfig#showHud()} 在两条路径上闸同一组元件。</p>
 */
final class HudComposer {

    private HudComposer() {
    }

    static void world(GuiGraphics g, Minecraft mc) {
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();
        compose(g, mc, w, h, 1f, 1f, Pass.WORLD);
        int mx = (int) (mc.mouseHandler.xpos() * w / (double) mc.getWindow().getScreenWidth());
        int my = (int) (mc.mouseHandler.ypos() * h / (double) mc.getWindow().getScreenHeight());
        Player p = mc.player;
        if (hudOn(p)) {
            SAOHotbarDots.renderHoverTooltip(g, mc, w, h, p, mx, my);
        }
        SAOMapPanel.renderHud(g, mc, w, h);
    }

    static void menuUnderlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha) {
        compose(g, mc, width, height, alpha, 1f, Pass.MENU_UNDERLAY);
    }

    static void menuOverlay(GuiGraphics g, Minecraft mc, int width, int height, float alpha) {
        compose(g, mc, width, height, alpha, 1f, Pass.MENU_OVERLAY);
    }

    private enum Pass {
        WORLD, MENU_UNDERLAY, MENU_OVERLAY
    }

    private static boolean hudOn(Player p) {
        return SAOConfig.showHud() && p != null;
    }

    private static void compose(GuiGraphics g, Minecraft mc, int w, int h,
                                float fade, float plateAlpha, Pass pass) {
        Player p = mc.player;
        boolean hud = hudOn(p);
        if (pass == Pass.MENU_UNDERLAY) {
            if (hud) {
                SAOHotbarDots.render(g, w, h, p, fade);
            }
            return;
        }
        if (pass == Pass.MENU_OVERLAY) {
            SAOMapPanel.render(g, mc, w, h, fade);
        }
        if (!hud) {
            return;
        }
        int px = SAOPlayerPlate.plateX(w);
        int py = SAOPlayerPlate.plateY(h);
        String name = p.getGameProfile().getName();
        SAOPlayerPlate.render(g, px, py, SAOPlayerPlate.plateW(w), SAOPlayerPlate.plateH(w),
                name, p, plateAlpha);
        SAOPlayerPlate.renderTeamBars(g, mc, px, py + SAOPlayerPlate.plateH(w) + 2, w, p);
        SAOCombatHud.render(g, mc, w, h, fade);
        if (pass == Pass.WORLD) {
            SAOHotbarDots.render(g, w, h, p, fade);
        }
        SAONotification.render(g, w, h, net.minecraft.Util.getMillis(), fade);
        SAOClockPanel.render(g, mc, w, h, fade);
        SaoSkillBar.render(g, mc, w, h, fade);
        float max = p.getMaxHealth();
        SAOPlayerPlate.renderLowHpVignette(g, w, h, max <= 0f ? 0f : p.getHealth() / max);
    }
}
