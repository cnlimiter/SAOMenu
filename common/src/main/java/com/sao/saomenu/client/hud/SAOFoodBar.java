package com.sao.saomenu.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

/**
 * 居中原版饥饿条 + 氧气泡。几何与拖动命中只在这里,绘制仍由平台 mixin 经
 * {@link SAOHud#renderVanillaFoodCentered} 接入。
 */
public final class SAOFoodBar {

    /** 饥饿条整条宽度:10 格 × 8px 步进 + 末格多出的 1px。 */
    public static final int FOOD_W = 81;
    /** 饥饿条高度(单行图标)。 */
    public static final int FOOD_H = 9;
    /** 拖动命中框上探量:把上方氧气泡行也算进可抓范围。 */
    private static final int FOOD_HIT_TOP = 10;

    private static final ResourceLocation GUI_ICONS = new ResourceLocation("textures/gui/icons.png");

    private SAOFoodBar() {
    }

    public static int foodX(int screenW) {
        float fx = Mth.clamp(SAOConfig.foodPanelX(), 0f, 1f);
        return Math.round(fx * (screenW - FOOD_W));
    }

    /** 饥饿条顶缘 Y;锚点 1.0 落在原版行高(屏底上方 39px)。 */
    public static int foodY(int screenH) {
        float fy = Mth.clamp(SAOConfig.foodPanelY(), 0f, 1f);
        int lo = FOOD_HIT_TOP;
        int hi = screenH - 39;
        return Math.round(lo + fy * (hi - lo));
    }

    public static boolean hit(int screenW, int screenH, int mx, int my) {
        if (!SAOConfig.hideVanillaHealth()) {
            return false;
        }
        int fx = foodX(screenW);
        int fy = foodY(screenH) - FOOD_HIT_TOP;
        return mx >= fx && mx < fx + FOOD_W && my >= fy && my < fy + FOOD_H + FOOD_HIT_TOP;
    }

    static void moveTo(int screenW, int screenH, float grabFx, float grabFy, int mx, int my) {
        float fx = (mx - grabFx * FOOD_W) / (float) Math.max(1, screenW - FOOD_W);
        int lo = FOOD_HIT_TOP;
        int hi = screenH - 39;
        float fy = (my - grabFy * FOOD_H - lo) / (float) Math.max(1, hi - lo);
        SAOConfig.setFoodPanelX(fx);
        SAOConfig.setFoodPanelY(fy);
    }

    /**
     * 贴图 u/v 与原版 1.20.1 Gui.renderPlayerHealth 字节码逐项核对:
     * 空格 (16,27)、满格 (52,27)、半格 (61,27);饥饿药水效果时
     * 空格 u+117、满格 u+36、半格 u+36。氧气泡 (16,18)、破裂泡 (25,18),
     * 画在饥饿条上方一行,数量公式 ceil((air-2)*10/max) 同原版。
     */
    public static void render(GuiGraphics g, Player p) {
        int w = g.guiWidth();
        int h = g.guiHeight();
        int x0 = foodX(w);
        int y = foodY(h);
        int food = p.getFoodData().getFoodLevel();
        boolean hungerFx = p.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER);
        RenderSystem.enableBlend();
        for (int i = 0; i < 10; i++) {
            int x = x0 + i * 8;
            g.blit(GUI_ICONS, x, y, 16 + (hungerFx ? 117 : 0), 27, 9, 9);
            int v = food - i * 2;
            if (v >= 2) {
                g.blit(GUI_ICONS, x, y, 52 + (hungerFx ? 36 : 0), 27, 9, 9);
            } else if (v == 1) {
                g.blit(GUI_ICONS, x, y, 61 + (hungerFx ? 36 : 0), 27, 9, 9);
            }
        }
        int maxAir = p.getMaxAirSupply();
        int air = Math.min(p.getAirSupply(), maxAir);
        if (!p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER) && air >= maxAir) {
            return;
        }
        int yAir = y - 10;
        int full = Mth.ceil((air - 2) * 10.0D / maxAir);
        int total = Mth.ceil(air * 10.0D / maxAir);
        for (int i = 0; i < total; i++) {
            g.blit(GUI_ICONS, x0 + i * 8, yAir, i < full ? 16 : 25, 18, 9, 9);
        }
    }
}
