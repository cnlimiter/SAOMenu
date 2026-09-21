package com.sao.saomenu.client;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.SAOMenuPlatform;
import com.sao.saomenu.client.menu.MenuContext;
import com.sao.saomenu.client.menu.MenuEntry;
import com.sao.saomenu.client.menu.MenuHost;
import com.sao.saomenu.client.menu.SaoMenuRegistry;
import com.sao.saomenu.client.menu.SaoPanel;
import com.sao.saomenu.client.menu.SaoPanels;
import com.sao.saomenu.ui.SaoDraw;
import com.sao.saomenu.ui.SaoText;
import com.sao.saomenu.ui.SaoTheme;
import com.sao.saomenu.ui.ThemeColors;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.scores.PlayerTeam;
import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.sao.saomenu.ui.SaoDraw.mulAlpha;
import static com.sao.saomenu.ui.SaoDraw.shaderAlpha;
import static com.sao.saomenu.ui.SaoMotion.BOB_PERIOD_MS;
import static com.sao.saomenu.ui.SaoMotion.CLOSE_MS;
import static com.sao.saomenu.ui.SaoMotion.ITEM_MS;
import static com.sao.saomenu.ui.SaoMotion.ITEM_STAGGER_MS;
import static com.sao.saomenu.ui.SaoMotion.OPEN_MS;
import static com.sao.saomenu.ui.SaoMotion.PANEL_MS;
import static com.sao.saomenu.ui.SaoMotion.PRESS_MS;
import static com.sao.saomenu.ui.SaoMotion.UNFOLD_MS;
import static com.sao.saomenu.ui.SaoMotion.UNFOLD_STAGGER_MS;
import static com.sao.saomenu.ui.SaoMotion.clamp01;
import static com.sao.saomenu.ui.SaoMotion.easeOutBack;
import static com.sao.saomenu.ui.SaoMotion.easeOutCubic;
import static com.sao.saomenu.ui.SaoText.resolveLabel;
import static com.sao.saomenu.ui.SaoText.tr;

/**
 * SAO Utils 风格圆形菜单主界面。
 *
 * <p>对照 SAO-World 参考截图 1:1 还原:世界保持全亮(无遮罩),
 * 菜单以活动按钮为锚点位于屏幕左上区域、整体缓缓上下浮动;
 * 打开时从按钮处缩放弹出,卡片与菜单项带级联滑入动画,
 * 关闭时缩回消失。不含原视频右侧任务栏。</p>
 */
public class SAOMenuScreen extends Screen implements MenuHost {

    // 配色改由主题提供:SaoTheme.colors()。accent 随用户色相实时派生,
    // 其余 token 是主题预设固定值(见 ui/ThemeColors)。

    private static final ResourceLocation TEX_BTN = tex("btn_circle.png");
    private static final ResourceLocation TEX_BTN_NORMAL = tex("btn_normal.png");
    private static final ResourceLocation TEX_BTN_HOVER = tex("btn_hover.png");
    private static final ResourceLocation TEX_LIST_NORMAL = tex("list_normal.png");
    private static final ResourceLocation TEX_LIST_HOVER = tex("list_hover.png");
    private static final ResourceLocation TEX_INDICATOR = tex("indicator.png");
    private static final ResourceLocation TEX_PANEL = tex("panel.png");
    private static final ResourceLocation TEX_ALERT = tex("alert.png");
    private static final ResourceLocation TEX_BTN_OK = tex("btn_ok.png");
    private static final ResourceLocation TEX_BTN_OK_HOVER = tex("btn_ok_hover.png");
    private static final ResourceLocation TEX_BTN_CANCEL = tex("btn_cancel.png");
    private static final ResourceLocation TEX_BTN_CANCEL_HOVER = tex("btn_cancel_hover.png");
    private static final ResourceLocation TEX_BTN_PRESS = tex("btn_press.png");
    private static final ResourceLocation TEX_LIST_PRESS = tex("list_press.png");
    private static final ResourceLocation TEX_ITEM_MAP = tex("item_map.png");
    private static final ResourceLocation TEX_ACT_EQUIP = tex("item_run.png");
    private static final ResourceLocation TEX_ACT_EQUIP_H = tex("item_run_hover.png");
    private static final ResourceLocation TEX_ACT_INFO = tex("item_help.png");
    private static final ResourceLocation TEX_ACT_INFO_H = tex("item_help_hover.png");
    private static final ResourceLocation TEX_ACT_DROP = tex("item_remove.png");
    private static final ResourceLocation TEX_ACT_DROP_H = tex("item_remove_hover.png");
    private static final ResourceLocation TEX_ARROW_RIGHT = tex("arrow_right.png");
    private static final ResourceLocation TEX_RING = tex("ring.png");
    private static final ResourceLocation TEX_SILHOUETTE = tex("card_silhouette.png");

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/" + name);
    }

    // 图元与缓动改由 ui/SaoDraw 与 ui/SaoMotion 提供(见文件头静态导入)。

    // ---------------------------------------------------------------- 菜单模型
    // 条目与行为都在 client/menu/SaoPanels 里;本类只负责渲染、命中与动画。
    // 主按钮列 = 注册表里的面板,顺序即注册顺序。

    /** 装备条目:一行 = 一个已装备的物品。stack 为空表示「暂无装备」占位行。 */
    private record EquipEntry(ItemStack stack, boolean empty) {
    }

    /** 当前面板列表(注册顺序 = 主按钮列顺序)。 */
    private static List<SaoPanel> panels() {
        return SaoMenuRegistry.panels();
    }

    /** 第 index 个面板的一级项列;越界返回空列。 */
    private List<MenuEntry> itemsForPanel(int index) {
        List<SaoPanel> ps = panels();
        if (index < 0 || index >= ps.size()) {
            return List.of();
        }
        List<MenuEntry> items = ps.get(index).items().get();
        return items == null ? List.of() : items;
    }

    /** 第 index 个面板;越界返回 null。 */
    private static SaoPanel panelAt(int index) {
        List<SaoPanel> ps = panels();
        return index >= 0 && index < ps.size() ? ps.get(index) : null;
    }

    // ---------------------------------------------------------------- 动画状态
    // 时长常量见 ui/SaoMotion(静态导入)。


    private long openedAt;
    /** 开启动画只在新实例首次 init 时计时(resize 触发的 init 不重置)。 */
    private boolean openAnimArmed = true;
    private long panelAt = Long.MIN_VALUE;
    private int panelOwner = -1;
    private long childAt;
    private int childOwner = -1;
    private boolean closing;
    private long closedAt;

    // 装备第三列:哪个二级子项(武器/护甲/首饰)正在展示已装备物品
    private int equipOwner = -1;
    private long equipAt;
    /** 当前正在渲染装备列的子项(含悬停临时切换),变化时重置列动画。 */
    private int equipShownOwner = -1;

    private int selectedMain = -1;
    /** 是否已点击/滚轮选择过主按钮:未选择前列内全亮,选择后其余按钮压暗。 */
    private boolean mainTouched = false;
    private int hoverMain = -1;
    private int hoverItem = -1;
    private int hoverChild = -1;
    private int hoverEquip = -1;
    private int expandedItem = -1;

    // Logout 确认弹窗(参照 SAO_Utils Alert 窗)
    private boolean confirmClose;
    private long confirmAt;

    // 上一帧菜单组的变换参数(命中判定需做逆变换,与渲染保持一致)
    private float menuScale = 1f;
    private int menuAnchorX;
    private int menuAnchorY;

    // 打开时的首按钮锚点:跟随鼠标模式由光标位置钳制(参照 SAO_Utils),否则用配置锚点
    private int baseAnchorX;
    private int baseAnchorY;

    public SAOMenuScreen() {
        super(Component.translatable("saomenu.title"));
    }

    @Override
    protected void init() {
        // 从设置等子界面返回:菜单此前 beginClose 后被挂起(closing 未走完),
        // 直接显示会因 closeP>=1 第一帧即置空,这里复位关闭状态并重播开启动画
        if (closing) {
            closing = false;
            confirmClose = false;
            openAnimArmed = true;
        }
        // 只有新开菜单才重播开启动画;resize 重新 init 不重置(否则窗口变化时闪烁)
        if (openAnimArmed) {
            openedAt = now();
            openAnimArmed = false;
        }
        mainTouched = false;
        // 菜单位置固定:锚点 = 屏幕中线左侧(SAO-World 参照),不跟随鼠标
        baseAnchorX = MenuLayout.firstButtonCenterX(this.width);
        baseAnchorY = MenuLayout.firstButtonCenterY(this.height);
        menuAnchorX = baseAnchorX;
        menuAnchorY = baseAnchorY;
        playLauncher();
        SAOMenu.LOGGER.info("[SAOMenu] gui size {}x{} anchor {}x{} (fixed)",
                this.width, this.height, baseAnchorX, baseAnchorY);
    }

    /** 个人面板一级项数量(预览自检复用,直接问注册表,避免与面板定义脱钩)。 */
    static int profileItemCount() {
        SaoPanel p = SaoMenuRegistry.byId(SaoPanels.PROFILE);
        return p == null ? 0 : p.items().get().size();
    }

    /** 第 index 个主按钮圆心 Y(基于打开时锚点)。 */
    private int buttonY(int index) {
        return MenuLayout.buttonCenterYAt(this.height, baseAnchorY, index);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static long now() {
        return Util.getMillis();
    }

    private Minecraft mc() {
        return Minecraft.getInstance();
    }

    /**
     * 当前生效的主按钮(面板跟随点击,不跟随悬停——参照 SAO-World)。
     * 未点击任何按钮时返回 -1:只显示主按钮列。
     */
    private int activeMain() {
        return selectedMain;
    }

    /**
     * 世界空间菜单板({@link SAOMenu3DPanel})的显示强度:菜单打开/关闭动画
     * 期间从 0 渐变到 1 再回落,与 HUD 菜单的开合节奏同源。
     */
    public float worldMenuAlpha() {
        long t = now();
        if (closing) {
            return Mth.clamp(1f - (t - closedAt) / (float) CLOSE_MS, 0f, 1f);
        }
        return Mth.clamp((t - openedAt) / 150f, 0f, 1f);
    }

    /** 世界空间菜单板当前应高亮的主按钮(0..3);菜单收起或未选择时 -1。 */
    public int worldMenuMain() {
        return closing ? -1 : selectedMain;
    }

    private boolean isActive(int index) {
        return hoverMain == index || selectedMain == index;
    }

    /**
     * 当前面板的一级项列。
     *
     * <p>动态子列(在线玩家、背包条目)改由面板自己的 supplier 提供并自带缓存,
     * 所以这里不再需要"每帧改写静态数组"——旧实现每帧重建 {@code MenuItem} 并覆盖
     * {@code PROFILE_ITEMS[0]/[2]}、{@code PARTY_ITEMS[0]},既是每帧垃圾,
     * 也是一份共享可变状态。</p>
     */
    private List<MenuEntry> activeItems(int main) {
        return itemsForPanel(main);
    }

    /**
     * 右键命中物品条目 → 切换置顶。命中返回 true(吞掉右键)。
     *
     * <p>置顶按物品注册名记录,写入 {@code config/saomenu.json};
     * 因为记的是「物品种类」而不是槽位,丢掉后重新捡起仍然置顶。</p>
     */
    /** 右键拖动换序:按下时的窗口行下标;-1 表示没在拖。 */
    private int pinDragFrom = -1;
    /** 拖动中的物品(跟随鼠标的幽灵图标);null 表示没在拖。 */
    private ItemStack pinDragStack;
    /** 拖动中的鼠标位置(屏幕坐标,幽灵图标直接画在这里)。 */
    private int pinDragMx;
    private int pinDragMy;
    /** 拖动起始时间(入场缩放动画)。 */
    private long pinDragAt;

    /**
     * 命中物品条目的窗口行下标;没命中返回 -1。
     *
     * <p>命中几何必须与渲染同源:窗口化行 + 菜单本地坐标。</p>
     */
    private int pinRowAt(int mx, int my) {
        if (selectedMain < 0) {
            return -1;
        }
        List<MenuEntry> items = activeItems(selectedMain);
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return -1;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children());
        int anchorY = buttonY(selectedMain);
        int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height, items.size(),
                baseAnchorX, anchorY, shown).centerY();
        // 菜单组有浮动/缩放变换:命中必须先逆变换回菜单本地坐标
        computeLocal(mx, my);
        int lx = Math.round(localPtX);
        int ly = Math.round(localPtY);
        for (int i = 0; i < children.size(); i++) {
            ItemStack st = children.get(i).stack();
            if (st == null || st.isEmpty()) {
                continue;
            }
            if (MenuLayout.childItemRectAt(this.width, this.height, children.size(),
                    baseAnchorX, childAnchor, i).contains(lx, ly)) {
                return i;
            }
        }
        return -1;
    }

    /** 当前选中面板的二级列窗口;没有二级列时返回 null。 */
    private List<MenuEntry> shownChildrenAt(int main) {
        if (main < 0) {
            return null;
        }
        List<MenuEntry> items = activeItems(main);
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return null;
        }
        return windowedChildren(items.get(shown).children());
    }

    /** 窗口行下标 → ItemStack;取不到返回 null。 */
    private ItemStack stackAtRow(int row) {
        List<MenuEntry> children = shownChildrenAt(selectedMain);
        if (children == null || row < 0 || row >= children.size()) {
            return null;
        }
        ItemStack st = children.get(row).stack();
        return st == null || st.isEmpty() ? null : st;
    }

    /**
     * 拖动中的幽灵图标:物品跟着鼠标走,落点行高亮一条主题色横线。
     *
     * <p>画在所有菜单元素之上(z=400),不参与菜单的浮动/缩放变换——
     * 鼠标坐标本来就是屏幕坐标,跟着变换反而会与光标错位。</p>
     */
    private void renderPinDragGhost(GuiGraphics g, long now) {
        if (pinDragFrom < 0 || pinDragStack == null) {
            return;
        }
        // 入场:120ms 内从 0.6 弹到 1.0
        float t = clamp01((now - pinDragAt) / 120f);
        float scale = 0.6f + 0.4f * easeOutCubic(t);
        int size = Math.round(20 * scale);

        // 落点提示:悬停在另一行上时,该行左缘画一条主题色竖条
        int target = pinRowAt(pinDragMx, pinDragMy);
        if (target >= 0 && target != pinDragFrom && selectedMain >= 0) {
            List<MenuEntry> items = activeItems(selectedMain);
            int shown = visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> children = windowedChildren(items.get(shown).children());
                int anchorY = buttonY(selectedMain);
                int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height,
                        items.size(), baseAnchorX, anchorY, shown).centerY();
                MenuLayout.Rect at = MenuLayout.childItemRectAt(this.width, this.height,
                        children.size(), baseAnchorX, childAnchor, target);
                g.pose().pushPose();
                g.pose().translate(0, 0, 400f);
                // 插入位置示意:目标行上缘一条主题色横线
                g.fill(at.x(), at.y() - 1, at.x() + at.w(), at.y() + 1, theme().accent());
                g.pose().popPose();
            }
        }

        g.pose().pushPose();
        g.pose().translate(pinDragMx + 8f, pinDragMy + 8f, 400f);
        g.pose().scale(size / 16f, size / 16f, 1f);
        g.renderItem(pinDragStack, -8, -8);
        g.pose().popPose();
    }

    /** 窗口行下标 → 物品注册名;取不到返回 null。 */
    private String itemIdAtRow(int row) {
        List<MenuEntry> children = shownChildrenAt(selectedMain);
        if (children == null || row < 0 || row >= children.size()) {
            return null;
        }
        String id = SaoPanels.itemId(children.get(row).stack());
        return id.isEmpty() ? null : id;
    }

    /** Shift+右键:切换该行物品的置顶态并落盘。 */
    private void togglePinAt(int row) {
        String id = itemIdAtRow(row);
        if (id == null) {
            return;
        }
        boolean pinned = SAOConfig.togglePinned(id);
        savePinConfig();
        SAONotification.push(itemNameAtRow(row),
                SaoText.tr(pinned ? "saomenu.inv.pinned" : "saomenu.inv.unpinned"));
    }

    /**
     * 右键拖动松手:把起点行的物品移到落点行的位置。
     *
     * <p>做法是取当前完整显示顺序,把被拖物品搬到目标位置,整表写回配置——
     * 只记被拖的两件会让「已排序」与「未排序」物品之间无从比较。
     * 置顶是独立标记,拖动**不会**顺带置顶任何物品。</p>
     */
    private void applyPinDrag(int fromRow, int toRow) {
        if (fromRow < 0 || toRow < 0 || fromRow == toRow || selectedMain < 0) {
            return;
        }
        List<MenuEntry> items = activeItems(selectedMain);
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return;
        }
        // 用全量 children(不是窗口)构造顺序表,滚动时拖动也不会打乱屏幕外条目
        List<MenuEntry> all = items.get(shown).children();
        String fromId = itemIdAtRow(fromRow);
        String toId = itemIdAtRow(toRow);
        if (fromId == null || toId == null || fromId.equals(toId)) {
            return;
        }
        java.util.List<String> order = new java.util.ArrayList<>();
        for (MenuEntry mi : all) {
            String id = SaoPanels.itemId(mi.stack());
            if (!id.isEmpty() && !order.contains(id)) {
                order.add(id);
            }
        }
        order.remove(fromId);
        int at = order.indexOf(toId);
        order.add(at < 0 ? order.size() : at, fromId);
        SAOConfig.setItemOrder(order);
        savePinConfig();
    }

    private String itemNameAtRow(int row) {
        List<MenuEntry> children = shownChildrenAt(selectedMain);
        if (children == null || row < 0 || row >= children.size()) {
            return "";
        }
        ItemStack st = children.get(row).stack();
        return st == null || st.isEmpty() ? "" : st.getHoverName().getString();
    }

    private void savePinConfig() {
        java.nio.file.Path cfg = SAOConfig.path();
        if (cfg == null) {
            cfg = mc().gameDirectory.toPath().resolve("config").resolve("saomenu.json");
        }
        SAOConfig.save(cfg);
        MenuContext.invalidateCached(); // 立刻按新顺序重排
        playPanel();
    }

    // 物品操作按钮(参照动画:选中行右侧弹出三圆钮)
    private boolean actionMenuOpen;
    private int actionRow = -1;      // 二级列窗口内行下标
    private long actionAt;
    private int hoverAction = -1;
    private static final String[] ACT_KEYS = {
            "saomenu.act.equip", "saomenu.act.info", "saomenu.act.drop"};

    // 物品信息弹窗(白卡属性行 + 官方圆钮)
    private boolean infoOpen;
    private long infoAt;
    private ItemStack infoStack;

    // 伪3D:整组菜单随鼠标轻微倾斜(平滑后的倾斜量)
    private float swayXs;
    private float swayYs;

    // 二级展开时整组左移(参照 SAO-World:子菜单落在一级列原位),平滑跟随
    private float shiftXs;

    // 整组随鼠标轻微漂移(悬浮感),平滑后的屏幕像素偏移
    private float followXs;
    private float followYs;
    private float followPxX;
    private float followPxY;

    // 移动穿透:GLFW 键值缓存(首次遍历键值空间解析各移动键的键码)
    private static final int[] MOVE_KEY_CODES = new int[7];
    private static boolean moveKeyCodesResolved;

    /**
     * 每帧直接轮询 GLFW 按键状态同步移动键(事件路径不可靠时的保底,
     * 直接驱动 KeyMapping.isDown,而 LocalPlayer.aiStep 每帧无条件读取它)。
     */
    private void pollMoveKeys() {
        if (!moveKeyCodesResolved) {
            KeyMapping[] kms = moveKeys();
            for (int code = 32; code <= 348; code++) {
                for (int i = 0; i < kms.length; i++) {
                    if (MOVE_KEY_CODES[i] == 0 && kms[i].matches(code, 0)) {
                        MOVE_KEY_CODES[i] = code;
                    }
                }
            }
            moveKeyCodesResolved = true;
        }
        long win = mc().getWindow().getWindow();
        KeyMapping[] kms = moveKeys();
        for (int i = 0; i < kms.length; i++) {
            kms[i].setDown(MOVE_KEY_CODES[i] != 0
                    && com.mojang.blaze3d.platform.InputConstants.isKeyDown(win, MOVE_KEY_CODES[i]));
        }
    }

    /** 松开全部移动键(关菜单/弹窗时防卡键)。 */
    private void releaseMoveKeys() {
        for (KeyMapping km : moveKeys()) {
            km.setDown(false);
        }
    }

    // 按压态:点击瞬间高亮对应图元(模拟视频里手指点触反馈),120ms 后回弹

    private long mainPressAt = Long.MIN_VALUE;
    private int mainPressIndex = -1;
    private long itemPressAt = Long.MIN_VALUE;
    private int itemPressIndex = -1;
    private int itemPressColumn = -1; // 0=一级列,1=二级列

    // 二级列滚动(物品条目 30+ 行放不下一屏):childScroll = 窗口起点
    private int childScroll;
    /** 二级列可见行数(按屏高算)。 */
    private int childVisibleRows() {
        int step = MenuLayout.itemH(this.height) + MenuLayout.itemGap(this.height);
        return Math.max(3, (this.height - 24) / step);
    }

    /**
     * 窗口化二级 children:物品条目超过一屏时只取 [childScroll, childScroll+rows)。
     * 非物品列原样返回。窗口列表带缓存(同源同滚动直接复用,避免每帧新建)。
     */
    private List<MenuEntry> windowedChildren(List<MenuEntry> children) {
        int rows = childVisibleRows();
        if (children.size() <= rows) {
            // 不满一屏:整个列表直接显示,滚动归零
            // (注意不能走下面的 clamp——size-rows 为负时 clamp 会返回负数导致越界崩溃)
            childScroll = 0;
            windowCacheSrc = null;
            return children;
        }
        if (windowCacheSrc == children && windowCacheScroll == childScroll && windowCacheOut != null) {
            return windowCacheOut;
        }
        childScroll = Mth.clamp(childScroll, 0, children.size() - rows);
        List<MenuEntry> win = new ArrayList<>(children.subList(childScroll, childScroll + rows));
        windowCacheSrc = children;
        windowCacheScroll = childScroll;
        windowCacheOut = win;
        return win;
    }

    /** 窗口缓存键:源列表引用 + 滚动偏移。 */
    private List<MenuEntry> windowCacheSrc;
    private int windowCacheScroll = -1;
    private List<MenuEntry> windowCacheOut;

    private boolean mainPressing(int index) {
        return mainPressIndex == index
                && now() - mainPressAt < PRESS_MS;
    }

    private boolean itemPressing(int column, int index) {
        return itemPressColumn == column && itemPressIndex == index
                && now() - itemPressAt < PRESS_MS;
    }

    // ---------------------------------------------------------------- 物品条目(二级 children)

    private void switchPanelIfChanged(int main) {
        if (panelOwner != main) {
            panelOwner = main;
            panelAt = now();
            expandedItem = -1;
            childOwner = -1;
            equipOwner = -1;
            equipShownOwner = -1;
            actionMenuOpen = false;
            childScroll = 0;
            infoOpen = false;
        }
    }

    // ---------------------------------------------------------------- 渲染

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        long now = now();

        // 世界已卸载(退网/回主菜单):立即关闭,避免后续渲染读到 null 玩家数据
        if (mc().player == null || mc().level == null) {
            super.onClose();
            return;
        }

        // 移动穿透:每帧轮询 GLFW 按键;弹窗/关闭时全部松开防卡键
        if (!closing && !confirmClose && !infoOpen) {
            pollMoveKeys();
        } else {
            releaseMoveKeys();
        }

        float openP = closing ? 1f : clamp01((now - openedAt) / (float) OPEN_MS);
        float closeP = closing ? clamp01((now - closedAt) / (float) CLOSE_MS) : 0f;
        if (closing && closeP >= 1f) {
            super.onClose();
            return;
        }
        float globalAlpha = closing ? 1f - closeP : clamp01((now - openedAt) / 150f);

        if (!closing) {
            updateHovers(mouseX, mouseY);
        }

        int main = activeMain();
        if (main >= 0 && !closing) {
            switchPanelIfChanged(main);
        }

        // 底部圆点 = 物品栏,不参与浮动/缩放(第 1 个为副手,与主栏隔开一档;圆点内渲染真实物品图标)
        Player pp = mc().player;
        SAOHud.renderHotbarDots(g, this.width, this.height, pp, globalAlpha);

        // 菜单组:整体上下浮动 + 打开/关闭缩放,锚点 = 活动按钮圆心
        float bobY = Mth.sin(now / (float) BOB_PERIOD_MS * Mth.TWO_PI) * this.height * 0.006f * SAOConfig.bobAmp();
        float scale = closing
                ? 1f - 0.15f * easeOutCubic(closeP)
                : (0.55f + 0.45f * easeOutBack(openP)) * SAOConfig.menuScale();
        int ax = baseAnchorX;
        int ay = buttonY(Math.max(0, main)) + Math.round(bobY);
        menuScale = scale;
        menuAnchorX = ax;
        menuAnchorY = ay;

        // 二级列可见时整组左移一个列宽(子菜单正好落在一级列原位),平滑跟随
        float shiftTarget = 0f;
        if (main >= 0) {
            List<MenuEntry> its = activeItems(main);
            if (visibleChildrenItem(its) >= 0) {
                shiftTarget = MenuLayout.childColumnXAt(baseAnchorX, this.height)
                        - MenuLayout.itemColumnXAt(baseAnchorX, this.height);
            }
        }
        shiftXs += (shiftTarget - shiftXs) * 0.18f;

        // 整组随鼠标轻微漂移(以屏幕中心为原点,幅度 ~1.8% 屏宽)
        float fx = Mth.clamp((mouseX - this.width / 2f) / (float) this.width, -0.5f, 0.5f);
        float fy = Mth.clamp((mouseY - this.height / 2f) / (float) this.height, -0.5f, 0.5f);
        followXs += (fx - followXs) * 0.08f;
        followYs += (fy - followYs) * 0.08f;
        followPxX = followXs * this.width * 0.035f;
        followPxY = followYs * this.height * 0.035f;

        var pose = g.pose();
        pose.pushPose();
        pose.translate(ax, ay, 0);
        // 伪3D(2D 仿射安全版):Z 轴微旋转 + 错切模拟透视倾斜,全程 z=0。
        // 之前用 X/Y 轴真 3D 旋转会把顶点推出 GUI 深度安全范围,
        // 在 ImmediatelyFast/Oculus 合批渲染下表现为整组菜单闪烁重影
        float tx = Mth.clamp((mouseX - ax) / (float) Math.max(1, this.width), -0.6f, 0.6f);
        float ty = Mth.clamp((mouseY - ay) / (float) Math.max(1, this.height), -0.6f, 0.6f);
        swayXs += (tx - swayXs) * 0.14f;
        swayYs += (ty - swayYs) * 0.14f;
        pose.mulPose(com.mojang.math.Axis.ZP.rotation(swayXs * 0.03f));
        org.joml.Matrix4f sway = new org.joml.Matrix4f(
                1f, swayYs * 0.05f, 0f, 0f,
                swayXs * 0.06f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f);
        pose.last().pose().mul(sway);
        pose.scale(scale, scale, 1f);
        pose.translate(-ax, -ay, 0);
        pose.translate(-shiftXs, 0.0F, 0.0F);
        pose.translate(followPxX, followPxY, 0.0F);

        if (main >= 0) {
            renderPanelFor(g, main, mouseX, mouseY, globalAlpha, now);
            renderMenuItems(g, main, mouseX, mouseY, globalAlpha, now);
        }
        renderMainButtons(g, globalAlpha);

        pose.popPose();

        // 悬停物品条目(二级列)的原版 tooltip:名称/附魔/耐久,压在所有图元上
        // hoverChild 是窗口内下标,真实条目 = childScroll + hoverChild
        // 弹窗/操作按钮打开时不画(tooltip 会盖在它们上面)
        if (!infoOpen && !confirmClose && !actionMenuOpen) {
            int mainTip = activeMain();
            List<MenuEntry> itemsTip = mainTip >= 0 ? activeItems(mainTip) : null;
            if (itemsTip != null && hoverChild >= 0) {
                int shownTip = visibleChildrenItem(itemsTip);
                if (shownTip >= 0 && itemsTip.get(shownTip).children() != null) {
                    List<MenuEntry> all = itemsTip.get(shownTip).children();
                    int real = childScroll + hoverChild;
                    if (real >= 0 && real < all.size()) {
                        ItemStack st = all.get(real).stack();
                        if (st != null && !st.isEmpty()) {
                            g.renderTooltip(this.font, st, mouseX, mouseY);
                        }
                    }
                }
            }
        }

        // 地图面板:浮在菜单组之上,独立于浮动/缩放变换(自带滑入动画)
        SAOMapPanel.render(g, mc(), this.width, this.height, globalAlpha);

        // 菜单打开期间常驻 HUD 由本 Screen 接管(平台 HUD 钩子此时跳过):
        // 血条板保持全透明度无缝衔接,圆点(上方已绘制)随开关动画淡入淡出
        int hudPx = SAOHud.plateX(this.width);
        int hudPy = SAOHud.plateY(this.height);
        SAOHud.renderPlate(g, hudPx, hudPy, SAOHud.plateW(this.width), SAOHud.plateH(this.width),
                playerName(), pp, 1f);
        if (pp != null) {
            SAOHud.renderTeamBars(g, mc(), hudPx, hudPy + SAOHud.plateH(this.width) + 2, this.width, pp);
        }
        SAOClockPanel.render(g, mc(), this.width, this.height, globalAlpha);
        if (pp != null && pp.getMaxHealth() > 0f) {
            SAOHud.renderLowHpVignette(g, this.width, this.height, pp.getHealth() / pp.getMaxHealth());
        }
        if (confirmClose) {
            renderConfirmDialog(g, mouseX, mouseY, globalAlpha, now);
        }
        if (infoOpen) {
            renderInfoDialog(g, mouseX, mouseY, globalAlpha, now);
        }
        // 右键拖动排序的幽灵图标:画在最上层,跟随光标
        renderPinDragGhost(g, now);
    }



    /** Logout 确认弹窗(SAO Utils 官方 alert 窗素材,官方蓝◎/粉✕圆钮,翻转入场)。 */
    private void renderConfirmDialog(GuiGraphics g, int mouseX, int mouseY, float alpha, long now) {
        MenuLayout.Rect at = dialogRect();
        float p = clamp01((now - confirmAt) / 160f);
        float s = 0.1f + 0.9f * easeOutBack(p);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(this.width / 2f, this.height / 2f, 0);
        pose.scale(1f, s, 1f);
        pose.translate(-this.width / 2f, -this.height / 2f, 0);

        // 物品图标延迟合批先刷掉;垫近实心底防菜单内容透出(同信息弹窗)
        g.flush();
        RenderSystem.disableDepthTest();
        g.fill(at.x() + 3, at.y() + 3, at.x() + at.w() + 3, at.y() + at.h() + 3, mulAlpha(theme().dialogShadow(), alpha));
        int insX = Math.max(2, Math.round(at.w() * 0.02f));
        int insTop = Math.max(2, Math.round(at.h() * 0.045f));
        int insBot = Math.max(2, Math.round(at.h() * 0.02f));
        g.fill(at.x() + insX, at.y() + insTop, at.x() + at.w() - insX, at.y() + at.h() - insBot,
                mulAlpha(theme().dialogSurface(), alpha));
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, TEX_ALERT, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        Font f = this.font;
        String title = tr("saomenu.logout.title");
        g.drawString(f, title, at.centerX() - f.width(title) / 2, at.y() + 10,
                mulAlpha(theme().textOnSurface(), alpha), false);
        String msg = tr("saomenu.logout.msg");
        g.drawString(f, msg, at.centerX() - f.width(msg) / 2,
                at.y() + Math.round(at.h() * 0.44f),
                mulAlpha(theme().textOnSurface(), alpha), false);

        // 官方圆钮:蓝◎确认 / 粉✕取消(悬停换亮版贴图)
        int d = 26;
        int by = at.y() + Math.round(at.h() * 0.80f) - d / 2;
        int b1x = at.x() + at.w() / 4 - d / 2;
        int b2x = at.x() + at.w() * 3 / 4 - d / 2;
        boolean h1 = inCircle(b1x + d / 2, by + d / 2, d / 2, mouseX, mouseY);
        boolean h2 = inCircle(b2x + d / 2, by + d / 2, d / 2, mouseX, mouseY);
        RenderSystem.enableBlend();
        g.blit(h1 ? TEX_BTN_OK_HOVER : TEX_BTN_OK, b1x, by, 0, 0, d, d, d, d);
        g.blit(h2 ? TEX_BTN_CANCEL_HOVER : TEX_BTN_CANCEL, b2x, by, 0, 0, d, d, d, d);
        shaderAlpha(1f);

        pose.popPose();
    }

    private boolean inCircle(int cx, int cy, int r, int x, int y) {
        return MenuLayout.inCircle(cx, cy, r, x, y);
    }

    /** 弹窗矩形(alert.png 350x253 比例,居中)。 */
    private MenuLayout.Rect dialogRect() {
        int w = Math.min(280, this.width - 20);
        int h = Math.round(w * 253f / 350f);
        return new MenuLayout.Rect((this.width - w) / 2, (this.height - h) / 2, w, h);
    }

    // ------------------------------------------------------------ 物品操作按钮

    /** 第 b 个操作按钮的矩形:缩小后水平居中排布在选中行上(参照动画)。 */
    private MenuLayout.Rect actionButtonRect(MenuLayout.Rect row, int b) {
        int d = Math.max(8, Math.round(row.h() * 0.85f));
        int cx = row.centerX() + (b - 1) * Math.round(d * 1.18f);
        int cy = row.centerY();
        return new MenuLayout.Rect(cx - d / 2, cy - d / 2, d, d);
    }

    /** 执行物品操作:0=装备(服务端换位) 1=信息弹窗 2=丢弃(服务端掉落)。 */
    private void executeItemAction(int b, MenuEntry target) {
        actionMenuOpen = false;
        if (b == 0) {
            new com.sao.saomenu.party.EquipItemC2S(target.invSlot()).sendToServer();
            playClick();
        } else if (b == 1) {
            infoStack = target.stack().copy();
            infoOpen = true;
            infoAt = now();
            playAlert();
        } else {
            new com.sao.saomenu.party.DropItemC2S(target.invSlot(), true).sendToServer();
            playClick();
        }
    }

    // ------------------------------------------------------------ 物品信息弹窗

    /** 物品信息弹窗(参照动画图三:alert 白卡 + 属性行 + 官方蓝◎/粉✕圆钮)。 */
    private void renderInfoDialog(GuiGraphics g, int mouseX, int mouseY, float alpha, long now) {
        ItemStack st = infoStack;
        if (st == null || st.isEmpty()) {
            infoOpen = false;
            return;
        }
        MenuLayout.Rect at = dialogRect();
        float p = clamp01((now - infoAt) / 160f);
        float s = 0.1f + 0.9f * easeOutBack(p);
        var pose = g.pose();
        pose.pushPose();
        pose.translate(this.width / 2f, this.height / 2f, 0);
        pose.scale(1f, s, 1f);
        pose.translate(-this.width / 2f, -this.height / 2f, 0);

        // 物品图标延迟合批,先刷掉;alert 面板原设计 77% 玻璃感,
        // 底下垫一层近实心白,菜单内容才不会透出弹窗
        g.flush();
        RenderSystem.disableDepthTest();
        g.fill(at.x() + 3, at.y() + 3, at.x() + at.w() + 3, at.y() + at.h() + 3, mulAlpha(theme().dialogShadow(), alpha));
        int insX = Math.max(2, Math.round(at.w() * 0.02f));
        int insTop = Math.max(2, Math.round(at.h() * 0.045f));
        int insBot = Math.max(2, Math.round(at.h() * 0.02f));
        g.fill(at.x() + insX, at.y() + insTop, at.x() + at.w() - insX, at.y() + at.h() - insBot,
                mulAlpha(theme().dialogSurface(), alpha));
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, TEX_ALERT, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        Font f = this.font;
        // 标题 = 物品名(稀有度颜色)
        String title = st.getHoverName().getString();
        g.drawString(f, title, at.centerX() - f.width(title) / 2, at.y() + 8,
                mulAlpha(st.getRarity().color.getColor(), alpha), false);

        // 属性行(灰色带内居中排列)
        List<String> lines = buildInfoLines(st);
        int ly = at.y() + Math.round(at.h() * 0.22f);
        int maxLines = 7;
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            g.drawString(f, lines.get(i), at.centerX() - f.width(lines.get(i)) / 2,
                    ly + i * 11, mulAlpha(theme().textOnSurface(), alpha), false);
        }

        // 单个确认圆钮:底部中央,点击任意处只关闭弹窗(菜单保持打开)
        int d = 26;
        int bx = at.centerX() - d / 2;
        int by = at.y() + Math.round(at.h() * 0.82f) - d / 2;
        boolean hv = MenuLayout.inCircle(at.centerX(), by + d / 2, d / 2, mouseX, mouseY);
        RenderSystem.enableBlend();
        g.blit(hv ? TEX_BTN_OK_HOVER : TEX_BTN_OK, bx, by, 0, 0, d, d, d, d);

        pose.popPose();
    }

    /** 物品属性行:类型/数量/耐久/附魔/ID。 */
    private List<String> buildInfoLines(ItemStack st) {
        List<String> lines = new ArrayList<>();
        net.minecraft.world.item.Item item = st.getItem();
        String type;
        if (item instanceof net.minecraft.world.item.ArmorItem) {
            type = tr("saomenu.info.armor");
        } else if (item instanceof net.minecraft.world.item.SwordItem
                || item instanceof net.minecraft.world.item.ProjectileWeaponItem) {
            type = tr("saomenu.info.weapon");
        } else if (item instanceof net.minecraft.world.item.TieredItem
                || item instanceof net.minecraft.world.item.DiggerItem) {
            type = tr("saomenu.info.tool");
        } else {
            type = tr("saomenu.info.item");
        }
        lines.add(tr("saomenu.info.type") + ": " + type);
        lines.add(tr("saomenu.info.count") + ": " + st.getCount());
        if (st.isDamageableItem()) {
            lines.add(tr("saomenu.info.durability") + ": "
                    + (st.getMaxDamage() - st.getDamageValue()) + " / " + st.getMaxDamage());
        }
        var ench = net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(st);
        int shown = 0;
        for (var e : ench.entrySet()) {
            if (shown >= 4) {
                lines.add("… +" + (ench.size() - shown));
                break;
            }
            lines.add(tr("saomenu.info.enchant") + ": " + e.getKey().getFullname(e.getValue()).getString());
            shown++;
        }
        lines.add(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
        return lines;
    }

    private void updateHovers(int mouseX, int mouseY) {
        if (confirmClose) {
            return;
        }
        // 菜单组有浮动/缩放变换,先逆变换回菜单本地坐标再做命中
        computeLocal(mouseX, mouseY);
        int lx = Math.round(localPtX);
        int ly = Math.round(localPtY);
        hoverMain = MenuLayout.hoveredMainButtonAt(this.width, this.height, baseAnchorX, baseAnchorY,
                panels().size(), lx, ly);
        hoverItem = -1;
        hoverChild = -1;
        hoverEquip = -1;
        hoverAction = -1;
        int main = activeMain();
        if (main < 0) {
            return;
        }
        List<MenuEntry> items = activeItems(main);
        int anchorY = buttonY(main);
        // 操作按钮打开时:独占命中(只悬停三圆钮)
        if (actionMenuOpen) {
            List<MenuEntry> winA = shownChildren(items);
            if (winA != null && actionRow >= 0 && actionRow < winA.size()) {
                int shownA = visibleChildrenItem(items);
                int anchorA = shownA >= 0 ? MenuLayout.menuItemRectAt(this.width, this.height,
                        items.size(), baseAnchorX, anchorY, shownA).centerY() : anchorY;
                MenuLayout.Rect rowA = MenuLayout.childItemRectAt(this.width, this.height,
                        winA.size(), baseAnchorX, anchorA, actionRow);
                for (int b = 0; b < 3; b++) {
                    if (actionButtonRect(rowA, b).contains(lx, ly)) {
                        hoverAction = b;
                        break;
                    }
                }
            }
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, i).contains(lx, ly)) {
                hoverItem = i;
            }
        }
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children());
        int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, shown).centerY();
        for (int i = 0; i < children.size(); i++) {
            if (MenuLayout.childItemRectAt(this.width, this.height, children.size(), baseAnchorX, childAnchor, i)
                    .contains(lx, ly)) {
                hoverChild = i;
            }
        }
        int target = equipTargetIndex(items, shown);
        if (target < 0) {
            return;
        }
        List<EquipEntry> entries = equipEntries(equipKindAt(items, shown, target));
        int equipAnchor = equipAnchorY(items, shown, target);
        for (int i = 0; i < entries.size(); i++) {
            if (MenuLayout.equipItemRectAt(this.width, this.height, entries.size(), baseAnchorX, equipAnchor, i)
                    .contains(lx, ly)) {
                hoverEquip = i;
            }
        }
    }

    /**
     * 一级项里当前应展开子项列的那个:只看点击展开项(expandedItem)。
     * 悬停不再自动展开——参考 SAO-World,子菜单必须点一下才打开。
     */
    private int visibleChildrenItem(List<MenuEntry> items) {
        if (expandedItem >= 0 && expandedItem < items.size() && items.get(expandedItem).children() != null) {
            return expandedItem;
        }
        return -1;
    }

    /** 当前展开二级列的窗口化 children;未展开返回 null(渲染与命中共用同一几何)。 */
    private List<MenuEntry> shownChildren(List<MenuEntry> items) {
        int shown = visibleChildrenItem(items);
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return null;
        }
        return windowedChildren(items.get(shown).children());
    }

    /** 当前应展示装备列的二级子项下标;不展示返回 -1。物品条目列不触发装备列。 */
    private int equipTargetIndex(List<MenuEntry> items, int shown) {
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return -1;
        }
        List<MenuEntry> children = items.get(shown).children();
        // 物品条目列(条目带 stack)不关联装备展示
        if (!children.isEmpty() && children.get(0).stack() != null) {
            return -1;
        }
        // 只看点击选中的子项:悬停不再自动展开(参照 SAO-World,点一下才打开)
        int target = equipOwner;
        if (target < 0 || target >= children.size()) {
            return -1;
        }
        return children.get(target).equip() != null ? target : -1;
    }

    /** 目标二级子项要求的装备分类;不是装备列返回 null。 */
    private MenuEntry.EquipKind equipKindAt(List<MenuEntry> items, int shown, int target) {
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return null;
        }
        List<MenuEntry> children = items.get(shown).children();
        return target >= 0 && target < children.size() ? children.get(target).equip() : null;
    }

    /** 装备列锚点 Y:对齐目标二级子项行的纵向中心。 */
    private int equipAnchorY(List<MenuEntry> items, int shown, int target) {
        int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height, items.size(),
                baseAnchorX, buttonY(shown), shown).centerY();
        return MenuLayout.childItemRectAt(this.width, this.height, items.get(shown).children().size(),
                baseAnchorX, childAnchor, target).centerY();
    }

    /** 收集某分类下已装备的物品条目;全空时返回一条「暂无装备」占位。 */
    private List<EquipEntry> equipEntries(MenuEntry.EquipKind kind) {
        List<EquipEntry> list = new ArrayList<>();
        Player p = mc().player;
        if (p != null) {
            switch (kind) {
                case WEAPON -> {
                    if (!p.getMainHandItem().isEmpty()) {
                        list.add(new EquipEntry(p.getMainHandItem(), false));
                    }
                }
                case ARMOR -> {
                    // 头 → 胸 → 腿 → 脚
                    for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                        if (slot.getType() != net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) {
                            continue;
                        }
                        ItemStack s = p.getItemBySlot(slot);
                        if (!s.isEmpty()) {
                            list.add(new EquipEntry(s, false));
                        }
                    }
                }
                case TRINKET -> {
                    if (!p.getOffhandItem().isEmpty()) {
                        list.add(new EquipEntry(p.getOffhandItem(), false));
                    }
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new EquipEntry(ItemStack.EMPTY, true));
        }
        return list;
    }

    /**
     * 屏幕坐标 → 菜单本地坐标,结果写入 {@link #localPtX}/{@link #localPtY}。
     *
     * <p>逆用 {@link #render} 里 pushPose 的那一整条变换链,顺序为
     * 锚点位移 → 漂移 → 左移 → 缩放 → 错切 → Z 轴旋转 的逆序。</p>
     *
     * <p>X/Y 必须一起算:错切与 Z 轴旋转都会把两个轴耦合起来,
     * 分别逆 X、逆 Y 在数学上就还原不了——旧实现只逆了缩放与位移,
     * 屏幕边缘处(错切量最大)命中会偏出一个条目高。</p>
     */
    private void computeLocal(double mx, double my) {
        float dx = (float) mx - menuAnchorX;
        float dy = (float) my - menuAnchorY;
        float cos = Mth.cos(swayXs * 0.03f);
        float sin = Mth.sin(swayXs * 0.03f);
        // 逆 Z 轴旋转
        float rx = cos * dx + sin * dy;
        float ry = -sin * dx + cos * dy;
        // 逆错切:x' = x + a*y, y' = y + b*x
        float a = swayXs * 0.06f;
        float b = swayYs * 0.05f;
        float det = Math.abs(1f - a * b) < 1.0e-4f ? 1.0e-4f : 1f - a * b;
        float ux = (rx - a * ry) / det;
        float uy = (ry - b * rx) / det;
        // 逆缩放,再补回漂移/左移/锚点
        float s = Math.abs(menuScale) < 1.0e-4f ? 1.0e-4f : menuScale;
        localPtX = ux / s + menuAnchorX + shiftXs - followPxX;
        localPtY = uy / s + menuAnchorY - followPxY;
    }

    /** {@link #computeLocal} 的输出(避免每帧装箱/分配)。 */
    private float localPtX;
    private float localPtY;

    /** 正向变换的临时输出(菜单本地坐标 → 屏幕坐标)。 */
    private final float[] ptOut = new float[2];

    /**
     * 菜单本地坐标 → 屏幕坐标。与 {@link #computeLocal} 严格互逆,
     * 同为 render 里 pose 链的正序:漂移 → 左移 → 缩放 → 错切 → Z 轴旋转 → 锚点位移。
     */
    private void localToScreen(float lx, float ly, float[] out) {
        float x = (lx + followPxX - shiftXs - menuAnchorX) * menuScale;
        float y = (ly + followPxY - menuAnchorY) * menuScale;
        float a = swayXs * 0.06f;
        float b = swayYs * 0.05f;
        float sx = x + a * y;
        float sy = y + b * x;
        float cos = Mth.cos(swayXs * 0.03f);
        float sin = Mth.sin(swayXs * 0.03f);
        out[0] = cos * sx - sin * sy + menuAnchorX;
        out[1] = sin * sx + cos * sy + menuAnchorY;
    }

    /**
     * 菜单本地矩形 → 屏幕轴对齐包围盒。
     *
     * <p>剪裁框(enableScissor)只收屏幕空间的轴对齐矩形,而菜单整组带旋转与错切,
     * 四角投影后不再是轴对齐的;取四角外接盒,代价是略微裁宽(安全方向)。</p>
     */
    @Override
    public MenuLayout.Rect localBoxToScreen(int lx, int ly, int w, int h) {
        localToScreen(lx, ly, ptOut);
        float minX = ptOut[0];
        float maxX = ptOut[0];
        float minY = ptOut[1];
        float maxY = ptOut[1];
        int[][] corners = {{lx + w, ly}, {lx, ly + h}, {lx + w, ly + h}};
        for (int[] c : corners) {
            localToScreen(c[0], c[1], ptOut);
            minX = Math.min(minX, ptOut[0]);
            maxX = Math.max(maxX, ptOut[0]);
            minY = Math.min(minY, ptOut[1]);
            maxY = Math.max(maxY, ptOut[1]);
        }
        int x0 = Math.round(minX);
        int y0 = Math.round(minY);
        return new MenuLayout.Rect(x0, y0, Math.max(1, Math.round(maxX) - x0), Math.max(1, Math.round(maxY) - y0));
    }

    /**
     * 菜单本地坐标 → 屏幕坐标(预览自检复用),返回 {@code {x, y}}。
     *
     * <p>自检经 {@code Screen#mouseClicked} 直接注入事件,收的是<b>屏幕</b>坐标,
     * 而布局算式给的是本地坐标;整组带缩放/左移(展开二级列时整组左移一列宽)/
     * 漂移/错切,不换算就会按偏,展开二级列后点按钮会直接落到空白处。</p>
     */
    @Override
    public float[] screenPointOf(float lx, float ly) {
        float[] out = new float[2];
        localToScreen(lx, ly, out);
        return out;
    }

    /** 当前主题调色板(每次取用都反映最新色相)。 */
    private static ThemeColors theme() {
        return SaoTheme.palette();
    }

    // ---------------------------------------------------------------- 主按钮

    // 主按钮堆叠展开的时长与错峰见 ui/SaoMotion。


    private void renderMainButtons(GuiGraphics g, float globalAlpha) {
        // 堆叠向下展开(参照 SAO-World):打开时所有按钮叠在首按钮位,
        // 随后逐个错峰向下滑到自己的位置,带 easeOutBack 回弹
        int stackY = buttonY(0);
        long unfoldNow = now();
        List<SaoPanel> ps = panels();
        for (int i = 0; i < ps.size(); i++) {
            boolean active = isActive(i);
            int d = MenuLayout.btnSize(this.height);
            int cx = baseAnchorX;
            float p = closing ? 1f
                    : clamp01((unfoldNow - openedAt - i * UNFOLD_STAGGER_MS) / (float) UNFOLD_MS);
            float eased = easeOutBack(p);
            int cy = Math.round(stackY + (buttonY(i) - stackY) * eased);
            // SAO Utils 官方按钮素材:常态白圆,悬停/选中橙圆,按压用 press 帧反馈点触
            // 未点击过任何主按钮前列内全部全亮;点击后其余按钮压到 45% 隐约可见
            boolean dim = mainTouched && !active;
            float a = globalAlpha * (dim ? 0.45f : 1f);
            ResourceLocation btnTex = mainPressing(i) ? TEX_BTN_PRESS
                    : active ? TEX_BTN_HOVER : TEX_BTN_NORMAL;
            shaderAlpha(a);
            RenderSystem.enableBlend();
            g.blit(btnTex, cx - d / 2, cy - d / 2, 0, 0, d, d, d, d);
            shaderAlpha(1f);
            // SAO Utils 官方符号图标(46x46):常态深色版,悬停/选中反白版
            ResourceLocation glyph = tex("symbol_" + ps.get(i).icon()
                    + (active ? "_hover" : "_normal") + ".png");
            int pad = Math.max(2, Math.round(d * 0.22f));
            int isz = d - pad * 2;
            shaderAlpha(a);
            RenderSystem.enableBlend();
            g.blit(glyph, cx - d / 2 + pad, cy - d / 2 + pad, 0, 0, isz, isz, isz, isz);
            shaderAlpha(1f);
        }
    }

    // ---------------------------------------------------------------- 面板(卡片)

    private void renderPanelFor(GuiGraphics g, int main, int mouseX, int mouseY,
                                float globalAlpha, long now) {
        int anchorY = buttonY(main);
        float p = panelAt == Long.MIN_VALUE ? 1f
                : clamp01((now - panelAt) / (float) PANEL_MS);
        float eased = easeOutCubic(p);
        float alpha = globalAlpha * p;

        SaoPanel panel = panelAt(main);
        if (panel == null) {
            return;
        }
        // 面板自带侧卡时走接口(第三方面板扩展点);内置三张卡仍由本类渲染
        if (panel.sideCard() != null) {
            MenuLayout.Rect rect = MenuLayout.cardRectAt(this.width, this.height, baseAnchorX, anchorY);
            int slide = Math.round((1f - eased) * rect.w() * 0.35f);
            panel.sideCard().render(g, mc(), this.font, this,
                    new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h()),
                    eased, alpha, mouseX, mouseY);
            return;
        }

        switch (panel.id()) {
            case SaoPanels.PROFILE -> renderPlayerCard(g, anchorY, mouseX, mouseY, eased, alpha);
            case SaoPanels.PARTY -> renderTeamCard(g, anchorY, eased, alpha);
            case SaoPanels.FRIENDS -> renderFriendsCard(g, anchorY, eased, alpha);
            default -> { /* 设置等面板没有左侧卡 */ }
        }
    }

    private void renderPlayerCard(GuiGraphics g, int anchorY, int mouseX, int mouseY, float eased, float alpha) {
        MenuLayout.Rect rect = MenuLayout.cardRectAt(this.width, this.height, baseAnchorX, anchorY);
        int slide = Math.round((1f - eased) * rect.w() * 0.35f);
        MenuLayout.Rect at = new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h());

        // 主体:属性区按文字行数预留(大卡 8 行含饥饿/护甲,有手持物品时 9 行),剩余全部给 3D 头像
        ItemStack held = mc().player != null ? mc().player.getInventory().getSelected() : ItemStack.EMPTY;
        boolean hasHeld = !held.isEmpty();
        int statLines = at.h() >= 140 ? 8 + (hasHeld ? 1 : 0) : 6;
        int split = Math.max(Math.round(at.h() * 0.40f), at.h() - (statLines * 10 + 6));
        // SAO Utils 官方玩家卡面板贴图(上半白、下半浅灰属性区)
        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, TEX_PANEL, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        // 名字 + 下划线(头部区尽量紧凑,把空间让给剪影)
        Font f = this.font;
        String name = playerName();
        int nameY = at.y() + 3;
        g.drawString(f, name, at.centerX() - f.width(name) / 2, nameY,
                mulAlpha(theme().textOnSurface(), alpha), false);
        int lineY = nameY + 10;
        g.fill(at.x() + at.w() / 10, lineY, at.x() + at.w() - at.w() / 10, lineY + 1, mulAlpha(theme().divider(), alpha));

        // 手持物品图标(卡片左上角,SAO 槽位样式)
        if (hasHeld) {
            int isz = 14;
            int ix = at.x() + 8;
            int iy = at.y() + 5;
            g.fill(ix, iy, ix + isz, iy + isz, mulAlpha(theme().surfaceSlot(), alpha));
            g.fill(ix, iy, ix + isz, iy + 1, mulAlpha(theme().accent(), alpha));
            g.fill(ix, iy + isz - 1, ix + isz, iy + isz, mulAlpha(theme().accent(), alpha));
            g.fill(ix, iy, ix + 1, iy + isz, mulAlpha(theme().accent(), alpha));
            g.fill(ix + isz - 1, iy, ix + isz, iy + isz, mulAlpha(theme().accent(), alpha));
            g.pose().pushPose();
            g.pose().translate(ix + isz / 2f, iy + isz / 2f, 120f);
            g.pose().scale(isz / 16f, isz / 16f, 1f);
            g.renderItem(held, -8, -8);
            g.pose().popPose();
        }

        // 头像区:占满头部线与属性区之间(split 为卡片内相对高度)
        int areaTop = lineY + 2;
        int areaH = Math.max(8, split - (areaTop - at.y()) - 2);
        if (mc().player != null) {
            // 3D 玩家(与原版背包同源渲染),朝向跟随鼠标转动。
            // 原版语义:(x,y)=脚部锚点,size=缩放系数,体高≈1.9*size;裁剪由调用者负责。
            int k = Math.max(6, Math.round(areaH / 1.95f));
            int anchorX = at.centerX();
            int feetY = areaTop + areaH - 2;
            int halfW = Math.round(k * 0.8f);
            // 原版 FollowsMouse 传的是"锚点 - 光标"增量,内部 atan(delta/40)*20° 转向
            float dx = anchorX - mouseX;
            float dy = feetY - mouseY;
            // 剪裁框必须跟随视觉变换(缩放/左移/浮动):菜单整组左移后,
            // 3D 人物画在新位置,旧坐标的剪裁框会把人物整个裁掉
            MenuLayout.Rect clip = localBoxToScreen(anchorX - halfW, areaTop, halfW * 2, areaH);
            g.enableScissor(clip.x(), clip.y(), clip.x() + clip.w(), clip.y() + clip.h());
            shaderAlpha(alpha);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, anchorX, feetY, k, dx, dy, mc().player);
            shaderAlpha(1f);
            g.disableScissor();
        } else {
            // 无玩家(标题界面等)回退全身剪影
            int sh = areaH;
            int sw = Math.max(6, Math.round(sh * (64f / 96f)));
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(TEX_SILHOUETTE, at.centerX() - sw / 2, areaTop, 0, 0, sw, sh, sw, sh);
            shaderAlpha(1f);
        }

        // 属性
        Player p = mc().player;
        if (p != null) {
            int statsTop = at.y() + split;
            int lineStep = 10;
            List<String> stats = new ArrayList<>();
            if (hasHeld) {
                stats.add(tr("saomenu.stat.held", held.getHoverName().getString()));
            }
            stats.add(tr("saomenu.stat.level", p.experienceLevel));
            stats.add(tr("saomenu.stat.experience", Math.round(p.experienceProgress * 100.0f)));
            stats.add(tr("saomenu.stat.health", trim(p.getHealth()), trim(p.getMaxHealth())));
            if (statLines >= 8) {
                stats.add(tr("saomenu.stat.hunger", p.getFoodData().getFoodLevel()));
                stats.add(tr("saomenu.stat.armor", p.getArmorValue()));
            }
            stats.add(tr("saomenu.stat.strength", trim((float) p.getAttributeValue(Attributes.ATTACK_DAMAGE))));
            stats.add(tr("saomenu.stat.agility", trim((float) p.getAttributeValue(Attributes.MOVEMENT_SPEED))));
            stats.add(tr("saomenu.stat.resistance", trim((float) p.getAttributeValue(Attributes.ARMOR))));
            for (int i = 0; i < stats.size(); i++) {
                g.drawString(f, stats.get(i), at.x() + 8, statsTop + 4 + i * lineStep,
                        mulAlpha(theme().textOnSurface(), alpha), false);
            }
        }

        // ▶ 指向按钮
        renderArrowRight(g, at, anchorY, alpha);
    }

    /** 队伍面板:scoreboard 队伍名 + 成员列表;无队伍时显示提示。 */
    private void renderTeamCard(GuiGraphics g, int anchorY, float eased, float alpha) {
        List<String> rows = new ArrayList<>();
        String title = tr("saomenu.party");
        String subtitle = null;
        String footer = tr("saomenu.panel.team_members", 0);
        Player p = mc().player;
        if (p != null && mc().level != null) {
            PlayerTeam team = mc().level.getScoreboard().getPlayersTeam(p.getGameProfile().getName());
            if (team != null) {
                title = team.getDisplayName().getString();
                List<String> members = new ArrayList<>(team.getPlayers());
                members.sort(String::compareToIgnoreCase);
                rows.addAll(members);
                footer = tr("saomenu.panel.team_members", members.size());
            } else {
                subtitle = tr("saomenu.panel.no_team");
            }
        }
        renderListCard(g, title, subtitle, rows, footer, anchorY, eased, alpha);
    }

    /** 好友面板:Tab 在线玩家列表。 */
    private void renderFriendsCard(GuiGraphics g, int anchorY, float eased, float alpha) {
        List<String> rows = new ArrayList<>();
        int online = 0;
        if (mc().getConnection() != null) {
            List<String> names = new ArrayList<>();
            for (PlayerInfo info : mc().getConnection().getOnlinePlayers()) {
                names.add(info.getProfile().getName());
            }
            names.sort(String::compareToIgnoreCase);
            online = names.size();
            rows.addAll(names);
        }
        renderListCard(g, tr("saomenu.friends"), null, rows,
                tr("saomenu.panel.online", online), anchorY, eased, alpha);
    }

    /** 通用列表卡:标题/副标题 + 最多 maxRows 行 + 底部统计。 */
    private void renderListCard(GuiGraphics g, String title, String subtitle,
                                List<String> rows, String footer, int anchorY, float eased, float alpha) {
        MenuLayout.Rect rect = MenuLayout.cardRectAt(this.width, this.height, baseAnchorX, anchorY);
        int slide = Math.round((1f - eased) * rect.w() * 0.35f);
        MenuLayout.Rect at = new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h());

        shaderAlpha(alpha);
        SaoDraw.blendedBlit(g, TEX_PANEL, at.x(), at.y(), at.w(), at.h());
        shaderAlpha(1f);

        Font f = this.font;
        g.drawString(f, title, at.centerX() - f.width(title) / 2,
                at.y() + 5, mulAlpha(theme().textOnSurface(), alpha), false);
        int lineY;
        if (subtitle != null && !subtitle.isEmpty()) {
            g.drawString(f, subtitle, at.centerX() - f.width(subtitle) / 2,
                    at.y() + 17, mulAlpha(theme().textOnSurface(), alpha), false);
            lineY = at.y() + 30;
        } else {
            lineY = at.y() + 18;
        }
        g.fill(at.x() + at.w() / 10, lineY, at.x() + at.w() - at.w() / 10, lineY + 1, mulAlpha(theme().divider(), alpha));

        // 行数按卡片实际高度自适应,超出部分折叠为 "+N 更多"
        int maxRows = Mth.clamp((at.h() - 56) / 12, 1, 8);
        if (rows.size() > maxRows) {
            int extra = rows.size() - maxRows + 1;
            List<String> shown = new ArrayList<>(rows.subList(0, Math.max(0, maxRows - 1)));
            shown.add(tr("saomenu.panel.more", extra));
            rows = shown;
        }
        int rowY = lineY + 6;
        for (int i = 0; i < rows.size(); i++) {
            g.drawString(f, rows.get(i), at.x() + 12, rowY + i * 12,
                    mulAlpha(theme().textOnSurface(), alpha), false);
        }
        if (rows.isEmpty() && (subtitle == null || subtitle.isEmpty())) {
            g.drawString(f, tr("saomenu.panel.no_players"), at.x() + 12, rowY,
                    mulAlpha(theme().textOnSurface(), alpha), false);
        }

        g.drawString(f, footer, at.x() + at.w() - 12 - f.width(footer),
                at.y() + at.h() - 13, mulAlpha(theme().textOnSurface(), alpha), false);
        renderArrowRight(g, at, anchorY, alpha);
    }

    /** 卡片右侧指向按钮的 ▶,横跨两者之间的间隙。 */
    private void renderArrowRight(GuiGraphics g, MenuLayout.Rect card, int anchorY, float alpha) {
        int btnLeft = baseAnchorX - MenuLayout.btnSize(this.height) / 2;
        int x0 = card.x() + card.w() + 1;
        int w = btnLeft - x0 - 1;
        if (w < 3) {
            return;
        }
        int h = Math.max(6, Math.round(w * (19f / 24f)));
        shaderAlpha(alpha);
        RenderSystem.enableBlend();
        g.blit(TEX_ARROW_RIGHT, x0, anchorY - h / 2, 0, 0, w, h, w, h);
        shaderAlpha(1f);
    }

    // ---------------------------------------------------------------- 菜单项

    private void renderMenuItems(GuiGraphics g, int main, int mouseX, int mouseY,
                                 float globalAlpha, long now) {
        List<MenuEntry> items = activeItems(main);
        int anchorY = buttonY(main);
        long base = panelAt == Long.MIN_VALUE ? now - PANEL_MS : panelAt;

        // SAO Utils 官方指示器:列左缘长箭头,中段菱形对准活动行
        renderIndicator(g, items.size(), anchorY, baseAnchorX, globalAlpha, true);

        for (int i = 0; i < items.size(); i++) {
            float p = clamp01((now - base - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
            if (p <= 0f) {
                continue;
            }
            float eased = easeOutCubic(p);
            MenuLayout.Rect rect = MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, i);
            int slide = Math.round((1f - eased) * rect.w() * 0.45f);
            MenuLayout.Rect at = new MenuLayout.Rect(rect.x() - slide, rect.y(), rect.w(), rect.h());
            // 与主按钮同一规则:本列已点击过(expandedItem 生效)后,非当前项压暗
            boolean dim = expandedItem != -1 && expandedItem != i;
            renderMenuItem(g, at, items.get(i).label(), items.get(i).icon(),
                    hoverItem == i, false, globalAlpha * eased * (dim ? 0.45f : 1f),
                    itemPressing(0, i), items.get(i).stack());
        }

        int shown = visibleChildrenItem(items);
        if (shown >= 0 && items.get(shown).children() != null) {
            if (childOwner != shown) {
                childOwner = shown;
                childAt = now;
            }
            List<MenuEntry> children = windowedChildren(items.get(shown).children());
            int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, shown).centerY();
            // 二级列不再画指示器(其定位公式落在一级列位置,与原指示器重叠成双线);
            // 只保留主按钮旁那条原始指示器
            // 二级列同样:点选某个子项(equipOwner)后,其余子项压暗
            boolean childDim = equipOwner != -1;
            for (int i = 0; i < children.size(); i++) {
                float p = clamp01((now - childAt - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
                if (p <= 0f) {
                    continue;
                }
                float eased = easeOutCubic(p);
                MenuLayout.Rect rect = MenuLayout.childItemRectAt(this.width, this.height, children.size(), baseAnchorX, childAnchor, i);
                int slide = Math.round((1f - eased) * rect.w() * 0.45f);
                MenuLayout.Rect at = new MenuLayout.Rect(rect.x() - slide, rect.y(), rect.w(), rect.h());
                boolean dim = childDim && equipOwner != i;
                renderMenuItem(g, at, children.get(i).label(), children.get(i).icon(),
                        hoverChild == i || (actionMenuOpen && actionRow == i), true,
                        globalAlpha * eased * (dim ? 0.45f : 1f),
                        itemPressing(1, i), children.get(i).stack());
            }
            // 物品操作按钮:缩小后水平排布在选中行上(级联弹出 + 悬停标签)
            if (actionMenuOpen && actionRow >= 0 && actionRow < children.size()
                    && children.get(actionRow).stack() != null) {
                MenuLayout.Rect rowA = MenuLayout.childItemRectAt(this.width, this.height,
                        children.size(), baseAnchorX, childAnchor, actionRow);
                // 物品图标是延迟合批的,先刷掉;物品渲染还会往深度缓冲写 z=150 的深度,
                // 之后 blit 继承"深度测试开启"状态会被图标深度挡住(表现为图标盖在按钮上),
                // 所以 flush 后必须关掉深度测试
                g.flush();
                RenderSystem.disableDepthTest();
                long age = now - actionAt;
                for (int b = 0; b < 3; b++) {
                    float bp = clamp01((age - b * 45) / 150f);
                    float bs = 0.2f + 0.8f * easeOutBack(bp);
                    MenuLayout.Rect full = actionButtonRect(rowA, b);
                    int ds = Math.max(2, Math.round(full.w() * bs));
                    boolean hv = hoverAction == b;
                    ResourceLocation t = b == 0 ? (hv ? TEX_ACT_EQUIP_H : TEX_ACT_EQUIP)
                            : b == 1 ? (hv ? TEX_ACT_INFO_H : TEX_ACT_INFO)
                            : (hv ? TEX_ACT_DROP_H : TEX_ACT_DROP);
                    shaderAlpha(globalAlpha);
                    RenderSystem.enableBlend();
                    g.blit(t, full.centerX() - ds / 2, full.centerY() - ds / 2, 0, 0, ds, ds, ds, ds);
                    shaderAlpha(1f);
                    if (hv) {
                        String lbl = SaoText.tr(ACT_KEYS[b]);
                        g.drawString(this.font, lbl, full.centerX() - this.font.width(lbl) / 2,
                                full.y() - 11, mulAlpha(theme().highlight(), globalAlpha), true);
                    }
                }
            }
            // 装备第三列:参考 SAO-World,武器/护甲/首饰展开后右侧直接列出已装备物品
            int equipTarget = equipTargetIndex(items, shown);
            if (equipTarget >= 0) {
                if (equipTarget != equipShownOwner) {
                    equipShownOwner = equipTarget;
                    equipAt = now;
                }
                renderEquipColumn(g, equipKindAt(items, shown, equipTarget),
                        equipAnchorY(items, shown, equipTarget), globalAlpha, now);
            } else {
                equipShownOwner = -1;
            }
        } else {
            if (childOwner != -1) {
                childOwner = -1;
            }
            equipShownOwner = -1;
        }
    }

    /** 交给条目自己的处理器;没有处理器时只发一声点击(与原 Action.NONE 行为一致)。 */
    private void activate(MenuEntry entry) {
        if (entry.onActivate() == null) {
            playClick();
            return;
        }
        entry.onActivate().accept(MenuContext.of(this, entry));
    }

    /** 装备条目列(第三列):每个已装备物品一行,白底条目 + 物品图标 + 名称。 */
    private void renderEquipColumn(GuiGraphics g, MenuEntry.EquipKind kind, int anchorY, float globalAlpha, long now) {
        List<EquipEntry> entries = equipEntries(kind);
        int count = entries.size();
        for (int i = 0; i < count; i++) {
            float p = clamp01((now - equipAt - i * ITEM_STAGGER_MS) / (float) ITEM_MS);
            if (p <= 0f) {
                continue;
            }
            float eased = easeOutCubic(p);
            MenuLayout.Rect rect = MenuLayout.equipItemRectAt(this.width, this.height, count, baseAnchorX, anchorY, i);
            int slide = Math.round((1f - eased) * rect.w() * 0.45f);
            MenuLayout.Rect at = new MenuLayout.Rect(rect.x() + slide, rect.y(), rect.w(), rect.h());
            renderEquipItem(g, at, entries.get(i), hoverEquip == i, globalAlpha * eased);
        }
    }

    /** 单个装备条目:SAO Utils 条目贴图 + 物品图标(3D)+ 名称;空条目为灰色占位。 */
    private void renderEquipItem(GuiGraphics g, MenuLayout.Rect at, EquipEntry e, boolean hovered, float alpha) {
        SaoDraw.roundedRect(g, at.x() + 2, at.y() + 2, at.w(), at.h(),
                Math.max(2, Math.round(at.h() * 0.12f)), mulAlpha(theme().shadow(), alpha));
        if (hovered) {
            SaoDraw.tint(theme().accent(), alpha);
            SaoDraw.blendedBlit(g, TEX_LIST_HOVER, at.x(), at.y(), at.w(), at.h());
        } else {
            RenderSystem.enableBlend();
            shaderAlpha(alpha * 0.92f);
            g.blit(TEX_LIST_NORMAL, at.x(), at.y(), 0, 0, at.w(), at.h(), at.w(), at.h());
        }
        shaderAlpha(1f);

        int iconSize = Math.round(at.h() * 0.78f);
        int iconX = at.x() + Math.round(at.h() * 0.18f);
        int iconY = at.y() + (at.h() - iconSize) / 2;
        if (!e.empty()) {
            g.pose().pushPose();
            g.pose().translate(iconX + iconSize / 2f, iconY + iconSize / 2f, 120f);
            g.pose().scale(iconSize / 16f, iconSize / 16f, 1f);
            g.renderItem(e.stack(), -8, -8);
            g.pose().popPose();
        }

        Font f = this.font;
        String label = e.empty() ? tr("saomenu.equip.empty") : e.stack().getHoverName().getString();
        int textX = iconX + iconSize + Math.round(at.h() * 0.18f);
        int maxW = at.x() + at.w() - textX - 6;
        int textY = at.y() + (at.h() - f.lineHeight) / 2;
        int color = e.empty() ? mulAlpha(theme().textMuted(), alpha)
                : hovered ? mulAlpha(theme().textOnAccent(), alpha) : mulAlpha(theme().textOnSurface(), alpha);
        drawScrollingLabel(g, f, label, textX, textY, maxW, color, hovered);
    }

    /**
     * 菜单列指示器(SAO Utils 官方素材):列左缘双头长箭头,中段菱形对准活动行;
     * 一级列在按钮边缘保留接头小环。
     */
    private void renderIndicator(GuiGraphics g, int count, int anchorY, int anchorX, float alpha, boolean mainColumn) {
        int itemH = MenuLayout.itemH(this.height);
        int step = itemH + MenuLayout.itemGap(this.height);
        int totalH = (count - 1) * step + itemH;
        int top = MenuLayout.clampedAnchorY(this.height, count, anchorY) - totalH / 2;
        int colX = MenuLayout.itemColumnXAt(anchorX, this.height);
        int indH = totalH + Math.max(8, itemH * 2);
        int indW = Math.max(6, Math.round(indH * 28f / 230f));
        int x = colX - MenuLayout.arrowGap(this.height) / 2 - indW / 2 - 1;
        int y = top - (indH - totalH) / 2;
        shaderAlpha(alpha);
        RenderSystem.enableBlend();
        g.blit(TEX_INDICATOR, x, y, 0, 0, indW, indH, indW, indH);
        shaderAlpha(1f);
        if (mainColumn) {
            int btnRight = anchorX + MenuLayout.btnSize(this.height) / 2;
            int ringD = Math.max(4, Math.round(itemH * 0.30f));
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(TEX_RING, btnRight + 1, anchorY - ringD / 2, 0, 0, ringD, ringD, ringD, ringD);
            shaderAlpha(1f);
        }
    }

    /** 单个菜单项:SAO Utils 官方条目素材(白/橙圆角条)+ 阴影 + 图标 + 文字;press 为按压帧。 */
    private void renderMenuItem(GuiGraphics g, MenuLayout.Rect at, String labelKey, String icon,
                                boolean hovered, boolean child, float alpha, boolean pressed, ItemStack stack) {
        int r = Math.max(2, Math.round(at.h() * 0.12f));
        SaoDraw.roundedRect(g, at.x() + 2, at.y() + 2, at.w(), at.h(), r, mulAlpha(theme().shadow(), alpha));
        if (pressed) {
            SaoDraw.blendedBlit(g, TEX_LIST_PRESS, at.x(), at.y(), at.w(), at.h());
        } else if (hovered) {
            SaoDraw.tint(theme().accent(), alpha);
            SaoDraw.blendedBlit(g, TEX_LIST_HOVER, at.x(), at.y(), at.w(), at.h());
        } else {
            RenderSystem.enableBlend();
            shaderAlpha(alpha * 0.9f);
            g.blit(TEX_LIST_NORMAL, at.x(), at.y(), 0, 0, at.w(), at.h(), at.w(), at.h());
        }
        shaderAlpha(1f);

        // 图标:物品条目画 3D 物品,普通条目画贴图符号
        // 弹窗打开时跳过图标渲染——图标是延迟合批的,某些优化 mod 会推迟到帧末刷新,
        // 弹窗盖不住它们;而弹窗本来就遮住这些行,图标不渲染也无视觉损失
        int iconSize = Math.round(at.h() * 0.72f);
        int iconY = at.y() + (at.h() - iconSize) / 2;
        int iconX = at.x() + Math.round(at.h() * 0.18f);
        if (stack != null && !stack.isEmpty()) {
            if (!infoOpen && !confirmClose) {
                g.pose().pushPose();
                g.pose().translate(iconX + iconSize / 2f, iconY + iconSize / 2f, 120f);
                g.pose().scale(iconSize / 16f, iconSize / 16f, 1f);
                g.renderItem(stack, -8, -8);
                g.pose().popPose();
            }
        } else {
            shaderAlpha(alpha);
            RenderSystem.enableBlend();
            g.blit(tex(icon + ".png"), iconX, iconY,
                    0, 0, iconSize, iconSize, iconSize, iconSize);
            shaderAlpha(1f);
        }

        // 置顶标识:左上角主题色小三角 + 白色高光,一眼分辨哪些被置顶
        if (stack != null && !stack.isEmpty() && SaoPanels.pinOrderOf(stack) != Integer.MAX_VALUE) {
            int t = Math.max(3, Math.round(at.h() * 0.30f));
            g.pose().pushPose();
            g.pose().translate(0, 0, 260f);
            // 阶梯直角三角:逐行递减宽度,贴条目左上角
            for (int row = 0; row < t; row++) {
                int wRow = t - row;
                g.fill(at.x() + 1, at.y() + 1 + row, at.x() + 1 + wRow, at.y() + 2 + row,
                        mulAlpha(theme().accent(), alpha));
            }
            // 斜边高光,深色条目上也看得清
            for (int row = 0; row < t; row++) {
                int xr = at.x() + (t - row);
                g.fill(xr, at.y() + 1 + row, xr + 1, at.y() + 2 + row, mulAlpha(theme().highlight(), alpha));
            }
            g.pose().popPose();
        }

        // 文字(SAOUI 字体;动态 label(在线玩家名/语言键)二选一)
        Font f = this.font;
        String label = resolveLabel(labelKey);
        int textX = at.x() + Math.round(at.h() * 0.18f) + iconSize + Math.round(at.h() * 0.22f);
        int textY = at.y() + (at.h() - f.lineHeight) / 2;
        int maxW = at.x() + at.w() - textX - Math.round(at.h() * 0.16f);
        int color = hovered ? mulAlpha(theme().textOnAccent(), alpha) : mulAlpha(theme().textOnSurface(), alpha);
        drawScrollingLabel(g, f, label, textX, textY, maxW, color, hovered);
    }

    /**
     * 条目文字:放得下就照常画;放不下时悬停行做跑马灯滚动,非悬停行截断加省略号。
     *
     * <p>滚动用剪裁窗口(enableScissor 的坐标是<b>物理像素</b>,须按 GUI 缩放换算),
     * 平移量走 {@link SAOScrollText} 的纯函数,与渲染解耦、可单测。</p>
     */
    private void drawScrollingLabel(GuiGraphics g, Font f, String label,
                                    int x, int y, int maxW, int color, boolean hovered) {
        int textW = f.width(label);
        if (maxW <= 0) {
            return;
        }
        if (textW <= maxW) {
            g.drawString(f, label, x, y, color, false);
            return;
        }
        if (!hovered) {
            String cut = f.plainSubstrByWidth(label, Math.max(0, maxW - f.width("…"))) + "…";
            g.drawString(f, cut, x, y, color, false);
            return;
        }
        // 逐字形滑动窗口:从 shift 像素处开始截 maxW 宽的一段。
        // 不用 enableScissor——菜单整体有浮动/缩放 pose 变换,scissor 是
        // 屏幕空间矩形,不跟随 pose,会把文字裁错位置
        int shift = SAOScrollText.offset(textW, maxW, now(), label.hashCode());
        String visible = SAOScrollText.window(label, f::width, shift, maxW);
        if (!visible.isEmpty()) {
            g.drawString(f, visible, x, y, color, false);
        }
    }

    /** 圆角矩形填充:见 {@link SaoDraw#roundedRect}。 */

    // ---------------------------------------------------------------- 输入

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        // 悬停命中由 render() 每帧统一处理(事件与渲染双路径会造成状态抖动);
        // 这里只保留地图/时钟拖动的平滑跟随
        if (!closing) {
            if (SAOMapPanel.isShown()) {
                SAOMapPanel.dragTo(this.width, this.height, (int) mouseX, (int) mouseY);
            }
            SAOClockPanel.dragTo(this.width, this.height, (int) mouseX, (int) mouseY);
            SAOHud.dragPlateTo(mc(), this.width, this.height, (int) mouseX, (int) mouseY);
            SAOHud.dragFoodTo(this.width, this.height, (int) mouseX, (int) mouseY);
        }
        if (pinDragFrom >= 0) {
            pinDragMx = (int) mouseX;
            pinDragMy = (int) mouseY;
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 1 && pinDragFrom >= 0) {
            applyPinDrag(pinDragFrom, pinRowAt((int) mouseX, (int) mouseY));
            pinDragFrom = -1;
            pinDragStack = null;
            return true;
        }
        SAOMapPanel.endDragAndSave();
        SAOClockPanel.endDragAndSave();
        SAOHud.endPlateDragAndSave();
        SAOHud.endFoodDragAndSave();
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) {
            return false;
        }
        // 右键:Shift+右键 = 置顶/取消置顶;单纯右键 = 按住拖动换序
        if (button == 1) {
            int row = pinRowAt((int) mouseX, (int) mouseY);
            if (row >= 0) {
                if (hasShiftDown()) {
                    togglePinAt(row);
                } else {
                    pinDragFrom = row;
                    pinDragStack = stackAtRow(row);
                    pinDragMx = (int) mouseX;
                    pinDragMy = (int) mouseY;
                    pinDragAt = now();
                    playPanel();
                }
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;

        // 时钟面板拖动(优先于地图面板,体积更小)
        if (SAOClockPanel.hitCard(this.width, this.height, mx, my)) {
            SAOClockPanel.beginDrag(this.width, this.height, mx, my);
            return true;
        }

        // 血条板拖动:整组(板+队友血条+状态行)按住即可挪到任意位置,
        // 落点存配置;与地图/时钟不重叠时互不干扰
        if (SAOConfig.showHud() && SAOHud.hitPlateGroup(mc(), this.width, this.height, mx, my)) {
            SAOHud.beginPlateDrag(mc(), this.width, this.height, mx, my);
            return true;
        }

        // 饥饿条拖动(隐藏原版血条时才有这条自绘饥饿条)
        if (SAOHud.hitFoodBar(this.width, this.height, mx, my)) {
            SAOHud.beginFoodDrag(this.width, this.height, mx, my);
            return true;
        }

        // 地图面板优先:图钉 → 面板内(开始拖动并吞掉点击);拖动结束才轮到菜单
        if (SAOMapPanel.isShown()) {
            if (SAOMapPanel.hitPin(this.width, this.height, mx, my)) {
                SAOMapPanel.togglePin();
                playClick();
                return true;
            }
            if (SAOMapPanel.hitCard(this.width, this.height, mx, my)) {
                SAOMapPanel.beginDrag(this.width, this.height, mx, my);
                return true;
            }
        }

        // 确认弹窗打开时:蓝钮=确认关闭,红钮/弹窗外=取消
        if (confirmClose) {
            MenuLayout.Rect at = dialogRect();
            int d = 26;
            int by = at.y() + Math.round(at.h() * 0.80f) - d / 2;
            int b1x = at.x() + at.w() / 4 - d / 2;
            int b2x = at.x() + at.w() * 3 / 4 - d / 2;
            if (MenuLayout.inCircle(b1x + d / 2, by + d / 2, d / 2 + 2, mx, my)) {
                playClick();
                beginClose();
                return true;
            }
            if (MenuLayout.inCircle(b2x + d / 2, by + d / 2, d / 2 + 2, mx, my)) {
                playPanel();
                confirmClose = false;
                return true;
            }
            if (mx < at.x() || mx >= at.x() + at.w() || my < at.y() || my >= at.y() + at.h()) {
                playPanel();
                confirmClose = false;
                return true;
            }
            return true;
        }

        // 信息弹窗打开:任意点击(圆钮/面板/外部)都只关闭弹窗,菜单保持打开
        if (infoOpen) {
            infoOpen = false;
            playPanel();
            return true;
        }

        computeLocal(mx, my);
        int lx = Math.round(localPtX);
        int ly = Math.round(localPtY);

        // 物品操作按钮(装备/信息/丢弃):优先于其他命中。
        // 几何必须与渲染完全一致:行矩形按窗口化 children 的长度布局
        if (actionMenuOpen) {
            int mainA = activeMain();
            List<MenuEntry> itemsA = mainA >= 0 ? activeItems(mainA) : null;
            int shownA = itemsA != null ? visibleChildrenItem(itemsA) : -1;
            if (itemsA != null && shownA >= 0 && itemsA.get(shownA).children() != null) {
                List<MenuEntry> winA = windowedChildren(itemsA.get(shownA).children());
                int anchorA = MenuLayout.menuItemRectAt(this.width, this.height, itemsA.size(),
                        baseAnchorX, buttonY(mainA), shownA).centerY();
                if (actionRow >= 0 && actionRow < winA.size() && winA.get(actionRow).stack() != null) {
                    MenuLayout.Rect rowA = MenuLayout.childItemRectAt(this.width, this.height,
                            winA.size(), baseAnchorX, anchorA, actionRow);
                    for (int b = 0; b < 3; b++) {
                        MenuLayout.Rect br = actionButtonRect(rowA, b);
                        if (MenuLayout.inCircle(br.centerX(), br.centerY(), br.w() / 2 + 2, lx, ly)) {
                            List<MenuEntry> all = itemsA.get(shownA).children();
                            int real = childScroll + actionRow;
                            if (real < all.size() && all.get(real).stack() != null) {
                                executeItemAction(b, all.get(real));
                            } else {
                                actionMenuOpen = false;
                            }
                            return true;
                        }
                    }
                }
            }
        }

        int hitMain = MenuLayout.hoveredMainButtonAt(this.width, this.height, baseAnchorX, baseAnchorY,
                panels().size(), lx, ly);
        if (hitMain != -1) {
            mainTouched = true;
            if (selectedMain == hitMain) {
                // 再点已选中的按钮:收起面板回到初始按钮列(参照 SAO-World)
                selectedMain = -1;
                panelOwner = -1;
                expandedItem = -1;
                equipOwner = -1;
                actionMenuOpen = false;
                infoOpen = false;
                childScroll = 0;
                playPanel();
            } else {
                selectedMain = hitMain;
                expandedItem = -1;
                equipOwner = -1;
                actionMenuOpen = false;
                infoOpen = false;
                childScroll = 0;
                playClick();
            }
            mainPressIndex = hitMain;
            mainPressAt = now();
            return true;
        }

        // 圆点 = 物品栏槽位(第 1 个为副手指示,仅吞掉点击;其后 9 个切换选中槽位)
        // 仅当原版快捷栏被隐藏、圆点可见时响应
        if (SAOConfig.hideHotbar()) {
            for (int i = 0; i < MenuLayout.DOT_COUNT; i++) {
                if (MenuLayout.inDot(this.width, this.height, i, mx, my)) {
                    Player p = mc().player;
                    if (p != null && i > 0 && p.getInventory().selected != i - 1) {
                        p.getInventory().selected = i - 1;
                        playClick();
                    }
                    return true;
                }
            }
        }

        int main = activeMain();
        if (main >= 0) {
            List<MenuEntry> items = activeItems(main);
            int anchorY = buttonY(main);
            for (int i = 0; i < items.size(); i++) {
                if (MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, i).contains(lx, ly)) {
                    itemPressColumn = 0;
                    itemPressIndex = i;
                    itemPressAt = now();
                    if (items.get(i).hasChildren()) {
                        // 展开二级列表(动态子列由面板自己的 supplier 提供)
                        int newExpanded = expandedItem == i ? -1 : i;
                        if (newExpanded != expandedItem) {
                            // 切换展开项必须清掉上一列的选中状态:残留的 equipOwner
                            // 会让新列表按旧下标压暗(表现为只有一行亮、其余全透明)
                            equipOwner = -1;
                            childScroll = 0;
                            actionMenuOpen = false;
                        }
                        expandedItem = newExpanded;
                        playPanel();
                    } else {
                        activate(items.get(i));
                    }
                    return true;
                }
            }
            int shown = visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> children = windowedChildren(items.get(shown).children());
                int childAnchor = MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, anchorY, shown).centerY();
                // 装备条目列(第三列):只读展示,点击不落穿关闭菜单
                int equipTarget = equipTargetIndex(items, shown);
                if (equipTarget >= 0) {
                    int equipAnchor = equipAnchorY(items, shown, equipTarget);
                    List<EquipEntry> entries = equipEntries(equipKindAt(items, shown, equipTarget));
                    for (int i = 0; i < entries.size(); i++) {
                        if (MenuLayout.equipItemRectAt(this.width, this.height, entries.size(), baseAnchorX, equipAnchor, i)
                                .contains(lx, ly)) {
                            playClick();
                            return true;
                        }
                    }
                }
                for (int i = 0; i < children.size(); i++) {
                    if (MenuLayout.childItemRectAt(this.width, this.height, children.size(), baseAnchorX, childAnchor, i).contains(lx, ly)) {
                        itemPressColumn = 1;
                        itemPressIndex = i;
                        itemPressAt = now();
                        MenuEntry child = children.get(i);
                        if (child.equip() != null) {
                            // 武器/护甲/首饰:展开/切换第三列已装备列表,不再打开物品栏
                            actionMenuOpen = false;
                            if (equipOwner != i) {
                                equipOwner = i;
                                equipAt = now();
                                playPanel();
                            }
                        } else if (child.isItem()) {
                            // 物品条目:行右侧弹出 装备/信息/丢弃 三按钮(再点同行收起)
                            if (actionMenuOpen && actionRow == i) {
                                actionMenuOpen = false;
                                playPanel();
                            } else {
                                actionMenuOpen = true;
                                actionRow = i;
                                actionAt = now();
                                playPanel();
                            }
                        } else {
                            activate(child);
                        }
                        return true;
                    }
                }
            }
        }

        if (actionMenuOpen) {
            // 操作按钮打开时空点:只收按钮,不关菜单
            actionMenuOpen = false;
            playPanel();
            return true;
        }
        beginClose();
        return true;
    }

    /** 打开登出确认弹窗(参照 SAO_Utils:点击 Logout 弹 Alert)。 */
    private void openConfirm() {
        confirmClose = true;
        confirmAt = now();
        releaseMoveKeys();
        playAlert();
    }

    @Override
    public void onClose() {
        beginClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // 信息弹窗开着:任意关闭键先收弹窗
        if (infoOpen && (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_O)) {
            infoOpen = false;
            return true;
        }
        // 移动键穿透:SAO 式非阻塞菜单,开着也能走/跳/潜行/疾跑
        // (确认弹窗/信息弹窗开着时不穿透,防止误操作)
        if (!closing && !confirmClose && !infoOpen) {
            for (KeyMapping km : moveKeys()) {
                if (km.matches(keyCode, scanCode)) {
                    km.setDown(true);
                    return true;
                }
            }
        }
        // F5 切换视角(原版机制:Screen 打开时 keybind 不生效,需在此自行处理),
        // 便于切第三人称查看角色面前的世界空间菜单板
        if (keyCode == GLFW.GLFW_KEY_F5 && mc().player != null) {
            var opt = mc().options;
            switch (opt.getCameraType()) {
                case FIRST_PERSON -> opt.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                case THIRD_PERSON_BACK -> opt.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                default -> opt.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            return true;
        }
        // 原版机制:屏幕打开时 KeyMapping 不会触发 click,必须在这里直接处理 O 键
        if (keyCode == GLFW.GLFW_KEY_O) {
            if (confirmClose) {
                confirmClose = false;
                return true;
            }
            beginClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        // 移动键松开同步(菜单开着走动时松 W/空格等要停)
        for (KeyMapping km : moveKeys()) {
            if (km.matches(keyCode, scanCode)) {
                km.setDown(false);
                return true;
            }
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /** 允许在菜单打开时使用的移动键(前后左右/跳跃/潜行/疾跑)。供 Mixin 输入接管读取。 */
    public KeyMapping[] moveKeys() {
        var o = mc().options;
        return new KeyMapping[]{o.keyUp, o.keyDown, o.keyLeft, o.keyRight,
                o.keyJump, o.keyShift, o.keySprint};
    }

    /** 移动是否被暂时封锁(确认弹窗/信息弹窗/关闭动画)。供 Mixin 输入接管读取。 */
    public boolean isMovementBlocked() {
        return closing || confirmClose || infoOpen;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        // 滚轮不再切换主按钮(一级菜单固定);悬停在二级物品条目列上时滚动窗口
        if (!closing && !confirmClose && delta != 0) {
            int main = activeMain();
            List<MenuEntry> items = main >= 0 ? activeItems(main) : null;
            int shown = -1;
            if (items != null) {
                shown = visibleChildrenItem(items);
            }
            if (items != null && shown >= 0 && items.get(shown).children() != null
                    && !items.get(shown).children().isEmpty()
                    && items.get(shown).children().get(0).stack() != null) {
                // 物品条目列:命中任一可见行(或其附近)即滚动
                List<MenuEntry> children = items.get(shown).children();
                int rows = childVisibleRows();
                computeLocal(mouseX, mouseY);
                int lx = Math.round(localPtX);
                int ly = Math.round(localPtY);
                int anchorY = MenuLayout.menuItemRectAt(this.width, this.height, items.size(), baseAnchorX, buttonY(main), shown).centerY();
                boolean over = false;
                for (int v = 0; v < Math.min(rows, children.size()); v++) {
                    if (MenuLayout.childItemRectAt(this.width, this.height, rows, baseAnchorX, anchorY, v).contains(lx, ly)) {
                        over = true;
                        break;
                    }
                }
                if (over) {
                    // clamp 上界必须 >= 0:条目不足一屏时 max 为负,
                    // 直接钳会得到负 childScroll,后续下标运算越界崩溃
                    int max = Math.max(0, children.size() - rows);
                    int before = childScroll;
                    childScroll = Mth.clamp(childScroll - (int) Math.signum(delta), 0, max);
                    if (childScroll != before) {
                        actionMenuOpen = false;
                        playClick();
                    }
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    private void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        closedAt = now();
        actionMenuOpen = false;
        infoOpen = false;
        releaseMoveKeys();
        // 地图未固定时随菜单一起收回;固定(图钉)则保留为 HUD 常显
        if (!SAOMapPanel.isPinned()) {
            if (SAOMapPanel.isShown()) {
                SAOMapPanel.toggle();
            }
        }
        playAlert();
    }

    // ---------------------------------------------------------------- 工具


    private String playerName() {
        Player p = mc().player;
        return p != null ? p.getGameProfile().getName() : "Player";
    }


    private static String trim(float v) {
        float r = Math.round(v * 10f) / 10f;
        return (r == Math.rint(r)) ? String.valueOf((int) r) : String.valueOf(r);
    }



    // ---------------------------------------------------------------- MenuHost

    @Override
    public Screen screen() {
        return this;
    }

    @Override
    public void selectMain(int index) {
        selectedMain = index;
        mainTouched = true;
        expandedItem = -1;
        equipOwner = -1;
        actionMenuOpen = false;
        childScroll = 0;
    }

    @Override
    public void expandItem(int index) {
        expandedItem = index;
        equipOwner = -1;
        childScroll = 0;
        actionMenuOpen = false;
    }

    @Override
    public void showEquipColumn(int childIndex) {
        equipOwner = childIndex;
        equipAt = now();
    }

    @Override
    public void openCloseConfirm() {
        openConfirm();
    }

    @Override
    public void toggleMap() {
        SAOMapPanel.toggle();
    }

    private void playLauncher() {
        if (!SAOConfig.sounds()) {
            return;
        }
        mc().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.launcherSound(), 1.0F));
    }

    @Override
    public void playClick() {
        if (!SAOConfig.sounds()) {
            return;
        }
        mc().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.clickSound(), 1.0F));
    }

    @Override
    public void playPanel() {
        if (!SAOConfig.sounds()) {
            return;
        }
        mc().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.panelSound(), 1.0F));
    }

    @Override
    public void playAlert() {
        if (!SAOConfig.sounds()) {
            return;
        }
        mc().getSoundManager().play(SimpleSoundInstance.forUI(SAOMenuPlatform.alertSound(), 1.0F));
    }
}
