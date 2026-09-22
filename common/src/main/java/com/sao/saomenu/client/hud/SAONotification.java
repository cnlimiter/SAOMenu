package com.sao.saomenu.client.hud;

import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import static com.sao.saomenu.ui.render.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;
import static com.sao.saomenu.ui.animation.SaoMotion.easeOutCubic;

/**
 * SAO 风格通知系统:右上角白底半透明横幅(主题色镶边),滑入停留后淡出。
 *
 * <p>数据层({@link #push}/{@link #prune})不依赖渲染,可单元测试;
 * 标题与正文保留 {@link Component} 样式。图标入队时复制 ItemStack,避免展示物被原持有者改掉。</p>
 */
public final class SAONotification {

    /** 一条通知。message 可为空(只显示标题);icon 可为 null。 */
    public record Entry(Component title, Component message, ItemStack icon, long at) {
    }

    private static final List<Entry> QUEUE = new ArrayList<>();

    private static final long SLIDE_MS = 150;
    private static final long STAY_MS = 2600;
    private static final long FADE_MS = 300;
    private static final int MAX_ENTRIES = 4;

    // 白色底、略透明(85%);文字改用深色保证可读性
    private static final int PANEL_BG = 0xD9FFFFFF;
    private static final int SHADOW = 0x3A000000;
    private static final int TITLE_DARK = 0xFF1B1D1F;
    private static final int MSG_DARK = 0xFF585C5E;

    private SAONotification() {
    }

    /** 入队一条通知并播放提示音(受 SAOConfig.sounds 控制)。 */
    public static void push(Component title, Component message) {
        push(title, message, null);
    }

    /** 带图标入队(成就通知传入成就展示物品)。图标按拷贝入队。 */
    public static void push(Component title, Component message, ItemStack icon) {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(message, "message");
        ItemStack copied = icon == null ? null : icon.copy();
        long now = now();
        QUEUE.add(new Entry(title, message, copied, now));
        while (QUEUE.size() > MAX_ENTRIES) {
            QUEUE.remove(0);
        }
        playSound();
    }

    /** 移除已完全淡出的通知。 */
    public static void prune(long now) {
        Iterator<Entry> it = QUEUE.iterator();
        while (it.hasNext()) {
            if (now - it.next().at() > STAY_MS + FADE_MS) {
                it.remove();
            }
        }
    }

    public static int size() {
        return QUEUE.size();
    }

    public static void clear() {
        QUEUE.clear();
    }

    static Entry at(int index) {
        return QUEUE.get(index);
    }

    /** 每帧绘制(右上角纵向堆叠,滑入 + 淡出)。 */
    public static void render(GuiGraphics g, int screenW, int screenH, long now, float alphaMul) {
        prune(now);
        Font font = Minecraft.getInstance().font;
        float mul = Mth.clamp(alphaMul, 0f, 1f);
        int i = 0;
        for (Entry e : QUEUE) {
            long age = now - e.at();
            if (age < 0) {
                continue;
            }
            float slide = easeOutCubic(clamp01(age / (float) SLIDE_MS));
            float alpha = (1f - clamp01((age - STAY_MS) / (float) FADE_MS)) * mul;
            if (alpha <= 0f) {
                continue;
            }
            boolean noMsg = blank(e.message());
            int w = Math.max(140, Math.round(screenW * 0.30f));
            int h = noMsg ? 26 : 34;
            int x = Math.round(screenW + 8 - (w + 16) * slide);
            int y = 8 + i * 40;

            g.fill(x + 3, y + 3, x + w + 3, y + h + 3, mulAlpha(SHADOW, alpha));
            g.fill(x, y, x + w, y + h, mulAlpha(PANEL_BG, alpha));
            g.fill(x, y, x + 3, y + h, mulAlpha(SaoTheme.accent(), alpha));
            int textX = x + 10;
            if (e.icon() != null && !e.icon().isEmpty()) {
                int isz = Math.min(16, h - 8);
                g.pose().pushPose();
                g.pose().translate(x + 8 + isz / 2f, y + h / 2f, 120f);
                g.pose().scale(isz / 16f, isz / 16f, 1f);
                g.renderItem(e.icon(), -8, -8);
                g.pose().popPose();
                textX = x + 8 + isz + 6;
            }
            int maxW = w - (textX - x) - 8;
            drawLine(g, font, e.title(), textX, y + 5, maxW, mulAlpha(TITLE_DARK, alpha));
            if (!noMsg) {
                drawLine(g, font, e.message(), textX, y + 19, maxW, mulAlpha(MSG_DARK, alpha));
            }
            i++;
        }
    }

    private static boolean blank(Component text) {
        return text.getString().isEmpty();
    }

    private static void drawLine(GuiGraphics g, Font font, Component text, int x, int y, int maxW, int color) {
        g.drawString(font, com.sao.saomenu.ui.render.SaoDraw.clipTo(font, text, maxW),
                x, y, color, false);
    }

    private static void playSound() {
        if (!SAOConfig.sounds()) {
            return;
        }
        try {
            // 单元测试等非客户端环境下 Minecraft.getInstance() 为 null,静默跳过
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SAOMenuPlatform.panelSound(), 0.6F));
        } catch (Throwable ignored) {
            // 非客户端环境:忽略
        }
    }

    private static long now() {
        return net.minecraft.Util.getMillis();
    }

}
