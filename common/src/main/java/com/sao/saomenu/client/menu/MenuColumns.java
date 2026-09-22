package com.sao.saomenu.client.menu;

import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.text.SAOScrollText;
import com.sao.saomenu.ui.text.SaoText;
import com.sao.saomenu.ui.theme.SaoTheme;
import com.sao.saomenu.api.theme.ThemeColors;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.sao.saomenu.ui.animation.SaoMotion.ITEM_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.ITEM_STAGGER_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.PANEL_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.UNFOLD_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.UNFOLD_STAGGER_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutBack;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic;
import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.render.SaoDraw.shaderAlpha;
import static com.sao.saomenu.ui.text.SaoText.tr;


/** 主按钮列、一/二/三级菜单项与置顶拖拽幽灵。 */
final class MenuColumns {

    private MenuColumns() {
    }

    private static ThemeColors theme() {
        return SaoTheme.palette();
    }

    static void renderMainButtons(GuiGraphics g, SAOMenuScreen screen, MenuSession s, float globalAlpha) {
        List<SaoPanel> ps = MenuSession.panels();
        int vis = MenuLayout.mainVisibleCount(screen.height, ps.size());
        int stackY = s.buttonY(s.mainScroll, screen.height);
        long unfoldNow = MenuSession.now();
        for (int v = 0; v < vis; v++) {
            int i = s.mainScroll + v;
            if (i < 0 || i >= ps.size()) {
                break;
            }
            boolean active = s.isActive(i);
            int d = MenuLayout.btnSize(screen.height);
            int cx = s.baseAnchorX;
            float p = s.closing ? 1f
                    : clamp01((unfoldNow - s.openedAt - v * UNFOLD_STAGGER_MS) / (float) UNFOLD_MS);
            float eased = easeOutBack(p);
            int cy = Math.round(stackY + (s.buttonY(i, screen.height) - stackY) * eased);
            boolean dim = s.mainTouched && !active;
            float a = globalAlpha * (dim ? 0.45f : 1f);
            ResourceLocation btnTex = s.mainPressing(i) ? MenuAssets.BTN_PRESS
                    : active ? MenuAssets.BTN_HOVER : MenuAssets.BTN_NORMAL;
            shaderAlpha(a);
            RenderSystem.enableBlend();
            g.blit(btnTex, cx - d / 2, cy - d / 2, 0, 0, d, d, d, d);
            shaderAlpha(1f);
            ResourceLocation glyph = ps.get(i).icons().pick(active);
            int pad = Math.max(2, Math.round(d * 0.22f));
            int isz = d - pad * 2;
            shaderAlpha(a);
            RenderSystem.enableBlend();
            g.blit(glyph, cx - d / 2 + pad, cy - d / 2 + pad, 0, 0, isz, isz, isz, isz);
            shaderAlpha(1f);
        }
    }

    static void renderItems(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                            float globalAlpha, long now) {
        int main = s.activeMain();
        List<MenuEntry> items = s.activeItems(main);
        List<MenuEntry> win = s.windowedItems(items, screen.height);
        int anchorY = s.buttonY(main, screen.height);
        long base = s.panelAt == Long.MIN_VALUE ? now - PANEL_MS : s.panelAt;
        Font font = screen.menuFont();

        renderIndicator(g, screen, s, win.size(), anchorY, s.baseAnchorX, globalAlpha, true);

        for (int i = 0; i < win.size(); i++) {
            int real = s.itemScroll + i;
            float p = clamp01((now - base - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
            if (p <= 0f) {
                continue;
            }
            float eased = easeOutCubic(p);
            MenuLayout.Rect rect = MenuLayout.menuItemRectAt(screen.width, screen.height, win.size(),
                    s.baseAnchorX, anchorY, i);
            int slide = Math.round((1f - eased) * rect.w() * 0.45f);
            MenuLayout.Rect at = new MenuLayout.Rect(rect.x() - slide, rect.y(), rect.w(), rect.h());
            boolean dim = s.expandedItem != -1 && s.expandedItem != real;
            renderMenuItem(g, font, s, at, win.get(i).label(), win.get(i).icon(),
                    s.hoverItem == i, globalAlpha * eased * (dim ? 0.45f : 1f),
                    s.itemPressing(0, i), win.get(i).stack());
        }

        int shown = s.visibleChildrenItem(items);
        if (shown >= 0 && items.get(shown).children() != null) {
            if (s.childOwner != shown) {
                s.childOwner = shown;
                s.childAt = now;
            }
            List<MenuEntry> children = s.windowedChildren(items.get(shown).children(), screen.height);
            int childAnchor = s.childAnchorY(items, shown, screen.width, screen.height);
            boolean childDim = s.equipOwner != -1;
            for (int i = 0; i < children.size(); i++) {
                float p = clamp01((now - s.childAt - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
                if (p <= 0f) {
                    continue;
                }
                float eased = easeOutCubic(p);
                MenuLayout.Rect rect = MenuLayout.childItemRectAt(screen.width, screen.height, children.size(),
                        s.baseAnchorX, childAnchor, i);
                int slide = Math.round((1f - eased) * rect.w() * 0.45f);
                MenuLayout.Rect at = new MenuLayout.Rect(rect.x() - slide, rect.y(), rect.w(), rect.h());
                boolean dim = childDim && s.equipOwner != (s.childScroll + i);
                renderMenuItem(g, font, s, at, children.get(i).label(), children.get(i).icon(),
                        s.hoverChild == i || (s.actionMenuOpen && s.actionRow == i),
                        globalAlpha * eased * (dim ? 0.45f : 1f),
                        s.itemPressing(1, i), children.get(i).stack());
            }
            if (s.actionMenuOpen && s.actionRow >= 0 && s.actionRow < children.size()
                    && children.get(s.actionRow).stack() != null) {
                MenuLayout.Rect rowA = MenuLayout.childItemRectAt(screen.width, screen.height,
                        children.size(), s.baseAnchorX, childAnchor, s.actionRow);
                g.flush();
                RenderSystem.disableDepthTest();
                long age = now - s.actionAt;
                for (int b = 0; b < 3; b++) {
                    float bp = clamp01((age - b * 45) / 150f);
                    float bs = 0.2f + 0.8f * easeOutBack(bp);
                    MenuLayout.Rect full = MenuDialogs.actionButtonRect(rowA, b);
                    int ds = Math.max(2, Math.round(full.w() * bs));
                    boolean hv = s.hoverAction == b;
                    ResourceLocation t = b == 0 ? (hv ? MenuAssets.ACT_EQUIP_H : MenuAssets.ACT_EQUIP)
                            : b == 1 ? (hv ? MenuAssets.ACT_INFO_H : MenuAssets.ACT_INFO)
                            : (hv ? MenuAssets.ACT_DROP_H : MenuAssets.ACT_DROP);
                    shaderAlpha(globalAlpha);
                    RenderSystem.enableBlend();
                    g.blit(t, full.centerX() - ds / 2, full.centerY() - ds / 2, 0, 0, ds, ds, ds, ds);
                    shaderAlpha(1f);
                    if (hv) {
                        String lbl = SaoText.tr(MenuDialogs.ACT_KEYS[b]);
                        g.drawString(font, lbl, full.centerX() - font.width(lbl) / 2,
                                full.y() - 11, mulAlpha(theme().highlight(), globalAlpha), true);
                    }
                }
            }
            int equipTarget = s.equipTargetIndex(items, shown);
            if (equipTarget >= 0) {
                if (equipTarget != s.equipShownOwner) {
                    s.equipShownOwner = equipTarget;
                    s.equipAt = now;
                }
                renderEquipColumn(g, screen, s, font,
                        s.equipKindAt(items, shown, equipTarget),
                        s.equipAnchorY(items, shown, equipTarget, screen.width, screen.height),
                        globalAlpha, now);
            } else {
                s.equipShownOwner = -1;
            }
        } else {
            if (s.childOwner != -1) {
                s.childOwner = -1;
            }
            s.equipShownOwner = -1;
        }
    }

    static void renderTooltip(GuiGraphics g, SAOMenuScreen screen, MenuSession s, int mouseX, int mouseY) {
        if (s.infoOpen || s.confirmClose || s.actionMenuOpen) {
            return;
        }
        int mainTip = s.activeMain();
        List<MenuEntry> itemsTip = mainTip >= 0 ? s.activeItems(mainTip) : null;
        if (itemsTip == null || s.hoverChild < 0) {
            return;
        }
        int shownTip = s.visibleChildrenItem(itemsTip);
        if (shownTip < 0 || itemsTip.get(shownTip).children() == null) {
            return;
        }
        List<MenuEntry> all = itemsTip.get(shownTip).children();
        int real = s.childScroll + s.hoverChild;
        if (real < 0 || real >= all.size()) {
            return;
        }
        ItemStack st = all.get(real).stack();
        if (st != null && !st.isEmpty()) {
            g.renderTooltip(screen.menuFont(), st, mouseX, mouseY);
        }
    }

    static void renderPinGhost(GuiGraphics g, SAOMenuScreen screen, MenuSession s, MenuTransform xform, long now) {
        if (s.pinDragFrom < 0 || s.pinDragStack == null) {
            return;
        }
        float t = clamp01((now - s.pinDragAt) / 120f);
        float scale = 0.6f + 0.4f * easeOutCubic(t);
        int size = Math.round(20 * scale);

        xform.toLocal(s.pinDragMx, s.pinDragMy);
        int target = s.pinRowAt(xform, screen.width, screen.height);
        if (target >= 0 && target != s.pinDragFrom && s.selectedMain >= 0) {
            List<MenuEntry> items = s.activeItems(s.selectedMain);
            int shown = s.visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> children = s.windowedChildren(items.get(shown).children(), screen.height);
                int childAnchor = s.childAnchorY(items, shown, screen.width, screen.height);
                MenuLayout.Rect at = MenuLayout.childItemRectAt(screen.width, screen.height,
                        children.size(), s.baseAnchorX, childAnchor, target);
                MenuLayout.Rect scr = xform.boxToScreen(at.x(), at.y() - 1, at.w(), 2);
                g.pose().pushPose();
                g.pose().translate(0, 0, 400f);
                g.fill(scr.x(), scr.y(), scr.x() + scr.w(), scr.y() + scr.h(), SaoTheme.accent());
                g.pose().popPose();
            }
        }

        g.pose().pushPose();
        g.pose().translate(s.pinDragMx + 8f, s.pinDragMy + 8f, 400f);
        g.pose().scale(size / 16f, size / 16f, 1f);
        g.renderItem(s.pinDragStack, -8, -8);
        g.pose().popPose();
    }

    private static void renderEquipColumn(GuiGraphics g, SAOMenuScreen screen, MenuSession s, Font font,
                                          MenuEntry.EquipKind kind, int anchorY, float globalAlpha, long now) {
        List<MenuSession.EquipEntry> entries = s.equipEntries(kind, net.minecraft.client.Minecraft.getInstance().player);
        int count = entries.size();
        for (int i = 0; i < count; i++) {
            float p = clamp01((now - s.equipAt - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
            if (p <= 0f) {
                continue;
            }
            float eased = easeOutCubic(p);
            MenuLayout.Rect rect = MenuLayout.equipItemRectAt(screen.width, screen.height, count,
                    s.baseAnchorX, anchorY, i);
            int slide = Math.round((1f - eased) * rect.w() * 0.45f);
            MenuLayout.Rect at = new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h());
            renderEquipItem(g, font, at, entries.get(i), s.hoverEquip == i, globalAlpha * eased);
        }
    }

    private static void renderEquipItem(GuiGraphics g, Font f, MenuLayout.Rect at,
                                        MenuSession.EquipEntry e, boolean hovered, float alpha) {
        SaoDraw.roundedRect(g, at.x() + 2, at.y() + 2, at.w(), at.h(),
                Math.max(2, Math.round(at.h() * 0.12f)), mulAlpha(theme().shadow(), alpha));
        if (hovered) {
            SaoDraw.tint(SaoTheme.accent(), alpha);
            SaoDraw.blendedBlit(g, MenuAssets.LIST_HOVER, at.x(), at.y(), at.w(), at.h());
        } else {
            RenderSystem.enableBlend();
            shaderAlpha(alpha * 0.92f);
            g.blit(MenuAssets.LIST_NORMAL, at.x(), at.y(), 0, 0, at.w(), at.h(), at.w(), at.h());
        }
        shaderAlpha(1f);

        int iconSize = Math.round(at.h() * 0.78f);
        int iconX = at.x() + Math.round(at.h() * 0.18f);
        int iconY = at.y() + (at.h() - iconSize) / 2;
        if (!e.empty()) {
            g.pose().pushPose();
            g.pose().translate(iconX + iconSize / 2f, iconY + iconSize / 2f, 120f);
            g.pose().scale(iconSize / 16f, iconSize / 16f, 1f);
            g.renderItem(e.stack(), -8, -8);
            g.pose().popPose();
        }

        net.minecraft.network.chat.Component label = e.empty()
                ? net.minecraft.network.chat.Component.translatable("saomenu.equip.empty") : e.stack().getHoverName();
        int textX = iconX + iconSize + Math.round(at.h() * 0.18f);
        int maxW = at.x() + at.w() - textX - 6;
        int color = e.empty() ? mulAlpha(theme().textMuted(), alpha)
                : hovered ? mulAlpha(theme().textOnAccent(), alpha) : mulAlpha(theme().textOnSurface(), alpha);
        drawScrollingLabel(g, f, label, textX, at.y(), at.h(), maxW, color, hovered);
    }

    private static void renderIndicator(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                                        int count, int anchorY, int anchorX, float alpha, boolean mainColumn) {
        int itemH = MenuLayout.itemH(screen.height);
        int step = itemH + MenuLayout.itemGap(screen.height);
        int totalH = (count - 1) * step + itemH;
        int top = MenuLayout.clampedAnchorY(screen.height, count, anchorY) - totalH / 2;
        int colX = MenuLayout.itemColumnXAt(anchorX, screen.height);
        int indH = totalH + Math.max(8, itemH * 2);
        int indW = Math.max(6, Math.round(indH * 28f / 230f));
        int x = colX - MenuLayout.arrowGap(screen.height) / 2 - indW / 2 - 1;
        int y = top - (indH - totalH) / 2;
        shaderAlpha(alpha);
        RenderSystem.enableBlend();
        g.blit(MenuAssets.INDICATOR, x, y, 0, 0, indW, indH, indW, indH);
        shaderAlpha(1f);
        if (mainColumn) {
            int btnRight = anchorX + MenuLayout.btnSize(screen.height) / 2;
            int ringD = Math.max(4, Math.round(itemH * 0.30f));
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(MenuAssets.RING, btnRight + 1, anchorY - ringD / 2, 0, 0, ringD, ringD, ringD, ringD);
            shaderAlpha(1f);
        }
    }

    private static void renderMenuItem(GuiGraphics g, Font f, MenuSession s, MenuLayout.Rect at,
                                       Component labelComp, ResourceLocation icon, boolean hovered, float alpha,
                                       boolean pressed, ItemStack stack) {
        int r = Math.max(2, Math.round(at.h() * 0.12f));
        SaoDraw.roundedRect(g, at.x() + 2, at.y() + 2, at.w(), at.h(), r, mulAlpha(theme().shadow(), alpha));
        if (pressed) {
            SaoDraw.blendedBlit(g, MenuAssets.LIST_PRESS, at.x(), at.y(), at.w(), at.h());
        } else if (hovered) {
            SaoDraw.tint(SaoTheme.accent(), alpha);
            SaoDraw.blendedBlit(g, MenuAssets.LIST_HOVER, at.x(), at.y(), at.w(), at.h());
        } else {
            RenderSystem.enableBlend();
            shaderAlpha(alpha * 0.9f);
            g.blit(MenuAssets.LIST_NORMAL, at.x(), at.y(), 0, 0, at.w(), at.h(), at.w(), at.h());
        }
        shaderAlpha(1f);

        int iconSize = Math.round(at.h() * 0.72f);
        int iconY = at.y() + (at.h() - iconSize) / 2;
        int iconX = at.x() + Math.round(at.h() * 0.18f);
        if (stack != null && !stack.isEmpty()) {
            if (!s.infoOpen && !s.confirmClose) {
                g.pose().pushPose();
                g.pose().translate(iconX + iconSize / 2f, iconY + iconSize / 2f, 120f);
                g.pose().scale(iconSize / 16f, iconSize / 16f, 1f);
                g.renderItem(stack, -8, -8);
                g.pose().popPose();
            }
        } else if (icon != null) {
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(icon, iconX, iconY, 0, 0, iconSize, iconSize, iconSize, iconSize);
            shaderAlpha(1f);
        }

        if (stack != null && !stack.isEmpty() && SaoPanels.pinOrderOf(stack) != Integer.MAX_VALUE) {
            int tri = Math.max(3, Math.round(at.h() * 0.30f));
            g.pose().pushPose();
            g.pose().translate(0, 0, 260f);
            int accent = SaoTheme.accent();
            for (int row = 0; row < tri; row++) {
                int wRow = tri - row;
                g.fill(at.x() + 1, at.y() + 1 + row, at.x() + 1 + wRow, at.y() + 2 + row,
                        mulAlpha(accent, alpha));
            }
            for (int row = 0; row < tri; row++) {
                int xr = at.x() + (tri - row);
                g.fill(xr, at.y() + 1 + row, xr + 1, at.y() + 2 + row, mulAlpha(theme().highlight(), alpha));
            }
            g.pose().popPose();
        }

        int textX = at.x() + Math.round(at.h() * 0.18f) + iconSize + Math.round(at.h() * 0.22f);
        int maxW = at.x() + at.w() - textX - Math.round(at.h() * 0.16f);
        int color = hovered ? mulAlpha(theme().textOnAccent(), alpha) : mulAlpha(theme().textOnSurface(), alpha);
        drawScrollingLabel(g, f, labelComp, textX, at.y(), at.h(), maxW, color, hovered);
    }

    private static void drawScrollingLabel(GuiGraphics g, Font f, net.minecraft.network.chat.Component label,
                                           float x, float rowY, float rowH, float maxW, int color, boolean hovered) {
        if (maxW <= 0f || rowH <= 0f) {
            return;
        }
        float sc = SaoDraw.fitScale(f, rowH);
        int cap = Math.max(1, Math.round(maxW / sc));
        int textW = f.width(label);
        float th = f.lineHeight * sc;
        float y = rowY + (rowH - th) / 2f;
        if (textW <= cap) {
            SaoDraw.drawScaled(g, f, label.getVisualOrderText(), x, y, sc, color, false);
            return;
        }
        if (!hovered) {
            SaoDraw.drawScaled(g, f, SaoDraw.clipTo(f, label, cap), x, y, sc, color, false);
            return;
        }
        int shift = SAOScrollText.offset(textW, cap, MenuSession.now(), label.hashCode());
        // 1.20.1 GuiGraphics scissor coordinates are screen GUI pixels, not pose-local pixels.
        var matrix = g.pose().last().pose();
        float sx = matrix.m00() * x + matrix.m10() * rowY + matrix.m30();
        float sy = matrix.m01() * x + matrix.m11() * rowY + matrix.m31();
        float wx = matrix.m00() * maxW;
        float hx = matrix.m10() * rowH;
        float wy = matrix.m01() * maxW;
        float hy = matrix.m11() * rowH;
        g.enableScissor(Mth.floor(sx + Math.min(0, wx) + Math.min(0, hx)),
                Mth.floor(sy + Math.min(0, wy) + Math.min(0, hy)),
                Mth.ceil(sx + Math.max(0, wx) + Math.max(0, hx)),
                Mth.ceil(sy + Math.max(0, wy) + Math.max(0, hy)));
        try {
            SaoDraw.drawScaled(g, f, label.getVisualOrderText(), x - shift * sc, y, sc, color, false);
        } finally {
            g.disableScissor();
        }
    }
}
