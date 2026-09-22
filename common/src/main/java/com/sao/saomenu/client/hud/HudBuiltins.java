package com.sao.saomenu.client.hud;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.hud.HudBox;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.hud.HudPass;
import com.sao.saomenu.api.hud.HudRenderContext;
import com.sao.saomenu.client.effect.SAOWelcome;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Builtin HUD contributions. Registration order values encode the historical compose
 * sequence; layout {@link HudLayoutBinding#priority()} encodes historical grab order.
 */
final class HudBuiltins {
    static final ResourceLocation MAP = new ResourceLocation(SAOMenu.MOD_ID, "map");
    static final ResourceLocation PLATE = new ResourceLocation(SAOMenu.MOD_ID, "plate");
    static final ResourceLocation COMBAT = new ResourceLocation(SAOMenu.MOD_ID, "combat");
    static final ResourceLocation HOTBAR = new ResourceLocation(SAOMenu.MOD_ID, "hotbar");
    static final ResourceLocation NOTIFICATION = new ResourceLocation(SAOMenu.MOD_ID, "notification");
    static final ResourceLocation CLOCK = new ResourceLocation(SAOMenu.MOD_ID, "clock");
    static final ResourceLocation SKILL_BAR = new ResourceLocation(SAOMenu.MOD_ID, "skill_bar");
    static final ResourceLocation VIGNETTE = new ResourceLocation(SAOMenu.MOD_ID, "vignette");
    static final ResourceLocation HOTBAR_TOOLTIP = new ResourceLocation(SAOMenu.MOD_ID, "hotbar_tooltip");
    static final ResourceLocation MAP_WORLD = new ResourceLocation(SAOMenu.MOD_ID, "map_world");
    static final ResourceLocation FOOD = new ResourceLocation(SAOMenu.MOD_ID, "food");

    private HudBuiltins() {
    }

    static void register(SaoUiRegistry registry) {
        registry.hud(mapMenu());
        registry.hud(plate());
        registry.hud(combat());
        registry.hud(hotbar());
        registry.hud(notification());
        registry.hud(clock());
        registry.hud(skillBar());
        registry.hud(vignette());
        registry.hud(hotbarTooltip());
        registry.hud(mapWorld());
        registry.hud(food());
        registry.hud(HudElement.builder(new ResourceLocation(SAOMenu.MOD_ID, "welcome"), 1000,
                        Component.translatable("saomenu.config.show_welcome"),
                        ctx -> SAOWelcome.render(ctx.graphics(), ctx.width(), ctx.height()))
                .passes(HudPass.GAME_OVERLAY).build());
        registry.hud(HudElement.builder(new ResourceLocation(SAOMenu.MOD_ID, "boss_banner"), 1100,
                        Component.translatable("saomenu.config.show_boss_banner"),
                        ctx -> SAOBossBanner.render(ctx.graphics(), ctx.width(), ctx.height()))
                .passes(HudPass.GAME_OVERLAY).build());
    }

    private static boolean hudOn(HudRenderContext ctx) {
        return SAOConfig.showHud() && ctx.player() != null;
    }

    private static HudElement mapMenu() {
        return HudElement.builder(MAP, 50, Component.translatable("saomenu.menu.map"), ctx ->
                        SAOMapPanel.render(ctx.graphics(), ctx.minecraft(), ctx.width(), ctx.height(), ctx.fade()))
                .passes(HudPass.MENU_OVERLAY)
                .visible(ctx -> SAOMapPanel.isShown())
                .layout(new HudLayoutBinding(
                        10,
                        (mc, w, h, mx, my) -> SAOMapPanel.hitCard(w, h, mx, my),
                        (mc, w, h) -> new HudBox(SAOMapPanel.cardX(w, h), SAOMapPanel.cardY(w, h),
                                SAOMapPanel.panelW(h), SAOMapPanel.panelH(h)),
                        SAOConfig::mapPanelX,
                        SAOConfig::mapPanelY,
                        (mc, w, h, gx, gy, mx, my) -> SAOMapPanel.moveTo(w, h, gx, gy, mx, my),
                        (x, y) -> {
                            SAOConfig.setMapPanelX(x);
                            SAOConfig.setMapPanelY(y);
                        },
                        SAOConfig::save))
                .build();
    }

    private static HudElement plate() {
        return HudElement.builder(PLATE, 100, Component.translatable("saomenu.hud.plate"), ctx -> {
                    Player p = ctx.player();
                    if (p == null) {
                        return;
                    }
                    int px = SAOPlayerPlate.plateX(ctx.width());
                    int py = SAOPlayerPlate.plateY(ctx.height());
                    SAOPlayerPlate.render(ctx.graphics(), px, py,
                            SAOPlayerPlate.plateW(ctx.width()), SAOPlayerPlate.plateH(ctx.width()),
                            p.getGameProfile().getName(), p, ctx.plateAlpha());
                    SAOPlayerPlate.renderTeamBars(ctx.graphics(), ctx.minecraft(),
                            px, py + SAOPlayerPlate.plateH(ctx.width()) + 2, ctx.width(), p);
                })
                .visible(HudBuiltins::hudOn)
                .layout(new HudLayoutBinding(
                        30,
                        (mc, w, h, mx, my) -> SAOPlayerPlate.hit(mc, w, h, mx, my),
                        (mc, w, h) -> new HudBox(SAOPlayerPlate.plateX(w), SAOPlayerPlate.plateY(h),
                                SAOPlayerPlate.plateW(w), SAOPlayerPlate.plateGroupH(w, mc)),
                        SAOConfig::platePanelX,
                        SAOConfig::platePanelY,
                        (mc, w, h, gx, gy, mx, my) -> SAOPlayerPlate.moveTo(mc, w, h, gx, gy, mx, my),
                        (x, y) -> {
                            SAOConfig.setPlatePanelX(x);
                            SAOConfig.setPlatePanelY(y);
                        },
                        SAOConfig::save))
                .build();
    }

    private static HudElement combat() {
        return HudElement.builder(COMBAT, 200, Component.translatable("saomenu.hud.combat"), ctx ->
                        SAOCombatHud.render(ctx.graphics(), ctx.minecraft(), ctx.width(), ctx.height(), ctx.fade()))
                .visible(HudBuiltins::hudOn)
                .build();
    }

    private static HudElement hotbar() {
        return HudElement.builder(HOTBAR, 250, Component.translatable("saomenu.inv.hotbar"), ctx ->
                        SAOHotbarDots.render(ctx.graphics(), ctx.width(), ctx.height(), ctx.player(), ctx.fade()))
                .passes(HudPass.WORLD, HudPass.MENU_UNDERLAY)
                .visible(HudBuiltins::hudOn)
                .build();
    }

    private static HudElement notification() {
        return HudElement.builder(NOTIFICATION, 300, Component.translatable("saomenu.notify.demo"), ctx ->
                        SAONotification.render(ctx.graphics(), ctx.width(), ctx.height(),
                                net.minecraft.Util.getMillis(), ctx.fade()))
                .visible(HudBuiltins::hudOn)
                .build();
    }

    private static HudElement clock() {
        return HudElement.builder(CLOCK, 400, Component.translatable("saomenu.config.show_clock"), ctx ->
                        SAOClockPanel.render(ctx.graphics(), ctx.minecraft(), ctx.width(), ctx.height(), ctx.fade()))
                .visible(HudBuiltins::clockVisible)
                .layout(new HudLayoutBinding(
                        50,
                        (mc, w, h, mx, my) -> SAOClockPanel.hitCard(w, h, mx, my),
                        (mc, w, h) -> new HudBox(SAOClockPanel.panelX(w), SAOClockPanel.panelY(h),
                                SAOClockPanel.panelW(), SAOClockPanel.panelH()),
                        SAOConfig::clockPanelX,
                        SAOConfig::clockPanelY,
                        (mc, w, h, gx, gy, mx, my) -> SAOClockPanel.moveTo(w, h, gx, gy, mx, my),
                        (x, y) -> {
                            SAOConfig.setClockPanelX(x);
                            SAOConfig.setClockPanelY(y);
                        },
                        SAOConfig::save))
                .build();
    }

    private static boolean clockVisible(HudRenderContext ctx) {
        if (!hudOn(ctx) || !SAOConfig.showClock()) {
            return false;
        }
        Minecraft mc = ctx.minecraft();
        return !SAOConfig.clockOnlyInMenu() || (mc != null && mc.screen instanceof SAOMenuScreen);
    }

    private static HudElement skillBar() {
        return HudElement.builder(SKILL_BAR, 500, Component.translatable("saomenu.hud.skill_bar"), ctx ->
                        SaoSkillBar.render(ctx.graphics(), ctx.minecraft(), ctx.width(), ctx.height(), ctx.fade()))
                .visible(HudBuiltins::hudOn)
                .layout(new HudLayoutBinding(
                        40,
                        (mc, w, h, mx, my) -> SaoSkillBar.hitSkillBar(w, h, mx, my),
                        (mc, w, h) -> {
                            int bh = SaoSkillBar.slotSize(h);
                            int bw = SaoSkillBar.barWidth(h, SaoSkillBar.slotCount());
                            return new HudBox(SaoSkillBar.barX(w, bw), SaoSkillBar.barY(h, bh), bw, bh);
                        },
                        SAOConfig::skillBarX,
                        SAOConfig::skillBarY,
                        (mc, w, h, gx, gy, mx, my) -> SaoSkillBar.moveTo(w, h, gx, gy, mx, my),
                        (x, y) -> {
                            SAOConfig.setSkillBarX(x);
                            SAOConfig.setSkillBarY(y);
                        },
                        SAOConfig::save))
                .build();
    }

    private static HudElement vignette() {
        return HudElement.builder(VIGNETTE, 600, Component.translatable("saomenu.hud.vignette"), ctx -> {
                    Player p = ctx.player();
                    if (p == null) {
                        return;
                    }
                    float max = p.getMaxHealth();
                    SAOPlayerPlate.renderLowHpVignette(ctx.graphics(), ctx.width(), ctx.height(),
                            max <= 0f ? 0f : p.getHealth() / max);
                })
                .visible(HudBuiltins::hudOn)
                .build();
    }

    private static HudElement hotbarTooltip() {
        return HudElement.builder(HOTBAR_TOOLTIP, 700, Component.translatable("saomenu.inv.hotbar"), ctx ->
                        SAOHotbarDots.renderHoverTooltip(ctx.graphics(), ctx.minecraft(),
                                ctx.width(), ctx.height(), ctx.player(), ctx.mouseX(), ctx.mouseY()))
                .passes(HudPass.WORLD)
                .visible(HudBuiltins::hudOn)
                .build();
    }

    private static HudElement mapWorld() {
        return HudElement.builder(MAP_WORLD, 800, Component.translatable("saomenu.menu.map"), ctx ->
                        SAOMapPanel.renderHud(ctx.graphics(), ctx.minecraft(), ctx.width(), ctx.height()))
                .passes(HudPass.WORLD)
                .visible(ctx -> SAOMapPanel.isShown() && SAOConfig.mapPinned())
                .build();
    }

    private static HudElement food() {
        return HudElement.builder(FOOD, 900, Component.translatable("saomenu.hud.food"),
                        ctx -> SAOFoodBar.render(ctx.graphics(), ctx.player()))
                .passes()
                .visible(ctx -> SAOConfig.hideVanillaHealth())
                .layout(new HudLayoutBinding(
                        20,
                        (mc, w, h, mx, my) -> SAOFoodBar.hit(w, h, mx, my),
                        (mc, w, h) -> new HudBox(SAOFoodBar.foodX(w), SAOFoodBar.foodY(h),
                                SAOFoodBar.FOOD_W, SAOFoodBar.FOOD_H),
                        SAOConfig::foodPanelX,
                        SAOConfig::foodPanelY,
                        (mc, w, h, gx, gy, mx, my) -> SAOFoodBar.moveTo(w, h, gx, gy, mx, my),
                        (x, y) -> {
                            SAOConfig.setFoodPanelX(x);
                            SAOConfig.setFoodPanelY(y);
                        },
                        SAOConfig::save))
                .build();
    }
}
