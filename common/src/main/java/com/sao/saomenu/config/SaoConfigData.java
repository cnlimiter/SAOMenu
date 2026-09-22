package com.sao.saomenu.config;

import com.sao.saomenu.ui.theme.SaoTheme;

import java.util.ArrayList;
import java.util.List;

/**
 * 运行中的配置状态。字段名即 JSON 名;缺省值写在声明上,Gson 反序列化缺字段时不会清成 Java 零值。
 */
final class SaoConfigData {

    boolean frameworkEnabled = true;

    float anchorX = SAOConfig.DEF_ANCHOR_X;
    float anchorY = SAOConfig.DEF_ANCHOR_Y;
    float menuScale = SAOConfig.DEF_MENU_SCALE;
    float bobAmp = SAOConfig.DEF_BOB_AMP;
    boolean sounds = true;
    boolean hideHotbar = true;
    boolean showHud = true;
    boolean showAvatar = true;
    boolean anchorFollowMouse = false;
    boolean showTargetBar = true;
    boolean showDamageNumbers = true;
    boolean saoToasts = true;
    boolean showClock = true;
    boolean clock24h = true;
    boolean clockDate = false;
    boolean showWelcome = true;
    boolean deathShatter = true;
    float deathShatterDensity = SAOConfig.DEF_SHATTER_DENSITY;
    float accentHue = SAOConfig.DEF_ACCENT_HUE;
    float mapPanelX = SAOConfig.DEF_MAP_PANEL_X;
    float mapPanelY = SAOConfig.DEF_MAP_PANEL_Y;
    boolean mapPinned = false;
    float clockPanelX = SAOConfig.DEF_CLOCK_PANEL_X;
    float clockPanelY = SAOConfig.DEF_CLOCK_PANEL_Y;
    float clockScale = SAOConfig.DEF_CLOCK_SCALE;
    float hotbarScale = SAOConfig.DEF_HOTBAR_SCALE;
    boolean thirdPersonMenu = SAOConfig.DEF_THIRD_PERSON;
    boolean showBossBanner = SAOConfig.DEF_BOSS_BANNER;
    float platePanelX = SAOConfig.DEF_PLATE_PANEL_X;
    float platePanelY = SAOConfig.DEF_PLATE_PANEL_Y;
    boolean clockOnlyInMenu = SAOConfig.DEF_CLOCK_MENU_ONLY;
    boolean autoSprint = SAOConfig.DEF_AUTO_SPRINT;
    boolean hideVanillaHealth = SAOConfig.DEF_HIDE_VANILLA_HEALTH;
    float foodPanelX = SAOConfig.DEF_FOOD_PANEL_X;
    float foodPanelY = SAOConfig.DEF_FOOD_PANEL_Y;
    float skillBarX = SAOConfig.DEF_SKILL_BAR_X;
    float skillBarY = SAOConfig.DEF_SKILL_BAR_Y;
    String themeId = SaoTheme.SAO;
    List<String> pinnedItems = new ArrayList<>();
    List<String> itemOrder = new ArrayList<>();
    boolean hasOpenedSettings;

    /**
     * 载入后钳制范围、迁移旧锚点和主题 ID、洗净列表。空白 themeId 的保留策略由门面处理。
     */
    void normalize() {
        if (Math.abs(anchorX - 0.32f) < 0.0001f) {
            anchorX = SAOConfig.DEF_ANCHOR_X;
        } else {
            anchorX = SAOConfig.clamp(anchorX, SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, SAOConfig.DEF_ANCHOR_X);
        }
        anchorY = SAOConfig.clamp(anchorY, SAOConfig.ANCHOR_MIN, SAOConfig.ANCHOR_MAX, SAOConfig.DEF_ANCHOR_Y);
        menuScale = SAOConfig.clamp(menuScale, SAOConfig.SCALE_MIN, SAOConfig.SCALE_MAX, SAOConfig.DEF_MENU_SCALE);
        bobAmp = SAOConfig.clamp(bobAmp, SAOConfig.BOB_MIN, SAOConfig.BOB_MAX, SAOConfig.DEF_BOB_AMP);
        deathShatterDensity = SAOConfig.clamp(deathShatterDensity, SAOConfig.SHATTER_MIN, SAOConfig.SHATTER_MAX, SAOConfig.DEF_SHATTER_DENSITY);
        accentHue = SAOConfig.clamp(accentHue, 0f, 360f, SAOConfig.DEF_ACCENT_HUE);
        mapPanelX = SAOConfig.clamp(mapPanelX, 0f, 1f, SAOConfig.DEF_MAP_PANEL_X);
        mapPanelY = SAOConfig.clamp(mapPanelY, 0f, 1f, SAOConfig.DEF_MAP_PANEL_Y);
        clockPanelX = SAOConfig.clamp(clockPanelX, 0f, 1f, SAOConfig.DEF_CLOCK_PANEL_X);
        clockPanelY = SAOConfig.clamp(clockPanelY, 0f, 1f, SAOConfig.DEF_CLOCK_PANEL_Y);
        clockScale = SAOConfig.clamp(clockScale, SAOConfig.CLOCK_SCALE_MIN, SAOConfig.CLOCK_SCALE_MAX, SAOConfig.DEF_CLOCK_SCALE);
        hotbarScale = SAOConfig.clamp(hotbarScale, SAOConfig.HOTBAR_MIN, SAOConfig.HOTBAR_MAX, SAOConfig.DEF_HOTBAR_SCALE);
        platePanelX = SAOConfig.clamp(platePanelX, 0f, 1f, SAOConfig.DEF_PLATE_PANEL_X);
        platePanelY = SAOConfig.clamp(platePanelY, 0f, 1f, SAOConfig.DEF_PLATE_PANEL_Y);
        foodPanelX = SAOConfig.clamp(foodPanelX, 0f, 1f, SAOConfig.DEF_FOOD_PANEL_X);
        foodPanelY = SAOConfig.clamp(foodPanelY, 0f, 1f, SAOConfig.DEF_FOOD_PANEL_Y);
        skillBarX = SAOConfig.clamp(skillBarX, 0f, 1f, SAOConfig.DEF_SKILL_BAR_X);
        skillBarY = SAOConfig.clamp(skillBarY, 0f, 1f, SAOConfig.DEF_SKILL_BAR_Y);
        themeId = SaoTheme.canonicalizeId(themeId);
        pinnedItems = sanitize(pinnedItems);
        itemOrder = sanitize(itemOrder);
    }

    static ArrayList<String> sanitize(List<String> src) {
        ArrayList<String> out = new ArrayList<>();
        if (src == null) {
            return out;
        }
        for (String id : src) {
            if (id != null && !id.isEmpty() && !out.contains(id)) {
                out.add(id);
            }
        }
        return out;
    }
}
