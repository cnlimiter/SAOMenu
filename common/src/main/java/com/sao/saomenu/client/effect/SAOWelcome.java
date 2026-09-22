package com.sao.saomenu.client.effect;

import com.sao.saomenu.client.hud.SAOHud;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.config.SAOConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.SAOMenuPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic;

/**
 * 进入世界时的 SAO 开场:先全屏 LINK START,再接 "Welcome to Sword Art Online !"
 * 横幅与 Message 面板,停留后整体淡出。
 *
 * <p>时间轴({@link #bannerAlpha} / {@link #linkAlpha} 等)是不依赖 Minecraft 的纯函数,
 * 可单元测试。由 {@link #clientTick} 在「世界真正可见」(加载地形屏已关掉)时触发一次,
 * {@link SAOHud#render} 每帧调用 {@link #render} 绘制。</p>
 */
public final class SAOWelcome {

    /** 横幅贴图 625x97:上半为标题文字,下半为装饰横条。 */
    private static final ResourceLocation TEX_WELCOME =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/welcome.png");
    /** Message 面板贴图 350x237(自带投影与圆角,body x8..343 / y8..227)。 */
    private static final ResourceLocation TEX_PANEL =
            new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/message_panel.png");

    private static final int TEX_W_W = 625;
    private static final int TEX_W_H = 97;
    private static final int TEX_P_W = 350;
    private static final int TEX_P_H = 237;

    /** 面板灰色文字带在贴图内的纵向中心(v 81..161)。 */
    private static final float PANEL_MSG_V = 121f / TEX_P_H;
    /** 面板 body 的横向中心(x 8..343)。 */
    private static final float PANEL_MSG_U = 175.5f / TEX_P_W;

    private static final int MSG_DARK = 0xFF3A3D3F;
    /** 面板贴图本身只有 80% 不透明,先垫一层白底,避免地形透上来压掉文字。 */
    private static final int PANEL_BASE = 0xFFF7F7F7;

    // ------------------------------------------------------------ 时间轴(毫秒)

    /** LINK START 全屏:淡入 + 停留 + 淡出;结束后才开始欢迎横幅。 */
    public static final long LINK_IN_MS = 220;
    public static final long LINK_OUT_MS = 280;
    public static final long LINK_MS = 860;

    public static final long BANNER_IN_MS = 480;
    public static final long PANEL_DELAY_MS = 300;
    public static final long PANEL_IN_MS = 420;
    public static final long TEXT_DELAY_MS = 820;
    public static final long TEXT_IN_MS = 320;
    public static final long HOLD_MS = 2600;
    public static final long FADE_MS = 700;
    /** 动画总时长:LINK START + 欢迎文字出现完毕 + 停留 + 淡出。 */
    public static final long TOTAL_MS = LINK_MS + TEXT_DELAY_MS + TEXT_IN_MS + HOLD_MS + FADE_MS;

    /** 淡出开始时刻。 */
    public static final long FADE_AT_MS = TOTAL_MS - FADE_MS;

    private static long startAt = Long.MIN_VALUE;
    /** 等加载地形屏关掉再开播,避免 4 秒动画在「正在加载地形」期间就播完。 */
    private static boolean pendingStart;

    private SAOWelcome() {
    }

    // ------------------------------------------------------------ 触发

    /** Runtime arms one welcome for a new session; loading screens must finish first. */
    public static void scheduleStart() {
        dismiss();
        pendingStart = true;
    }

    public static void reset() {
        pendingStart = false;
        dismiss();
    }

    public static void clientTick(Minecraft mc) {
        if (pendingStart && mc.player != null && mc.level != null
                && mc.screen == null && mc.getOverlay() == null) {
            start();
            pendingStart = false;
        }
    }

    /** 立即开始播放(重复调用会重新计时)。 */
    public static void start() {
        if (!SAOConfig.showWelcome()) {
            return;
        }
        startAt = net.minecraft.Util.getMillis();
        if (SAOConfig.sounds()) {
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SAOMenuPlatform.launcherSound(), 0.8F));
        }
    }

    /** 立即结束(预览自检在抓其他截图前调用,避免遮挡)。 */
    public static void dismiss() {
        startAt = Long.MIN_VALUE;
    }

    /** 当前是否正在播放。 */
    public static boolean active() {
        return startAt != Long.MIN_VALUE
                && !finished(net.minecraft.Util.getMillis() - startAt);
    }

    // ------------------------------------------------------------ 时间轴纯函数

    public static boolean finished(long elapsed) {
        return elapsed < 0 || elapsed >= TOTAL_MS;
    }

    /** 横幅:淡入后保持,最后随整体淡出。 */
    public static float bannerAlpha(long elapsed) {
        return easeOutCubic(clamp01((elapsed - LINK_MS) / (float) BANNER_IN_MS)) * globalFade(elapsed);
    }

    /** 面板:延迟后淡入,最后随整体淡出。 */
    public static float panelAlpha(long elapsed) {
        return easeOutCubic(clamp01((elapsed - LINK_MS - PANEL_DELAY_MS) / (float) PANEL_IN_MS))
                * globalFade(elapsed);
    }

    /** 面板内提示文字:面板站稳后才出现。 */
    public static float textAlpha(long elapsed) {
        return clamp01((elapsed - LINK_MS - TEXT_DELAY_MS) / (float) TEXT_IN_MS) * globalFade(elapsed);
    }

    /** 整体淡出系数:淡出开始前恒为 1。 */
    public static float globalFade(long elapsed) {
        if (elapsed <= FADE_AT_MS) {
            return 1f;
        }
        return 1f - clamp01((elapsed - FADE_AT_MS) / (float) FADE_MS);
    }

    /** 面板弹出缩放:0.88 → 1.0。 */
    public static float panelScale(long elapsed) {
        return 0.88f + 0.12f * easeOutCubic(clamp01((elapsed - LINK_MS - PANEL_DELAY_MS) / (float) PANEL_IN_MS));
    }

    /** 横幅从中心向外展开的进度(0 → 1)。 */
    public static float bannerReveal(long elapsed) {
        return easeOutCubic(clamp01((elapsed - LINK_MS) / (float) BANNER_IN_MS));
    }

    /** LINK START 全屏不透明系数;欢迎阶段开始前完全归零。 */
    public static float linkAlpha(long elapsed) {
        if (elapsed < 0 || elapsed >= LINK_MS) {
            return 0f;
        }
        if (elapsed < LINK_IN_MS) {
            return easeOutCubic(clamp01(elapsed / (float) LINK_IN_MS));
        }
        if (elapsed <= LINK_MS - LINK_OUT_MS) {
            return 1f;
        }
        return 1f - clamp01((elapsed - (LINK_MS - LINK_OUT_MS)) / (float) LINK_OUT_MS);
    }

    /** LINK START 字号弹出:0.72 → 1.0。 */
    public static float linkScale(long elapsed) {
        return 0.72f + 0.28f * easeOutCubic(clamp01(elapsed / (float) LINK_IN_MS));
    }

    /** 中心横线从中点向外展开的进度。 */
    public static float linkLine(long elapsed) {
        return easeOutCubic(clamp01(elapsed / (float) LINK_IN_MS));
    }

    // ------------------------------------------------------------ 渲染

    /** 每帧绘制(由 SAOHud.render 调用,不受 showHud 开关影响)。 */
    public static void render(GuiGraphics g, int screenW, int screenH) {
        if (startAt == Long.MIN_VALUE) {
            return;
        }
        long elapsed = net.minecraft.Util.getMillis() - startAt;
        if (finished(elapsed)) {
            startAt = Long.MIN_VALUE;
            return;
        }
        if (elapsed < LINK_MS) {
            float alpha = linkAlpha(elapsed);
            if (alpha > 0.004f) {
                renderLinkStart(g, screenW, screenH, elapsed, alpha);
            }
            return;
        }

        // 整体缩小到原设计的 2/3(横幅与面板都由横幅宽度派生)
        int bannerW = Math.min(Math.round(screenW * 0.70f * 2f / 3f), Math.max(80, screenW - 24));
        int bannerH = Math.max(8, Math.round(bannerW * TEX_W_H / (float) TEX_W_W));
        int bannerX = (screenW - bannerW) / 2;
        // 横幅横跨屏幕中上部,会压到左上角血条板与效果图标行,故下移到它们之下
        int top = Math.round(screenH * 0.10f);
        if (SAOConfig.showHud()) {
            top = Math.max(top, MenuLayout.plateBottom(screenW) + 22 + 6);
        }
        int bannerY = top;

        int panelW = Math.max(60, Math.round(bannerW * 0.42f));
        int panelH = Math.max(40, Math.round(panelW * TEX_P_H / (float) TEX_P_W));
        int panelCx = screenW / 2;
        int panelTop = Math.min(
                bannerY + bannerH + Math.round(screenH * 0.05f),
                Math.max(bannerY + bannerH, screenH - panelH - 8));
        int panelCy = panelTop + panelH / 2;

        float ba = bannerAlpha(elapsed);
        if (ba > 0.004f) {
            // 从中间向外展开:宽度从 0 拉伸到全宽,中心固定
            float reveal = bannerReveal(elapsed);
            int drawW = Math.max(1, Math.round(bannerW * reveal));
            int drawX = bannerX + (bannerW - drawW) / 2;
            RenderSystem.enableBlend();
            shaderAlpha(ba);
            g.blit(TEX_WELCOME, drawX, bannerY, drawW, bannerH,
                    0f, 0f, TEX_W_W, TEX_W_H, TEX_W_W, TEX_W_H);
            shaderAlpha(1f);
        }

        float pa = panelAlpha(elapsed);
        if (pa > 0.004f) {
            float s = panelScale(elapsed);
            g.pose().pushPose();
            g.pose().translate(panelCx, panelCy, 0f);
            g.pose().scale(s, s, 1f);
            g.pose().translate(-panelW / 2f, -panelH / 2f, 0f);

            // 贴图 body(x8..343 / y8..227)只有 80% 不透明,先垫白底再叠贴图,
            // 否则地形会从面板里透出来盖掉 Message 文字
            int bodyL = Math.round(panelW * 8f / TEX_P_W);
            int bodyR = Math.round(panelW * 343f / TEX_P_W);
            int bodyT = Math.round(panelH * 8f / TEX_P_H);
            int bodyB = Math.round(panelH * 227f / TEX_P_H);
            g.fill(bodyL, bodyT, bodyR, bodyB, mulAlpha(PANEL_BASE, pa));
            // fill() 收尾会关掉混合,必须在 blit 前重新开启,
            // 否则贴图四周的半透明投影会被画成实心黑框
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            shaderAlpha(pa);
            g.blit(TEX_PANEL, 0, 0, panelW, panelH, 0f, 0f, TEX_P_W, TEX_P_H, TEX_P_W, TEX_P_H);
            shaderAlpha(1f);

            // 灰色文字带内的主题色左镶边(与通知横幅统一的视觉语言)
            int bandTop = Math.round(panelH * 81f / TEX_P_H);
            int bandBot = Math.round(panelH * 161f / TEX_P_H);
            g.fill(bodyL, bandTop, bodyL + Math.max(1, Math.round(panelW * 0.011f)), bandBot,
                    mulAlpha(SaoTheme.accent(), pa));

            float ta = textAlpha(elapsed);
            if (ta > 0.004f) {
                Font font = com.sao.saomenu.api.SaoUi.bodyFont();
                String msg = Component.translatable("saomenu.welcome.msg").getString();
                // 参考图里提示文字约占面板 body 宽的 44%,8px 字体直接画偏小,放大后再绘制
                float ts = 1.5f;
                g.pose().pushPose();
                g.pose().translate(panelW * PANEL_MSG_U, panelH * PANEL_MSG_V, 0f);
                g.pose().scale(ts, ts, 1f);
                g.drawString(font, msg, -font.width(msg) / 2, -font.lineHeight / 2,
                        mulAlpha(MSG_DARK, ta), false);
                g.pose().popPose();
            }
            g.pose().popPose();
        }
    }

    /**
     * LINK START 全屏:深色罩 + 中心横线外扩 + 大字弹出。
     * 此阶段不推进也不绘制 Welcome 横幅与 Message 面板。
     */
    private static void renderLinkStart(GuiGraphics g, int screenW, int screenH,
                                        long elapsed, float la) {
        g.fill(0, 0, screenW, screenH, mulAlpha(0xFF05070A, la * 0.90f));
        int cy = screenH / 2;
        float reveal = linkLine(elapsed);
        int lineW = Math.max(8, Math.round(screenW * 0.62f * reveal));
        int lineH = Math.max(1, Math.round(screenH * 0.005f));
        int lineY = cy + Math.round(screenH * 0.055f);
        g.fill(screenW / 2 - lineW / 2, lineY, screenW / 2 + lineW / 2, lineY + lineH,
                mulAlpha(SaoTheme.accent(), la));
        Font font = com.sao.saomenu.api.SaoUi.bodyFont();
        float ts = Math.max(2.6f, screenH / 85f) * linkScale(elapsed);
        SaoDraw.drawCentered(g, font, "LINK START",
                screenW / 2f, cy - 2f, ts, mulAlpha(0xFFF4F7FA, la), false);
    }

    // ------------------------------------------------------------ 小工具

    private static void shaderAlpha(float a) {
        RenderSystem.setShaderColor(1f, 1f, 1f, Mth.clamp(a, 0f, 1f));
    }


}
