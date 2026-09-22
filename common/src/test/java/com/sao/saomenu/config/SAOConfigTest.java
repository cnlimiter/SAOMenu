package com.sao.saomenu.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 配置持久化回归:默认值、范围钳制、JSON 往返、旧文件迁移。
 */
class SAOConfigTest {

    @TempDir
    Path tmp;

    @AfterEach
    void restoreDefaults() {
        SAOConfig.reset();
        SAOConfig.load(null);
    }

    @Test
    void defaultsMatchReferenceScreenshot() {
        assertEquals(0.44f, SAOConfig.anchorX());
        assertEquals(0.363f, SAOConfig.anchorY());
        assertEquals(1f, SAOConfig.menuScale());
        assertEquals(1f, SAOConfig.bobAmp());
        assertTrue(SAOConfig.sounds());
        assertTrue(SAOConfig.hideHotbar());
        assertTrue(SAOConfig.showHud());
        assertTrue(SAOConfig.showAvatar());
        assertFalse(SAOConfig.anchorFollowMouse(), "菜单位置固定,不跟随鼠标");
        assertTrue(SAOConfig.showTargetBar());
        assertTrue(SAOConfig.showDamageNumbers());
        assertTrue(SAOConfig.saoToasts());
    }

    @Test
    void settersClampToBounds() {
        SAOConfig.setAnchorX(2f);
        assertEquals(SAOConfig.ANCHOR_MAX, SAOConfig.anchorX());
        SAOConfig.setAnchorX(-1f);
        assertEquals(SAOConfig.ANCHOR_MIN, SAOConfig.anchorX());
        SAOConfig.setMenuScale(99f);
        assertEquals(SAOConfig.SCALE_MAX, SAOConfig.menuScale());
        SAOConfig.setBobAmp(-5f);
        assertEquals(SAOConfig.BOB_MIN, SAOConfig.bobAmp());
    }

    @Test
    void nonFiniteFloatsDoNotPoisonLayout() {
        float before = SAOConfig.anchorX();
        SAOConfig.setAnchorX(Float.NaN);
        assertEquals(before, SAOConfig.anchorX(), "NaN 不得写入锚点");
        SAOConfig.setAnchorX(Float.POSITIVE_INFINITY);
        assertEquals(SAOConfig.ANCHOR_MAX, SAOConfig.anchorX());
        SAOConfig.setAnchorY(Float.NEGATIVE_INFINITY);
        assertEquals(SAOConfig.ANCHOR_MIN, SAOConfig.anchorY());
    }

    @Test
    void saveAndLoadRoundTrip() {
        SAOConfig.setAnchorX(0.44f);
        SAOConfig.setAnchorY(0.55f);
        SAOConfig.setMenuScale(1.2f);
        SAOConfig.setBobAmp(2f);
        SAOConfig.setSounds(false);
        SAOConfig.setHideHotbar(false);
        SAOConfig.setShowHud(false);
        SAOConfig.setSaoToasts(false);
        Path file = tmp.resolve("saomenu.json");
        SAOConfig.save(file);
        assertTrue(java.nio.file.Files.exists(file));

        SAOConfig.reset();
        SAOConfig.load(file);
        assertEquals(0.44f, SAOConfig.anchorX());
        assertEquals(0.55f, SAOConfig.anchorY());
        assertEquals(1.2f, SAOConfig.menuScale());
        assertEquals(2f, SAOConfig.bobAmp());
        assertFalse(SAOConfig.sounds());
        assertFalse(SAOConfig.hideHotbar());
        assertFalse(SAOConfig.showHud());
        assertFalse(SAOConfig.saoToasts());
    }

    @Test
    void loadMissingFileKeepsDefaults() {
        SAOConfig.load(tmp.resolve("nope.json"));
        assertEquals(SAOConfig.DEF_ANCHOR_X, SAOConfig.anchorX());
        assertTrue(SAOConfig.sounds());
    }

    @Test
    void saveWritesPreviouslyLoadedPath() {
        Path file = tmp.resolve("chosen.json");
        SAOConfig.load(file);
        SAOConfig.setSounds(false);
        SAOConfig.save();
        assertTrue(java.nio.file.Files.exists(file));
        SAOConfig.reset();
        SAOConfig.load(file);
        assertFalse(SAOConfig.sounds());
    }

    @Test
    void saveWithoutLoadIsProgrammingError() {
        assertThrows(IllegalStateException.class, SAOConfig::save);
    }

    @Test
    void accentHueDefaultsAndClamps() {
        assertEquals(41.44f, SAOConfig.accentHue(), 0.01f);
        SAOConfig.setAccentHue(999f);
        assertEquals(360f, SAOConfig.accentHue(), "色相应钳制到 360");
        SAOConfig.setAccentHue(-5f);
        assertEquals(0f, SAOConfig.accentHue(), "色相应钳制到 0");
        SAOConfig.setAccentHue(Float.NaN);
        assertEquals(0f, SAOConfig.accentHue(), "NaN 色相不得污染当前值");
    }

    @Test
    void accentHuePersistsInJson() {
        SAOConfig.setAccentHue(200f);
        Path file = tmp.resolve("accent.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        SAOConfig.load(file);
        assertEquals(200f, SAOConfig.accentHue(), 0.01f);
    }

    @Test
    void bossBannerDefaultsOnAndPersists() {
        assertTrue(SAOConfig.showBossBanner(), "新开关默认开,不改变现有观感");
        SAOConfig.setShowBossBanner(false);
        Path file = tmp.resolve("boss.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertTrue(SAOConfig.showBossBanner());
        SAOConfig.load(file);
        assertFalse(SAOConfig.showBossBanner(), "关闭状态应写入并读回");
    }

    @Test
    void clockOnlyInMenuDefaultsOffAndPersists() {
        assertFalse(SAOConfig.clockOnlyInMenu(), "时钟默认常显,不改变现有观感");
        SAOConfig.setClockOnlyInMenu(true);
        Path file = tmp.resolve("clock_menu.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertFalse(SAOConfig.clockOnlyInMenu());
        SAOConfig.load(file);
        assertTrue(SAOConfig.clockOnlyInMenu(), "仅菜单内显示应写入并读回");
    }

    @Test
    void autoSprintDefaultsOnAndPersists() {
        assertTrue(SAOConfig.autoSprint(), "按住 W 自动疾跑默认开");
        SAOConfig.setAutoSprint(false);
        Path file = tmp.resolve("sprint.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertTrue(SAOConfig.autoSprint());
        SAOConfig.load(file);
        assertFalse(SAOConfig.autoSprint(), "关闭状态应写入并读回");
    }

    @Test
    void hideVanillaHealthDefaultsOnAndPersists() {
        assertTrue(SAOConfig.hideVanillaHealth(), "隐藏原版血条默认开");
        SAOConfig.setHideVanillaHealth(false);
        Path file = tmp.resolve("hvh.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertTrue(SAOConfig.hideVanillaHealth());
        SAOConfig.load(file);
        assertFalse(SAOConfig.hideVanillaHealth(), "关闭状态应写入并读回");
    }

    @Test
    void pinnedItemsPersistAndSort() {
        SAOConfig.togglePinned("minecraft:diamond_sword");
        SAOConfig.togglePinned("minecraft:golden_apple");
        assertTrue(SAOConfig.isPinned("minecraft:diamond_sword"));
        assertTrue(SAOConfig.pinOrder("minecraft:diamond_sword")
                        < SAOConfig.pinOrder("minecraft:golden_apple"),
                "先置顶的排更前");
        assertEquals(Integer.MAX_VALUE, SAOConfig.pinOrder("minecraft:stick"), "未置顶沉底");
        Path file = tmp.resolve("pinned.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertTrue(SAOConfig.pinnedItems().isEmpty(), "reset 清空置顶");
        SAOConfig.load(file);
        assertTrue(SAOConfig.isPinned("minecraft:diamond_sword"), "置顶应写入并读回");
        assertTrue(!SAOConfig.togglePinned("minecraft:diamond_sword"), "再切换应取消置顶(返回 false)");
        assertTrue(!SAOConfig.isPinned("minecraft:diamond_sword"));
        assertEquals(1, SAOConfig.pinnedItems().size());
    }

    @Test
    void pinnedSnapshotIsUnmodifiable() {
        SAOConfig.togglePinned("minecraft:stick");
        assertThrows(UnsupportedOperationException.class, () -> SAOConfig.pinnedItems().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> SAOConfig.itemOrder().add("x"));
        assertTrue(SAOConfig.isPinned("minecraft:stick"));
    }

    @Test
    void foodAnchorDefaultsToCenteredVanillaRow() {
        assertEquals(0.5f, SAOConfig.foodPanelX(), "默认水平居中");
        assertEquals(1f, SAOConfig.foodPanelY(), "默认贴原版行高");
        SAOConfig.setFoodPanelX(0.25f);
        SAOConfig.setFoodPanelY(0.5f);
        Path file = tmp.resolve("food.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        SAOConfig.load(file);
        assertEquals(0.25f, SAOConfig.foodPanelX());
        assertEquals(0.5f, SAOConfig.foodPanelY(), "饥饿条锚点应持久化");
    }

    @Test
    void skillBarAnchorDefaultsAboveHotbarAndPersists() {
        assertEquals(SAOConfig.DEF_SKILL_BAR_X, SAOConfig.skillBarX(), "默认水平居中");
        assertEquals(SAOConfig.DEF_SKILL_BAR_Y, SAOConfig.skillBarY(), "默认在圆点物品栏上方");
        SAOConfig.setSkillBarX(0.2f);
        SAOConfig.setSkillBarY(0.3f);
        Path file = tmp.resolve("skillbar.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertEquals(SAOConfig.DEF_SKILL_BAR_X, SAOConfig.skillBarX(), "reset 应回到默认位置");
        SAOConfig.load(file);
        assertEquals(0.2f, SAOConfig.skillBarX());
        assertEquals(0.3f, SAOConfig.skillBarY(), "技能浮条锚点应持久化");
    }

    @Test
    void skillBarAnchorIsClampedToScreen() {
        SAOConfig.setSkillBarX(9f);
        SAOConfig.setSkillBarY(-4f);
        assertEquals(1f, SAOConfig.skillBarX(), "X 超出范围应钳制");
        assertEquals(0f, SAOConfig.skillBarY(), "Y 超出范围应钳制");
    }

    @Test
    void themeIdPersistsAndDefaultsToSao() {
        assertEquals(com.sao.saomenu.ui.theme.SaoTheme.SAO, SAOConfig.themeId(), "默认 SAO");
        SAOConfig.setThemeId("qinglan");
        Path file = tmp.resolve("theme-id.json");
        SAOConfig.save(file);
        SAOConfig.reset();
        assertEquals(com.sao.saomenu.ui.theme.SaoTheme.SAO, SAOConfig.themeId(), "reset 应回到 SAO");
        SAOConfig.load(file);
        assertEquals("saomenu:qinglan", SAOConfig.themeId(), "主题 id 应迁移并持久化");
    }

    @Test
    void blankOrNullThemeIdFallsBackToSao() {
        SAOConfig.setThemeId("   ");
        assertEquals(com.sao.saomenu.ui.theme.SaoTheme.SAO, SAOConfig.themeId());
        SAOConfig.setThemeId(null);
        assertEquals(com.sao.saomenu.ui.theme.SaoTheme.SAO, SAOConfig.themeId());
    }

    @Test
    void partialJsonFallsBackToDefaultsForMissingFields() throws Exception {
        Path file = tmp.resolve("partial.json");
        java.nio.file.Files.writeString(file,
                "{\"anchorX\":0.5,\"anchorY\":0.5}");
        SAOConfig.load(file);
        assertEquals(0.5f, SAOConfig.anchorX());
        assertEquals(0.5f, SAOConfig.anchorY());
        assertEquals(SAOConfig.DEF_MENU_SCALE, SAOConfig.menuScale(), "缺失字段应回退默认");
        assertTrue(SAOConfig.sounds(), "缺失布尔字段应回退默认 true");
        assertEquals(SAOConfig.DEF_ACCENT_HUE, SAOConfig.accentHue(), 0.01f, "缺失 accentHue 应回退默认");
        assertTrue(SAOConfig.showBossBanner(), "旧配置文件缺 showBossBanner 应回退默认开");
    }

    @Test
    void oldJsonKeepsPinsOrderAndTheme() throws Exception {
        Path file = tmp.resolve("old.json");
        java.nio.file.Files.writeString(file,
                "{\"anchorX\":0.5,\"pinnedItems\":[\"minecraft:stick\"],"
                        + "\"itemOrder\":[\"minecraft:stick\",\"minecraft:dirt\"],"
                        + "\"themeId\":\"qinglan\"}");
        SAOConfig.load(file);
        assertEquals(0.5f, SAOConfig.anchorX());
        assertTrue(SAOConfig.isPinned("minecraft:stick"));
        assertEquals(0, SAOConfig.orderIndex("minecraft:stick"));
        assertEquals(1, SAOConfig.orderIndex("minecraft:dirt"));
        assertEquals("saomenu:qinglan", SAOConfig.themeId());
        SAOConfig.save(file);
        SAOConfig.reset();
        SAOConfig.load(file);
        assertTrue(SAOConfig.isPinned("minecraft:stick"), "回写不得丢掉置顶");
        assertEquals(0, SAOConfig.orderIndex("minecraft:stick"), "回写不得丢掉手动顺序");
        assertEquals("saomenu:qinglan", SAOConfig.themeId(), "回写不得丢掉迁移后的主题 id");
    }

    @Test
    void oldDefaultAnchorMigrates() throws Exception {
        Path file = tmp.resolve("old-anchor.json");
        java.nio.file.Files.writeString(file, "{\"anchorX\":0.32}");
        SAOConfig.load(file);
        assertEquals(SAOConfig.DEF_ANCHOR_X, SAOConfig.anchorX());
    }

    @Test
    void resetPreservesHasOpenedSettings() {
        assertFalse(SAOConfig.hasOpenedSettings());
        SAOConfig.markSettingsOpened();
        SAOConfig.reset();
        assertTrue(SAOConfig.hasOpenedSettings(), "reset 不得清掉已打开过设置");
        assertEquals(SAOConfig.DEF_ANCHOR_X, SAOConfig.anchorX());
    }
}
