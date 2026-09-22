package com.sao.saomenu.server.skill;

import com.sao.saomenu.skill.DualWieldSkill;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaoSkillCooldownsTest {
    private static final UUID FIRST = new UUID(0, 1);
    private static final UUID SECOND = new UUID(0, 2);

    @AfterEach
    void reset() {
        SaoSkillCooldowns.reset();
    }

    @Test
    void rejectsRepeatUntilBoundaryWithoutBlockingAnotherPlayer() {
        var skill = DualWieldSkill.DEFINITION;
        assertTrue(SaoSkillCooldowns.tryUse(FIRST, skill, 100));
        assertFalse(SaoSkillCooldowns.tryUse(FIRST, skill, 100));
        assertTrue(SaoSkillCooldowns.tryUse(SECOND, skill, 100));
        assertFalse(SaoSkillCooldowns.tryUse(FIRST, skill, 100 + skill.cooldownTicks() - 1));
        assertTrue(SaoSkillCooldowns.tryUse(FIRST, skill, 100 + skill.cooldownTicks()));
    }

    @Test
    void logoutDiscardsOnlyThatPlayersCooldown() {
        var skill = DualWieldSkill.DEFINITION;
        assertTrue(SaoSkillCooldowns.tryUse(FIRST, skill, 0));
        assertTrue(SaoSkillCooldowns.tryUse(SECOND, skill, 0));
        SaoSkillCooldowns.clearPlayer(FIRST);
        assertTrue(SaoSkillCooldowns.tryUse(FIRST, skill, 1));
        assertFalse(SaoSkillCooldowns.tryUse(SECOND, skill, 1));
    }

    @Test
    void absentMetadataCannotBypassAuthority() {
        assertThrows(NullPointerException.class, () -> SaoSkillCooldowns.tryUse(FIRST, null, 0));
    }
}
