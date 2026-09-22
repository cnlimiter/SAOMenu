package com.sao.saomenu.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.client.party.SAOClientPartyState;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.text.SaoText;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;

/**
 * 玩家状态板:血条/等级贴图、队友紧凑血条、受击闪红与低血量红晕。
 *
 * <p>几何与绘制只在这里,菜单与常驻 HUD 共用,避免两套坐标。</p>
 */
public final class SAOPlayerPlate {

    private static final ResourceLocation TEX_HP_BAR =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/hp_bar.png");
    private static final ResourceLocation TEX_HP_GREEN =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/hp_green.png");
    private static final ResourceLocation TEX_HP_ICON =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/hp_icon.png");

    private static final int TEXT_WHITE = 0xFFF4F4F4;
    private static final int TEXT_DARK = 0xFF262829;

    private static final float GROOVE_X0 = 87f;
    private static final float GROOVE_STEP = 211f;
    private static final float GROOVE_X1 = 328f;
    private static final float GROOVE_TOP = 39f;
    private static final float GROOVE_THICK_BOT = 51f;
    private static final float GROOVE_THIN_BOT = 43f;
    private static final float GREEN_STEP_U = 134f;

    private static final float PLATE_PAD_L = 13f;
    private static final float PLATE_PAD_T = 25f;

    private static final long FLASH_MS = 400;

    private static int lastLevel = -1;
    private static boolean lastLow;
    private static float lastHp = -1f;
    private static long flashAt = Long.MIN_VALUE;

    private SAOPlayerPlate() {
    }

    public static int plateW(int screenW) {
        return MenuLayout.plateW(screenW);
    }

    public static int plateH(int screenW) {
        return MenuLayout.plateH(screenW);
    }

    /**
     * 血条板整组(板 + 队友血条 + 状态效果行)的总高度,拖动命中范围按整组算。
     *
     * <p>命中比可见范围多留 4 格余量:队友列表为空、也没有药水效果时,
     * 板下方其实什么都没有,但拖动目标太小不好抓;反之整组最高时不超过此值。
     * 命中框恒定也避免拖动中队伍人数变化导致命中区跳变。</p>
     */
    public static int plateGroupH(int screenW, Minecraft mc) {
        return plateH(screenW) + 4 + Math.max(teamRows(mc) * (compactRowH(screenW) + 2), 24);
    }

    public static int plateX(int screenW) {
        float fx = Mth.clamp(SAOConfig.platePanelX(), 0f, 1f);
        return Math.max(0, Math.min(Math.round(fx * (screenW - plateW(screenW))), screenW - plateW(screenW)));
    }

    public static int plateY(int screenH) {
        float fy = Mth.clamp(SAOConfig.platePanelY(), 0f, 1f);
        return Math.max(0, Math.min(Math.round(fy * Math.max(1, screenH - 20)), screenH - 20));
    }

    public static boolean hit(Minecraft mc, int screenW, int screenH, int mx, int my) {
        int gh = plateGroupH(screenW, mc);
        int gx = plateX(screenW);
        int gy = plateY(screenH);
        return mx >= gx && mx < gx + plateW(screenW) && my >= gy && my < gy + gh;
    }

    static void moveTo(Minecraft mc, int screenW, int screenH, float grabFx, float grabFy, int mx, int my) {
        int w = plateW(screenW);
        float fx = (mx - grabFx * w) / (float) Math.max(1, screenW - w);
        float fy = (my - grabFy * plateGroupH(screenW, mc)) / (float) Math.max(1, screenH - 20);
        SAOConfig.setPlatePanelX(fx);
        SAOConfig.setPlatePanelY(fy);
    }

    static int compactRowH(int screenW) {
        return Math.max(8, Math.round(plateW(screenW) * 0.13f));
    }

    /** 当前应显示的队友行数(不含自己;无队伍为 0;菜单打开时同样显示)。 */
    public static int teamRows(Minecraft mc) {
        if (!SAOConfig.showHud() || !SAOClientPartyState.inParty()) {
            return 0;
        }
        String self = mc.player != null ? mc.player.getGameProfile().getName() : "";
        int n = 0;
        for (String name : SAOClientPartyState.teamMembers()) {
            if (!name.equals(self) && mc.getConnection() != null
                    && mc.getConnection().getPlayerInfo(name) != null) {
                n++;
            }
        }
        return n;
    }

    /**
     * SAO 血条板(SAO Utils 官方贴图复刻,几何逐像素对齐 hp_bar.png 360x83):
     * 左端 [+] 徽章块(x12..34)+ 名字/头像位(x36..86)+ 血条凹槽
     * (粗段 x87..211 y39..51,台阶细段 x211..328 y39..43)+
     * 右下双格标签(x220..348 y64..79,分隔线 x305:左格血量、右格等级)。
     * 名称位用第一人称头像;受击闪红与低血量红色脉冲/变色效果保留。
     */
    public static void render(GuiGraphics g, int x, int y, int w, int h,
                              String name, Player p, float alpha) {
        float hp = p != null ? p.getHealth() : 20f;
        float maxHp = p != null ? p.getMaxHealth() : 20f;
        int level = p != null ? p.experienceLevel : 1;
        float frac = maxHp <= 0 ? 0f : Mth.clamp(hp / maxHp, 0f, 1f);
        float s = w / 360f;
        x = Math.round(x - PLATE_PAD_L * s);
        y = Math.round(y - PLATE_PAD_T * s);

        RenderSystem.enableBlend();
        SaoDraw.shaderAlpha(alpha);
        g.blit(TEX_HP_BAR, x, y, w, h, 0f, 0f, 360, 83, 360, 83);

        int barX = x + Math.round(GROOVE_X0 * s);
        int barY = y + Math.round(GROOVE_TOP * s);
        int barH = Math.max(2, Math.round((GROOVE_THICK_BOT - GROOVE_TOP) * s));
        if (frac > 0f) {
            float warm = Mth.clamp((0.6f - frac) / 0.45f, 0f, 1f);
            RenderSystem.setShaderColor(1f, 1f - warm * 0.72f, 1f - warm * 0.68f, alpha);
            float fillEnd = GROOVE_X0 + (GROOVE_X1 - GROOVE_X0) * frac;
            float thickEnd = Math.min(fillEnd, GROOVE_STEP);
            int thickW = Math.round((thickEnd - GROOVE_X0) * s);
            if (thickW > 0) {
                int uW = Math.max(1, Math.round(GREEN_STEP_U
                        * (thickEnd - GROOVE_X0) / (GROOVE_STEP - GROOVE_X0)));
                g.blit(TEX_HP_GREEN, barX, barY, thickW, barH, 0f, 0f, uW, 25, 258, 25);
            }
            if (fillEnd > GROOVE_STEP) {
                int thinX = x + Math.round(GROOVE_STEP * s);
                int thinW = Math.round((fillEnd - GROOVE_STEP) * s);
                int thinH = Math.max(1, Math.round((GROOVE_THIN_BOT - GROOVE_TOP + 1.5f) * s));
                int uW = Math.max(1, Math.round((258 - GREEN_STEP_U)
                        * (fillEnd - GROOVE_STEP) / (GROOVE_X1 - GROOVE_STEP)));
                if (thinW > 0) {
                    g.blit(TEX_HP_GREEN, thinX, barY, thinW, thinH,
                            GREEN_STEP_U, 0f, uW, 17, 258, 25);
                }
            }
            RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
        }

        int iconW = Math.max(8, Math.round(18f * s));
        int iconX = x + Math.round(23f * s) - iconW / 2;
        int iconY = y + Math.round(45.5f * s) - iconW / 2;
        RenderSystem.enableBlend();
        SaoDraw.shaderAlpha(alpha);
        g.blit(TEX_HP_ICON, iconX, iconY, iconW, iconW, 0f, 0f, 22, 22, 22, 22);

        Font font = Minecraft.getInstance().font;
        int avS = Math.max(8, Math.round(26f * s));
        int avX = x + Math.round(61f * s) - avS / 2;
        int avY = y + Math.round(45.5f * s) - avS / 2;
        if (SAOConfig.showAvatar() && p != null) {
            renderAvatar(g, Minecraft.getInstance(), p, avX, avY, avS, alpha);
        } else {
            int maxW = 48;
            String nameText = font.width(name) <= maxW ? name
                    : font.plainSubstrByWidth(name, maxW - font.width("…")) + "…";
            SaoDraw.drawCentered(g, font, nameText,
                    x + 61f * s, y + 45.5f * s, s, mulAlpha(TEXT_DARK, alpha), false);
        }

        String hpText = trimHp(hp) + " / " + trimHp(maxHp);
        String lvText = "Lv:" + level;
        SaoDraw.drawCentered(g, font, hpText,
                x + 262.5f * s, y + 71.5f * s, s, mulAlpha(TEXT_DARK, alpha), false);
        SaoDraw.drawCentered(g, font, lvText,
                x + 326.5f * s, y + 71.5f * s, s, mulAlpha(TEXT_DARK, alpha), false);

        long flashAge = net.minecraft.Util.getMillis() - flashAt;
        if (flashAge >= 0 && flashAge < FLASH_MS) {
            float fa = (1f - flashAge / (float) FLASH_MS) * alpha;
            g.fill(x, y, x + w, y + h, mulAlpha(0x59FF3030, fa));
        }
        if (frac > 0f && frac < 0.2f) {
            float pulse = 0.35f + 0.45f * Mth.sin(net.minecraft.Util.getMillis() / 130f);
            int pulseW = Math.round((GROOVE_X1 - GROOVE_X0) * frac * s);
            if (pulseW > 0) {
                g.fill(barX, barY, barX + pulseW, barY + barH, mulAlpha(0x66FF2020, pulse * alpha));
            }
        }
    }

    /**
     * 队友血条组:每个在线队友一行紧凑血条板(复用 hp_bar.png,右端不带标签格)。
     * 参照 SAO 动画:名字白色、绿条按队友真实血量涨落。
     */
    public static void renderTeamBars(GuiGraphics g, Minecraft mc, int x, int y, int screenW, Player self) {
        if (!SAOClientPartyState.inParty()) {
            return;
        }
        String selfName = self.getGameProfile().getName();
        var conn = mc.getConnection();
        if (conn == null) {
            return;
        }
        int rowH = compactRowH(screenW);
        int w = Math.round(plateW(screenW) * 0.78f);
        float s = w / 360f;
        int yy = y;
        for (String name : SAOClientPartyState.teamMembers()) {
            if (name.equals(selfName)) {
                continue;
            }
            var info = conn.getPlayerInfo(name);
            if (info == null) {
                continue;
            }
            float hp = 20f;
            float maxHp = 20f;
            var ent = mc.level != null ? mc.level.getPlayerByUUID(info.getProfile().getId()) : null;
            if (ent != null) {
                hp = ent.getHealth();
                maxHp = ent.getMaxHealth();
            }
            float frac = maxHp <= 0f ? 0f : Mth.clamp(hp / maxHp, 0f, 1f);
            renderCompactBar(g, x, yy, w, rowH, s, name, frac);
            yy += rowH + 2;
        }
    }

    /** 低血量屏幕边缘红晕:血量越低越浓,呼吸式闪烁。 */
    public static void renderLowHpVignette(GuiGraphics g, int w, int h, float hpFrac) {
        if (hpFrac >= 0.2f || hpFrac <= 0f) {
            return;
        }
        float danger = Mth.clamp((0.25f - hpFrac) * 4f, 0f, 1f);
        float pulse = 0.6f + 0.4f * Mth.sin(net.minecraft.Util.getMillis() / 150f);
        int layers = 6;
        for (int i = 0; i < layers; i++) {
            int t = Math.round(h * 0.03f + i * (h * 0.06f / layers));
            int a = Math.round(150f * danger * pulse * (layers - i) / layers);
            if (a <= 0) {
                continue;
            }
            int color = (a << 24) | 0xFF2020;
            g.fill(0, 0, w, t, color);
            g.fill(0, h - t, w, h, color);
            g.fill(0, 0, t, h, color);
            g.fill(w - t, 0, w, h, color);
        }
    }

    static void detectEvents(Player p) {
        if (lastLevel >= 0 && p.experienceLevel > lastLevel) {
            SAONotification.push(SaoText.tr("saomenu.notify.levelup.title"),
                    SaoText.tr("saomenu.notify.levelup.msg", p.experienceLevel));
        }
        lastLevel = p.experienceLevel;
        float hpNow = p.getHealth();
        if (lastHp >= 0f && hpNow < lastHp - 0.01f) {
            flashAt = net.minecraft.Util.getMillis();
        }
        lastHp = hpNow;
        float frac = p.getMaxHealth() <= 0f ? 0f : p.getHealth() / p.getMaxHealth();
        boolean low = frac > 0f && frac < 0.2f;
        if (low && !lastLow) {
            SAONotification.push(SaoText.tr("saomenu.notify.lowhp"), "");
        }
        lastLow = low;
    }

    static void resetEvents() {
        lastLevel = -1;
        lastLow = false;
        lastHp = -1f;
        flashAt = Long.MIN_VALUE;
    }

    private static void renderCompactBar(GuiGraphics g, int x, int y, int w, int h,
                                         float s, String name, float frac) {
        RenderSystem.enableBlend();
        g.blit(TEX_HP_BAR, x, y, w, h, PLATE_PAD_L, 30.0F, 315, 22, 360, 83);
        Font font = Minecraft.getInstance().font;
        String label = font.width(name) > 200
                ? font.plainSubstrByWidth(name, 190 - font.width("…")) + "…" : name;
        SaoDraw.drawScaled(g, font, label, x + 4f * s, y + 1f * s, s, mulAlpha(TEXT_WHITE, 1f), false);
        if (frac > 0f) {
            float barScale = s;
            int barX = x + Math.round((GROOVE_X0 - PLATE_PAD_L) * barScale);
            int barY = y + Math.round((GROOVE_TOP - 30f) * barScale);
            int barH = Math.max(2, Math.round((GROOVE_THICK_BOT - GROOVE_TOP) * barScale));
            float fillEnd = GROOVE_X0 + (GROOVE_X1 - GROOVE_X0) * frac;
            float thickEnd = Math.min(fillEnd, GROOVE_STEP);
            int thickW = Math.round((thickEnd - GROOVE_X0) * barScale);
            if (thickW > 0) {
                int uW = Math.max(1, Math.round(GREEN_STEP_U
                        * (thickEnd - GROOVE_X0) / (GROOVE_STEP - GROOVE_X0)));
                g.blit(TEX_HP_GREEN, barX, barY, thickW, barH, 0f, 0f, uW, 25, 258, 25);
            }
            if (fillEnd > GROOVE_STEP) {
                int thinX = x + Math.round((GROOVE_STEP - PLATE_PAD_L) * barScale);
                int thinW = Math.round((fillEnd - GROOVE_STEP) * barScale);
                int thinH = Math.max(1, Math.round((GROOVE_THIN_BOT - GROOVE_TOP + 1.5f) * barScale));
                int uW = Math.max(1, Math.round((258 - GREEN_STEP_U)
                        * (fillEnd - GROOVE_STEP) / (GROOVE_X1 - GROOVE_STEP)));
                if (thinW > 0) {
                    g.blit(TEX_HP_GREEN, thinX, barY, thinW, thinH,
                            GREEN_STEP_U, 0f, uW, 17, 258, 25);
                }
            }
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    private static void renderAvatar(GuiGraphics g, Minecraft mc, Player p,
                                     int x, int y, int size, float alpha) {
        ResourceLocation skin = mc.getSkinManager().getInsecureSkinLocation(p.getGameProfile());
        g.fill(x - 1, y - 1, x + size + 1, y + size + 1, mulAlpha(SaoTheme.accent(), alpha));
        RenderSystem.enableBlend();
        SaoDraw.shaderAlpha(alpha);
        g.blit(skin, x, y, size, size, 8f, 8f, 8, 8, 64, 64);
        SaoDraw.shaderAlpha(1f);
    }

    private static String trimHp(float v) {
        float r = Math.round(v * 10f) / 10f;
        return (r == Math.rint(r)) ? String.valueOf((int) r) : String.valueOf(r);
    }
}
