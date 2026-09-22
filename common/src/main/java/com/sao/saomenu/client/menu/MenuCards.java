package com.sao.saomenu.client.menu;

import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.menu.MenuContext;
import com.sao.saomenu.api.menu.SideCard;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import com.sao.saomenu.api.theme.ThemeColors;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;

import java.util.ArrayList;
import java.util.List;

import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.render.SaoDraw.shaderAlpha;
import static com.sao.saomenu.ui.text.SaoText.tr;

/** Builtin side cards. Addon panels supply their own {@link SideCard}. */
final class MenuCards {

    private MenuCards() {
    }

    static SideCard playerCard() {
        return MenuCards::renderPlayerCard;
    }

    static SideCard teamCard() {
        return MenuCards::renderTeamCard;
    }

    static SideCard friendsCard() {
        return MenuCards::renderFriendsCard;
    }
    static void render(GuiGraphics g, SAOMenuScreen screen, MenuSession s,
                       int main, int mouseX, int mouseY, float globalAlpha, long now) {
        int anchorY = s.buttonY(main, screen.height);
        float p = s.panelAt == Long.MIN_VALUE ? 1f
                : com.sao.saomenu.ui.animation.SaoMotion.clamp01(
                (now - s.panelAt) / (float) com.sao.saomenu.ui.animation.SaoMotion.PANEL_MS);
        float eased = com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic(p);
        float alpha = globalAlpha * p;

        var panel = MenuSession.panelAt(main);
        if (panel == null || panel.sideCard() == null) {
            return;
        }
        MenuLayout.Rect rect = MenuLayout.cardRectAt(screen.width, screen.height, s.baseAnchorX, anchorY);
        int slide = Math.round((1f - eased) * rect.w() * 0.35f);
        MenuLayout.Rect at = new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h());
        UiRect clip = screen.localBoxToScreen(at.x(), at.y(), at.w(), at.h());
        if (clip.width() > 0 && clip.height() > 0) {
            g.flush();
            g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
            try {
                panel.sideCard().render(g, MenuContext.of(screen), MenuRects.ui(at), eased, alpha, mouseX, mouseY);
                g.flush();
            } finally {
                g.disableScissor();
            }
        }
        renderArrowRight(g, s, at, anchorY, alpha, screen.height);
    }

    private static ThemeColors theme() {
        return SaoTheme.palette();
    }

    private static void renderPlayerCard(GuiGraphics g, MenuContext ctx, UiRect bounds, float eased, float alpha,
                                         int mouseX, int mouseY) {
        Minecraft mc = ctx.minecraft();
        Font f = mc.font;
        MenuLayout.Rect at = MenuRects.local(bounds);

        ItemStack held = mc.player != null ? mc.player.getInventory().getSelected() : ItemStack.EMPTY;
        boolean hasHeld = !held.isEmpty();
        int split = at.h() / 2;
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, MenuAssets.PANEL, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        String name = playerName(mc);
        float headH = Math.max(8f, at.h() * 0.07f);
        float nameS = SaoDraw.fitScale(f, headH, 0.80f);
        SaoDraw.drawCentered(g, f, SaoDraw.clipTo(f, name, Math.round(at.w() * 0.80f / nameS)),
                at.centerX(), at.y() + headH * 0.55f, nameS,
                mulAlpha(theme().textOnSurface(), alpha), false);
        int lineY = Math.round(at.y() + headH + 2);
        g.fill(at.x() + at.w() / 10, lineY, at.x() + at.w() - at.w() / 10, lineY + 1, mulAlpha(theme().divider(), alpha));

        if (hasHeld) {
            int isz = 14;
            int ix = at.x() + 8;
            int iy = at.y() + 5;
            int accent = SaoTheme.accent();
            g.fill(ix, iy, ix + isz, iy + isz, mulAlpha(theme().surfaceSlot(), alpha));
            g.fill(ix, iy, ix + isz, iy + 1, mulAlpha(accent, alpha));
            g.fill(ix, iy + isz - 1, ix + isz, iy + isz, mulAlpha(accent, alpha));
            g.fill(ix, iy, ix + 1, iy + isz, mulAlpha(accent, alpha));
            g.fill(ix + isz - 1, iy, ix + isz, iy + isz, mulAlpha(accent, alpha));
            g.pose().pushPose();
            g.pose().translate(ix + isz / 2f, iy + isz / 2f, 120f);
            g.pose().scale(isz / 16f, isz / 16f, 1f);
            g.renderItem(held, -8, -8);
            g.pose().popPose();
        }

        int areaTop = lineY + 2;
        int areaH = Math.max(8, split - (areaTop - at.y()) - 2);
        if (mc.player != null) {
            int k = Math.max(6, Math.round(areaH / 1.95f));
            int ax = at.centerX();
            int feetY = areaTop + areaH - 2;
            int halfW = Math.round(k * 0.8f);
            float dx = ax - mouseX;
            float dy = feetY - mouseY;
            UiRect clip = ctx.host().localBoxToScreen(ax - halfW, areaTop, halfW * 2, areaH);
            g.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
            shaderAlpha(alpha);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, ax, feetY, k, dx, dy, mc.player);
            shaderAlpha(1f);
            g.disableScissor();
        } else {
            int sh = areaH;
            int sw = Math.max(6, Math.round(sh * (64f / 96f)));
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(MenuAssets.SILHOUETTE, at.centerX() - sw / 2, areaTop, 0, 0, sw, sh, sw, sh);
            shaderAlpha(1f);
        }

        Player p = mc.player;
        if (p != null) {
            int statsTop = at.y() + split;
            int statsH = Math.max(8, at.y() + at.h() - statsTop - 2);
            List<String> stats = new ArrayList<>();
            stats.add(tr("saomenu.stat.health", trim(p.getHealth()), trim(p.getMaxHealth())));
            stats.add(tr("saomenu.stat.level", p.experienceLevel));
            if (hasHeld) {
                stats.add(tr("saomenu.stat.held", held.getHoverName().getString()));
            }
            stats.add(tr("saomenu.stat.experience", Math.round(p.experienceProgress * 100.0f)));
            stats.add(tr("saomenu.stat.strength", trim((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE))));
            stats.add(tr("saomenu.stat.agility", trim((float) p.getAttributeValue(Attributes.MOVEMENT_SPEED))));
            stats.add(tr("saomenu.stat.armor", p.getArmorValue()));
            int keep = SaoDraw.rowsKept(statsH, stats.size(), 8);
            if (stats.size() > keep) {
                stats = new ArrayList<>(stats.subList(0, keep));
            }
            float lineH = statsH / (float) Math.max(1, stats.size());
            for (int i = 0; i < stats.size(); i++) {
                SaoDraw.drawInRow(g, f, stats.get(i), at.x() + 6, statsTop + i * lineH, lineH,
                        at.w() - 12, mulAlpha(theme().textOnSurface(), alpha), false);
            }
        }
    }

    private static void renderTeamCard(GuiGraphics g, MenuContext ctx, UiRect bounds, float eased, float alpha,
                                       int mouseX, int mouseY) {
        Minecraft mc = ctx.minecraft();
        List<String> rows = new ArrayList<>();
        String title = tr("saomenu.party");
        String subtitle = null;
        String footer = tr("saomenu.panel.team_members", 0);
        Player p = mc.player;
        if (p != null && mc.level != null) {
            PlayerTeam team = mc.level.getScoreboard().getPlayersTeam(p.getGameProfile().getName());
            if (team != null) {
                title = team.getDisplayName().getString();
                List<String> members = new ArrayList<>(team.getPlayers());
                members.sort(String::compareToIgnoreCase);
                rows.addAll(members);
                footer = tr("saomenu.panel.team_members", members.size());
            } else {
                subtitle = tr("saomenu.panel.no_team");
            }
        }
        renderListCard(g, ctx.minecraft().font, MenuRects.local(bounds), title, subtitle, rows, footer, alpha);
    }

    private static void renderFriendsCard(GuiGraphics g, MenuContext ctx, UiRect bounds, float eased, float alpha,
                                          int mouseX, int mouseY) {
        Minecraft mc = ctx.minecraft();
        List<String> rows = new ArrayList<>();
        int online = 0;
        if (mc.getConnection() != null) {
            List<String> names = new ArrayList<>();
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                names.add(info.getProfile().getName());
            }
            names.sort(String::compareToIgnoreCase);
            online = names.size();
            rows.addAll(names);
        }
        renderListCard(g, mc.font, MenuRects.local(bounds), tr("saomenu.friends"), null, rows,
                tr("saomenu.panel.online", online), alpha);
    }

    private static void renderListCard(GuiGraphics g, Font f, MenuLayout.Rect at,
                                       String title, String subtitle, List<String> rows, String footer, float alpha) {
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, MenuAssets.PANEL, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        float headH = Math.max(8f, at.h() * 0.08f);
        SaoDraw.drawCentered(g, f, title, at.centerX(), at.y() + headH * 0.45f,
                SaoDraw.fitScale(f, headH, 0.78f), mulAlpha(theme().textOnSurface(), alpha), false);
        int lineY;
        if (subtitle != null && !subtitle.isEmpty()) {
            float subH = Math.max(7f, at.h() * 0.055f);
            SaoDraw.drawCentered(g, f, subtitle, at.centerX(), at.y() + headH + subH * 0.45f,
                    SaoDraw.fitScale(f, subH, 0.75f), mulAlpha(theme().textOnSurface(), alpha), false);
            lineY = Math.round(at.y() + headH + subH + 2);
        } else {
            lineY = Math.round(at.y() + headH + 2);
        }
        g.fill(at.x() + at.w() / 10, lineY, at.x() + at.w() - at.w() / 10, lineY + 1, mulAlpha(theme().divider(), alpha));

        int bodyTop = lineY + 4;
        int bodyBot = at.y() + at.h() - Math.round(Math.max(8f, at.h() * 0.08f));
        int bodyH = Math.max(8, bodyBot - bodyTop);
        float lineH = Math.max(7f, at.h() * 0.055f);
        int maxRows = Mth.clamp((int) (bodyH / lineH), 1, 8);
        if (rows.size() > maxRows) {
            int extra = rows.size() - maxRows + 1;
            List<String> shown = new ArrayList<>(rows.subList(0, Math.max(0, maxRows - 1)));
            shown.add(tr("saomenu.panel.more", extra));
            rows = shown;
        }
        for (int i = 0; i < rows.size(); i++) {
            SaoDraw.drawInRow(g, f, rows.get(i), at.x() + 10, bodyTop + i * lineH, lineH,
                    at.w() - 20, mulAlpha(theme().textOnSurface(), alpha), false);
        }
        if (rows.isEmpty() && (subtitle == null || subtitle.isEmpty())) {
            SaoDraw.drawInRow(g, f, tr("saomenu.panel.no_players"), at.x() + 10, bodyTop, lineH,
                    at.w() - 20, mulAlpha(theme().textOnSurface(), alpha), false);
        }
        float footH = Math.max(8f, at.h() * 0.07f);
        SaoDraw.drawInRow(g, f, footer, at.x() + at.w() * 0.35f, at.y() + at.h() - footH, footH,
                at.w() * 0.60f, mulAlpha(theme().textOnSurface(), alpha), false);
    }

    private static void renderArrowRight(GuiGraphics g, MenuSession s, MenuLayout.Rect card,
                                         int anchorY, float alpha, int height) {
        int btnLeft = s.baseAnchorX - MenuLayout.btnSize(height) / 2;
        int x0 = card.x() + card.w() + 1;
        int w = btnLeft - x0 - 1;
        if (w < 3) {
            return;
        }
        int h = Math.max(6, Math.round(w * (19f / 24f)));
        shaderAlpha(alpha);
        RenderSystem.enableBlend();
        g.blit(MenuAssets.ARROW_RIGHT, x0, anchorY - h / 2, 0, 0, w, h, w, h);
        shaderAlpha(1f);
    }

    static String playerName(Minecraft mc) {
        Player p = mc.player;
        return p != null ? p.getGameProfile().getName() : "Player";
    }

    static String trim(float v) {
        float r = Math.round(v * 10f) / 10f;
        return (r == Math.rint(r)) ? String.valueOf((int) r) : String.valueOf(r);
    }
}
