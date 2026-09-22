package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.api.settings.ChoiceSetting;
import com.sao.saomenu.api.settings.Setting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.SliderSetting;
import com.sao.saomenu.api.settings.ToggleSetting;
import com.sao.saomenu.config.SAOConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builtin groups keep layout/combat/HUD/theme order and reset only their own config. */
class SettingsCatalogTest {

    @BeforeEach
    void resetConfig() {
        SAOConfig.reset();
    }

    @Test
    void builtinsUseNamespacedIdsAndStableOrder() {
        List<SettingsGroup> groups = SettingsCatalog.builtins();
        assertEquals(4, groups.size());
        assertEquals(SettingsCatalog.LAYOUT_ID, groups.get(0).id());
        assertEquals(100, groups.get(0).order());
        assertEquals(SettingsCatalog.COMBAT_ID, groups.get(1).id());
        assertEquals(200, groups.get(1).order());
        assertEquals(SettingsCatalog.HUD_ID, groups.get(2).id());
        assertEquals(300, groups.get(2).order());
        assertEquals(SettingsCatalog.THEME_ID, groups.get(3).id());
        assertEquals(400, groups.get(3).order());
    }

    @Test
    void builtinsExposeTypedRowsNotStringKeys() {
        SettingsGroup layout = SettingsCatalog.builtins().get(0);
        assertInstanceOf(SliderSetting.class, layout.options().get(0));
        assertInstanceOf(ToggleSetting.class, layout.options().get(4));
        SettingsGroup theme = SettingsCatalog.builtins().get(3);
        boolean sawSlider = false;
        boolean sawChoice = false;
        for (Setting setting : theme.options()) {
            if (setting instanceof SliderSetting) {
                sawSlider = true;
            }
            if (setting instanceof ChoiceSetting) {
                sawChoice = true;
            }
        }
        assertTrue(sawSlider);
        assertTrue(sawChoice);
    }

    @Test
    void layoutResetDoesNotTouchCombatOrTheme() {
        SAOConfig.setAnchorX(0.9f);
        SAOConfig.setShowHud(false);
        SAOConfig.setAccentHue(200f);
        SettingsCatalog.builtins().get(0).reset();
        assertEquals(SAOConfig.DEF_ANCHOR_X, SAOConfig.anchorX(), 0.0001f);
        assertFalse(SAOConfig.showHud());
        assertEquals(200f, SAOConfig.accentHue(), 0.0001f);
    }

    @Test
    void combatResetDoesNotTouchLayout() {
        SAOConfig.setAnchorX(0.9f);
        SAOConfig.setShowHud(false);
        SettingsCatalog.builtins().get(1).reset();
        assertEquals(0.9f, SAOConfig.anchorX(), 0.0001f);
        assertTrue(SAOConfig.showHud());
    }
}
