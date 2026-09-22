package com.sao.saomenu.client.screen.settings;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * 设置背景视频帧状态机与页面转场时间轴。不持有选项表或 P5 控件几何。
 */
final class SettingsTimeline {

    static final ResourceLocation TEX_BG =
            new ResourceLocation("saomenu", "textures/gui/settings_bg.png");
    static final int FRAME_W = 480;
    static final int FRAME_H = 270;
    static final int FRAME_COLS = 16;
    static final int FRAME_COUNT = 156;
    static final int FRAME_FPS = 15;
    static final int ATLAS_W = FRAME_COLS * FRAME_W;
    static final int ATLAS_H = ((FRAME_COUNT + FRAME_COLS - 1) / FRAME_COLS) * FRAME_H;
    /** 开场定格点:桐人眼睛特写(视频 t≈5s → 15fps 帧 75)。 */
    static final int BASE_FRAME = 75;
    /** 分类页正播终点:亚斯娜脸特写(视频 t≈10s → 15fps 帧 150)。 */
    static final int END_FRAME = 150;
    /** 开场扫入倍速(0 → BASE_FRAME)。 */
    static final float SCAN_SPEED = 4f;

    static final long TR_SWAP_MS = 290;
    static final long TR_TOTAL_MS = 480;
    static final long ENTER_STAGGER_MS = 55;

    private enum BgMode {FORWARD, REVERSE}

    private SettingsPage page = SettingsPage.ROOT;
    private SettingsPage transitionFrom = SettingsPage.ROOT;
    private long transitionStart = -1;
    private boolean transitionForward;
    private int clickedCat = -1;
    private long pageStartMs;
    private long lastFrameMs;
    private boolean initialized;

    private BgMode bgMode = BgMode.FORWARD;
    private int bgFrame0;
    private int bgFrame1 = BASE_FRAME;
    private float bgSpeed = SCAN_SPEED;
    private long bgModeAt;

    /**
     * 仅首次进入时启动开场扫入。窗口 resize 会再调 {@code Screen.init()},
     * 不得重置当前页、转场或入场时间戳。
     */
    void initOnce(long now) {
        if (initialized) {
            return;
        }
        initialized = true;
        bgMode = BgMode.FORWARD;
        bgFrame0 = 0;
        bgFrame1 = BASE_FRAME;
        bgSpeed = SCAN_SPEED;
        bgModeAt = now;
        pageStartMs = now;
        lastFrameMs = now;
    }

    float tickDt(long now) {
        float dt = Math.min(0.1f, (now - lastFrameMs) / 1000f);
        lastFrameMs = now;
        return dt;
    }

    SettingsPage page() {
        return page;
    }

    SettingsPage transitionFrom() {
        return transitionFrom;
    }

    boolean transitionForward() {
        return transitionForward;
    }

    int clickedCat() {
        return clickedCat;
    }

    long pageStartMs() {
        return pageStartMs;
    }

    boolean inTransition() {
        return transitionStart >= 0;
    }

    long transitionElapsed(long now) {
        return inTransition() ? now - transitionStart : 0;
    }

    SettingsPage pageToRender(long now) {
        boolean inTransition = this.transitionStart >= 0;
        long trT = inTransition ? now - this.transitionStart : 0;
        return inTransition && trT < TR_SWAP_MS ? this.transitionFrom : this.page;
    }

    SettingsPage transitionTarget() {
        return this.transitionForward ? SettingsPage.CATEGORIES[this.clickedCat] : SettingsPage.ROOT;
    }

    void beginEnterCategory(int catIndex, long now) {
        this.clickedCat = catIndex;
        this.transitionFrom = SettingsPage.ROOT;
        this.transitionForward = true;
        this.transitionStart = now;
        bgMode = BgMode.FORWARD;
        bgFrame0 = currentBgFrame(now);
        bgFrame1 = END_FRAME;
        bgSpeed = 1f;
        bgModeAt = now;
    }

    void beginReturnToRoot(long now) {
        this.transitionFrom = this.page;
        this.transitionForward = false;
        this.transitionStart = now;
        bgMode = BgMode.REVERSE;
        bgFrame0 = currentBgFrame(now);
        bgFrame1 = BASE_FRAME;
        bgSpeed = 1f;
        bgModeAt = now;
    }

    /**
     * 推进换页。返回 true 表示本帧刚切到目标页(调用方播放 panel 音)。
     */
    boolean tick(long now) {
        if (this.transitionStart < 0) {
            return false;
        }
        long trT = now - this.transitionStart;
        boolean swapped = false;
        if (trT >= TR_SWAP_MS && this.page != transitionTarget()) {
            this.page = transitionTarget();
            this.pageStartMs = now;
            swapped = true;
        }
        if (trT >= TR_TOTAL_MS) {
            this.transitionStart = -1;
            this.clickedCat = -1;
        }
        return swapped;
    }

    void debugShowPage(SettingsPage want, long now) {
        for (int i = 0; i < SettingsPage.CATEGORIES.length; i++) {
            if (SettingsPage.CATEGORIES[i] == want) {
                this.clickedCat = i;
            }
        }
        this.page = want;
        this.transitionFrom = want;
        this.transitionStart = -1;
        this.clickedCat = -1;
        this.pageStartMs = now - 1000L;
    }

    int currentBgFrame(long now) {
        long step = Math.round((now - bgModeAt) * FRAME_FPS * bgSpeed / 1000.0);
        if (bgMode == BgMode.FORWARD) {
            return (int) Mth.clamp(bgFrame0 + step, Math.min(bgFrame0, bgFrame1),
                    Math.max(bgFrame0, bgFrame1));
        }
        return (int) Mth.clamp(bgFrame0 - step, Math.min(bgFrame0, bgFrame1),
                Math.max(bgFrame0, bgFrame1));
    }

    void renderVideoBackground(GuiGraphics g, int width, int height, long now) {
        int idx = Mth.clamp(currentBgFrame(now), 0, FRAME_COUNT - 1);
        int col = idx % FRAME_COLS;
        int row = idx / FRAME_COLS;
        float scale = Math.max(width / (float) FRAME_W, height / (float) FRAME_H);
        int dw = Math.round(FRAME_W * scale);
        int dh = Math.round(FRAME_H * scale);
        g.blit(TEX_BG, (width - dw) / 2, (height - dh) / 2, dw, dh,
                (float) (col * FRAME_W), (float) (row * FRAME_H),
                FRAME_W, FRAME_H, ATLAS_W, ATLAS_H);
    }
}
