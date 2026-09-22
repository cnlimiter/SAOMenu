package com.sao.saomenu.client.skill;

import com.sao.saomenu.client.hud.SAONotification;
import com.sao.saomenu.skill.DualWieldSkill;
import com.sao.saomenu.network.c2s.DualWieldC2S;
import com.sao.saomenu.ui.text.SaoText;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;

/**
 * 内置技能。
 *
 * <p>目前只有二刀流是真的;另外六项是菜单里原有的装饰性剑技,注册成占位技能
 * (点击给"暂未开放")——这样技能列<b>由注册表生成</b>,加真技能时不必再改菜单,
 * 而菜单外观与旧版一致。</p>
 */
public final class SaoSkills {

    /** 占位技能的统一提示。 */
    private static final String NOT_YET = "saomenu.coming_soon";

    private SaoSkills() {
    }

    /**
     * 技能快捷键槽位数(同时是技能浮条的格数)。
     *
     * <p>键位表必须在注册期就是定长数组,所以这里是个硬上界;注册的技能多于槽位时,
     * {@link com.sao.saomenu.client.skill.SaoSkillRegistry#registerBuiltins()} 会记一条警告,
     * 而不是像以前那样静默丢掉后面的技能(菜单列有、快捷键与浮条却没有)。</p>
     */
    public static final int HOTKEY_SLOTS = 9;

    /** 内置技能,顺序 = 菜单技能列顺序。 */
    public static List<SaoSkill> all() {
        return List.of(
                new SaoSkill(DualWieldSkill.DEFINITION.id(), "saomenu.skill.dual_wield", "item_weapon",
                        DualWieldSkill.DEFINITION.cooldownTicks(),
                        p -> SAODualWield.findTwoSwords(p) != null,
                        "saomenu.skill.dual_wield.need_two",
                        SaoSkills::requestDualWield),
                placeholder("horizontal", "item_weapon"),
                placeholder("slant", "item_weapon"),
                placeholder("vertical", "item_weapon"),
                placeholder("linear", "item_weapon"),
                placeholder("sonic_leap", "item_run"),
                placeholder("starburst", "item_weapon"));
    }

    /**
     * 占位技能:预检恒不通过,于是激活时只给提示。
     *
     * <p>用"预检失败 + 提示"而不是"空请求",是为了让菜单与快捷键走同一条路径:
     * 未实现的技能在两个入口上表现一致(都只弹提示),不会出现"菜单能点、快捷键没反应"。</p>
     */
    private static SaoSkill placeholder(String id, String icon) {
        return new SaoSkill(id, "saomenu.skill." + id, icon, 0, p -> false, NOT_YET, p -> {
        });
    }

    /**
     * 统一激活入口:先客户端预检,失败给提示,通过才发起。
     *
     * <p>菜单项与技能快捷键都调这一个方法,保证两个入口行为一致。</p>
     */
    public static void activate(LocalPlayer player, SaoSkill skill) {
        if (player == null || skill == null) {
            return;
        }
        if (skill.precheck() != null && !skill.precheck().canActivate(player)) {
            if (skill.notice() != null) {
                SAONotification.push(SaoText.tr(skill.notice()), "");
            }
            return;
        }
        skill.request().send(player);
        // 浮条乐观计时:服务端仍会独立校验,这里只是为了让冷却条立刻转起来
        SaoSkillClientState.markUsed(skill);
    }

    /**
     * 二刀流:挑两把剑交给服务端,并延后切史诗战斗的战斗模式。
     *
     * <p>延后切模式是必须的:Epic Fight 进战斗模式时按"当前主手武器"解析动作集,
     * 必须等服务端把剑同步回客户端之后再切,否则它按空手解析,
     * 表现为切了模式但没进入持剑架势。</p>
     */
    private static void requestDualWield(LocalPlayer player) {
        int[] slots = SAODualWield.findTwoSwords(player);
        if (slots == null) {
            return;
        }
        new DualWieldC2S(slots[0], slots[1]).sendToServer();
        SAODualWield.requestBattleMode();
        SAONotification.push(SaoText.tr("saomenu.skill.dual_wield"),
                SAODualWield.epicFightPresent()
                        ? SaoText.tr("saomenu.skill.dual_wield.on")
                        : SaoText.tr("saomenu.skill.dual_wield.no_ef"));
    }
}
