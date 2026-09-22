package com.sao.saomenu.client.skill;

import java.util.HashMap;
import java.util.Map;

/**
 * 客户端技能冷却(用于技能浮条显示)。
 *
 * <p>这是<b>乐观计时</b>:客户端只在自认为就绪时才发起请求,所以正常情况下与服务端
 * 记账一致;真正的一致性由服务端 {@link com.sao.saomenu.server.skill.SaoSkillCooldowns} 保证(它会拒绝冷却中的请求)。
 * 这样做的代价是"服务端拒绝时浮条会短暂显示可用",换来的是不必为一个纯显示需求
 * 增加一条 S2C 冷却同步包。</p>
 */
public final class SaoSkillClientState {

    private static final Map<String, Long> READY_AT = new HashMap<>();

    private SaoSkillClientState() {
    }

    /** 记一次使用:按技能冷却时长开始倒计时。 */
    public static void markUsed(SaoSkill skill) {
        if (skill == null || !skill.hasCooldown()) {
            return;
        }
        READY_AT.put(skill.id(), net.minecraft.Util.getMillis() + skill.cooldownTicks() * 50L);
    }

    /** 剩余冷却毫秒;就绪返回 0。 */
    public static long remainingMs(SaoSkill skill) {
        if (skill == null || !skill.hasCooldown()) {
            return 0L;
        }
        Long readyAt = READY_AT.get(skill.id());
        if (readyAt == null) {
            return 0L;
        }
        long left = readyAt - net.minecraft.Util.getMillis();
        return left > 0 ? left : 0L;
    }

    /** 冷却进度 0..1(1 = 刚用、0 = 就绪),供浮条画遮罩。 */
    public static float cooldownFraction(SaoSkill skill) {
        if (skill == null || !skill.hasCooldown()) {
            return 0f;
        }
        return remainingMs(skill) / (float) (skill.cooldownTicks() * 50L);
    }

    /** 切换世界时清空(时间基准是本地毫秒,不需要跨世界保留)。 */
    public static void reset() {
        READY_AT.clear();
    }
}
