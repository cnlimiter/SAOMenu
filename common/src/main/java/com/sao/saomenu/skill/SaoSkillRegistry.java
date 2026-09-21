package com.sao.saomenu.skill;

import com.sao.saomenu.SAOMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 技能注册表。
 *
 * <p>菜单技能列、技能浮条、快捷键绑定都从这里取。注册顺序 = 菜单里技能列的显示顺序,
 * 所以内置技能的先后不能随意改。</p>
 */
public final class SaoSkillRegistry {

    private static final List<SaoSkill> SKILLS = new ArrayList<>();
    private static boolean builtinsRegistered;

    private SaoSkillRegistry() {
    }

    /** 注册一个技能;id 重复时忽略并警告(同一个技能挂两次会让冷却记账互相打架)。 */
    public static void register(SaoSkill skill) {
        if (skill == null || skill.id() == null || skill.id().isEmpty()) {
            SAOMenu.LOGGER.warn("[SAOMenu] 拒绝注册无 id 的技能");
            return;
        }
        if (byId(skill.id()) != null) {
            SAOMenu.LOGGER.warn("[SAOMenu] 技能 id 重复,已忽略: {}", skill.id());
            return;
        }
        SKILLS.add(skill);
    }

    /** 已注册技能(只读)。 */
    public static List<SaoSkill> skills() {
        return Collections.unmodifiableList(SKILLS);
    }

    /** 按 id 取技能;不存在返回 null。 */
    public static SaoSkill byId(String id) {
        for (SaoSkill s : SKILLS) {
            if (s.id().equals(id)) {
                return s;
            }
        }
        return null;
    }

    /** 注册内置技能(幂等)。 */
    public static void registerBuiltins() {
        if (builtinsRegistered) {
            return;
        }
        builtinsRegistered = true;
        for (SaoSkill s : SaoSkills.all()) {
            register(s);
        }
        // 快捷键表是定长的:技能多于槽位时后面的技能将没有快捷键、也不上浮条。
        // 静默截断过(菜单列 7 项、快捷键只有 6 个),所以这里把溢出说出来。
        if (SKILLS.size() > SaoSkills.HOTKEY_SLOTS) {
            com.sao.saomenu.SAOMenu.LOGGER.warn(
                    "[SAOMenu] 已注册 {} 个技能,但只有 {} 个快捷键槽位;多出的技能将没有快捷键与浮条格",
                    SKILLS.size(), SaoSkills.HOTKEY_SLOTS);
        }
    }

    /** 清空(仅测试用)。 */
    public static void clearForTest() {
        SKILLS.clear();
        builtinsRegistered = false;
    }
}
