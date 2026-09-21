package com.sao.saomenu.client;

import com.sao.saomenu.ui.SaoDraw;
import com.sao.saomenu.ui.SaoText;
import com.sao.saomenu.ui.SaoTheme;
import com.sao.saomenu.skill.SaoSkill;
import com.sao.saomenu.skill.SaoSkillClientState;
import com.sao.saomenu.skill.SaoSkillRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 技能浮条:注册表里每个技能一格,画在底部圆点物品栏上方。
 *
 * <p>格内是技能图标 + 冷却遮罩(自下而上退去)+ 快捷键位号。技能从
 * {@link SaoSkillRegistry} 取,所以注册一个技能就会自动多出一格。</p>
 */
public final class SaoSkillBar {

    /** 最多显示几格:与快捷键槽位同源,避免两处各写一个数字。 */
    private static final int MAX_SLOTS = SAOKeybinds.SKILL_KEYS.length;

    private SaoSkillBar() {
    }

    /** 由 {@link SAOHud#renderHud} 调用。 */
    public static void render(GuiGraphics g, Minecraft mc, int screenW, int screenH, float alpha) {
        List<SaoSkill> skills = SaoSkillRegistry.skills();
        if (skills.isEmpty()) {
            return;
        }
        int count = Math.min(skills.size(), MAX_SLOTS);
        int size = Math.max(12, Math.round(screenH * 0.032f));
        int gap = Math.max(2, Math.round(size * 0.22f));
        int step = size + gap;
        int total = count * step - gap;

        // 贴在底部圆点物品栏上方:圆点直径与位置都由 MenuLayout 算,这里只借它的 Y
        int dotCy = MenuLayout.dotCenterY(screenH);
        int dotR = MenuLayout.dotSize(screenH) / 2;
        int y = dotCy - dotR - 8 - size;
        int x0 = screenW / 2 - total / 2;

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

            // 快捷键位号
            String slot = String.valueOf(i + 1);
            g.drawString(mc.font, slot, x + 2, y + 1,
                    SaoDraw.mulAlpha(SaoTheme.palette().textOnSurface(), alpha), false);
        }
    }

    /** 技能图标贴图;名字为空时回落到通用武器图标。 */
    private static ResourceLocation icon(SaoSkill skill) {
        String name = skill.icon() == null || skill.icon().isEmpty() ? "item_weapon" : skill.icon();
        return new ResourceLocation(com.sao.saomenu.SAOMenu.MOD_ID, "textures/gui/" + name + ".png");
    }

    /** 供 tooltip / 调试:第 i 格的技能名。 */
    static String slotLabel(int i) {
        List<SaoSkill> skills = SaoSkillRegistry.skills();
        return i >= 0 && i < skills.size() ? SaoText.resolveLabel(skills.get(i).nameKey()) : "";
    }
}
