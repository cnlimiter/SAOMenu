package com.sao.saomenu.client.menu;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.menu.MenuContext;
import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.MenuHost;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.network.c2s.DropItemC2S;
import com.sao.saomenu.network.c2s.EquipItemC2S;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static com.sao.saomenu.ui.animation.SaoMotion.PRESS_MS;
import static com.sao.saomenu.ui.animation.SaoMotion.clamp01;

/**
 * Menu navigation, open/close motion, selection and popups. Drawing and affine live elsewhere.
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
    int itemScroll;
    int mainScroll;
    boolean keyboardFocus;
    int lastMouseLx = Integer.MIN_VALUE;
    int lastMouseLy = Integer.MIN_VALUE;
    int focusCol;

    ResourceLocation selectedPanelId;
    ResourceLocation expandedEntryId;
    ResourceLocation equipEntryId;
    ResourceLocation actionEntryId;
    ResourceLocation focusEntryId;
    private List<MenuEntry> windowCacheSrc;
    private int windowCacheScroll = -1;
    private List<MenuEntry> windowCacheOut;
    private List<MenuEntry> itemWindowSrc;
    private int itemWindowScroll = -1;
    private List<MenuEntry> itemWindowOut;

    static long now() {
        return Util.getMillis();
    }

    static List<SaoPanel> panels() {
        return SaoUi.panels();
    }

    static SaoPanel panelAt(int index) {
        List<SaoPanel> ps = panels();
        return index >= 0 && index < ps.size() ? ps.get(index) : null;
    }

    int enterMillis() {
        return Math.max(1, SaoUi.theme().enterMillis());
    }

    int exitMillis() {
        return Math.max(1, SaoUi.theme().exitMillis());
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

    int buttonY(int realIndex, int height) {
        return MenuLayout.buttonCenterYAt(height, baseAnchorY, realIndex - mainScroll);
    }

    boolean isActive(int realIndex) {
        return hoverMain == realIndex || selectedMain == realIndex;
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
            return Mth.clamp(1f - (t - closedAt) / (float) exitMillis(), 0f, 1f);
        }
        return Mth.clamp((t - openedAt) / (float) enterMillis(), 0f, 1f);
    }

    int worldMenuMain() {
        return closing ? -1 : selectedMain;
    }

    float openP(long now) {
        return closing ? 1f : clamp01((now - openedAt) / (float) enterMillis());
    }

    float closeP(long now) {
        return closing ? clamp01((now - closedAt) / (float) exitMillis()) : 0f;
    }

    float globalAlpha(long now) {
        return closing ? 1f - closeP(now) : clamp01((now - openedAt) / (float) enterMillis());
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
        ensureMainVisible(selectedMain, height);
    }

    void switchPanelIfChanged(int main) {
        if (panelOwner != main) {
            panelOwner = main;
            panelAt = now();
            expandedItem = -1;
            expandedEntryId = null;
            childOwner = -1;
            equipOwner = -1;
            equipEntryId = null;
            equipShownOwner = -1;
            actionMenuOpen = false;
            actionEntryId = null;
            childScroll = 0;
            itemScroll = 0;
            infoOpen = false;
        }
    }

    void selectMain(int index) {
        assignSelectedMain(index);
        mainTouched = true;
        expandedItem = -1;
        expandedEntryId = null;
        equipOwner = -1;
        equipEntryId = null;
        actionMenuOpen = false;
        actionEntryId = null;
        childScroll = 0;
        itemScroll = 0;
    }

    void assignSelectedMain(int index) {
        selectedMain = index;
        SaoPanel panel = panelAt(index);
        selectedPanelId = panel == null ? null : panel.id();
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
        return MenuLayout.itemVisibleRows(height, 24, 3);
    }

    int itemVisibleRows(int height) {
        return MenuLayout.itemVisibleRows(height, 8, 1);
    }

    void ensureMainVisible(int realIndex, int height) {
        int total = panels().size();
        int vis = MenuLayout.mainVisibleCount(height, total);
        if (realIndex >= 0) {
            if (realIndex < mainScroll) {
                mainScroll = realIndex;
            } else if (realIndex >= mainScroll + vis) {
                mainScroll = realIndex - vis + 1;
            }
        }
        mainScroll = Mth.clamp(mainScroll, 0, Math.max(0, total - vis));
    }

    void ensureItemVisible(int realIndex, int height, int total) {
        int rows = itemVisibleRows(height);
        if (realIndex >= 0) {
            if (realIndex < itemScroll) {
                itemScroll = realIndex;
            } else if (realIndex >= itemScroll + rows) {
                itemScroll = realIndex - rows + 1;
            }
        }
        itemScroll = Mth.clamp(itemScroll, 0, Math.max(0, total - rows));
        itemWindowSrc = null;
    }

    void ensureChildVisible(int realIndex, int height, int total) {
        int rows = childVisibleRows(height);
        if (realIndex >= 0) {
            if (realIndex < childScroll) {
                childScroll = realIndex;
            } else if (realIndex >= childScroll + rows) {
                childScroll = realIndex - rows + 1;
            }
        }
        childScroll = Mth.clamp(childScroll, 0, Math.max(0, total - rows));
        windowCacheSrc = null;
    }

    static int indexOfEntry(List<MenuEntry> list, ResourceLocation id) {
        if (list == null || id == null) {
            return -1;
        }
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.get(i).id())) {
                return i;
            }
        }
        return -1;
    }

    void rebindSelection(int height) {
        List<SaoPanel> ps = panels();
        if (selectedPanelId != null) {
            int idx = -1;
            for (int i = 0; i < ps.size(); i++) {
                if (selectedPanelId.equals(ps.get(i).id())) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                selectedMain = -1;
                selectedPanelId = null;
                expandedItem = -1;
                expandedEntryId = null;
                equipOwner = -1;
                equipEntryId = null;
                actionMenuOpen = false;
                actionEntryId = null;
            } else {
                selectedMain = idx;
            }
        } else {
            selectedMain = -1;
        }
        int vis = MenuLayout.mainVisibleCount(height, ps.size());
        mainScroll = Mth.clamp(mainScroll, 0, Math.max(0, ps.size() - vis));
        if (keyboardFocus && focusCol == 0 && hoverMain >= 0) {
            ensureMainVisible(hoverMain, height);
        }
        if (selectedMain < 0) {
            expandedItem = -1;
            expandedEntryId = null;
            equipOwner = -1;
            equipEntryId = null;
            actionMenuOpen = false;
            actionEntryId = null;
            return;
        }
        List<MenuEntry> items = activeItems(selectedMain);
        if (expandedEntryId != null) {
            expandedItem = indexOfEntry(items, expandedEntryId);
            if (expandedItem < 0 || items.get(expandedItem).children() == null) {
                expandedItem = -1;
                expandedEntryId = null;
                equipOwner = -1;
                equipEntryId = null;
                actionMenuOpen = false;
                actionEntryId = null;
            }
        } else {
            expandedItem = -1;
        }
        itemScroll = Mth.clamp(itemScroll, 0, Math.max(0, items.size() - itemVisibleRows(height)));
        if (keyboardFocus && focusCol == 1 && focusEntryId != null && !items.isEmpty()) {
            int real = indexOfEntry(items, focusEntryId);
            if (real >= 0) {
                ensureItemVisible(real, height, items.size());
                hoverItem = real - itemScroll;
            }
        }
        if (expandedItem < 0 || items.get(expandedItem).children() == null) {
            equipOwner = -1;
            equipEntryId = null;
            actionMenuOpen = false;
            actionEntryId = null;
            return;
        }
        List<MenuEntry> children = items.get(expandedItem).children();
        childScroll = Mth.clamp(childScroll, 0, Math.max(0, children.size() - childVisibleRows(height)));
        if (equipEntryId != null) {
            equipOwner = indexOfEntry(children, equipEntryId);
            if (equipOwner < 0 || children.get(equipOwner).equip() == null) {
                equipOwner = -1;
                equipEntryId = null;
            }
        }
        if (actionMenuOpen && actionEntryId != null) {
            int real = indexOfEntry(children, actionEntryId);
            if (real < 0) {
                actionMenuOpen = false;
                actionRow = -1;
                actionEntryId = null;
            } else {
                ensureChildVisible(real, height, children.size());
                actionRow = real - childScroll;
            }
        }
        if (keyboardFocus && focusCol == 2 && focusEntryId != null) {
            int real = indexOfEntry(children, focusEntryId);
            if (real >= 0) {
                ensureChildVisible(real, height, children.size());
                hoverChild = real - childScroll;
            }
        }
    }

    int childAnchorY(List<MenuEntry> items, int shown, int width, int height) {
        int main = activeMain();
        int anchorY = main >= 0 ? buttonY(main, height) : baseAnchorY;
        if (shown < 0 || items == null) {
            return anchorY;
        }
        List<MenuEntry> win = windowedItems(items, height);
        int vis = shown - itemScroll;
        if (vis >= 0 && vis < win.size()) {
            return MenuLayout.menuItemRectAt(width, height, win.size(), baseAnchorX, anchorY, vis).centerY();
        }
        return anchorY;
    }

    List<MenuEntry> windowedChildren(List<MenuEntry> children, int height) {
        return window(children, childVisibleRows(height), true);
    }

    List<MenuEntry> windowedItems(List<MenuEntry> items, int height) {
        return window(items, itemVisibleRows(height), false);
    }

    private List<MenuEntry> window(List<MenuEntry> src, int rows, boolean child) {
        if (src.size() <= rows) {
            if (child) {
                childScroll = 0;
                windowCacheSrc = null;
            } else {
                itemScroll = 0;
                itemWindowSrc = null;
            }
            return src;
        }
        if (child) {
            if (windowCacheSrc == src && windowCacheScroll == childScroll && windowCacheOut != null) {
                return windowCacheOut;
            }
            childScroll = Mth.clamp(childScroll, 0, src.size() - rows);
            List<MenuEntry> win = new ArrayList<>(src.subList(childScroll, childScroll + rows));
            windowCacheSrc = src;
            windowCacheScroll = childScroll;
            windowCacheOut = win;
            return win;
        }
        if (itemWindowSrc == src && itemWindowScroll == itemScroll && itemWindowOut != null) {
            return itemWindowOut;
        }
        itemScroll = Mth.clamp(itemScroll, 0, src.size() - rows);
        List<MenuEntry> win = new ArrayList<>(src.subList(itemScroll, itemScroll + rows));
        itemWindowSrc = src;
        itemWindowScroll = itemScroll;
        itemWindowOut = win;
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
        int target = equipEntryId != null ? indexOfEntry(children, equipEntryId) : equipOwner;
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

    int equipAnchorY(List<MenuEntry> items, int shown, int target, int width, int height) {
        int childAnchor = childAnchorY(items, shown, width, height);
        if (shown < 0 || shown >= items.size() || items.get(shown).children() == null) {
            return childAnchor;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children(), height);
        int visTarget = target - childScroll;
        if (visTarget >= 0 && visTarget < children.size()) {
            return MenuLayout.childItemRectAt(width, height, children.size(),
                    baseAnchorX, childAnchor, visTarget).centerY();
        }
        return childAnchor;
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
        int childAnchor = childAnchorY(items, shown, width, height);
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
        SaoUi.notify(Component.literal(name),
                Component.translatable(pinned ? "saomenu.inv.pinned" : "saomenu.inv.unpinned"));
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
        actionEntryId = null;
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
        if (keyboardFocus && lx == lastMouseLx && ly == lastMouseLy) {
            return;
        }
        keyboardFocus = false;
        lastMouseLx = lx;
        lastMouseLy = ly;
        int visMain = MenuLayout.mainVisibleCount(height, panels().size());
        int hit = MenuLayout.hoveredMainButtonAt(width, height, baseAnchorX, baseAnchorY, visMain, lx, ly);
        hoverMain = hit < 0 ? -1 : hit + mainScroll;
        hoverItem = -1;
        hoverChild = -1;
        hoverEquip = -1;
        hoverAction = -1;
        int main = activeMain();
        if (main < 0) {
            return;
        }
        List<MenuEntry> items = activeItems(main);
        List<MenuEntry> win = windowedItems(items, height);
        int anchorY = buttonY(main, height);
        if (actionMenuOpen) {
            List<MenuEntry> winA = shownChildren(items, height);
            if (winA != null && actionRow >= 0 && actionRow < winA.size()) {
                int shownA = visibleChildrenItem(items);
                int anchorA = childAnchorY(items, shownA, width, height);
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
        for (int i = 0; i < win.size(); i++) {
            if (MenuLayout.menuItemRectAt(width, height, win.size(), baseAnchorX, anchorY, i).contains(lx, ly)) {
                hoverItem = i;
            }
        }
        int shown = visibleChildrenItem(items);
        if (shown < 0 || items.get(shown).children() == null) {
            return;
        }
        List<MenuEntry> children = windowedChildren(items.get(shown).children(), height);
        int childAnchor = childAnchorY(items, shown, width, height);
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
        int equipAnchor = equipAnchorY(items, shown, target, width, height);
        for (int i = 0; i < entries.size(); i++) {
            if (MenuLayout.equipItemRectAt(width, height, entries.size(), baseAnchorX, equipAnchor, i)
                    .contains(lx, ly)) {
                hoverEquip = i;
            }
        }
    }
}
