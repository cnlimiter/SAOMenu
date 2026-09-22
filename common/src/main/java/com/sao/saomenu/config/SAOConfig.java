package com.sao.saomenu.config;

import com.google.gson.JsonSyntaxException;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.ui.theme.SaoTheme;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 客户端配置门面:静态读写入口,状态在 {@link SaoConfigData},落盘在 {@link SaoConfigStore}。
 *
 * <p>{@link #load(Path)} 选定文件;{@link #save()} 写回该文件。未先 load 非空路径就
 * {@link #save()} 是编程错误,不会猜测 Minecraft 目录。{@link #save(Path)} 仍供显式路径
 * 与测试使用。</p>
 */
public final class SAOConfig {

    // ------------------------------------------------------------ 默认值(参考截图实测)
    public static final float DEF_ANCHOR_X = 0.44f;
    public static final float DEF_ANCHOR_Y = 0.363f;
    public static final float DEF_MENU_SCALE = 1f;
    public static final float DEF_BOB_AMP = 1f;

    // 范围
    public static final float ANCHOR_MIN = 0.05f;
    public static final float ANCHOR_MAX = 0.95f;
    public static final float SCALE_MIN = 0.6f;
    public static final float SCALE_MAX = 1.5f;
    public static final float BOB_MIN = 0f;
    public static final float BOB_MAX = 3f;
    /** 死亡碎裂粒子密度倍率范围。 */
    public static final float SHATTER_MIN = 0.2f;
    public static final float SHATTER_MAX = 2.5f;
    public static final float DEF_SHATTER_DENSITY = 1f;

    /** 主题色(色相 0-360)。默认 41.44° = SAO 橙 #EFA603。 */
    public static final float DEF_ACCENT_HUE = 41.44f;

    /** 血条板默认锚点(屏幕比例;SAO 原版位置=左上角)。 */
    public static final float DEF_PLATE_PANEL_X = 0f;
    public static final float DEF_PLATE_PANEL_Y = 0f;

    /** 时钟默认常显;开启「仅菜单内显示」后随菜单关闭而隐藏。 */
    public static final boolean DEF_CLOCK_MENU_ONLY = false;

    /** 按住 W 自动疾跑(程序性按住疾跑键,原版条件照常生效),默认开。 */
    public static final boolean DEF_AUTO_SPRINT = true;

    /** 隐藏原版血条,饥饿条居中(氧气泡保留),默认开。 */
    public static final boolean DEF_HIDE_VANILLA_HEALTH = true;

    /** 饥饿条锚点默认值:X 居中、Y 贴原版行高(与未拖动前位置一致)。 */
    public static final float DEF_FOOD_PANEL_X = 0.5f;
    public static final float DEF_FOOD_PANEL_Y = 1f;

    /**
     * 技能浮条锚点默认值:X 居中、Y 落在底部圆点物品栏正上方。
     *
     * <p>0.94 是原硬编码位置的等效比例(圆点圆心在 97.2%H、直径 2.8%H,
     * 浮条底边与圆点顶边留 8px),换成分数后各分辨率下位置一致。</p>
     */
    public static final float DEF_SKILL_BAR_X = 0.5f;
    public static final float DEF_SKILL_BAR_Y = 0.94f;

    /** 地图面板默认锚点(屏幕比例;参照动画里地图卡浮在人物左前方)。 */
    public static final float DEF_MAP_PANEL_X = 0.10f;
    public static final float DEF_MAP_PANEL_Y = 0.28f;

    /** 时钟面板默认锚点(屏幕比例;默认顶部居中偏右,接近旧时钟位置)。 */
    public static final float DEF_CLOCK_PANEL_X = 0.42f;
    public static final float DEF_CLOCK_PANEL_Y = 0.04f;

    /** 时钟大小倍率范围。 */
    public static final float CLOCK_SCALE_MIN = 0.5f;
    public static final float CLOCK_SCALE_MAX = 2.0f;
    public static final float DEF_CLOCK_SCALE = 1.0f;

    /** 底部圆点物品栏大小倍率范围。 */
    public static final float HOTBAR_MIN = 0.6f;
    public static final float HOTBAR_MAX = 2.4f;
    public static final float DEF_HOTBAR_SCALE = 1.3f;
    /** 第三人称菜单板(打开菜单时角色面前出现 SAO 菜单,F5 可见)默认开。 */
    public static final boolean DEF_THIRD_PERSON = true;
    /** Boss「Immortal Object」横幅默认开。 */
    public static final boolean DEF_BOSS_BANNER = true;

    private static SaoConfigData data = new SaoConfigData();
    private static Path loadedFrom;

    private SAOConfig() {
    }

    // ------------------------------------------------------------ 读取(布局/渲染用)

    /** Persisted visual-framework switch; startup safe mode is enforced by the client facade. */
    public static boolean frameworkEnabled() {
        return data.frameworkEnabled;
    }

    public static float anchorX() {
        return data.anchorX;
    }

    public static float anchorY() {
        return data.anchorY;
    }

    public static float menuScale() {
        return data.menuScale;
    }

    public static float bobAmp() {
        return data.bobAmp;
    }

    public static boolean sounds() {
        return data.sounds;
    }

    public static boolean hideHotbar() {
        return data.hideHotbar;
    }

    public static boolean showHud() {
        return data.showHud;
    }

    /** 血条板名字下方是否显示皮肤头像。 */
    public static boolean showAvatar() {
        return data.showAvatar;
    }

    /** 菜单是否在鼠标位置打开(参照 SAO_Utils;关闭时使用锚点 X/Y)。 */
    public static boolean anchorFollowMouse() {
        return data.anchorFollowMouse;
    }

    /** 准星对准目标时是否显示目标血条。 */
    public static boolean showTargetBar() {
        return data.showTargetBar;
    }

    /** 是否显示伤害数字。 */
    public static boolean showDamageNumbers() {
        return data.showDamageNumbers;
    }

    /** 地图面板锚点 X(屏幕比例 0-1,拖动后持久化)。 */
    public static float mapPanelX() {
        return data.mapPanelX;
    }

    /** 地图面板锚点 Y(屏幕比例 0-1)。 */
    public static float mapPanelY() {
        return data.mapPanelY;
    }

    /** 地图面板是否图钉固定(关菜单后仍显示)。 */
    public static boolean mapPinned() {
        return data.mapPinned;
    }

    /** 时钟面板锚点 X(屏幕比例 0-1,拖动后持久化)。 */
    public static float clockPanelX() {
        return data.clockPanelX;
    }

    /** 时钟面板锚点 Y(屏幕比例 0-1)。 */
    public static float clockPanelY() {
        return data.clockPanelY;
    }

    /** 时钟大小倍率(0.5-2.0)。 */
    public static float clockScale() {
        return data.clockScale;
    }

    /** 底部圆点物品栏大小倍率(0.6-1.6)。 */
    public static float hotbarScale() {
        return data.hotbarScale;
    }

    /** 打开菜单时是否在角色面前渲染世界空间菜单板(第三人称可见)。 */
    public static boolean thirdPersonMenu() {
        return data.thirdPersonMenu;
    }

    /** 视线对准 Boss 时是否显示「Immortal Object」横幅。 */
    public static boolean showBossBanner() {
        return data.showBossBanner;
    }

    /** 血条板锚点(屏幕比例;SAO 原版位置 = 左上角 (0,0))。 */
    public static float platePanelX() {
        return data.platePanelX;
    }

    public static float platePanelY() {
        return data.platePanelY;
    }

    public static float accentHue() {
        return data.accentHue;
    }

    public static boolean saoToasts() {
        return data.saoToasts;
    }

    public static boolean showClock() {
        return data.showClock;
    }

    public static boolean clock24h() {
        return data.clock24h;
    }

    public static boolean clockDate() {
        return data.clockDate;
    }

    /** 进入世界时是否播放 SAO 欢迎动画。 */
    public static boolean showWelcome() {
        return data.showWelcome;
    }

    /** 生物死亡时是否播放 SAO 碎裂特效。 */
    public static boolean deathShatter() {
        return data.deathShatter;
    }

    /** 碎裂粒子密度倍率(1.0 为默认)。 */
    public static float deathShatterDensity() {
        return data.deathShatterDensity;
    }

    /** 是否打开过设置界面(用于判断是否播放完整转场动画)。 */
    public static boolean hasOpenedSettings() {
        return data.hasOpenedSettings;
    }

    /** 标记已打开过设置界面。 */
    public static void markSettingsOpened() {
        data.hasOpenedSettings = true;
    }

    // ------------------------------------------------------------ 修改(带钳制;由界面负责 save)

    public static void setFrameworkEnabled(boolean enabled) {
        data.frameworkEnabled = enabled;
    }

    public static void setAnchorX(float v) {
        data.anchorX = clamp(v, ANCHOR_MIN, ANCHOR_MAX, data.anchorX);
    }

    public static void setAnchorY(float v) {
        data.anchorY = clamp(v, ANCHOR_MIN, ANCHOR_MAX, data.anchorY);
    }

    public static void setMenuScale(float v) {
        data.menuScale = clamp(v, SCALE_MIN, SCALE_MAX, data.menuScale);
    }

    public static void setBobAmp(float v) {
        data.bobAmp = clamp(v, BOB_MIN, BOB_MAX, data.bobAmp);
    }

    public static void setSounds(boolean v) {
        data.sounds = v;
    }

    public static void setHideHotbar(boolean v) {
        data.hideHotbar = v;
    }

    public static void setShowHud(boolean v) {
        data.showHud = v;
    }

    public static void setShowAvatar(boolean v) {
        data.showAvatar = v;
    }

    public static void setAnchorFollowMouse(boolean v) {
        data.anchorFollowMouse = v;
    }

    public static void setShowTargetBar(boolean v) {
        data.showTargetBar = v;
    }

    public static void setShowDamageNumbers(boolean v) {
        data.showDamageNumbers = v;
    }

    public static void setAccentHue(float v) {
        data.accentHue = clamp(v, 0f, 360f, data.accentHue);
    }

    public static void setMapPanelX(float v) {
        data.mapPanelX = clamp(v, 0f, 1f, data.mapPanelX);
    }

    public static void setMapPanelY(float v) {
        data.mapPanelY = clamp(v, 0f, 1f, data.mapPanelY);
    }

    public static void setMapPinned(boolean v) {
        data.mapPinned = v;
    }

    public static void setClockPanelX(float v) {
        data.clockPanelX = clamp(v, 0f, 1f, data.clockPanelX);
    }

    public static void setClockPanelY(float v) {
        data.clockPanelY = clamp(v, 0f, 1f, data.clockPanelY);
    }

    public static void setClockScale(float v) {
        data.clockScale = clamp(v, CLOCK_SCALE_MIN, CLOCK_SCALE_MAX, data.clockScale);
    }

    public static void setHotbarScale(float v) {
        data.hotbarScale = clamp(v, HOTBAR_MIN, HOTBAR_MAX, data.hotbarScale);
    }

    public static void setThirdPersonMenu(boolean v) {
        data.thirdPersonMenu = v;
    }

    public static void setShowBossBanner(boolean v) {
        data.showBossBanner = v;
    }

    public static void setPlatePanelX(float v) {
        data.platePanelX = clamp(v, 0f, 1f, data.platePanelX);
    }

    public static void setPlatePanelY(float v) {
        data.platePanelY = clamp(v, 0f, 1f, data.platePanelY);
    }

    /** 时钟是否仅 SAO 菜单打开期间显示(关闭菜单即隐藏)。 */
    public static boolean clockOnlyInMenu() {
        return data.clockOnlyInMenu;
    }

    public static void setClockOnlyInMenu(boolean v) {
        data.clockOnlyInMenu = v;
    }

    /** 按住 W 自动疾跑。 */
    public static boolean autoSprint() {
        return data.autoSprint;
    }

    public static void setAutoSprint(boolean v) {
        data.autoSprint = v;
    }

    /** 隐藏原版血条(饥饿条居中显示,氧气泡保留)。 */
    public static boolean hideVanillaHealth() {
        return data.hideVanillaHealth;
    }

    public static void setHideVanillaHealth(boolean v) {
        data.hideVanillaHealth = v;
    }

    /** 饥饿条锚点(屏幕比例)。 */
    public static float foodPanelX() {
        return data.foodPanelX;
    }

    public static float foodPanelY() {
        return data.foodPanelY;
    }

    public static void setFoodPanelX(float v) {
        data.foodPanelX = clamp(v, 0f, 1f, data.foodPanelX);
    }

    public static void setFoodPanelY(float v) {
        data.foodPanelY = clamp(v, 0f, 1f, data.foodPanelY);
    }

    /** 技能浮条锚点(屏幕比例;X/Y 都是 0-1 的比例,不是像素)。 */
    public static float skillBarX() {
        return data.skillBarX;
    }

    public static float skillBarY() {
        return data.skillBarY;
    }

    public static void setSkillBarX(float v) {
        data.skillBarX = clamp(v, 0f, 1f, data.skillBarX);
    }

    public static void setSkillBarY(float v) {
        data.skillBarY = clamp(v, 0f, 1f, data.skillBarY);
    }

    /** 当前选中的主题 id(未知 id 由主题层回落,这里只负责存取)。 */
    public static String themeId() {
        return data.themeId;
    }

    public static void setThemeId(String v) {
        data.themeId = SaoTheme.canonicalizeId(v);
    }

    /** 置顶物品注册名列表(只读快照)。 */
    public static List<String> pinnedItems() {
        return List.copyOf(data.pinnedItems);
    }

    /** 该物品是否已置顶。 */
    public static boolean isPinned(String id) {
        return data.pinnedItems.contains(id);
    }

    /**
     * 置顶顺序号;未置顶返回 {@link Integer#MAX_VALUE}(排序时自然沉底)。
     */
    public static int pinOrder(String id) {
        int i = data.pinnedItems.indexOf(id);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** 手动顺序号;不在自定义顺序里返回 {@link Integer#MAX_VALUE}。 */
    public static int orderIndex(String id) {
        int i = data.itemOrder.indexOf(id);
        return i < 0 ? Integer.MAX_VALUE : i;
    }

    /** 自定义顺序快照(只读)。 */
    public static List<String> itemOrder() {
        return List.copyOf(data.itemOrder);
    }

    /**
     * 整体覆盖自定义顺序(右键拖动换序时由菜单传入「当前完整显示顺序」)。
     *
     * <p>整表覆盖而不是只记两件:只记被拖的两件会让「已排序」与「未排序」
     * 物品之间无从比较,必须给所有物品一个确定位次。这也是为什么
     * 拖动不再顺带把物品置顶——置顶是独立的标记,不再被排序借用。</p>
     */
    public static void setItemOrder(List<String> order) {
        data.itemOrder = SaoConfigData.sanitize(order);
    }

    /** 切换置顶;返回切换后是否为置顶态。 */
    public static boolean togglePinned(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        if (data.pinnedItems.remove(id)) {
            return false;
        }
        data.pinnedItems.add(id);
        return true;
    }

    public static void setSaoToasts(boolean v) {
        data.saoToasts = v;
    }

    public static void setShowClock(boolean v) {
        data.showClock = v;
    }

    public static void setClock24h(boolean v) {
        data.clock24h = v;
    }

    public static void setClockDate(boolean v) {
        data.clockDate = v;
    }

    public static void setShowWelcome(boolean v) {
        data.showWelcome = v;
    }

    public static void setDeathShatter(boolean v) {
        data.deathShatter = v;
    }

    public static void setDeathShatterDensity(float v) {
        data.deathShatterDensity = clamp(v, SHATTER_MIN, SHATTER_MAX, data.deathShatterDensity);
    }

    /**
     * 恢复界面默认值。不改 {@link #hasOpenedSettings()}(是否打开过设置是会话标记,不是外观预设)。
     */
    public static void reset() {
        boolean opened = data.hasOpenedSettings;
        boolean enabled = data.frameworkEnabled;
        data = new SaoConfigData();
        data.hasOpenedSettings = opened;
        data.frameworkEnabled = enabled;
    }

    // ------------------------------------------------------------ 持久化

    /** 最近一次 load 的文件;从未加载时返回 null。 */
    public static Path path() {
        return loadedFrom;
    }

    public static void load(Path file) {
        loadedFrom = file;
        if (file == null || !Files.exists(file)) {
            return;
        }
        try {
            SaoConfigData loaded = SaoConfigStore.read(file);
            if (loaded == null) {
                return;
            }
            String keepTheme = loaded.themeId == null || loaded.themeId.isBlank() ? data.themeId : null;
            loaded.normalize();
            if (keepTheme != null) {
                loaded.themeId = keepTheme;
            }
            data = loaded;
        } catch (IOException | JsonSyntaxException e) {
            SAOMenu.LOGGER.warn("[SAOMenu] config load failed, keeping defaults: {}", e.toString());
        }
    }

    /**
     * 写回 {@link #load(Path)} 选定的非空路径。
     *
     * @throws IllegalStateException 尚未 load 非空路径
     */
    public static void save() {
        if (loadedFrom == null) {
            throw new IllegalStateException("SAOConfig.save() requires a prior load(Path) with a non-null path");
        }
        save(loadedFrom);
    }

    public static void save(Path file) {
        if (file == null) {
            return;
        }
        try {
            SaoConfigStore.write(file, data);
        } catch (IOException e) {
            SAOMenu.LOGGER.error("[SAOMenu] config save failed: {}", e.toString());
        }
    }

    /**
     * 有限值钳入 [lo, hi];NaN 保留 fallback,避免布局得到 NaN。±Inf 落入边界。
     */
    static float clamp(float v, float lo, float hi, float fallback) {
        if (Float.isNaN(v)) {
            return fallback;
        }
        if (v < lo) {
            return lo;
        }
        if (v > hi) {
            return hi;
        }
        return v;
    }
}
