package com.sao.saomenu.client.menu;

import com.sao.saomenu.client.hud.HudLayoutEditor;
import com.sao.saomenu.client.hud.SAOMapPanel;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.client.input.SAOMenuMovement;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** 菜单键鼠、移动穿透、物品置顶拖拽。HUD 拖拽交给 {@link HudLayoutEditor}。 */
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
                int anchorA = MenuLayout.menuItemRectAt(screen.width, screen.height, itemsA.size(),
                        s.baseAnchorX, s.buttonY(mainA, screen.height), shownA).centerY();
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
                            }
                            return true;
                        }
                    }
                }
            }
        }

        int hitMain = MenuLayout.hoveredMainButtonAt(screen.width, screen.height, s.baseAnchorX, s.baseAnchorY,
                MenuSession.panels().size(), lx, ly);
        if (hitMain != -1) {
            s.mainTouched = true;
            if (s.selectedMain == hitMain) {
                s.selectedMain = -1;
                s.panelOwner = -1;
                s.expandedItem = -1;
                s.equipOwner = -1;
                s.actionMenuOpen = false;
                s.infoOpen = false;
                s.childScroll = 0;
                screen.playPanel();
            } else {
                s.selectedMain = hitMain;
                s.expandedItem = -1;
                s.equipOwner = -1;
                s.actionMenuOpen = false;
                s.infoOpen = false;
                s.childScroll = 0;
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
            int anchorY = s.buttonY(main, screen.height);
            for (int i = 0; i < items.size(); i++) {
                if (MenuLayout.menuItemRectAt(screen.width, screen.height, items.size(),
                        s.baseAnchorX, anchorY, i).contains(lx, ly)) {
                    s.itemPressColumn = 0;
                    s.itemPressIndex = i;
                    s.itemPressAt = MenuSession.now();
                    if (items.get(i).hasChildren()) {
                        int newExpanded = s.expandedItem == i ? -1 : i;
                        if (newExpanded != s.expandedItem) {
                            s.equipOwner = -1;
                            s.childScroll = 0;
                            s.actionMenuOpen = false;
                        }
                        s.expandedItem = newExpanded;
                        screen.playPanel();
                    } else {
                        s.activate(screen, items.get(i));
                    }
                    return true;
                }
            }
            int shown = s.visibleChildrenItem(items);
            if (shown >= 0 && items.get(shown).children() != null) {
                List<MenuEntry> children = s.windowedChildren(items.get(shown).children(), screen.height);
                int childAnchor = MenuLayout.menuItemRectAt(screen.width, screen.height, items.size(),
                        s.baseAnchorX, anchorY, shown).centerY();
                int equipTarget = s.equipTargetIndex(items, shown);
                if (equipTarget >= 0) {
                    int equipAnchor = s.equipAnchorY(items, shown, equipTarget, screen.height);
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
                            if (s.equipOwner != i) {
                                s.equipOwner = i;
                                s.equipAt = MenuSession.now();
                                screen.playPanel();
                            }
                        } else if (child.isItem()) {
                            if (s.actionMenuOpen && s.actionRow == i) {
                                s.actionMenuOpen = false;
                                screen.playPanel();
                            } else {
                                s.actionMenuOpen = true;
                                s.actionRow = i;
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
        int main = s.activeMain();
        List<MenuEntry> items = main >= 0 ? s.activeItems(main) : null;
        int shown = items != null ? s.visibleChildrenItem(items) : -1;
        if (items == null || shown < 0 || items.get(shown).children() == null
                || items.get(shown).children().isEmpty()
                || items.get(shown).children().get(0).stack() == null) {
            return false;
        }
        List<MenuEntry> children = items.get(shown).children();
        int rows = s.childVisibleRows(screen.height);
        xform.toLocal(mouseX, mouseY);
        int lx = xform.localXi();
        int ly = xform.localYi();
        int anchorY = MenuLayout.menuItemRectAt(screen.width, screen.height, items.size(),
                s.baseAnchorX, s.buttonY(main, screen.height), shown).centerY();
        boolean over = false;
        for (int v = 0; v < Math.min(rows, children.size()); v++) {
            if (MenuLayout.childItemRectAt(screen.width, screen.height, rows, s.baseAnchorX, anchorY, v)
                    .contains(lx, ly)) {
                over = true;
                break;
            }
        }
        if (!over) {
            return false;
        }
        int max = Math.max(0, children.size() - rows);
        int before = s.childScroll;
        s.childScroll = Mth.clamp(s.childScroll - (int) Math.signum(delta), 0, max);
        if (s.childScroll != before) {
            s.actionMenuOpen = false;
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
        return false;
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
