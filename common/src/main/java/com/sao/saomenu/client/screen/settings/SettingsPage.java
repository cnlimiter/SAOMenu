package com.sao.saomenu.client.screen.settings;

/**
 * 设置界面页面:根目录与四个分类。顺序固定,与根页阶梯条目一致。
 */
enum SettingsPage {
    ROOT,
    LAYOUT,
    COMBAT,
    HUD,
    THEME;

    /** 根页分类顺序:布局 / 战斗 / 界面 / 主题。 */
    static final SettingsPage[] CATEGORIES = {LAYOUT, COMBAT, HUD, THEME};

    String titleKey() {
        return switch (this) {
            case LAYOUT -> "saomenu.settings.cat_layout";
            case COMBAT -> "saomenu.settings.cat_combat";
            case HUD -> "saomenu.settings.cat_hud";
            default -> "saomenu.settings.cat_theme";
        };
    }

    String subKey() {
        return switch (this) {
            case LAYOUT -> "saomenu.settings.cat_layout_sub";
            case COMBAT -> "saomenu.settings.cat_combat_sub";
            case HUD -> "saomenu.settings.cat_hud_sub";
            default -> "saomenu.settings.cat_theme_sub";
        };
    }

    static SettingsPage named(String name) {
        if (name == null) {
            return null;
        }
        if (ROOT.name().equalsIgnoreCase(name)) {
            return ROOT;
        }
        for (SettingsPage p : CATEGORIES) {
            if (p.name().equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }
}
