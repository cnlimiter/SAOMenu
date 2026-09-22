package com.sao.saomenu.client.skill;

import net.minecraft.client.player.LocalPlayer;

/**
 * 一个技能。
 *
 * <p>技能不是 UI 概念:菜单里的技能列、技能浮条上的按钮、快捷键,三者都应当只是
 * 「注册表里同一个 {@link SaoSkill} 的不同入口」。所以这里只描述"是什么、能不能用、
 * 怎么发起",绘制交给界面。</p>
 *
 * <p><b>权威性</b>:{@link #canActivate} 是客户端预检,只为省一次往返与给玩家提示;
 * 真正的校验与冷却记账在服务端({@link com.sao.saomenu.server.skill.SaoSkillCooldowns}),客户端的判断不可信。</p>
 *
 * @param id             稳定标识(键位绑定与冷却记账都用它;改名等于换一个技能)
 * @param nameKey        名称语言键
 * @param icon           图标贴图名(缺省渲染成 {@code textures/gui/<icon>.png})
 * @param cooldownTicks  冷却时长(tick);0 表示无冷却
 * @param precheck       客户端预检:返回 false 表示条件不满足(例如"需要两把剑")
 * @param notice         预检失败时给玩家的提示语言键;null 表示静默
 * @param request        发起技能:由客户端把请求发给服务端
 */
public record SaoSkill(
        String id,
        String nameKey,
        String icon,
        int cooldownTicks,
        Precheck precheck,
        String notice,
        Request request
) {

    /** 客户端预检。 */
    @FunctionalInterface
    public interface Precheck {
        boolean canActivate(LocalPlayer player);
    }

    /** 客户端发起(通常是发一个既有 C2S 包)。 */
    @FunctionalInterface
    public interface Request {
        void send(LocalPlayer player);
    }

    public boolean hasCooldown() {
        return cooldownTicks > 0;
    }
}
