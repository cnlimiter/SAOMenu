package com.sao.saomenu.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;

/**
 * 底部圆点物品栏:第 1 个圆点是副手(与主栏隔开一档,不参与选中高亮),
 * 其后 9 个圆点对应快捷栏槽位 0..8,选中槽位主题色高亮;圆点内渲染真实物品图标。
 */
public final class SAOHotbarDots {

    private static final ResourceLocation TEX_DOT =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/dot.png");

    private SAOHotbarDots() {
    }

    public static void render(GuiGraphics g, int screenW, int screenH, Player player, float alpha) {
        if (player == null || !SAOConfig.hideHotbar()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        for (int i = 0; i < MenuLayout.DOT_COUNT; i++) {
            boolean offhand = i == 0;
            int slot = i - 1;
            boolean active = !offhand && slot == Mth.clamp(player.getInventory().selected, 0, 8);
            int d = MenuLayout.dotSize(screenH);
            int x = MenuLayout.dotCenterX(screenW, screenH, i) - d / 2;
            int y = MenuLayout.dotCenterY(screenH) - d / 2;
            if (active) {
                SaoDraw.tint(SaoTheme.accent(), alpha);
                RenderSystem.enableBlend();
                g.blit(TEX_DOT, x, y, 0, 0, d, d, d, d);
            } else {
                RenderSystem.enableBlend();
                SaoDraw.shaderAlpha(alpha);
                g.blit(TEX_DOT, x, y, 0, 0, d, d, d, d);
            }
            SaoDraw.shaderAlpha(1f);

            ItemStack stack = offhand ? player.getOffhandItem() : player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int isz = Mth.clamp(Math.round(d * 0.62f), 4, Math.max(4, Math.min(16, d - 2)));
            float s = isz / 16f;
            boolean ownCount = s < 0.75f && stack.getCount() > 1;
            g.pose().pushPose();
            g.pose().translate(x + d / 2f, y + d / 2f, 120f);
            g.pose().scale(s, s, 1f);
            g.renderItem(stack, -8, -8);
            if (ownCount) {
                ItemStack noCount = stack.copy();
                noCount.setCount(1);
                g.renderItemDecorations(com.sao.saomenu.api.SaoUi.bodyFont(), noCount, -8, -8);
            } else {
                g.renderItemDecorations(com.sao.saomenu.api.SaoUi.bodyFont(), stack, -8, -8);
            }
            g.pose().popPose();
            if (ownCount) {
                String cnt = String.valueOf(stack.getCount());
                var font = com.sao.saomenu.api.SaoUi.bodyFont();
                float ns = Math.max(0.45f, d / 16f);
                float tx = x + d - 3;
                float ty = y + d + 2 - font.lineHeight * ns;
                g.pose().pushPose();
                g.pose().translate(0, 0, 300f);
                SaoDraw.drawScaled(g, font, cnt, tx, ty, ns,
                        mulAlpha(0xFFFFFFFF, alpha), true);
                g.pose().popPose();
            }
        }
    }

    static void renderHoverTooltip(GuiGraphics g, Minecraft mc, int screenW, int screenH,
                                   Player player, int mx, int my) {
        if (player == null || !SAOConfig.hideHotbar()) {
            return;
        }
        for (int i = 0; i < MenuLayout.DOT_COUNT; i++) {
            if (MenuLayout.inDot(screenW, screenH, i, mx, my)) {
                ItemStack stack = i == 0 ? player.getOffhandItem() : player.getInventory().getItem(i - 1);
                if (!stack.isEmpty()) {
                    g.renderTooltip(com.sao.saomenu.api.SaoUi.bodyFont(), stack, mx, my);
                }
                break;
            }
        }
    }
}
