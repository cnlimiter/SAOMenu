package com.sao.saomenu.server.skill;

import com.sao.saomenu.skill.SaoSkillDefinition;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** Session-local, server-thread cooldown ledger keyed by player and gameplay skill identity. */
public final class SaoSkillCooldowns {
    private static final Map<UUID, Map<String, Long>> READY_AT = new HashMap<>();

    private SaoSkillCooldowns() {
    }

    /** Call only after validating the complete action and before mutating inventory. */
    public static boolean tryUse(ServerPlayer player, SaoSkillDefinition skill) {
        Objects.requireNonNull(player, "player");
        return tryUse(player.getUUID(), skill, player.serverLevel().getServer().overworld().getGameTime());
    }

    static boolean tryUse(UUID player, SaoSkillDefinition skill, long now) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(skill, "skill");
        if (skill.cooldownTicks() == 0) {
            return true;
        }
        Map<String, Long> mine = READY_AT.computeIfAbsent(player, key -> new HashMap<>());
        Long readyAt = mine.get(skill.id());
        if (readyAt != null && now < readyAt) {
            return false;
        }
        mine.put(skill.id(), now + skill.cooldownTicks());
        return true;
    }

    public static void clearPlayer(UUID id) {
        READY_AT.remove(id);
    }

    public static void reset() {
        READY_AT.clear();
    }
}
