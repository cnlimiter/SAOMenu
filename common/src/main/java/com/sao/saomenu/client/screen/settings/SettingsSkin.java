package com.sao.saomenu.client.screen.settings;

import java.util.List;
import java.util.Locale;

import com.mojang.math.Axis;
import com.sao.saomenu.api.settings.ActionSetting;
import com.sao.saomenu.api.settings.ChoiceSetting;
import com.sao.saomenu.api.settings.NativeSetting;
import com.sao.saomenu.api.settings.Setting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.SliderSetting;
import com.sao.saomenu.api.settings.ToggleSetting;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;
import static com.sao.saomenu.ui.animation.SaoMotion.easeInCubic;
import static com.sao.saomenu.ui.animation.SaoMotion.easeInOutCubic;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic;

/**
 * P5 蓝粉双风格绘制与控件命中。几何与选项表分离,主题色块走 {@link SaoTheme}。
 */
final class SettingsSkin {

    private static final int MIN_PRESET_W = 44;
    private static final int MAX_PRESET_COLS = 4;
    private static final Component SELECT_VALUE = Component.translatable("saomenu.settings.select_value");

    static final int KIRITO_BLUE = 0x4FA8E8;
    static final int KIRITO_BRIGHT = 0xA8DFFF;
    static final int KIRITO_DEEP = 0x101C30;
    static final int ASUNA_PINK = 0xFF8AB0;
    static final int ASUNA_BRIGHT = 0xFFC9DA;
    static final int ASUNA_DEEP = 0x2C1420;
    static final int RGB_WHITE = 0xF8FAFC;
    static final int RGB_GRAY = 0xB9BEC6;
    static final int RGB_DARK_TEXT = 0x16171A;

    private float[] catHover = new float[0];
    private float backHover;
    private int hoverRow = -1;
    private int dragRow = -1;
    private boolean btnResetHover;
    private boolean btnDoneHover;
    private String lastPresetDebug = "";

    private Font font;
    private int w;
    private int h;
    private List<SettingsGroup> groups = List.of();
    private SettingsNavigator nav = new SettingsNavigator();
    private SettingsGroup pageGroup;

    void bind(Font font, int width, int height, List<SettingsGroup> groups, SettingsNavigator nav) {
        this.font = font;
        this.w = width;
        this.h = height;
        this.groups = groups == null ? List.of() : groups;
        this.nav = nav;
        if (this.catHover.length != this.groups.size()) {
            this.catHover = java.util.Arrays.copyOf(this.catHover, this.groups.size());
        }
        nav.sync(this.groups);
        nav.setVisibleGroups(SettingsLayout.VISIBLE_CATS);
        SettingsGroup current = nav.currentGroup();
        int count = current == null ? 0 : current.options().size();
        int avail = rowAvail();
        nav.setVisibleOptions(Math.max(1, SettingsLayout.visibleRows(avail, Math.max(1, count))));
    }

    int dragRow() {
        return dragRow;
    }

    void beginSliderDrag(int row) {
        this.dragRow = row;
    }

    void endDrag() {
        this.dragRow = -1;
    }

    String lastPresetDebug() {
        return lastPresetDebug;
    }

    int uiAccent(boolean root) {
        return root ? KIRITO_BLUE : ASUNA_PINK;
    }

    int uiDeep(boolean root) {
        return root ? KIRITO_DEEP : ASUNA_DEEP;
    }

    int sideX() {
        return Math.max(14, (int) (this.w * 0.07f));
    }

    int catW() {
        return Mth.clamp((int) (this.w * 0.36f), 170, 330);
    }

    int catH() {
        return Mth.clamp((this.h - 120) / SettingsLayout.VISIBLE_CATS - 8, 24, 44);
    }

    int catGap() {
        return Math.max(8, catH() / 4);
    }

    int catY0() {
        int n = SettingsLayout.VISIBLE_CATS;
        return Math.max(56, (this.h - (catH() * n + catGap() * (n - 1))) / 2 + 4);
    }

    int catX(int visibleSlot) {
        int group = nav.groupScroll() + visibleSlot;
        float hover = group >= 0 && group < catHover.length ? catHover[group] : 0f;
        return sideX() + visibleSlot * 18 + Math.round(hover * 8f);
    }

    int catY(int visibleSlot) {
        return catY0() + visibleSlot * (catH() + catGap());
    }

    boolean catHovered(int visibleSlot, int mx, int my) {
        int x = catX(visibleSlot);
        return mx >= x - 16 && mx <= x + catW() - 10
                && my >= catY(visibleSlot) - 2 && my < catY(visibleSlot) + catH() + 2;
    }

    int hitCategory(int mx, int my) {
        int vis = visibleGroupSlots();
        for (int slot = 0; slot < vis; slot++) {
            if (catHovered(slot, mx, my)) {
                int index = nav.groupScroll() + slot;
                if (index >= 0 && index < groups.size()) {
                    return index;
                }
            }
        }
        return -1;
    }

    int visibleGroupSlots() {
        return SettingsLayout.visibleSlots(groups.size(), nav.groupScroll(), nav.visibleGroups());
    }

    int rowsTop() {
        return Math.max(58, (int) (this.h * 0.20f));
    }

    int rowAvail() {
        return SettingsLayout.optionViewport(rowsTop(), smallBtnY() - 6);
    }

    int rowH(SettingsGroup group) {
        int n = group == null ? 1 : Math.max(1, group.options().size());
        return SettingsLayout.rowH(rowAvail(), n);
    }

    int rowY(SettingsGroup group, int absoluteIndex) {
        return rowsTop() + (absoluteIndex - nav.optionScroll()) * rowH(group);
    }

    int rowX0() {
        return sideX();
    }

    int rowX1() {
        return this.w - sideX();
    }

    boolean rowVisible(SettingsGroup group, int absoluteIndex) {
        if (group == null) {
            return false;
        }
        return SettingsLayout.indexInViewport(
                absoluteIndex, nav.optionScroll(), nav.visibleOptions(), group.options().size());
    }

    boolean rowHovered(SettingsGroup group, int i, int mx, int my) {
        if (!rowVisible(group, i)) {
            return false;
        }
        int y = rowY(group, i);
        return mx >= rowX0() && mx <= rowX1() && my >= y && my < y + rowH(group);
    }

    int[] nativeRect(SettingsGroup group, int i) {
        int y = rowY(group, i);
        int rh = rowH(group);
        int x0 = rowX0();
        int x1 = rowX1();
        int tx0 = x0 + (x1 - x0) * 55 / 100;
        return new int[]{tx0, y + 3, Math.max(24, x1 - tx0 - 8), Math.max(10, rh - 6)};
    }

    int smallBtnW() {
        return Math.min(96, (this.w - sideX() * 2 - 10) / 2);
    }

    int smallBtnY() {
        return this.h - Math.max(24, catH()) - 8;
    }

    int smallBtnH() {
        return Math.min(20, catH() - 2);
    }

    boolean btnHovered(int x, int y, int w, int h, int mx, int my) {
        return mx >= x && mx <= x + w + 8 && my >= y - 1 && my < y + h + 1;
    }

    boolean hitReset(int mx, int my) {
        return btnHovered(sideX(), smallBtnY(), smallBtnW(), smallBtnH(), mx, my);
    }

    boolean hitDone(int mx, int my) {
        int bw = smallBtnW();
        return btnHovered(this.w - sideX() - bw, smallBtnY(), bw, smallBtnH(), mx, my);
    }

    boolean backHovered(int mx, int my) {
        int x = sideX();
        int y = rowsTop() - 36;
        return mx >= x - 6 && mx <= x + 72 && my >= y - 2 && my < y + 22;
    }

    void updateHovers(SettingsTimeline tl, float dt, int mouseX, int mouseY) {
        float k = Math.min(1f, dt * 12f);
        boolean root = tl.isRoot();
        int vis = visibleGroupSlots();
        for (int i = 0; i < catHover.length; i++) {
            boolean target = false;
            if (root && !tl.inTransition()) {
                int slot = i - nav.groupScroll();
                target = slot >= 0 && slot < vis && catHovered(slot, mouseX, mouseY);
            }
            catHover[i] += ((target ? 1f : 0f) - catHover[i]) * k;
        }
        boolean backTarget = !root && !tl.inTransition() && backHovered(mouseX, mouseY);
        backHover += ((backTarget ? 1f : 0f) - backHover) * k;

        int newHover = -1;
        SettingsGroup group = nav.currentGroup();
        if (!root && !tl.inTransition() && group != null) {
            for (int i = 0; i < group.options().size(); i++) {
                if (rowHovered(group, i, mouseX, mouseY)) {
                    newHover = i;
                    break;
                }
            }
        }
        hoverRow = newHover;
        if (newHover >= 0) {
            nav.focusOption(newHover);
        } else if (root && !tl.inTransition()) {
            int cat = hitCategory(mouseX, mouseY);
            if (cat >= 0) {
                nav.focusGroup(cat);
            }
        }

        int bw = smallBtnW();
        int by = smallBtnY();
        int bh = smallBtnH();
        btnResetHover = btnHovered(sideX(), by, bw, bh, mouseX, mouseY);
        btnDoneHover = btnHovered(this.w - sideX() - bw, by, bw, bh, mouseX, mouseY);
    }

    void renderDecorStripes(GuiGraphics g, SettingsTimeline tl, long now) {
        float drift = (now % 6000L) / 6000f;
        float a1 = (drift * 1.6f - 0.3f) * this.w;
        float a2 = (drift * 1.6f - 1.1f) * this.w;
        fillSlab(g, a1, this.h * 0.16f, this.w * 0.5f, 2f, -18f, 0x30FFFFFF);
        fillSlab(g, a2, this.h * 0.88f, this.w * 0.5f, 2f, -18f, 0x2699CCDD);
        fillSlab(g, this.w * 0.86f, this.h * 0.5f, 3f, this.h * 1.6f, -18f,
                withAlpha(uiAccent(tl.isRoot()), 46));
    }

    void renderScrim(GuiGraphics g) {
        g.fillGradient(0, 0, this.w, this.h, 0x1E000000, 0x61000000);
    }

    void renderPage(GuiGraphics g, SettingsTimeline tl, boolean pageRoot, SettingsGroup pageGroup,
                    int mx, int my, long now, boolean inTransition, long trT) {
        this.pageGroup = pageGroup;
        float exitP = inTransition && trT < SettingsTimeline.TR_SWAP_MS
                ? Mth.clamp(trT / (float) SettingsTimeline.TR_SWAP_MS, 0f, 1f) : 0f;
        boolean exiting = exitP > 0f;
        if (pageRoot) {
            renderRootPage(g, tl, mx, my, now, exiting, exitP);
        } else {
            renderRowsPage(g, tl, pageGroup, mx, my, now, exiting, exitP);
        }
        renderBottomBar(g, tl, pageRoot, now, exiting, exitP);
    }

    void renderWipe(GuiGraphics g, SettingsTimeline tl, long trT) {
        if (trT < 110 || trT > SettingsTimeline.TR_TOTAL_MS - 30) {
            return;
        }
        float p = easeInOutCubic(clamp01((trT - 110f) / (float) (SettingsTimeline.TR_TOTAL_MS - 140f)));
        float width = this.w;
        float height = this.h;
        float cx = Mth.lerp(p, -0.5f, 1.5f) * width;
        int band = tl.transitionForward() ? ASUNA_PINK : KIRITO_BLUE;
        fillSlab(g, cx - width * 0.42f, height * 0.5f, width * 0.30f, height * 2.6f, -12f, 0xF4000000);
        fillSlab(g, cx, height * 0.5f, width * 0.52f, height * 2.6f, -12f, 0xFF000000 | band);
        fillSlab(g, cx + width * 0.30f, height * 0.5f, width * 0.035f, height * 2.6f, -12f, 0xF2F9F9F9);
    }

    private void renderRootPage(GuiGraphics g, SettingsTimeline tl, int mx, int my,
                                long now, boolean exiting, float exitP) {
        long enter = now - tl.pageStartMs();
        float titleIn = easeOutCubic(clamp01((enter - 60f) / 340f));

        float titleOff = exiting ? -easeInCubic(exitP) * this.w * 0.6f : (1f - titleIn) * -260f;
        float titleY = Math.max(12, catY0() - catH() - 26);
        fillSlab(g, sideX() + 10 + titleOff * 0.4f, titleY + 11, 24, 24, -8f,
                withAlpha(KIRITO_BLUE, alpha(titleIn, 1f)));
        drawScaled(g, tr("saomenu.settings.title"), sideX() + 30 + titleOff,
                titleY, 1.7f, withAlpha(RGB_WHITE, alpha(titleIn, 1f)), false);
        drawScaled(g, tr("saomenu.settings.subtitle"), sideX() + 32 + titleOff,
                titleY + 20, 0.8f, withAlpha(RGB_GRAY, alpha(titleIn, 0.88f)), false);

        int vis = visibleGroupSlots();
        for (int slot = 0; slot < vis; slot++) {
            int i = nav.groupScroll() + slot;
            float off;
            float alphaF;
            if (exiting) {
                boolean lead = tl.transitionForward() && i == tl.clickedCat();
                float local = clamp01((exitP * SettingsTimeline.TR_SWAP_MS - (lead ? 0f : 40f + slot * 30f)) / 230f);
                off = (lead ? -1f : 1f) * easeInCubic(local) * this.w * 0.75f;
                alphaF = 1f - easeInCubic(local);
            } else {
                float enterT = clamp01((enter - 140f - slot * SettingsTimeline.ENTER_STAGGER_MS) / 380f);
                off = (1f - easeOutBack(enterT)) * -220f;
                alphaF = clamp01(enterT * 1.8f);
            }
            renderCategory(g, tl, slot, i, off, alphaF);
        }

        int hi = -1;
        for (int i = 0; i < catHover.length; i++) {
            if (catHover[i] > 0.35f) {
                hi = i;
            }
        }
        if (hi < 0 && !exiting && nav.groupIndex() >= 0) {
            hi = nav.groupIndex();
        }
        if (hi >= 0 && !exiting) {
            String num = String.format(Locale.ROOT, "0%d", hi + 1);
            float gs = this.h / 26f;
            drawScaledRot(g, num, this.w * 0.70f, this.h * 0.26f, gs, -10f,
                    withAlpha(KIRITO_BLUE, Math.round(Math.max(catHover.length > hi ? catHover[hi] : 0f, 0.35f) * 52)),
                    false);
        }
    }

    private void renderCategory(GuiGraphics g, SettingsTimeline tl, int slot, int groupIndex,
                                float xOff, float alphaF) {
        if (alphaF <= 0.02f || groupIndex < 0 || groupIndex >= groups.size()) {
            return;
        }
        SettingsGroup group = groups.get(groupIndex);
        int a = alpha(alphaF, 1f);
        int width = catW();
        int height = catH();
        int x = catX(slot) + Math.round(xOff);
        int y = catY(slot);
        float hv = catHover.length > groupIndex ? catHover[groupIndex] : 0f;
        if (hv < 0.15f && nav.isRoot() && nav.groupIndex() == groupIndex && !tl.inTransition()) {
            hv = Math.max(hv, 0.4f);
        }
        boolean hovered = hv > 0.5f && !tl.inTransition() && alphaF > 0.9f;

        if (hv > 0.02f) {
            float spread = 0.55f + 0.45f * hv;
            fillSlab(g, x + 3 + width * spread * hv * 0.5f, y + height * 0.5f,
                    width * spread * hv, height, -3f,
                    withAlpha(hovered ? KIRITO_BLUE : KIRITO_DEEP, Math.round(a * hv * 0.68f)));
        }

        fillSlab(g, x + 3f, y + height * 0.5f, hovered ? 7f : 5f, height - 6f, -3f,
                withAlpha(hovered ? KIRITO_BRIGHT : KIRITO_BLUE, a));
        if (hv > 0.35f) {
            drawScaled(g, "▶", x - 15, y + height * 0.5f - 5,
                    0.9f, withAlpha(KIRITO_BRIGHT, Math.round(a * (hv - 0.35f) / 0.65f)), false);
        }

        var pose = g.pose();
        pose.pushPose();
        pose.translate(x + 14, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-2f));
        drawScaled(g, String.format(Locale.ROOT, "0%d", groupIndex + 1), 0, 2, 0.7f,
                withAlpha(hovered ? KIRITO_BRIGHT : RGB_GRAY, Math.round(a * 0.85f)), false);
        float labelScale = Math.min(1.6f, height / 20f);
        drawFitted(g, group.label(), 0, height * 0.5f - 6, labelScale, width - 28,
                withAlpha(hovered ? KIRITO_BRIGHT : RGB_WHITE, a));
        drawFitted(g, group.description(), 1, height - 10, 0.62f, width - 28,
                withAlpha(RGB_GRAY, Math.round(a * 0.8f)));
        pose.popPose();
    }

    private void renderRowsPage(GuiGraphics g, SettingsTimeline tl, SettingsGroup group, int mx, int my,
                                long now, boolean exiting, float exitP) {
        long enter = now - tl.pageStartMs();
        int accent = ASUNA_PINK;
        Component title = group == null ? Component.empty() : group.label();
        Component sub = group == null ? Component.empty() : group.description();

        float headIn = exiting ? 1f : easeOutCubic(clamp01(enter / 300f));
        float headOff = exiting ? easeInCubic(exitP) * this.w * 0.55f : (1f - headIn) * 200f;
        renderBackButton(g, tl, headOff, alpha(headIn, 1f), mx, my);
        int titleX = sideX() + 76;
        drawFitted(g, title, titleX + headOff, rowsTop() - 32, 1.45f, w - titleX - 16,
                withAlpha(RGB_WHITE, alpha(headIn, 1f)));
        fillSlab(g, titleX - 8 + headOff, rowsTop() - 22, 3.4f, 21f, -6f,
                withAlpha(accent, alpha(headIn, 1f)));
        drawFitted(g, sub, titleX + 2 + headOff, rowsTop() - 14, 0.62f, w - titleX - 18,
                withAlpha(RGB_GRAY, Math.round(headIn * 220)));

        if (group == null) {
            return;
        }
        int n = group.options().size();
        int start = nav.optionScroll();
        int vis = SettingsLayout.visibleSlots(n, start, nav.visibleOptions());
        int end = start + vis;
        for (int i = start; i < end; i++) {
            int visIndex = i - start;
            float local;
            if (exiting) {
                local = clamp01((exitP * SettingsTimeline.TR_SWAP_MS - visIndex * 22f) / 200f);
            } else {
                local = clamp01((enter - 60f - visIndex * SettingsTimeline.ENTER_STAGGER_MS) / 360f);
            }
            float off = exiting
                    ? easeInCubic(local) * this.w * 0.7f
                    : (1f - easeOutBack(local)) * 190f;
            float alphaF = exiting ? 1f - easeInCubic(local) : clamp01(local * 2.2f);
            renderRow(g, tl, group, i, off, alphaF);
        }
    }

    private void renderRow(GuiGraphics g, SettingsTimeline tl, SettingsGroup group, int i, float off,
                           float alphaF) {
        if (alphaF <= 0.02f) {
            return;
        }
        int a = alpha(alphaF, 1f);
        int accent = ASUNA_PINK;
        int x0 = rowX0() + Math.round(off);
        int x1 = rowX1() + Math.round(off);
        int y = rowY(group, i);
        int rh = rowH(group);
        boolean hovered = (this.hoverRow == i || (!tl.inTransition() && nav.optionIndex() == i))
                && !tl.inTransition() && alphaF > 0.95f;
        Setting spec = SettingsCatalog.setting(group, i);

        g.fill(x0 + 10, y + 1, x1, y + rh - 1,
                withAlpha(ASUNA_DEEP, Math.round(a * (hovered ? 0.72f : 0.34f))));
        fillSlab(g, x0 + 7f, y + rh / 2f, 4, rh - 6f, -8f,
                withAlpha(hovered ? ASUNA_BRIGHT : accent, Math.round(a * (hovered ? 1f : 0.72f))));
        SaoDraw.drawInRow(g, this.font, spec == null ? Component.empty() : spec.label(), x0 + 18, y, rh,
                Math.max(8, (x1 - x0) * 0.48f),
                withAlpha(RGB_WHITE, Math.round(a * (hovered ? 1f : 0.92f))), false);

        if (spec instanceof SliderSetting slider) {
            renderSliderControl(g, slider, i, x0, x1, y, rh, a);
        } else if (spec instanceof ChoiceSetting choice) {
            renderChoiceControl(g, choice, y, rh, a);
        } else if (spec instanceof ActionSetting action) {
            renderActionControl(g, action, x1, y, rh, a);
        } else if (spec instanceof NativeSetting) {
            renderNativeFrame(g, x0, x1, y, rh, a);
        } else {
            renderToggleControl(g, spec instanceof ToggleSetting toggle ? toggle : null, x1, y, rh, a);
        }
    }

    private void renderSliderControl(GuiGraphics g, SliderSetting spec, int i, int x0, int x1,
                                     int y, int rh, int a) {
        int accent = ASUNA_PINK;
        int tx0 = x0 + (x1 - x0) * 55 / 100;
        int tx1 = x1 - 58;
        float v = spec.value();
        float frac = spec.fraction();

        int trackH = Math.max(4, rh / 5);
        int trackY = y + (rh - trackH) / 2 + 1;
        g.fill(tx0, trackY, tx1, trackY + trackH, withAlpha(0xFFFFFF, Math.round(a * 0.20f)));
        int fillW = Math.round((tx1 - tx0) * frac);
        if (fillW > 1) {
            g.fill(tx0, trackY, tx0 + fillW, trackY + trackH, withAlpha(accent, a));
        }
        int kx = tx0 + fillW;
        int ky = y + rh / 2;
        int d = Math.max(9, Math.min(rh - 6, 12));
        boolean dragging = this.dragRow == i;
        fillSlab(g, kx, ky, d, d, 45f, withAlpha(dragging ? ASUNA_BRIGHT : accent, a));
        fillSlab(g, kx, ky, d * 0.45f, d * 0.45f, 45f, withAlpha(dragging ? accent : RGB_DARK_TEXT, a));

        SaoDraw.drawInRow(g, this.font, spec.formatValue(), tx1 + 6, y, rh,
                Math.max(8, x1 - tx1 - 8), withAlpha(RGB_GRAY, a), false);
    }

    private void renderToggleControl(GuiGraphics g, ToggleSetting spec, int x1, int y, int rh, int a) {
        boolean on = spec != null && spec.value();
        int accent = ASUNA_PINK;
        String s = tr(on ? "saomenu.config.on" : "saomenu.config.off");
        float fs = SaoDraw.fitScale(this.font, rh);
        float tw = this.font.width(s) * fs;
        SaoDraw.drawScaled(g, this.font, s, x1 - 10 - tw,
                y + (rh - this.font.lineHeight * fs) / 2f, fs,
                on ? withAlpha(accent, a) : withAlpha(RGB_GRAY, a), false);
        fillSlab(g, x1 - 20 - tw, y + rh / 2f - 1f, 7, 7, 45f,
                withAlpha(on ? accent : 0x55565A, a));
    }

    private void renderActionControl(GuiGraphics g, ActionSetting spec, int x1, int y, int rh, int a) {
        float fs = SaoDraw.fitScale(this.font, rh);
        var s = SaoDraw.clipTo(this.font, spec.actionLabel(),
                Math.max(0, Math.round(((rowX1() - rowX0()) * 0.42f - 16) / fs)));
        float tw = this.font.width(s) * fs;
        int bw = Math.round(tw + 16);
        int bh = Math.max(12, rh - 8);
        float cx = x1 - 10 - bw / 2f;
        float cy = y + rh / 2f;
        fillSlab(g, cx, cy, bw, bh, -4f, withAlpha(ASUNA_PINK, Math.round(a * 0.9f)));
        SaoDraw.drawScaled(g, this.font, s, x1 - 10 - tw - 6,
                y + (rh - this.font.lineHeight * fs) / 2f, fs,
                withAlpha(RGB_DARK_TEXT, a), false);
    }

    private void renderNativeFrame(GuiGraphics g, int x0, int x1, int y, int rh, int a) {
        int tx0 = x0 + (x1 - x0) * 55 / 100;
        g.fill(tx0, y + 3, x1 - 8, y + rh - 3, withAlpha(ASUNA_DEEP, Math.round(a * 0.55f)));
        g.fill(tx0, y + 3, tx0 + 2, y + rh - 3, withAlpha(ASUNA_PINK, a));
    }

    private int presetCols() {
        int usable = rowX1() - rowX0() - 150 - 12;
        return Mth.clamp(usable / (MIN_PRESET_W + 6), 1, MAX_PRESET_COLS);
    }

    private int presetBlockW(int cols) {
        return Math.min(78, (rowX1() - rowX0() - 150 - 12) / Math.max(1, cols));
    }

    private int[] presetRect(int t, int y, int rh) {
        int cols = presetCols();
        int bw = presetBlockW(cols);
        int bh = Math.max(12, rh - 10);
        int col = t % cols;
        int row = t / cols;
        int x = rowX1() - 10 - (cols - col) * (bw + 6) + 6;
        return new int[]{x, y + row * (bh + 6), bw, bh};
    }

    private void renderChoiceControl(GuiGraphics g, ChoiceSetting spec, int y, int rh, int a) {
        List<ChoiceSetting.Option> options = spec.options();
        ResourceLocation selId = spec.value();
        StringBuilder swatches = new StringBuilder();
        int cols = presetCols();
        if (options.size() > cols) {
            int selected = -1;
            for (int i = 0; i < options.size(); i++) {
                if (options.get(i).id().equals(selId)) {
                    selected = i;
                    break;
                }
            }
            int left = choiceControlLeft();
            int right = rowX1() - 8;
            g.fill(left, y + 2, right, y + rh - 2, withAlpha(ASUNA_PINK, a));
            Component caption = selected >= 0 ? options.get(selected).label() : SELECT_VALUE;
            SaoDraw.drawInRow(g, font, caption, left + 5, y, rh,
                    right - left - 24, withAlpha(RGB_DARK_TEXT, a), false);
            SaoDraw.drawInRow(g, font, "›", right - 15, y, rh, 12,
                    withAlpha(RGB_DARK_TEXT, a), false);
            lastPresetDebug = "hue=" + Math.round(SAOConfig.accentHue()) + " cycle=" + debugId(selId);
            return;
        }
        int shown = options.size();
        for (int t = 0; t < shown; t++) {
            ChoiceSetting.Option option = options.get(t);
            int[] r = presetRect(t, y, rh);
            int bx = r[0];
            int by = r[1];
            int bw = r[2];
            int bh = r[3];
            if (by + bh > y + rh) {
                break;
            }
            boolean sel = selId != null && option.id().equals(selId);
            swatches.append(debugId(option.id())).append(sel ? "*" : "")
                    .append('@').append(bx).append(',').append(by).append(' ');
            int fill = sel ? RGB_WHITE
                    : option.hasSwatch() ? SaoTheme.hsvToRgb(option.swatchHue(), 1f, 1f) : ASUNA_PINK;
            fillSlab(g, bx + bw / 2f, by + bh / 2f, bw, bh, -4f,
                    withAlpha(fill, Math.round(a * (sel ? 1f : 0.85f))));
            var pose = g.pose();
            pose.pushPose();
            pose.translate(bx, by, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-4f));
            drawFitted(g, option.label(), 3, (bh - this.font.lineHeight * 0.85f) / 2f,
                    0.85f, bw - 6, sel ? RGB_DARK_TEXT : RGB_WHITE);
            pose.popPose();
        }
        lastPresetDebug = "hue=" + Math.round(SAOConfig.accentHue()) + " cols=" + presetCols()
                + " | " + swatches.toString().trim();
    }

    private void renderBackButton(GuiGraphics g, SettingsTimeline tl, float off, int a, int mx, int my) {
        int x = sideX() + Math.round(off);
        int y = rowsTop() - 36;
        int width = 64;
        int height = 20;
        boolean hovered = backHover > 0.5f && !tl.inTransition();
        fillSlab(g, x + width / 2f, y + height / 2f, width, height, -4f,
                withAlpha(hovered ? ASUNA_PINK : ASUNA_DEEP, Math.round(a * (hovered ? 1f : 0.72f))));
        fillSlab(g, x - 4f, y + height / 2f, 4, height, -4f, withAlpha(ASUNA_PINK, a));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(-4f));
        drawScaled(g, "◀ " + tr("saomenu.settings.back"), 6, (height - this.font.lineHeight) / 2f + 1, 0.95f,
                withAlpha(hovered ? RGB_DARK_TEXT : RGB_WHITE, a), false);
        pose.popPose();
    }

    private void renderBottomBar(GuiGraphics g, SettingsTimeline tl, boolean root, long now,
                                 boolean exiting, float exitP) {
        long enter = now - tl.pageStartMs();
        float in = exiting
                ? 1f - easeInCubic(clamp01(exitP * 1.4f))
                : easeOutCubic(clamp01((enter - 200f) / 320f));
        if (in <= 0.02f) {
            return;
        }
        int a = alpha(in, 1f);
        int accent = uiAccent(root);
        int deep = uiDeep(root);
        int bw = smallBtnW();
        int by = smallBtnY();
        int bh = smallBtnH();
        renderSmallBtn(g, sideX(), by, bw, bh, tr("saomenu.config.reset"), btnResetHover, a, accent, deep, 0f);
        renderSmallBtn(g, this.w - sideX() - bw, by, bw, bh, tr("saomenu.config.done"),
                btnDoneHover, a, accent, deep, -4f);
    }

    private void renderSmallBtn(GuiGraphics g, int x, int y, int width, int height,
                                String label, boolean hovered, int a, int accent, int deep, float rot) {
        fillSlab(g, x + width / 2f, y + height / 2f, width, height, rot,
                withAlpha(hovered ? accent : deep, Math.round(a * (hovered ? 1f : 0.62f))));
        fillSlab(g, x - 3f, y + height / 2f, 3, height, rot, withAlpha(accent, a));
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rot));
        drawScaled(g, label, (width - this.font.width(label) * 0.95f) / 2f,
                (height - this.font.lineHeight * 0.95f) / 2f, 0.95f,
                withAlpha(hovered ? RGB_DARK_TEXT : RGB_WHITE, a), false);
        pose.popPose();
    }

    private int choiceControlLeft() {
        return rowX0() + (rowX1() - rowX0()) * 55 / 100;
    }

    boolean applyChoiceClick(SettingsGroup group, int mx, int my) {
        if (group == null) {
            return false;
        }
        List<Setting> options = group.options();
        for (int i = 0; i < options.size(); i++) {
            if (!(options.get(i) instanceof ChoiceSetting choice) || !rowVisible(group, i)) {
                continue;
            }
            int y = rowY(group, i);
            int rh = rowH(group);
            List<ChoiceSetting.Option> choices = choice.options();
            if (choices.size() > presetCols()) {
                if (mx >= choiceControlLeft() && mx < rowX1() - 8 && my >= y + 2 && my < y + rh - 2) {
                    choice.cycle(1);
                    return true;
                }
                continue;
            }
            for (int t = 0; t < choices.size(); t++) {
                int[] r = presetRect(t, y, rh);
                if (r[1] + r[3] > y + rh) {
                    break;
                }
                if (mx >= r[0] - 4 && mx <= r[0] + r[2] + 4
                        && my >= r[1] - 2 && my <= r[1] + r[3] + 2) {
                    choice.set(choices.get(t).id());
                    return true;
                }
            }
        }
        return false;
    }

    boolean applySliderAt(SettingsGroup group, int rowIdx, int mx) {
        Setting s = SettingsCatalog.setting(group, rowIdx);
        if (!(s instanceof SliderSetting slider)) {
            return false;
        }
        int x0 = rowX0() + (rowX1() - rowX0()) * 55 / 100;
        int x1 = rowX1() - 58;
        float frac = Mth.clamp((mx - x0) / (float) (x1 - x0), 0f, 1f);
        slider.setFraction(frac);
        return true;
    }

    boolean activateRow(SettingsGroup group, int rowIdx) {
        Setting s = SettingsCatalog.setting(group, rowIdx);
        if (s instanceof ToggleSetting toggle) {
            toggle.toggle();
            return true;
        }
        if (s instanceof ActionSetting action) {
            action.run();
            return true;
        }
        if (s instanceof ChoiceSetting choice) {
            choice.cycle(1);
            return true;
        }
        return false;
    }

    static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f * 1.25f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private static void fillSlab(GuiGraphics g, float cx, float cy, float width, float height,
                                 float angleDeg, int argb) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(cx, cy, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(angleDeg));
        g.fill(Math.round(-width / 2f), Math.round(-height / 2f), Math.round(width / 2f), Math.round(height / 2f), argb);
        pose.popPose();
    }

    private void drawScaled(GuiGraphics g, String text, float x, float y, float scale,
                            int argb, boolean shadow) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(scale, scale, 1f);
        g.drawString(this.font, text, 0, 0, argb, shadow);
        pose.popPose();
    }

    private void drawFitted(GuiGraphics g, Component text, float x, float y, float scale,
                            float maxWidth, int argb) {
        SaoDraw.drawScaled(g, font, SaoDraw.clipTo(font, text, Math.max(0, Math.round(maxWidth / scale))),
                x, y, scale, argb, false);
    }

    private void drawScaledRot(GuiGraphics g, String text, float x, float y, float scale,
                               float rotDeg, int argb, boolean shadow) {
        var pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.mulPose(Axis.ZP.rotationDegrees(rotDeg));
        pose.scale(scale, scale, 1f);
        g.drawString(this.font, text, 0, 0, argb, shadow);
        pose.popPose();
    }

    private static int withAlpha(int rgb, int alpha) {
        return (Mth.clamp(alpha, 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    private static int alpha(float fade, float base) {
        return Math.round(Mth.clamp(fade, 0f, 1f) * base * 255f);
    }

    private static String tr(String key) {
        return Component.translatable(key).getString();
    }


    static String debugId(ResourceLocation id) {
        if (id == null) {
            return "n/a";
        }
        return "saomenu".equals(id.getNamespace()) ? id.getPath() : id.toString();
    }
}
