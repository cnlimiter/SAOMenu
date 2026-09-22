package com.sao.saomenu.client.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 技能注册表。
 *
 * <p>注册顺序同时决定菜单技能列的显示顺序与技能快捷键的槽位对应,
 * 所以顺序与去重都必须钉死:顺序变了会让快捷键按到别的技能。</p>
 */
class SaoSkillRegistryTest {

    @BeforeEach
    void reset() {
        SaoSkillRegistry.clearForTest();
    }

    @Test
    void builtinsRegisterInStableOrder() {
        SaoSkillRegistry.registerBuiltins();
        var skills = SaoSkillRegistry.skills();

        assertEquals(7, skills.size(), "二刀流 + 六项占位剑技");
        assertEquals(SaoSkills.DUAL_WIELD, skills.get(0).id(),
                "二刀流必须是第一项:预览自检与快捷键槽位都按顺序取");
        assertEquals("horizontal", skills.get(1).id());
        assertEquals("starburst", skills.get(6).id());
    }

    @Test
    void registerBuiltinsIsIdempotent() {
        SaoSkillRegistry.registerBuiltins();
        SaoSkillRegistry.registerBuiltins();
        assertEquals(7, SaoSkillRegistry.skills().size(), "重复调用不得重复注册");
    }

    @Test
    void duplicateIdIsRejected() {
        SaoSkill first = new SaoSkill("x", "k", "i", 0, null, null, p -> {
        });
        SaoSkill second = new SaoSkill("x", "k2", "i2", 0, null, null, p -> {
        });
        SaoSkillRegistry.register(first);
        SaoSkillRegistry.register(second);

        assertEquals(1, SaoSkillRegistry.skills().size(), "同 id 不得注册两次");
        assertSame(first, SaoSkillRegistry.byId("x"), "保留先注册的那个");
    }

    @Test
    void blankIdIsRejected() {
        SaoSkillRegistry.register(new SaoSkill("", "k", "i", 0, null, null, p -> {
        }));
        assertTrue(SaoSkillRegistry.skills().isEmpty());
    }

    @Test
    void lookupByIdAndUnknown() {
        SaoSkillRegistry.registerBuiltins();
        assertNotNull(SaoSkillRegistry.byId(SaoSkills.DUAL_WIELD));
        assertNull(SaoSkillRegistry.byId("no-such-skill"));
    }

    @Test
    void everyRegisteredSkillFitsAHotkeySlot() {
        SaoSkillRegistry.registerBuiltins();
        assertTrue(SaoSkillRegistry.skills().size() <= SaoSkills.HOTKEY_SLOTS,
                "注册的技能多于快捷键槽位时会静默截断:菜单列有、快捷键与浮条却没有");
    }

    @Test
    void onlyDualWieldIsImplementedToday() {
        SaoSkillRegistry.registerBuiltins();
        // 占位技能的共同特征:预检恒不通过(于是激活只给提示),且没有冷却
        for (SaoSkill s : SaoSkillRegistry.skills()) {
            if (s.id().equals(SaoSkills.DUAL_WIELD)) {
                assertTrue(s.hasCooldown(), "二刀流应有冷却");
            } else {
                assertFalse(s.hasCooldown(), s.id() + " 是占位技能,不应有冷却");
                assertFalse(s.precheck().canActivate(null), s.id() + " 预检应恒不通过");
            }
        }
    }
}
