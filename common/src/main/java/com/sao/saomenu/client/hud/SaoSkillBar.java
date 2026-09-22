package com.sao.saomenu.client.hud;

import com.sao.saomenu.client.input.SAOKeybinds;
import com.sao.saomenu.client.menu.MenuLayout;
import com.sao.saomenu.client.skill.SaoSkill;
import com.sao.saomenu.client.skill.SaoSkillClientState;
import com.sao.saomenu.client.skill.SaoSkillRegistry;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.render.SaoDraw;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 技能浮条:注册表里每个技能一格,画在底部圆点物品栏上方。
 *
 * <p>格内是技能图标 + 冷却遮罩(自下而上退去)+ 快捷键位号。技能从
 * {@link SaoSkillRegistry} 取,所以注册一个技能就会自动多出一格。</p>
 *
 * <p>位置由 {@link SAOConfig#skillBarX()} / {@link SAOConfig#skillBarY()} 这对屏幕比例锚点决定,
 * 与血条板/饥饿条/时钟同一套约定:菜单打开时可直接拖动,松手落盘,设置页的"重置"一并复位。</p>
 */
public final class SaoSkillBar {

    /** 最多显示几格:与快捷键槽位同源,避免两处各写一个数字。 */
    private static final int MAX_SLOTS = SAOKeybinds.SKILL_KEYS.length;

    private SaoSkillBar() {
    }

    // ------------------------------------------------------------ 几何(渲染 / 命中 / 拖动共用)

    /** 单格边长。 */
    public static int slotSize(int screenH) {
        return Math.max(12, Math.round(screenH * 0.032f));
    }

    private static int slotGap(int size) {
        return Math.max(2, Math.round(size * 0.22f));
    }

    /** 可见格数 = min(已注册技能数, 快捷键槽位数)。 */
    public static int slotCount() {
        return Math.min(SaoSkillRegistry.skills().size(), MAX_SLOTS);
    }

    /** 整条浮条宽度;无技能时为 0。 */
    public static int barWidth(int screenH, int count) {
        if (count <= 0) {
            return 0;
        }
        int size = slotSize(screenH);
        return count * (size + slotGap(size)) - slotGap(size);
    }

    /**
     * 浮条左边缘 / 上边缘。
     *
     * <p>锚点是 0-1 比例,含义为"在可移动范围内"的相对位置(与血条板、时钟、饥饿条一致):
     * 0.5 即水平居中,Y=1 即贴底,所以换分辨率位置一致,且永远拖不出屏幕。</p>
     */
    public static int barX(int screenW, int barW) {
        return Math.round(SAOConfig.skillBarX() * Math.max(0, screenW - barW));
    }

    public static int barY(int screenH, int barH) {
        // 仍是默认锚点时,贴在圆点物品栏上方:hotbarScale 放大圆点时一起上移,不留缝也不重叠。
        // 用户拖走过就按比例锚点,不再去追圆点。
        if (Math.abs(SAOConfig.skillBarY() - SAOConfig.DEF_SKILL_BAR_Y) < 1.0e-4f) {
            int dotTop = MenuLayout.dotCenterY(screenH) - MenuLayout.dotSize(screenH) / 2;
            int gap = Math.max(3, Math.round(barH * 0.22f));
            return Math.max(0, dotTop - gap - barH);
        }
        return Math.round(SAOConfig.skillBarY() * Math.max(0, screenH - barH));
    }

    // ------------------------------------------------------------ 拖动(仅菜单打开时由 HudLayoutEditor 驱动)

    /** 命中检测:点在浮条范围内。 */
    public static boolean hitSkillBar(int screenW, int screenH, int mx, int my) {
        int count = slotCount();
        if (count <= 0) {
            return false;
        }
        int w = barWidth(screenH, count);
        int h = slotSize(screenH);
        int x = barX(screenW, w);
        int y = barY(screenH, h);
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    static void moveTo(int screenW, int screenH, float grabFx, float grabFy, int mx, int my) {
        int w = barWidth(screenH, slotCount());
        int h = slotSize(screenH);
        SAOConfig.setSkillBarX((mx - grabFx * w) / (float) Math.max(1, screenW - w));
        SAOConfig.setSkillBarY((my - grabFy * h) / (float) Math.max(1, screenH - h));
    }


    // ------------------------------------------------------------ 渲染

    /** 由 {@link HudComposer} 调用。 */
    public static void render(GuiGraphics g, Minecraft mc, int screenW, int screenH, float alpha) {
        int count = slotCount();
        if (count <= 0) {
            return;
        }
        List<SaoSkill> skills = SaoSkillRegistry.skills();
        int size = slotSize(screenH);
        int step = size + slotGap(size);
        int x0 = barX(screenW, barWidth(screenH, count));
        int y = barY(screenH, size);

        for (int i = 0; i < count; i++) {
            SaoSkill skill = skills.get(i);
            int x = x0 + i * step;

            // 底:半透明白格 + 主题色描边(与菜单条目同一套观感)
            g.fill(x, y, x + size, y + size, SaoDraw.mulAlpha(0x52F9F9F9, alpha));
            int accent = SaoTheme.accent();
            g.fill(x, y, x + size, y + 1, SaoDraw.mulAlpha(accent, alpha));
            g.fill(x, y + size - 1, x + size, y + size, SaoDraw.mulAlpha(accent, alpha));
            g.fill(x, y, x + 1, y + size, SaoDraw.mulAlpha(accent, alpha));
            g.fill(x + size - 1, y, x + size, y + size, SaoDraw.mulAlpha(accent, alpha));

            // 图标
            int pad = Math.max(1, Math.round(size * 0.18f));
            int isz = size - pad * 2;
            SaoDraw.shaderAlpha(alpha);
            g.blit(icon(skill), x + pad, y + pad, 0, 0, isz, isz, isz, isz);
            SaoDraw.shaderAlpha(1f);

            // 冷却遮罩:自下而上退去(顶部先亮)
            float cd = SaoSkillClientState.cooldownFraction(skill);
            if (cd > 0f) {
                int h = Math.round(size * cd);
                g.fill(x, y, x + size, y + h, SaoDraw.mulAlpha(0xB0000000, alpha));
            }

            // 快捷键位号:随格子缩放,钉在左上角内侧,不再用固定 2/1 px
            float ns = size / 16f;
            SaoDraw.drawScaled(g, mc.font, String.valueOf(i + 1),
                    x + size * 0.10f, y + size * 0.06f, ns,
                    SaoDraw.mulAlpha(SaoTheme.palette().textOnSurface(), alpha), false);
        }
    }

    /** 技能图标贴图;名字为空时回落到通用武器图标。 */
    private static ResourceLocation icon(SaoSkill skill) {
        String name = skill.icon() == null || skill.icon().isEmpty() ? "item_weapon" : skill.icon();
        return new ResourceLocation(com.sao.saomenu.SAOMenu.MOD_ID, "textures/gui/" + name + ".png");
    }
}
