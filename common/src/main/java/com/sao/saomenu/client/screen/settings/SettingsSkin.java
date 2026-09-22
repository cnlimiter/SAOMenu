package com.sao.saomenu.client.screen.settings;

import java.util.Locale;

import com.mojang.math.Axis;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import com.sao.saomenu.ui.theme.SaoThemeLibrary;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
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

    static final int KIRITO_BLUE = 0x4FA8E8;
    static final int KIRITO_BRIGHT = 0xA8DFFF;
    static final int KIRITO_DEEP = 0x101C30;
    static final int ASUNA_PINK = 0xFF8AB0;
    static final int ASUNA_BRIGHT = 0xFFC9DA;
    static final int ASUNA_DEEP = 0x2C1420;
    static final int RGB_WHITE = 0xF8FAFC;
    static final int RGB_GRAY = 0xB9BEC6;
    static final int RGB_DARK_TEXT = 0x16171A;

    private final float[] catHover = new float[SettingsPage.CATEGORIES.length];
    private float backHover;
    private int hoverRow = -1;
    private int dragRow = -1;
    private boolean btnResetHover;
    private boolean btnDoneHover;
    private String lastPresetDebug = "";

    private Font font;
    private int w;
    private int h;

    void bind(Font font, int width, int height) {
        this.font = font;
        this.w = width;
        this.h = height;
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

    int uiAccent(SettingsPage p) {
        return p == SettingsPage.ROOT ? KIRITO_BLUE : ASUNA_PINK;
    }

    int uiDeep(SettingsPage p) {
        return p == SettingsPage.ROOT ? KIRITO_DEEP : ASUNA_DEEP;
    }

    int sideX() {
        return Math.max(14, (int) (this.w * 0.07f));
    }

    int catW() {
        return Mth.clamp((int) (this.w * 0.36f), 170, 330);
    }

    int catH() {
        return Mth.clamp((this.h - 120) / 4 - 8, 24, 44);
    }

    int catGap() {
        return Math.max(8, catH() / 4);
    }

    int catY0() {
        return Math.max(56, (this.h - (catH() * 4 + catGap() * 3)) / 2 + 4);
    }

    int catX(int i) {
        return sideX() + i * 18 + Math.round(catHover[i] * 8f);
    }

    int catY(int i) {
        return catY0() + i * (catH() + catGap());
    }

    boolean catHovered(int i, int mx, int my) {
        int x = catX(i);
        return mx >= x - 16 && mx <= x + catW() - 10
                && my >= catY(i) - 2 && my < catY(i) + catH() + 2;
    }

    int hitCategory(int mx, int my) {
        for (int i = 0; i < SettingsPage.CATEGORIES.length; i++) {
            if (catHovered(i, mx, my)) {
                return i;
            }
        }
        return -1;
    }

    int rowsTop() {
        return Math.max(58, (int) (this.h * 0.20f));
    }

    int rowH(SettingsPage page) {
        int avail = this.h - rowsTop() - 46;
        return Mth.clamp(avail / Math.max(1, SettingsCatalog.rowCount(page)), 16, 34);
    }

    int rowY(SettingsPage page, int i) {
        return rowsTop() + i * rowH(page);
    }

    int rowX0() {
        return sideX();
    }

    int rowX1() {
        return this.w - sideX();
    }

    boolean rowHovered(SettingsPage page, int i, int mx, int my) {
        int y = rowY(page, i);
        return mx >= rowX0() && mx <= rowX1() && my >= y && my < y + rowH(page);
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
        for (int i = 0; i < SettingsPage.CATEGORIES.length; i++) {
            boolean target = tl.page() == SettingsPage.ROOT && !tl.inTransition()
                    && catHovered(i, mouseX, mouseY);
            catHover[i] += ((target ? 1f : 0f) - catHover[i]) * k;
        }
        boolean backTarget = tl.page() != SettingsPage.ROOT && !tl.inTransition()
                && backHovered(mouseX, mouseY);
        backHover += ((backTarget ? 1f : 0f) - backHover) * k;

        int newHover = -1;
        if (tl.page() != SettingsPage.ROOT && !tl.inTransition()) {
            for (int i = 0; i < SettingsCatalog.rowCount(tl.page()); i++) {
                if (rowHovered(tl.page(), i, mouseX, mouseY)) {
                    newHover = i;
                    break;
                }
            }
        }
        hoverRow = newHover;

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
                withAlpha(uiAccent(tl.page()), 46));
    }

    void renderScrim(GuiGraphics g) {
        g.fillGradient(0, 0, this.w, this.h, 0x1E000000, 0x61000000);
    }

    void renderPage(GuiGraphics g, SettingsTimeline tl, SettingsPage p, int mx, int my,
                    long now, boolean inTransition, long trT) {
        float exitP = inTransition && trT < SettingsTimeline.TR_SWAP_MS
                ? Mth.clamp(trT / (float) SettingsTimeline.TR_SWAP_MS, 0f, 1f) : 0f;
        boolean exiting = exitP > 0f;
        if (p == SettingsPage.ROOT) {
            renderRootPage(g, tl, mx, my, now, exiting, exitP);
        } else {
            renderRowsPage(g, tl, p, mx, my, now, exiting, exitP);
        }
        renderBottomBar(g, tl, p, now, exiting, exitP);
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

        for (int i = 0; i < SettingsPage.CATEGORIES.length; i++) {
            float off;
            float alphaF;
            if (exiting) {
                boolean lead = tl.transitionForward() && i == tl.clickedCat();
                float local = clamp01((exitP * SettingsTimeline.TR_SWAP_MS - (lead ? 0f : 40f + i * 30f)) / 230f);
                off = (lead ? -1f : 1f) * easeInCubic(local) * this.w * 0.75f;
                alphaF = 1f - easeInCubic(local);
            } else {
                float enterT = clamp01((enter - 140f - i * SettingsTimeline.ENTER_STAGGER_MS) / 380f);
                off = (1f - easeOutBack(enterT)) * -220f;
                alphaF = clamp01(enterT * 1.8f);
            }
            renderCategory(g, tl, i, off, alphaF, mx, my);
        }

        int hi = -1;
        for (int i = 0; i < SettingsPage.CATEGORIES.length; i++) {
            if (catHover[i] > 0.35f) {
                hi = i;
            }
        }
        if (hi >= 0 && !exiting) {
            String num = String.format(Locale.ROOT, "0%d", hi + 1);
            float gs = this.h / 26f;
            drawScaledRot(g, num, this.w * 0.70f, this.h * 0.26f, gs, -10f,
                    withAlpha(KIRITO_BLUE, Math.round(catHover[hi] * 52)), false);
        }
    }

    private void renderCategory(GuiGraphics g, SettingsTimeline tl, int i, float xOff, float alphaF,
                                int mx, int my) {
        if (alphaF <= 0.02f) {
            return;
        }
        int a = alpha(alphaF, 1f);
        int width = catW();
        int height = catH();
        int x = catX(i) + Math.round(xOff);
        int y = catY(i);
        float hv = catHover[i];
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
        drawScaled(g, String.format(Locale.ROOT, "0%d", i + 1), 0, 2, 0.7f,
                withAlpha(hovered ? KIRITO_BRIGHT : RGB_GRAY, Math.round(a * 0.85f)), false);
        drawScaled(g, tr(SettingsPage.CATEGORIES[i].titleKey()), 0, height * 0.5f - 6, Math.min(1.6f, height / 20f),
                withAlpha(hovered ? KIRITO_BRIGHT : RGB_WHITE, a), false);
        drawScaled(g, tr(SettingsPage.CATEGORIES[i].subKey()), 1, height - 10, 0.62f,
                withAlpha(RGB_GRAY, Math.round(a * 0.8f)), false);
        pose.popPose();
    }

    private void renderRowsPage(GuiGraphics g, SettingsTimeline tl, SettingsPage p, int mx, int my,
                                long now, boolean exiting, float exitP) {
        long enter = now - tl.pageStartMs();
        int accent = ASUNA_PINK;

        float headIn = exiting ? 1f : easeOutCubic(clamp01(enter / 300f));
        float headOff = exiting ? easeInCubic(exitP) * this.w * 0.55f : (1f - headIn) * 200f;
        renderBackButton(g, tl, headOff, alpha(headIn, 1f), mx, my);
        int titleX = sideX() + 76;
        drawScaled(g, tr(p.titleKey()), titleX + headOff, rowsTop() - 32, 1.45f,
                withAlpha(RGB_WHITE, alpha(headIn, 1f)), false);
        fillSlab(g, titleX - 8 + headOff, rowsTop() - 22, 3.4f, 21f, -6f,
                withAlpha(accent, alpha(headIn, 1f)));
        drawScaled(g, tr(p.subKey()), titleX + 2 + headOff, rowsTop() - 14, 0.62f,
                withAlpha(RGB_GRAY, Math.round(headIn * 220)), false);

        int n = SettingsCatalog.rowCount(p);
        for (int i = 0; i < n; i++) {
            float local;
            if (exiting) {
                local = clamp01((exitP * SettingsTimeline.TR_SWAP_MS - i * 22f) / 200f);
            } else {
                local = clamp01((enter - 60f - i * SettingsTimeline.ENTER_STAGGER_MS) / 360f);
            }
            float off = exiting
                    ? easeInCubic(local) * this.w * 0.7f
                    : (1f - easeOutBack(local)) * 190f;
            float alphaF = exiting ? 1f - easeInCubic(local) : clamp01(local * 2.2f);
            renderRow(g, tl, p, i, off, alphaF, mx, my);
        }
    }

    private void renderRow(GuiGraphics g, SettingsTimeline tl, SettingsPage p, int i, float off,
                           float alphaF, int mx, int my) {
        if (alphaF <= 0.02f) {
            return;
        }
        int a = alpha(alphaF, 1f);
        int accent = ASUNA_PINK;
        int x0 = rowX0() + Math.round(off);
        int x1 = rowX1() + Math.round(off);
        int y = rowY(p, i);
        int rh = rowH(p);
        boolean hovered = this.hoverRow == i && !tl.inTransition() && alphaF > 0.95f;
        OptionSpec spec = SettingsCatalog.spec(p, i);

        g.fill(x0 + 10, y + 1, x1, y + rh - 1,
                withAlpha(ASUNA_DEEP, Math.round(a * (hovered ? 0.72f : 0.34f))));
        fillSlab(g, x0 + 7f, y + rh / 2f, 4, rh - 6f, -8f,
                withAlpha(hovered ? ASUNA_BRIGHT : accent, Math.round(a * (hovered ? 1f : 0.72f))));
        SaoDraw.drawInRow(g, this.font, rowLabel(spec), x0 + 18, y, rh,
                Math.max(8, (x1 - x0) * 0.48f),
                withAlpha(RGB_WHITE, Math.round(a * (hovered ? 1f : 0.92f))), false);

        if (spec != null && spec.isSlider()) {
            renderSliderControl(g, spec, i, x0, x1, y, rh, a);
        } else if (spec != null && spec.kind() == OptionSpec.Kind.PRESET) {
            renderPresets(g, y, rh, a);
        } else {
            renderToggleControl(g, spec, x1, y, rh, a);
        }
    }

    private void renderSliderControl(GuiGraphics g, OptionSpec spec, int i, int x0, int x1,
                                     int y, int rh, int a) {
        int accent = ASUNA_PINK;
        int tx0 = x0 + (x1 - x0) * 55 / 100;
        int tx1 = x1 - 58;
        float v = spec.get() == null ? 0f : spec.get().get();
        float lo = spec.min();
        float hi = spec.max();
        float frac = Mth.clamp((v - lo) / (hi - lo), 0f, 1f);

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

        SaoDraw.drawInRow(g, this.font, spec.format(v), tx1 + 6, y, rh,
                Math.max(8, x1 - tx1 - 8), withAlpha(RGB_GRAY, a), false);
    }

    private void renderToggleControl(GuiGraphics g, OptionSpec spec, int x1, int y, int rh, int a) {
        boolean on = spec != null && spec.boolGet() != null && spec.boolGet().get();
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

    private void renderPresets(GuiGraphics g, int y, int rh, int a) {
        var presets = SaoTheme.presets();
        String selId = SaoTheme.matchingPreset(SAOConfig.accentHue());
        StringBuilder swatches = new StringBuilder();
        for (int t = 0; t < presets.size(); t++) {
            var preset = presets.get(t);
            int[] r = presetRect(t, y, rh);
            int bx = r[0];
            int by = r[1];
            int bw = r[2];
            int bh = r[3];
            boolean sel = preset.id().equals(selId);
            swatches.append(preset.id()).append(sel ? "*" : "")
                    .append('@').append(bx).append(',').append(by).append(' ');
            fillSlab(g, bx + bw / 2f, by + bh / 2f, bw, bh, -4f,
                    withAlpha(sel ? RGB_WHITE
                                    : SaoTheme.hsvToRgb(preset.defaultHue(), 1f, 1f),
                            Math.round(a * (sel ? 1f : 0.85f))));
            var pose = g.pose();
            pose.pushPose();
            pose.translate(bx, by, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(-4f));
            drawScaled(g, SaoThemeLibrary.label(preset.id()),
                    3, (bh - this.font.lineHeight * 0.85f) / 2f, 0.85f,
                    sel ? RGB_DARK_TEXT : RGB_WHITE, false);
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

    private void renderBottomBar(GuiGraphics g, SettingsTimeline tl, SettingsPage p, long now,
                                 boolean exiting, float exitP) {
        long enter = now - tl.pageStartMs();
        float in = exiting
                ? 1f - easeInCubic(clamp01(exitP * 1.4f))
                : easeOutCubic(clamp01((enter - 200f) / 320f));
        if (in <= 0.02f) {
            return;
        }
        int a = alpha(in, 1f);
        int accent = uiAccent(p);
        int deep = uiDeep(p);
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

    boolean applyPresetClick(SettingsPage page, int mx, int my) {
        int row = SettingsCatalog.indexOf(page, OptionSpec.Kind.PRESET);
        if (row < 0) {
            return false;
        }
        var presets = SaoTheme.presets();
        int y = rowY(page, row);
        int rh = rowH(page);
        for (int t = 0; t < presets.size(); t++) {
            int[] r = presetRect(t, y, rh);
            if (mx >= r[0] - 4 && mx <= r[0] + r[2] + 4
                    && my >= r[1] - 2 && my <= r[1] + r[3] + 2) {
                SaoTheme.select(presets.get(t).id());
                return true;
            }
        }
        return false;
    }

    boolean applySliderAt(SettingsPage page, int rowIdx, int mx) {
        OptionSpec s = SettingsCatalog.spec(page, rowIdx);
        if (s == null || s.set() == null) {
            return false;
        }
        int x0 = rowX0() + (rowX1() - rowX0()) * 55 / 100;
        int x1 = rowX1() - 58;
        float frac = Mth.clamp((mx - x0) / (float) (x1 - x0), 0f, 1f);
        s.set().set(s.min() + frac * (s.max() - s.min()));
        return true;
    }

    boolean flipToggle(SettingsPage page, int rowIdx) {
        OptionSpec s = SettingsCatalog.spec(page, rowIdx);
        if (s == null || s.boolFlip() == null) {
            return false;
        }
        s.boolFlip().flip();
        return true;
    }

    private static String rowLabel(OptionSpec s) {
        return tr(s == null ? "" : s.labelKey());
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
}
