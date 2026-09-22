package com.sao.saomenu.client.menu;

import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.client.hud.HudLayoutEditor;
import com.sao.saomenu.client.hud.SAOMapPanel;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.client.input.SAOMenuMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Menu keys, mouse, movement passthrough, item pin drag. HUD drag stays on {@link HudLayoutEditor}. */
final class MenuInput {

    private MenuInput() {
    }

    static void mouseMoved(SAOMenuScreen screen, MenuSession s,
                           HudLayoutEditor hud, double mouseX, double mouseY) {
        Minecraft mc = Minecraft.getInstance();
        if (!s.closing) {
            hud.mouseMoved(mc, screen.width, screen.height, mouseX, mouseY);
        }
        if (s.pinDragFrom >= 0) {
            s.pinDragMx = (int) mouseX;
            s.pinDragMy = (int) mouseY;
        }
    }

    static boolean mouseReleased(SAOMenuScreen screen, MenuSession s, MenuTransform xform,
                                 HudLayoutEditor hud, double mouseX, double mouseY, int button) {
        if (button == 1 && s.pinDragFrom >= 0) {
            xform.toLocal(mouseX, mouseY);
            int from = s.pinDragFrom;
            int to = s.pinRowAt(xform, screen.width, screen.height);
            s.pinDragFrom = -1;
            s.pinDragStack = null;
            if (s.applyPinDrag(from, to, screen.height)) {
                screen.playPanel();
            }
            return true;
        }
        return hud.mouseReleased(Minecraft.getInstance(), screen.width, screen.height, mouseX, mouseY, button);
    }

    static boolean mouseClicked(SAOMenuScreen screen, MenuSession s, MenuTransform xform,
                                HudLayoutEditor hud, double mouseX, double mouseY, int button) {
        if (s.closing) {
            return false;
        }
        s.keyboardFocus = false;
        s.rebindSelection(screen.height);
        Minecraft mc = Minecraft.getInstance();
        if (button == 1) {
            xform.toLocal(mouseX, mouseY);
            int row = s.pinRowAt(xform, screen.width, screen.height);
            if (row >= 0) {
                if (screen.hasShiftDown()) {
                    s.togglePinAt(row, screen.height);
                    screen.playPanel();
                } else {
                    s.pinDragFrom = row;
                    s.pinDragStack = s.stackAtRow(row, screen.height);
                    s.pinDragMx = (int) mouseX;
                    s.pinDragMy = (int) mouseY;
                    s.pinDragAt = MenuSession.now();
                    screen.playPanel();
                }
                return true;
            }
            return false;
        }
        if (button != 0) {
            return false;
        }
        if (hud.mouseClicked(mc, screen.width, screen.height, mouseX, mouseY, button)) {
            return true;
        }
        int mx = (int) mouseX;
        int my = (int) mouseY;

        if (s.confirmClose) {
            MenuLayout.Rect at = MenuDialogs.dialogRect(screen.width, screen.height);
            int d = 26;
            int by = at.y() + Math.round(at.h() * 0.80f) - d / 2;
            int b1x = at.x() + at.w() / 4 - d / 2;
            int b2x = at.x() + at.w() * 3 / 4 - d / 2;
            if (MenuLayout.inCircle(b1x + d / 2, by + d / 2, d / 2 + 2, mx, my)) {
                screen.playClick();
                beginClose(screen, s);
                return true;
            }
            if (MenuLayout.inCircle(b2x + d / 2, by + d / 2, d / 2 + 2, mx, my)) {
                screen.playPanel();
                s.confirmClose = false;
                return true;
            }
            if (mx < at.x() || mx >= at.x() + at.w() || my < at.y() || my >= at.y() + at.h()) {
                screen.playPanel();
                s.confirmClose = false;
                return true;
            }
            return true;
        }

        if (s.infoOpen) {
            s.infoOpen = false;
            screen.playPanel();
            return true;
        }

        xform.toLocal(mx, my);
        int lx = xform.localXi();
        int ly = xform.localYi();

        if (s.actionMenuOpen) {
            int mainA = s.activeMain();
            List<MenuEntry> itemsA = mainA >= 0 ? s.activeItems(mainA) : null;
            int shownA = itemsA != null ? s.visibleChildrenItem(itemsA) : -1;
            if (itemsA != null && shownA >= 0 && itemsA.get(shownA).children() != null) {
                List<MenuEntry> winA = s.windowedChildren(itemsA.get(shownA).children(), screen.height);
                int anchorA = s.childAnchorY(itemsA, shownA, screen.width, screen.height);
                if (s.actionRow >= 0 && s.actionRow < winA.size() && winA.get(s.actionRow).stack() != null) {
                    MenuLayout.Rect rowA = MenuLayout.childItemRectAt(screen.width, screen.height,
                            winA.size(), s.baseAnchorX, anchorA, s.actionRow);
                    for (int b = 0; b < 3; b++) {
                        MenuLayout.Rect br = MenuDialogs.actionButtonRect(rowA, b);
                        if (MenuLayout.inCircle(br.centerX(), br.centerY(), br.w() / 2 + 2, lx, ly)) {
                            List<MenuEntry> all = itemsA.get(shownA).children();
                            int real = s.childScroll + s.actionRow;
                            if (real < all.size() && all.get(real).stack() != null) {
                                int act = b;
                                s.executeItemAction(act, all.get(real));
                                if (act == 1) {
                                    screen.playAlert();
                                } else {
                                    screen.playClick();
                                }
                            } else {
                                s.actionMenuOpen = false;
                                s.actionEntryId = null;
                            }
                            return true;
                        }
                    }
                }
            }
        }

        int visMain = MenuLayout.mainVisibleCount(screen.height, MenuSession.panels().size());
        int hitVis = MenuLayout.hoveredMainButtonAt(screen.width, screen.height, s.baseAnchorX, s.baseAnchorY,
                visMain, lx, ly);
        int hitMain = hitVis < 0 ? -1 : hitVis + s.mainScroll;
        if (hitMain != -1) {
            s.mainTouched = true;
            if (s.selectedMain == hitMain) {
                s.selectMain(-1);
                s.panelOwner = -1;
                s.infoOpen = false;
                screen.playPanel();
            } else {
                s.selectMain(hitMain);
                s.infoOpen = false;
                s.ensureMainVisible(hitMain, screen.height);
                screen.playClick();
            }
            s.mainPressIndex = hitMain;
            s.mainPressAt = MenuSession.now();
            return true;
        }

        if (SAOConfig.hideHotbar()) {
            for (int i = 0; i < MenuLayout.DOT_COUNT; i++) {
                if (MenuLayout.inDot(screen.width, screen.height, i, mx, my)) {
                    Player p = mc.player;
                    if (p != null && i > 0 && p.getInventory().selected != i - 1) {
                        p.getInventory().selected = i - 1;
                        screen.playClick();
                    }
                    return true;
                }
            }
        }

        int main = s.activeMain();
        if (main >= 0) {
            List<MenuEntry> items = s.activeItems(main);
            List<MenuEntry> win = s.windowedItems(items, screen.height);
            int anchorY = s.buttonY(main, screen.height);
            for (int i = 0; i < win.size(); i++) {
                if (MenuLayout.menuItemRectAt(screen.width, screen.height, win.size(),
                        s.baseAnchorX, anchorY, i).contains(lx, ly)) {
                    s.itemPressColumn = 0;
                    s.itemPressIndex = i;
                    s.itemPressAt = MenuSession.now();
                    int real = s.itemScroll + i;
                    if (win.get(i).hasChildren()) {
                        int newExpanded = s.expandedItem == real ? -1 : real;
                        if (newExpanded != s.expandedItem) {
                            s.equipOwner = -1;
                            s.equipEntryId = null;
                            s.childScroll = 0;
                            s.actionMenuOpen = false;
                            s.actionEntryId = null;
                        }
                        s.expandedItem = newExpanded;
                        s.expandedEntryId = newExpanded < 0 ? null : win.get(i).id();
                        screen.playPanel();
                    } else {
                        s.activate(screen, win.get(i));
                    }
                    return true;
                }
            }
            int shown = s.visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> children = s.windowedChildren(items.get(shown).children(), screen.height);
                int childAnchor = s.childAnchorY(items, shown, screen.width, screen.height);
                int equipTarget = s.equipTargetIndex(items, shown);
                if (equipTarget >= 0) {
                    int equipAnchor = s.equipAnchorY(items, shown, equipTarget, screen.width, screen.height);
                    List<MenuSession.EquipEntry> entries = s.equipEntries(
                            s.equipKindAt(items, shown, equipTarget), mc.player);
                    for (int i = 0; i < entries.size(); i++) {
                        if (MenuLayout.equipItemRectAt(screen.width, screen.height, entries.size(),
                                s.baseAnchorX, equipAnchor, i).contains(lx, ly)) {
                            screen.playClick();
                            return true;
                        }
                    }
                }
                for (int i = 0; i < children.size(); i++) {
                    if (MenuLayout.childItemRectAt(screen.width, screen.height, children.size(),
                            s.baseAnchorX, childAnchor, i).contains(lx, ly)) {
                        s.itemPressColumn = 1;
                        s.itemPressIndex = i;
                        s.itemPressAt = MenuSession.now();
                        MenuEntry child = children.get(i);
                        if (child.equip() != null) {
                            s.actionMenuOpen = false;
                            s.actionEntryId = null;
                            int realChild = s.childScroll + i;
                            if (s.equipOwner != realChild) {
                                s.equipOwner = realChild;
                                s.equipEntryId = child.id();
                                s.equipAt = MenuSession.now();
                                screen.playPanel();
                            }
                        } else if (child.isItem()) {
                            if (s.actionMenuOpen && s.actionRow == i) {
                                s.actionMenuOpen = false;
                                s.actionEntryId = null;
                                screen.playPanel();
                            } else {
                                s.actionMenuOpen = true;
                                s.actionRow = i;
                                s.actionEntryId = child.id();
                                s.actionAt = MenuSession.now();
                                screen.playPanel();
                            }
                        } else {
                            s.activate(screen, child);
                        }
                        return true;
                    }
                }
            }
        }

        if (s.actionMenuOpen) {
            s.actionMenuOpen = false;
            s.actionEntryId = null;
            screen.playPanel();
            return true;
        }
        beginClose(screen, s);
        return true;
    }

    static boolean mouseScrolled(SAOMenuScreen screen, MenuSession s, MenuTransform xform,
                                 double mouseX, double mouseY, double delta) {
        if (s.closing || s.confirmClose || delta == 0) {
            return false;
        }
        s.keyboardFocus = false;
        s.rebindSelection(screen.height);
        xform.toLocal(mouseX, mouseY);
        int lx = xform.localXi();
        int ly = xform.localYi();
        int step = (int) Math.signum(delta);
        int main = s.activeMain();
        List<MenuEntry> items = main >= 0 ? s.activeItems(main) : null;

        if (items != null) {
            int shown = s.visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null && !items.get(shown).children().isEmpty()) {
                List<MenuEntry> children = items.get(shown).children();
                int rows = s.childVisibleRows(screen.height);
                List<MenuEntry> winChildren = s.windowedChildren(children, screen.height);
                int childAnchor = s.childAnchorY(items, shown, screen.width, screen.height);
                boolean over = false;
                for (int v = 0; v < winChildren.size(); v++) {
                    if (MenuLayout.childItemRectAt(screen.width, screen.height, winChildren.size(),
                            s.baseAnchorX, childAnchor, v).contains(lx, ly)) {
                        over = true;
                        break;
                    }
                }
                if (over) {
                    return scrollList(screen, s, children.size(), rows, true, step);
                }
            }
            List<MenuEntry> win = s.windowedItems(items, screen.height);
            int anchorY = s.buttonY(main, screen.height);
            for (int i = 0; i < win.size(); i++) {
                if (MenuLayout.menuItemRectAt(screen.width, screen.height, win.size(),
                        s.baseAnchorX, anchorY, i).contains(lx, ly)) {
                    return scrollList(screen, s, items.size(), s.itemVisibleRows(screen.height), false, step);
                }
            }
        }

        int visMain = MenuLayout.mainVisibleCount(screen.height, MenuSession.panels().size());
        int hit = MenuLayout.hoveredMainButtonAt(screen.width, screen.height, s.baseAnchorX, s.baseAnchorY,
                visMain, lx, ly);
        if (hit != -1) {
            int total = MenuSession.panels().size();
            int before = s.mainScroll;
            s.mainScroll = Mth.clamp(s.mainScroll - step, 0, Math.max(0, total - visMain));
            if (s.mainScroll != before) {
                screen.playClick();
            }
            return true;
        }
        return false;
    }

    private static boolean scrollList(SAOMenuScreen screen, MenuSession s, int size, int rows, boolean child, int step) {
        int max = Math.max(0, size - rows);
        int before = child ? s.childScroll : s.itemScroll;
        int next = Mth.clamp(before - step, 0, max);
        if (child) {
            s.childScroll = next;
        } else {
            s.itemScroll = next;
        }
        if (next != before) {
            s.actionMenuOpen = false;
            s.actionEntryId = null;
            screen.playClick();
        }
        return true;
    }

    static boolean keyPressed(SAOMenuScreen screen, MenuSession s, Minecraft mc,
                              int keyCode, int scanCode, int modifiers) {
        if (s.infoOpen && (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_O)) {
            s.infoOpen = false;
            return true;
        }
        if (!s.blocksMovement() && SAOMenuMovement.setKeyState(mc, keyCode, scanCode, true)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_F5 && mc.player != null) {
            var opt = mc.options;
            switch (opt.getCameraType()) {
                case FIRST_PERSON -> opt.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                case THIRD_PERSON_BACK -> opt.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
                default -> opt.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            }
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_O) {
            if (s.confirmClose) {
                s.confirmClose = false;
                return true;
            }
            beginClose(screen, s);
            return true;
        }
        if (s.closing || s.confirmClose) {
            return false;
        }
        s.rebindSelection(screen.height);
        if (keyCode == GLFW.GLFW_KEY_UP) {
            return navigate(screen, s, -1);
        }
        if (keyCode == GLFW.GLFW_KEY_DOWN) {
            return navigate(screen, s, 1);
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) {
            return navigateColumn(screen, s, -1);
        }
        if (keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            return navigateColumn(screen, s, 1);
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_UP) {
            return page(screen, s, -1);
        }
        if (keyCode == GLFW.GLFW_KEY_PAGE_DOWN) {
            return page(screen, s, 1);
        }
        return false;
    }

    private static boolean navigate(SAOMenuScreen screen, MenuSession s, int delta) {
        s.keyboardFocus = true;
        if (s.actionMenuOpen) {
            int cur = s.hoverAction < 0 ? 0 : s.hoverAction;
            s.hoverAction = Mth.clamp(cur + delta, 0, 2);
            return true;
        }
        int main = s.activeMain();
        List<MenuEntry> items = main >= 0 ? s.activeItems(main) : List.of();
        int shown = s.visibleChildrenItem(items);
        if (shown >= 0 && items.get(shown).children() != null && !items.get(shown).children().isEmpty()
                && (s.focusCol == 2 || s.hoverChild >= 0)) {
            List<MenuEntry> all = items.get(shown).children();
            int cur = s.focusCol == 2 ? MenuSession.indexOfEntry(all, s.focusEntryId) : -1;
            if (cur < 0) {
                cur = s.childScroll + Math.max(0, s.hoverChild);
            }
            int real = Mth.clamp(cur + delta, 0, all.size() - 1);
            s.ensureChildVisible(real, screen.height, all.size());
            s.hoverChild = real - s.childScroll;
            s.focusEntryId = all.get(real).id();
            s.focusCol = 2;
            screen.playClick();
            return true;
        }
        if (main >= 0 && !items.isEmpty() && (s.focusCol >= 1 || s.hoverItem >= 0 || s.selectedMain >= 0)) {
            int cur = s.focusCol == 1 ? MenuSession.indexOfEntry(items, s.focusEntryId) : -1;
            if (cur < 0) {
                cur = s.itemScroll + Math.max(0, s.hoverItem);
            }
            int real = Mth.clamp(cur + delta, 0, items.size() - 1);
            s.ensureItemVisible(real, screen.height, items.size());
            s.hoverItem = real - s.itemScroll;
            s.focusEntryId = items.get(real).id();
            s.focusCol = 1;
            screen.playClick();
            return true;
        }
        int total = MenuSession.panels().size();
        if (total == 0) {
            return false;
        }
        int cur = s.hoverMain >= 0 ? s.hoverMain : Math.max(0, s.selectedMain);
        cur = Mth.clamp(cur + delta, 0, total - 1);
        s.ensureMainVisible(cur, screen.height);
        s.hoverMain = cur;
        s.focusCol = 0;
        s.focusEntryId = null;
        screen.playClick();
        return true;
    }

    private static boolean navigateColumn(SAOMenuScreen screen, MenuSession s, int dir) {
        s.keyboardFocus = true;
        if (dir < 0) {
            if (s.actionMenuOpen) {
                s.actionMenuOpen = false;
                s.actionEntryId = null;
                screen.playPanel();
                return true;
            }
            if (s.focusCol == 2 || s.hoverChild >= 0) {
                s.hoverChild = -1;
                s.equipOwner = -1;
                s.equipEntryId = null;
                s.focusCol = 1;
                if (s.expandedEntryId != null) {
                    s.focusEntryId = s.expandedEntryId;
                }
                screen.playPanel();
                return true;
            }
            if (s.expandedItem >= 0) {
                s.expandedItem = -1;
                s.expandedEntryId = null;
                s.childScroll = 0;
                screen.playPanel();
                return true;
            }
            if (s.selectedMain >= 0) {
                s.selectMain(-1);
                s.focusCol = 0;
                s.focusEntryId = null;
                screen.playPanel();
                return true;
            }
            return false;
        }
        if (s.actionMenuOpen && s.hoverAction >= 0) {
            int main = s.activeMain();
            List<MenuEntry> items = main >= 0 ? s.activeItems(main) : null;
            int shown = items != null ? s.visibleChildrenItem(items) : -1;
            if (items != null && shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> all = items.get(shown).children();
                int real = s.actionEntryId != null
                        ? MenuSession.indexOfEntry(all, s.actionEntryId)
                        : s.childScroll + s.actionRow;
                if (real >= 0 && real < all.size() && all.get(real).stack() != null) {
                    s.executeItemAction(s.hoverAction, all.get(real));
                    screen.playClick();
                    return true;
                }
            }
        }
        if (s.focusCol == 2 || s.hoverChild >= 0) {
            int main = s.activeMain();
            List<MenuEntry> items = s.activeItems(main);
            List<MenuEntry> children = s.shownChildren(items, screen.height);
            if (children != null && s.hoverChild >= 0 && s.hoverChild < children.size()) {
                MenuEntry child = children.get(s.hoverChild);
                if (child.equip() != null) {
                    s.equipOwner = s.childScroll + s.hoverChild;
                    s.equipEntryId = child.id();
                    s.equipAt = MenuSession.now();
                    screen.playPanel();
                } else if (child.isItem()) {
                    s.actionMenuOpen = true;
                    s.actionRow = s.hoverChild;
                    s.actionEntryId = child.id();
                    s.actionAt = MenuSession.now();
                    s.hoverAction = 0;
                    screen.playPanel();
                } else {
                    s.activate(screen, child);
                }
                return true;
            }
        }
        if (s.selectedMain >= 0) {
            List<MenuEntry> items = s.activeItems(s.selectedMain);
            List<MenuEntry> win = s.windowedItems(items, screen.height);
            int vis = s.hoverItem >= 0 ? s.hoverItem : 0;
            if (vis < win.size()) {
                MenuEntry entry = win.get(vis);
                int real = s.itemScroll + vis;
                if (entry.hasChildren()) {
                    s.expandedItem = real;
                    s.expandedEntryId = entry.id();
                    s.focusCol = 2;
                    s.hoverChild = 0;
                    s.childScroll = 0;
                    List<MenuEntry> ch = entry.children();
                    s.focusEntryId = ch != null && !ch.isEmpty() ? ch.get(0).id() : null;
                    screen.playPanel();
                } else {
                    s.activate(screen, entry);
                }
                return true;
            }
        }
        if (s.hoverMain >= 0) {
            s.selectMain(s.hoverMain);
            s.ensureMainVisible(s.hoverMain, screen.height);
            s.focusCol = 1;
            s.hoverItem = 0;
            List<MenuEntry> opened = s.activeItems(s.selectedMain);
            s.focusEntryId = opened.isEmpty() ? null : opened.get(0).id();
            screen.playClick();
            return true;
        }
        return false;
    }

    private static boolean page(SAOMenuScreen screen, MenuSession s, int dir) {
        s.keyboardFocus = true;
        int main = s.activeMain();
        List<MenuEntry> items = main >= 0 ? s.activeItems(main) : null;
        int shown = items != null ? s.visibleChildrenItem(items) : -1;
        if (items != null && shown >= 0 && items.get(shown).children() != null
                && !items.get(shown).children().isEmpty()) {
            List<MenuEntry> all = items.get(shown).children();
            int rows = s.childVisibleRows(screen.height);
            int cur = MenuSession.indexOfEntry(all, s.focusEntryId);
            if (cur < 0) {
                cur = s.childScroll + Math.max(0, s.hoverChild);
            }
            int real = Mth.clamp(cur + dir * rows, 0, all.size() - 1);
            s.ensureChildVisible(real, screen.height, all.size());
            s.hoverChild = real - s.childScroll;
            s.focusEntryId = all.get(real).id();
            s.focusCol = 2;
            s.actionMenuOpen = false;
            s.actionEntryId = null;
            screen.playClick();
            return true;
        }
        if (items != null && !items.isEmpty()) {
            int rows = s.itemVisibleRows(screen.height);
            int cur = MenuSession.indexOfEntry(items, s.focusEntryId);
            if (cur < 0) {
                cur = s.itemScroll + Math.max(0, s.hoverItem);
            }
            int real = Mth.clamp(cur + dir * rows, 0, items.size() - 1);
            s.ensureItemVisible(real, screen.height, items.size());
            s.hoverItem = real - s.itemScroll;
            s.focusEntryId = items.get(real).id();
            s.focusCol = 1;
            screen.playClick();
            return true;
        }
        int total = MenuSession.panels().size();
        if (total == 0) {
            return false;
        }
        int vis = MenuLayout.mainVisibleCount(screen.height, total);
        int cur = s.hoverMain >= 0 ? s.hoverMain : Math.max(0, s.selectedMain);
        cur = Mth.clamp(cur + dir * vis, 0, total - 1);
        s.ensureMainVisible(cur, screen.height);
        s.hoverMain = cur;
        s.focusCol = 0;
        s.focusEntryId = null;
        screen.playClick();
        return true;
    }

    static void beginClose(SAOMenuScreen screen, MenuSession s) {
        if (s.closing) {
            return;
        }
        s.beginClose();
        SAOMenuMovement.releaseKeys(Minecraft.getInstance());
        if (!SAOMapPanel.isPinned()) {
            if (SAOMapPanel.isShown()) {
                SAOMapPanel.toggle();
            }
        }
        screen.playAlert();
    }
}
