package com.sao.saomenu.client.screen.settings;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.sao.saomenu.config.SAOConfig;

/**
 * 设置项声明表。与 P5 皮肤几何无关:加一项只追加一条 {@link OptionSpec}。
 */
final class SettingsCatalog {

    private static final Map<SettingsPage, List<OptionSpec>> ROWS = buildRows();

    private SettingsCatalog() {
    }

    private static Map<SettingsPage, List<OptionSpec>> buildRows() {
        Map<SettingsPage, List<OptionSpec>> m = new EnumMap<>(SettingsPage.class);
        m.put(SettingsPage.LAYOUT, List.of(
                OptionSpec.slider("saomenu.config.anchor_x", SAOConfig::anchorX, SAOConfig::setAnchorX,
                        SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, OptionSpec.Fmt.PERCENT),
                OptionSpec.slider("saomenu.config.anchor_y", SAOConfig::anchorY, SAOConfig::setAnchorY,
                        SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, OptionSpec.Fmt.PERCENT),
                OptionSpec.slider("saomenu.config.scale", SAOConfig::menuScale, SAOConfig::setMenuScale,
                        SAOConfig.SCALE_MIN, SAOConfig.SCALE_MAX, OptionSpec.Fmt.MULT2),
                OptionSpec.slider("saomenu.config.bob", SAOConfig::bobAmp, SAOConfig::setBobAmp,
                        SAOConfig.BOB_MIN, SAOConfig.BOB_MAX, OptionSpec.Fmt.MULT1),
                OptionSpec.toggle("saomenu.config.follow_mouse", SAOConfig::anchorFollowMouse,
                        () -> SAOConfig.setAnchorFollowMouse(!SAOConfig.anchorFollowMouse())),
                OptionSpec.toggle("saomenu.config.hide_hotbar", SAOConfig::hideHotbar,
                        () -> SAOConfig.setHideHotbar(!SAOConfig.hideHotbar())),
                OptionSpec.toggle("saomenu.config.auto_sprint", SAOConfig::autoSprint,
                        () -> SAOConfig.setAutoSprint(!SAOConfig.autoSprint())),
                OptionSpec.toggle("saomenu.config.hide_vanilla_health", SAOConfig::hideVanillaHealth,
                        () -> SAOConfig.setHideVanillaHealth(!SAOConfig.hideVanillaHealth()))));
        m.put(SettingsPage.COMBAT, List.of(
                OptionSpec.toggle("saomenu.config.show_hud", SAOConfig::showHud,
                        () -> SAOConfig.setShowHud(!SAOConfig.showHud())),
                OptionSpec.toggle("saomenu.config.show_avatar", SAOConfig::showAvatar,
                        () -> SAOConfig.setShowAvatar(!SAOConfig.showAvatar())),
                OptionSpec.toggle("saomenu.config.target_bar", SAOConfig::showTargetBar,
                        () -> SAOConfig.setShowTargetBar(!SAOConfig.showTargetBar())),
                OptionSpec.toggle("saomenu.config.damage_numbers", SAOConfig::showDamageNumbers,
                        () -> SAOConfig.setShowDamageNumbers(!SAOConfig.showDamageNumbers())),
                OptionSpec.toggle("saomenu.config.death_shatter", SAOConfig::deathShatter,
                        () -> SAOConfig.setDeathShatter(!SAOConfig.deathShatter())),
                OptionSpec.slider("saomenu.config.shatter_density", SAOConfig::deathShatterDensity,
                        SAOConfig::setDeathShatterDensity,
                        SAOConfig.SHATTER_MIN, SAOConfig.SHATTER_MAX, OptionSpec.Fmt.MULT1),
                OptionSpec.toggle("saomenu.config.boss_banner", SAOConfig::showBossBanner,
                        () -> SAOConfig.setShowBossBanner(!SAOConfig.showBossBanner()))));
        m.put(SettingsPage.HUD, List.of(
                OptionSpec.toggle("saomenu.config.show_clock", SAOConfig::showClock,
                        () -> SAOConfig.setShowClock(!SAOConfig.showClock())),
                OptionSpec.slider("saomenu.config.clock_scale", SAOConfig::clockScale, SAOConfig::setClockScale,
                        SAOConfig.CLOCK_SCALE_MIN, SAOConfig.CLOCK_SCALE_MAX, OptionSpec.Fmt.MULT2),
                OptionSpec.toggle("saomenu.config.clock_24h", SAOConfig::clock24h,
                        () -> SAOConfig.setClock24h(!SAOConfig.clock24h())),
                OptionSpec.toggle("saomenu.config.clock_date", SAOConfig::clockDate,
                        () -> SAOConfig.setClockDate(!SAOConfig.clockDate())),
                OptionSpec.toggle("saomenu.config.show_welcome", SAOConfig::showWelcome,
                        () -> SAOConfig.setShowWelcome(!SAOConfig.showWelcome())),
                OptionSpec.toggle("saomenu.config.sao_toasts", SAOConfig::saoToasts,
                        () -> SAOConfig.setSaoToasts(!SAOConfig.saoToasts())),
                OptionSpec.toggle("saomenu.config.sounds", SAOConfig::sounds,
                        () -> SAOConfig.setSounds(!SAOConfig.sounds())),
                OptionSpec.slider("saomenu.config.hotbar_scale", SAOConfig::hotbarScale, SAOConfig::setHotbarScale,
                        SAOConfig.HOTBAR_MIN, SAOConfig.HOTBAR_MAX, OptionSpec.Fmt.MULT2),
                OptionSpec.toggle("saomenu.config.third_person", SAOConfig::thirdPersonMenu,
                        () -> SAOConfig.setThirdPersonMenu(!SAOConfig.thirdPersonMenu())),
                OptionSpec.toggle("saomenu.config.clock_menu_only", SAOConfig::clockOnlyInMenu,
                        () -> SAOConfig.setClockOnlyInMenu(!SAOConfig.clockOnlyInMenu()))));
        m.put(SettingsPage.THEME, List.of(
                OptionSpec.slider("saomenu.config.theme", SAOConfig::accentHue, SAOConfig::setAccentHue,
                        0f, 360f, OptionSpec.Fmt.DEG),
                OptionSpec.preset("saomenu.config.theme", SAOConfig::accentHue, SAOConfig::setAccentHue)));
        return m;
    }

    static List<OptionSpec> rows(SettingsPage p) {
        return ROWS.getOrDefault(p, List.of());
    }

    static OptionSpec spec(SettingsPage p, int i) {
        List<OptionSpec> list = rows(p);
        return i >= 0 && i < list.size() ? list.get(i) : null;
    }

    static int rowCount(SettingsPage p) {
        return rows(p).size();
    }

    static int indexOf(SettingsPage p, OptionSpec.Kind kind) {
        List<OptionSpec> list = rows(p);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).kind() == kind) {
                return i;
            }
        }
        return -1;
    }
}
