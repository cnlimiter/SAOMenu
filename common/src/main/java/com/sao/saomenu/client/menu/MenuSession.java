package com.sao.saomenu.client.menu;

import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.network.c2s.DropItemC2S;
import com.sao.saomenu.network.c2s.EquipItemC2S;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.sao.saomenu.ui.animation.SaoMotion.CLOSE_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.OPEN_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.PRESS_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;

/**
 * 菜单导航、开合动画、选中/展开与弹层状态。不含绘制与仿射。
 */
final class MenuSession {

    record EquipEntry(ItemStack stack, boolean empty) {
    }

    long openedAt;
    boolean openAnimArmed = true;
    long panelAt = Long.MIN_VALUE;
    int panelOwner = -1;
    long childAt;
    int childOwner = -1;
    boolean closing;
    long closedAt;

    int equipOwner = -1;
    long equipAt;
    int equipShownOwner = -1;

    int selectedMain = -1;
    boolean mainTouched;
    int hoverMain = -1;
    int hoverItem = -1;
    int hoverChild = -1;
    int hoverEquip = -1;
    int expandedItem = -1;

    boolean confirmClose;
    long confirmAt;

    int baseAnchorX;
    int baseAnchorY;

    int pinDragFrom = -1;
    ItemStack pinDragStack;
    int pinDragMx;
    int pinDragMy;
    long pinDragAt;

    boolean actionMenuOpen;
    int actionRow = -1;
    long actionAt;
    int hoverAction = -1;

    boolean infoOpen;
    long infoAt;
    ItemStack infoStack;

    long mainPressAt = Long.MIN_VALUE;
    int mainPressIndex = -1;
    long itemPressAt = Long.MIN_VALUE;
    int itemPressIndex = -1;
    int itemPressColumn = -1;

    int childScroll;
    private List<MenuEntry> windowCacheSrc;
    private int windowCacheScroll = -1;
    private List<MenuEntry> windowCacheOut;

    static long now() {
        return Util.getMillis();
    }

    static List<SaoPanel> panels() {
        return SaoMenuRegistry.panels();
    }

    static SaoPanel panelAt(int index) {
        List<SaoPanel> ps = panels();
        return index >= 0 && index < ps.size() ? ps.get(index) : null;
    }

    List<MenuEntry> itemsForPanel(int index) {
        List<SaoPanel> ps = panels();
        if (index < 0 || index >= ps.size()) {
            return List.of();
        }
        List<MenuEntry> items = ps.get(index).items().get();
        return items == null ? List.of() : items;
    }

    List<MenuEntry> activeItems(int main) {
        return itemsForPanel(main);
    }

    int activeMain() {
        return selectedMain;
    }

    int buttonY(int index, int height) {
        return MenuLayout.buttonCenterYAt(height, baseAnchorY, index);
    }

    boolean isActive(int index) {
        return hoverMain == index || selectedMain == index;
    }

    boolean mainPressing(int index) {
        return mainPressIndex == index && now() - mainPressAt < PRESS_MS;
    }

    boolean itemPressing(int column, int index) {
        return itemPressColumn == column && itemPressIndex == index && now() - itemPressAt < PRESS_MS;
    }

    boolean blocksMovement() {
        return closing || confirmClose || infoOpen;
    }

    float worldMenuAlpha() {
        long t = now();
        if (closing) {
            return Mth.clamp(1f - (t - closedAt) / (float) CLOSE_MS, 0f, 1f);
        }
        return Mth.clamp((t - openedAt) / 150f, 0f, 1f);
    }

    int worldMenuMain() {
        return closing ? -1 : selectedMain;
    }

    float openP(long now) {
        return closing ? 1f : clamp01((now - openedAt) / (float) OPEN_MS);
    }

    float closeP(long now) {
        return closing ? clamp01((now - closedAt) / (float) CLOSE_MS) : 0f;
    }

    float globalAlpha(long now) {
        return closing ? 1f - closeP(now) : clamp01((now - openedAt) / 150f);
    }

    boolean closeFinished(long now) {
        return closing && closeP(now) >= 1f;
    }

    void onInit(int width, int height) {
        if (closing) {
            closing = false;
            confirmClose = false;
            openAnimArmed = true;
        }
        if (openAnimArmed) {
            openedAt = now();
            openAnimArmed = false;
            SaoPanels.resetSession();
        }
        mainTouched = false;
        baseAnchorX = MenuLayout.firstButtonCenterX(width);
        baseAnchorY = MenuLayout.firstButtonCenterY(height);
    }

    void switchPanelIfChanged(int main) {
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

    void selectMain(int index) {
        selectedMain = index;
        mainTouched = true;
        expandedItem = -1;
        equipOwner = -1;
        actionMenuOpen = false;
        childScroll = 0;
    }

    void beginClose() {
        if (closing) {
            return;
        }
        closing = true;
        closedAt = now();
        actionMenuOpen = false;
        infoOpen = false;
    }

    void openConfirm() {
        confirmClose = true;
        confirmAt = now();
    }

    int childVisibleRows(int height) {
        int step = MenuLayout.itemH(height) + MenuLayout.itemGap(height);
        return Math.max(3, (height - 24) / step);
    }

    List<MenuEntry> windowedChildren(List<MenuEntry> children, int height) {
        int rows = childVisibleRows(height);
        if (children.size() <= rows) {
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

    int visibleChildrenItem(List<MenuEntry> items) {
        if (expandedItem >= 0 && expandedItem < items.size() && items.get(expandedItem).children() != null) {
            return expandedItem;
        }
        return -1;
    }

    List<MenuEntry> shownChildren(List<MenuEntry> items, int height) {
        int shown = visibleChildrenItem(items);
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return null;
        }
        return windowedChildren(items.get(shown).children(), height);
    }

    List<MenuEntry> shownChildrenAt(int main, int height) {
        if (main < 0) {
            return null;
        }
        return shownChildren(activeItems(main), height);
    }

    float childShiftTarget(int height) {
        int main = activeMain();
        if (main < 0) {
            return 0f;
        }
        if (visibleChildrenItem(activeItems(main)) >= 0) {
            return MenuLayout.childColumnXAt(baseAnchorX, height)
                    - MenuLayout.itemColumnXAt(baseAnchorX, height);
        }
        return 0f;
    }

    int equipTargetIndex(List<MenuEntry> items, int shown) {
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return -1;
        }
        List<MenuEntry> children = items.get(shown).children();
        if (!children.isEmpty() && children.get(0).stack() != null) {
            return -1;
        }
        int target = equipOwner;
        if (target < 0 || target >= children.size()) {
            return -1;
        }
        return children.get(target).equip() != null ? target : -1;
    }

    MenuEntry.EquipKind equipKindAt(List<MenuEntry> items, int shown, int target) {
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return null;
        }
        List<MenuEntry> children = items.get(shown).children();
        return target >= 0 && target < children.size() ? children.get(target).equip() : null;
    }

    int equipAnchorY(List<MenuEntry> items, int shown, int target, int height) {
        int childAnchor = MenuLayout.menuItemRectAt(1, height, items.size(),
                baseAnchorX, buttonY(shown, height), shown).centerY();
        return MenuLayout.childItemRectAt(1, height, items.get(shown).children().size(),
                baseAnchorX, childAnchor, target).centerY();
    }

    List<EquipEntry> equipEntries(MenuEntry.EquipKind kind, Player player) {
        List<EquipEntry> list = new ArrayList<>();
        if (player != null && kind != null) {
            switch (kind) {
                case WEAPON -> {
                    if (!player.getMainHandItem().isEmpty()) {
                        list.add(new EquipEntry(player.getMainHandItem(), false));
                    }
                }
                case ARMOR -> {
                    for (EquipmentSlot slot : EquipmentSlot.values()) {
                        if (slot.getType() != EquipmentSlot.Type.ARMOR) {
                            continue;
                        }
                        ItemStack s = player.getItemBySlot(slot);
                        if (!s.isEmpty()) {
                            list.add(new EquipEntry(s, false));
                        }
                    }
                }
                case TRINKET -> {
                    if (!player.getOffhandItem().isEmpty()) {
                        list.add(new EquipEntry(player.getOffhandItem(), false));
                    }
                }
            }
        }
        if (list.isEmpty()) {
            list.add(new EquipEntry(ItemStack.EMPTY, true));
        }
        return list;
    }

    ItemStack stackAtRow(int row, int height) {
        List<MenuEntry> children = shownChildrenAt(selectedMain, height);
        if (children == null || row < 0 || row >= children.size()) {
            return null;
        }
        ItemStack st = children.get(row).stack();
        return st == null || st.isEmpty() ? null : st;
    }

    String itemIdAtRow(int row, int height) {
        List<MenuEntry> children = shownChildrenAt(selectedMain, height);
        if (children == null || row < 0 || row >= children.size()) {
            return null;
        }
        String id = SaoPanels.itemId(children.get(row).stack());
        return id.isEmpty() ? null : id;
    }

    String itemNameAtRow(int row, int height) {
        List<MenuEntry> children = shownChildrenAt(selectedMain, height);
        if (children == null || row < 0 || row >= children.size()) {
            return "";
        }
        ItemStack st = children.get(row).stack();
        return st == null || st.isEmpty() ? "" : st.getHoverName().getString();
    }

    int pinRowAt(MenuTransform xform, int width, int height) {
        if (selectedMain < 0) {
            return -1;
        }
        List<MenuEntry> items = activeItems(selectedMain);
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return -1;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children(), height);
        int anchorY = buttonY(selectedMain, height);
        int childAnchor = MenuLayout.menuItemRectAt(width, height, items.size(),
                baseAnchorX, anchorY, shown).centerY();
        int lx = xform.localXi();
        int ly = xform.localYi();
        for (int i = 0; i < children.size(); i++) {
            ItemStack st = children.get(i).stack();
            if (st == null || st.isEmpty()) {
                continue;
            }
            if (MenuLayout.childItemRectAt(width, height, children.size(),
                    baseAnchorX, childAnchor, i).contains(lx, ly)) {
                return i;
            }
        }
        return -1;
    }

    void togglePinAt(int row, int height) {
        String id = itemIdAtRow(row, height);
        if (id == null) {
            return;
        }
        String name = itemNameAtRow(row, height);
        boolean pinned = SAOConfig.togglePinned(id);
        savePinConfig();
        com.sao.saomenu.client.hud.SAONotification.push(name,
                com.sao.saomenu.ui.text.SaoText.tr(pinned ? "saomenu.inv.pinned" : "saomenu.inv.unpinned"));
    }

    boolean applyPinDrag(int fromRow, int toRow, int height) {
        if (fromRow < 0 || toRow < 0 || fromRow == toRow || selectedMain < 0) {
            return false;
        }
        List<MenuEntry> items = activeItems(selectedMain);
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return false;
        }
        List<MenuEntry> all = items.get(shown).children();
        String fromId = itemIdAtRow(fromRow, height);
        String toId = itemIdAtRow(toRow, height);
        if (fromId == null || toId == null || fromId.equals(toId)) {
            return false;
        }
        List<String> order = new ArrayList<>();
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
        return true;
    }

    private void savePinConfig() {
        SAOConfig.save();
        SaoPanels.resetSession();
    }

    void executeItemAction(int action, MenuEntry target) {
        actionMenuOpen = false;
        switch (action) {
            case 0 -> new EquipItemC2S(target.invSlot()).sendToServer();
            case 1 -> {
                infoStack = target.stack().copy();
                infoOpen = true;
                infoAt = now();
            }
            case 2 -> new DropItemC2S(target.invSlot(), true).sendToServer();
            default -> throw new IllegalArgumentException("Unknown inventory action: " + action);
        }
    }

    void activate(MenuHost host, MenuEntry entry) {
        if (entry.onActivate() == null) {
            host.playClick();
            return;
        }
        entry.onActivate().accept(MenuContext.of(host, entry));
    }

    void updateHovers(MenuTransform xform, int width, int height) {
        if (confirmClose) {
            return;
        }
        int lx = xform.localXi();
        int ly = xform.localYi();
        hoverMain = MenuLayout.hoveredMainButtonAt(width, height, baseAnchorX, baseAnchorY,
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
        int anchorY = buttonY(main, height);
        if (actionMenuOpen) {
            List<MenuEntry> winA = shownChildren(items, height);
            if (winA != null && actionRow >= 0 && actionRow < winA.size()) {
                int shownA = visibleChildrenItem(items);
                int anchorA = shownA >= 0 ? MenuLayout.menuItemRectAt(width, height,
                        items.size(), baseAnchorX, anchorY, shownA).centerY() : anchorY;
                MenuLayout.Rect rowA = MenuLayout.childItemRectAt(width, height,
                        winA.size(), baseAnchorX, anchorA, actionRow);
                for (int b = 0; b < 3; b++) {
                    if (MenuDialogs.actionButtonRect(rowA, b).contains(lx, ly)) {
                        hoverAction = b;
                        break;
                    }
                }
            }
            return;
        }
        for (int i = 0; i < items.size(); i++) {
            if (MenuLayout.menuItemRectAt(width, height, items.size(), baseAnchorX, anchorY, i).contains(lx, ly)) {
                hoverItem = i;
            }
        }
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children(), height);
        int childAnchor = MenuLayout.menuItemRectAt(width, height, items.size(), baseAnchorX, anchorY, shown).centerY();
        for (int i = 0; i < children.size(); i++) {
            if (MenuLayout.childItemRectAt(width, height, children.size(), baseAnchorX, childAnchor, i)
                    .contains(lx, ly)) {
                hoverChild = i;
            }
        }
        int target = equipTargetIndex(items, shown);
        if (target < 0) {
            return;
        }
        List<EquipEntry> entries = equipEntries(equipKindAt(items, shown, target), Minecraft.getInstance().player);
        int equipAnchor = equipAnchorY(items, shown, target, height);
        for (int i = 0; i < entries.size(); i++) {
            if (MenuLayout.equipItemRectAt(width, height, entries.size(), baseAnchorX, equipAnchor, i)
                    .contains(lx, ly)) {
                hoverEquip = i;
            }
        }
    }
}
