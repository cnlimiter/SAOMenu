package com.sao.saomenu.server.skill;

import com.sao.saomenu.client.skill.SaoSkill;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 技能冷却记账(服务端权威)。
 *
 * <p>键位是「玩家 + 技能」而不是只按技能:联机时每人一份冷却,互不影响。</p>
 *
 * <p>目前是会话级(服务端进程内),换周目/重启即清空。要做跨重启持久化应改为
 * {@code SavedData},但那会引入存档格式,先把机制跑通更划算。</p>
 */
public final class SaoSkillCooldowns {

    /** 玩家 → (技能 id → 冷却结束的世界时刻)。 */
    private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();

    private SaoSkillCooldowns() {
    }

    /**
     * 尝试使用技能:不在冷却中则记账并返回 true。
     *
     * <p>服务端在任何执行路径上都必须先过这一关——客户端预检只是提示,
     * 不能作为扣冷却的依据。</p>
     */
    public static boolean tryUse(ServerPlayer player, SaoSkill skill) {
        if (player == null || skill == null || !skill.hasCooldown()) {
            return true;
        }
        long now = player.level().getGameTime();
        Map<String, Long> mine = READY_AT.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        Long readyAt = mine.get(skill.id());
        if (readyAt != null && now < readyAt) {
            return false;
        }
        mine.put(skill.id(), now + skill.cooldownTicks());
        return true;
    }

    /** 玩家退网:清掉他的记账,避免 UUID 长期累积。 */
    public static void clearPlayer(UUID id) {
        READY_AT.remove(id);
    }

    /** 清空全部(仅测试用)。 */
    static void clearAll() {
        READY_AT.clear();
    }
}
