package com.sao.saomenu.client.menu;

import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import com.sao.saomenu.ui.theme.ThemeColors;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutBack;
import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.render.SaoDraw.shaderAlpha;
import static com.sao.saomenu.ui.text.SaoText.tr;

/** 登出确认、物品信息弹窗与物品操作钮几何。 */
final class MenuDialogs {

    static final String[] ACT_KEYS = {
            "saomenu.act.equip", "saomenu.act.info", "saomenu.act.drop"};

    private MenuDialogs() {
    }

    private static ThemeColors theme() {
        return SaoTheme.palette();
    }

    static MenuLayout.Rect actionButtonRect(MenuLayout.Rect row, int b) {
        int d = Math.max(8, Math.round(row.h() * 0.85f));
        int cx = row.centerX() + (b - 1) * Math.round(d * 1.18f);
        int cy = row.centerY();
        return new MenuLayout.Rect(cx - d / 2, cy - d / 2, d, d);
    }

    static MenuLayout.Rect dialogRect(int width, int height) {
        int w = Math.min(280, width - 20);
        int h = Math.round(w * 253f / 350f);
        return new MenuLayout.Rect((width - w) / 2, (height - h) / 2, w, h);
    }

    static void render(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                       int mouseX, int mouseY, float globalAlpha, long now) {
        if (s.confirmClose) {
            renderConfirm(g, screen, s, mouseX, mouseY, globalAlpha, now);
        }
        if (s.infoOpen) {
            renderInfo(g, screen, s, mouseX, mouseY, globalAlpha, now);
        }
    }

    private static void renderConfirm(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                                      int mouseX, int mouseY, float alpha, long now) {
        MenuLayout.Rect at = dialogRect(screen.width, screen.height);
        float p = clamp01((now - s.confirmAt) / 160f);
        float sc = 0.1f + 0.9f * easeOutBack(p);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(screen.width / 2f, screen.height / 2f, 0);
        pose.scale(1f, sc, 1f);
        pose.translate(-screen.width / 2f, -screen.height / 2f, 0);

        g.flush();
        RenderSystem.disableDepthTest();
        g.fill(at.x() + 3, at.y() + 3, at.x() + at.w() + 3, at.y() + at.h() + 3, mulAlpha(theme().dialogShadow(), alpha));
        int insX = Math.max(2, Math.round(at.w() * 0.02f));
        int insTop = Math.max(2, Math.round(at.h() * 0.045f));
        int insBot = Math.max(2, Math.round(at.h() * 0.02f));
        g.fill(at.x() + insX, at.y() + insTop, at.x() + at.w() - insX, at.y() + at.h() - insBot,
                mulAlpha(theme().dialogSurface(), alpha));
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, MenuAssets.ALERT, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        Font f = screen.menuFont();
        String title = tr("saomenu.logout.title");
        g.drawString(f, title, at.centerX() - f.width(title) / 2, at.y() + 10,
                mulAlpha(theme().textOnSurface(), alpha), false);
        String msg = tr("saomenu.logout.msg");
        g.drawString(f, msg, at.centerX() - f.width(msg) / 2,
                at.y() + Math.round(at.h() * 0.44f),
                mulAlpha(theme().textOnSurface(), alpha), false);

        int d = 26;
        int by = at.y() + Math.round(at.h() * 0.80f) - d / 2;
        int b1x = at.x() + at.w() / 4 - d / 2;
        int b2x = at.x() + at.w() * 3 / 4 - d / 2;
        boolean h1 = MenuLayout.inCircle(b1x + d / 2, by + d / 2, d / 2, mouseX, mouseY);
        boolean h2 = MenuLayout.inCircle(b2x + d / 2, by + d / 2, d / 2, mouseX, mouseY);
        RenderSystem.enableBlend();
        g.blit(h1 ? MenuAssets.BTN_OK_HOVER : MenuAssets.BTN_OK, b1x, by, 0, 0, d, d, d, d);
        g.blit(h2 ? MenuAssets.BTN_CANCEL_HOVER : MenuAssets.BTN_CANCEL, b2x, by, 0, 0, d, d, d, d);
        shaderAlpha(1f);
        pose.popPose();
    }

    private static void renderInfo(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                                   int mouseX, int mouseY, float alpha, long now) {
        ItemStack st = s.infoStack;
        if (st == null || st.isEmpty()) {
            s.infoOpen = false;
            return;
        }
        MenuLayout.Rect at = dialogRect(screen.width, screen.height);
        float p = clamp01((now - s.infoAt) / 160f);
        float sc = 0.1f + 0.9f * easeOutBack(p);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(screen.width / 2f, screen.height / 2f, 0);
        pose.scale(1f, sc, 1f);
        pose.translate(-screen.width / 2f, -screen.height / 2f, 0);

        g.flush();
        RenderSystem.disableDepthTest();
        g.fill(at.x() + 3, at.y() + 3, at.x() + at.w() + 3, at.y() + at.h() + 3, mulAlpha(theme().dialogShadow(), alpha));
        int insX = Math.max(2, Math.round(at.w() * 0.02f));
        int insTop = Math.max(2, Math.round(at.h() * 0.045f));
        int insBot = Math.max(2, Math.round(at.h() * 0.02f));
        g.fill(at.x() + insX, at.y() + insTop, at.x() + at.w() - insX, at.y() + at.h() - insBot,
                mulAlpha(theme().dialogSurface(), alpha));
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, MenuAssets.ALERT, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        Font f = screen.menuFont();
        String title = st.getHoverName().getString();
        g.drawString(f, title, at.centerX() - f.width(title) / 2, at.y() + 8,
                mulAlpha(st.getRarity().color.getColor(), alpha), false);

        List<String> lines = buildInfoLines(st);
        int ly = at.y() + Math.round(at.h() * 0.22f);
        int maxLines = 7;
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            g.drawString(f, lines.get(i), at.centerX() - f.width(lines.get(i)) / 2,
                    ly + i * 11, mulAlpha(theme().textOnSurface(), alpha), false);
        }

        int d = 26;
        int bx = at.centerX() - d / 2;
        int by = at.y() + Math.round(at.h() * 0.82f) - d / 2;
        boolean hv = MenuLayout.inCircle(at.centerX(), by + d / 2, d / 2, mouseX, mouseY);
        RenderSystem.enableBlend();
        g.blit(hv ? MenuAssets.BTN_OK_HOVER : MenuAssets.BTN_OK, bx, by, 0, 0, d, d, d, d);
        pose.popPose();
    }

    static List<String> buildInfoLines(ItemStack st) {
        List<String> lines = new ArrayList<>();
        net.minecraft.world.item.Item item = st.getItem();
        String type;
        if (item instanceof net.minecraft.world.item.ArmorItem) {
            type = tr("saomenu.info.armor");
        } else if (item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            type = tr("saomenu.info.weapon");
        } else if (item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.DiggerItem) {
            type = tr("saomenu.info.tool");
        } else {
            type = tr("saomenu.info.item");
        }
        lines.add(tr("saomenu.info.type") + ": " + type);
        lines.add(tr("saomenu.info.count") + ": " + st.getCount());
        if (st.isDamageableItem()) {
            lines.add(tr("saomenu.info.durability") + ": "
                    + (st.getMaxDamage() - st.getDamageValue()) + " / " + st.getMaxDamage());
        }
        var ench = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(st);
        int shown = 0;
        for (var e : ench.entrySet()) {
            if (shown >= 4) {
                lines.add("… +" + (ench.size() - shown));
                break;
            }
            lines.add(tr("saomenu.info.enchant") + ": " + e.getKey().getFullname(e.getValue()).getString());
            shown++;
        }
        lines.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
        return lines;
    }
}
