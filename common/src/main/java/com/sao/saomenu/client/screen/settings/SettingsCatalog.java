package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.settings.ChoiceSetting;
import com.sao.saomenu.api.settings.Setting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.SliderSetting;
import com.sao.saomenu.api.settings.ToggleSetting;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builtin settings groups. Public so client setup can call {@link #registerBuiltins}.
 * Addons register their own {@link SettingsGroup} on the same {@link SaoUiRegistry}.
 */
public final class SettingsCatalog {
    public static final ResourceLocation LAYOUT_ID = id("layout");
    public static final ResourceLocation COMBAT_ID = id("combat");
    public static final ResourceLocation HUD_ID = id("hud");
    public static final ResourceLocation THEME_ID = id("theme");

    private SettingsCatalog() {
    }

    public static void registerBuiltins(SaoUiRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        for (SettingsGroup group : builtins()) {
            registry.settings(group);
        }
    }

    static List<SettingsGroup> builtins() {
        return List.of(layout(), combat(), hud(), theme());
    }

    static List<SettingsGroup> groups() {
        try {
            return SaoUi.settingsGroups();
        } catch (IllegalStateException ignored) {
            return List.of();
        }
    }

    static int indexOf(ResourceLocation id) {
        List<SettingsGroup> groups = groups();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    static int debugIndex(String name) {
        if (name == null) {
            return Integer.MIN_VALUE;
        }
        if ("ROOT".equalsIgnoreCase(name)) {
            return -1;
        }
        List<SettingsGroup> groups = groups();
        for (int i = 0; i < groups.size(); i++) {
            ResourceLocation id = groups.get(i).id();
            if (name.equalsIgnoreCase(id.toString()) || name.equalsIgnoreCase(id.getPath())) {
                return i;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static SettingsGroup layout() {
        return new SettingsGroup(
                LAYOUT_ID, 100,
                Component.translatable("saomenu.settings.cat_layout"),
                Component.translatable("saomenu.settings.cat_layout_sub"),
                List.of(
                        slider("anchor_x", "saomenu.config.anchor_x",
                                SAOConfig::anchorX, SAOConfig::setAnchorX,
                                SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, SliderSetting.Format.PERCENT),
                        slider("anchor_y", "saomenu.config.anchor_y",
                                SAOConfig::anchorY, SAOConfig::setAnchorY,
                                SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, SliderSetting.Format.PERCENT),
                        slider("menu_scale", "saomenu.config.scale",
                                SAOConfig::menuScale, SAOConfig::setMenuScale,
                                SAOConfig.SCALE_MIN, SAOConfig.SCALE_MAX, SliderSetting.Format.MULTIPLIER_2),
                        slider("bob", "saomenu.config.bob",
                                SAOConfig::bobAmp, SAOConfig::setBobAmp,
                                SAOConfig.BOB_MIN, SAOConfig.BOB_MAX, SliderSetting.Format.MULTIPLIER_1),
                        toggle("follow_mouse", "saomenu.config.follow_mouse",
                                SAOConfig::anchorFollowMouse, SAOConfig::setAnchorFollowMouse),
                        toggle("hide_hotbar", "saomenu.config.hide_hotbar",
                                SAOConfig::hideHotbar, SAOConfig::setHideHotbar),
                        toggle("auto_sprint", "saomenu.config.auto_sprint",
                                SAOConfig::autoSprint, SAOConfig::setAutoSprint),
                        toggle("hide_vanilla_health", "saomenu.config.hide_vanilla_health",
                                SAOConfig::hideVanillaHealth, SAOConfig::setHideVanillaHealth)),
                SettingsCatalog::saveConfig,
                SettingsCatalog::resetLayout);
    }

    private static SettingsGroup combat() {
        return new SettingsGroup(
                COMBAT_ID, 200,
                Component.translatable("saomenu.settings.cat_combat"),
                Component.translatable("saomenu.settings.cat_combat_sub"),
                List.of(
                        toggle("show_hud", "saomenu.config.show_hud",
                                SAOConfig::showHud, SAOConfig::setShowHud),
                        toggle("show_avatar", "saomenu.config.show_avatar",
                                SAOConfig::showAvatar, SAOConfig::setShowAvatar),
                        toggle("target_bar", "saomenu.config.target_bar",
                                SAOConfig::showTargetBar, SAOConfig::setShowTargetBar),
                        toggle("damage_numbers", "saomenu.config.damage_numbers",
                                SAOConfig::showDamageNumbers, SAOConfig::setShowDamageNumbers),
                        toggle("death_shatter", "saomenu.config.death_shatter",
                                SAOConfig::deathShatter, SAOConfig::setDeathShatter),
                        slider("shatter_density", "saomenu.config.shatter_density",
                                SAOConfig::deathShatterDensity, SAOConfig::setDeathShatterDensity,
                                SAOConfig.SHATTER_MIN, SAOConfig.SHATTER_MAX, SliderSetting.Format.MULTIPLIER_1),
                        toggle("boss_banner", "saomenu.config.boss_banner",
                                SAOConfig::showBossBanner, SAOConfig::setShowBossBanner)),
                SettingsCatalog::saveConfig,
                SettingsCatalog::resetCombat);
    }

    private static SettingsGroup hud() {
        return new SettingsGroup(
                HUD_ID, 300,
                Component.translatable("saomenu.settings.cat_hud"),
                Component.translatable("saomenu.settings.cat_hud_sub"),
                List.of(
                        toggle("show_clock", "saomenu.config.show_clock",
                                SAOConfig::showClock, SAOConfig::setShowClock),
                        slider("clock_scale", "saomenu.config.clock_scale",
                                SAOConfig::clockScale, SAOConfig::setClockScale,
                                SAOConfig.CLOCK_SCALE_MIN, SAOConfig.CLOCK_SCALE_MAX, SliderSetting.Format.MULTIPLIER_2),
                        toggle("clock_24h", "saomenu.config.clock_24h",
                                SAOConfig::clock24h, SAOConfig::setClock24h),
                        toggle("clock_date", "saomenu.config.clock_date",
                                SAOConfig::clockDate, SAOConfig::setClockDate),
                        toggle("show_welcome", "saomenu.config.show_welcome",
                                SAOConfig::showWelcome, SAOConfig::setShowWelcome),
                        toggle("sao_toasts", "saomenu.config.sao_toasts",
                                SAOConfig::saoToasts, SAOConfig::setSaoToasts),
                        toggle("sounds", "saomenu.config.sounds",
                                SAOConfig::sounds, SAOConfig::setSounds),
                        slider("hotbar_scale", "saomenu.config.hotbar_scale",
                                SAOConfig::hotbarScale, SAOConfig::setHotbarScale,
                                SAOConfig.HOTBAR_MIN, SAOConfig.HOTBAR_MAX, SliderSetting.Format.MULTIPLIER_2),
                        toggle("third_person", "saomenu.config.third_person",
                                SAOConfig::thirdPersonMenu, SAOConfig::setThirdPersonMenu),
                        toggle("clock_menu_only", "saomenu.config.clock_menu_only",
                                SAOConfig::clockOnlyInMenu, SAOConfig::setClockOnlyInMenu)),
                SettingsCatalog::saveConfig,
                SettingsCatalog::resetHud);
    }

    private static SettingsGroup theme() {
        ChoiceSetting presets = new ChoiceSetting(
                id("theme_preset"),
                Component.translatable("saomenu.config.theme"),
                SettingsCatalog::themeChoices,
                SettingsCatalog::matchingThemeId,
                SettingsCatalog::selectTheme);
        return new SettingsGroup(
                THEME_ID, 400,
                Component.translatable("saomenu.settings.cat_theme"),
                Component.translatable("saomenu.settings.cat_theme_sub"),
                List.of(
                        slider("accent_hue", "saomenu.config.theme",
                                SAOConfig::accentHue, SAOConfig::setAccentHue,
                                0f, 360f, SliderSetting.Format.DEGREES),
                        presets),
                SettingsCatalog::saveConfig,
                SettingsCatalog::resetTheme);
    }

    private static List<ChoiceSetting.Option> themeChoices() {
        List<ChoiceSetting.Option> options = new ArrayList<>();
        try {
            for (ThemeDefinition theme : SaoUi.themes()) {
                options.add(new ChoiceSetting.Option(theme.id(), theme.label(), theme.defaultHue()));
            }
        } catch (RuntimeException ignored) {
            return List.of();
        }
        return options;
    }

    private static ResourceLocation matchingThemeId() {
        try {
            int hue = Math.round(SAOConfig.accentHue());
            for (ThemeDefinition theme : SaoUi.themes()) {
                if (Math.round(theme.defaultHue()) == hue) {
                    return theme.id();
                }
            }
        } catch (RuntimeException ignored) {
            return null;
        }
        return null;
    }

    private static void selectTheme(ResourceLocation id) {
        SaoUi.selectTheme(id);
    }

    private static void resetLayout() {
        SAOConfig.setAnchorX(SAOConfig.DEF_ANCHOR_X);
        SAOConfig.setAnchorY(SAOConfig.DEF_ANCHOR_Y);
        SAOConfig.setMenuScale(SAOConfig.DEF_MENU_SCALE);
        SAOConfig.setBobAmp(SAOConfig.DEF_BOB_AMP);
        SAOConfig.setAnchorFollowMouse(false);
        SAOConfig.setHideHotbar(true);
        SAOConfig.setAutoSprint(SAOConfig.DEF_AUTO_SPRINT);
        SAOConfig.setHideVanillaHealth(SAOConfig.DEF_HIDE_VANILLA_HEALTH);
    }

    private static void resetCombat() {
        SAOConfig.setShowHud(true);
        SAOConfig.setShowAvatar(true);
        SAOConfig.setShowTargetBar(true);
        SAOConfig.setShowDamageNumbers(true);
        SAOConfig.setDeathShatter(true);
        SAOConfig.setDeathShatterDensity(SAOConfig.DEF_SHATTER_DENSITY);
        SAOConfig.setShowBossBanner(SAOConfig.DEF_BOSS_BANNER);
    }

    private static void resetHud() {
        SAOConfig.setShowClock(true);
        SAOConfig.setClockScale(SAOConfig.DEF_CLOCK_SCALE);
        SAOConfig.setClock24h(true);
        SAOConfig.setClockDate(false);
        SAOConfig.setShowWelcome(true);
        SAOConfig.setSaoToasts(true);
        SAOConfig.setSounds(true);
        SAOConfig.setHotbarScale(SAOConfig.DEF_HOTBAR_SCALE);
        SAOConfig.setThirdPersonMenu(SAOConfig.DEF_THIRD_PERSON);
        SAOConfig.setClockOnlyInMenu(SAOConfig.DEF_CLOCK_MENU_ONLY);
    }

    private static void resetTheme() {
        SaoTheme.select(SaoTheme.SAO);
    }

    static void saveConfig() {
        if (SAOConfig.path() != null) {
            SAOConfig.save();
        }
    }

    private static ToggleSetting toggle(String path, String key,
                                        java.util.function.BooleanSupplier get,
                                        java.util.function.Consumer<Boolean> set) {
        return new ToggleSetting(id(path), Component.translatable(key), get, set);
    }

    private static SliderSetting slider(String path, String key, SliderSetting.FloatGetter get,
                                        SliderSetting.FloatSetter set, float min, float max,
                                        SliderSetting.Format format) {
        return new SliderSetting(id(path), Component.translatable(key), get, set, min, max, format);
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("saomenu", path);
    }

    static Setting setting(SettingsGroup group, int index) {
        if (group == null) {
            return null;
        }
        List<Setting> options = group.options();
        return index >= 0 && index < options.size() ? options.get(index) : null;
    }
}
