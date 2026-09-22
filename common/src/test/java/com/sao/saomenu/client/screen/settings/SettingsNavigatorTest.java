package com.sao.saomenu.client.screen.settings;

import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.settings.ToggleSetting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Long group/option lists stay inside a bounded viewport; focus follows scroll. */
class SettingsNavigatorTest {

    @Test
    void groupScrollIsClampedToViewport() {
        SettingsNavigator nav = new SettingsNavigator();
        nav.sync(groups(9));
        nav.setVisibleGroups(4);
        nav.focusGroup(0);
        assertFalse(nav.scrollGroups(-1));
        assertTrue(nav.scrollGroups(1));
        assertEquals(1, nav.groupScroll());
        nav.scrollGroups(100);
        assertEquals(5, nav.groupScroll());
        assertEquals(5, SettingsLayout.clampScroll(99, 9, 4));
    }

    @Test
    void firstGroupMoveFromUnfocusedSelectsEdge() {
        SettingsNavigator nav = new SettingsNavigator();
        nav.sync(groups(4));
        assertTrue(nav.moveGroup(1));
        assertEquals(0, nav.groupIndex());
        nav = new SettingsNavigator();
        nav.sync(groups(4));
        assertTrue(nav.moveGroup(-1));
        assertEquals(3, nav.groupIndex());
    }

    @Test
    void enteringGroupResetsOptionScroll() {
        SettingsNavigator nav = new SettingsNavigator();
        nav.sync(groups(6));
        nav.setVisibleGroups(4);
        nav.setVisibleOptions(3);
        nav.enterGroup(5);
        assertFalse(nav.isRoot());
        assertEquals(5, nav.groupIndex());
        assertEquals(2, nav.groupScroll());
        assertEquals(0, nav.optionIndex());
        assertEquals(0, nav.optionScroll());
    }

    @Test
    void optionFocusKeepsRowInsideViewport() {
        SettingsNavigator nav = new SettingsNavigator();
        nav.sync(List.of(group("g", 20)));
        nav.setVisibleOptions(5);
        nav.enterGroup(0);
        assertTrue(nav.moveOption(1));
        nav.focusOption(17);
        assertEquals(17, nav.optionIndex());
        assertEquals(13, nav.optionScroll());
        assertTrue(nav.moveOption(10));
        assertEquals(19, nav.optionIndex());
        assertEquals(15, nav.optionScroll());
        nav.scrollOptions(-2);
        assertEquals(13, nav.optionScroll());
        assertEquals(17, nav.optionIndex());
    }

    @Test
    void rowHeightStaysBounded() {
        assertEquals(16, SettingsLayout.rowH(16, 40));
        assertEquals(34, SettingsLayout.rowH(400, 2));
        assertEquals(4, SettingsLayout.visibleRows(64, 40));
        assertEquals(3, SettingsLayout.visibleRows(64, 3));
    }

    @Test
    void nineGroupsNeverPaintPastFourVisibleSlots() {
        assertEquals(4, SettingsLayout.visibleSlots(9, 0, 4));
        assertEquals(4, SettingsLayout.visibleSlots(9, 5, 4));
        assertEquals(4, SettingsLayout.visibleSlots(5, 1, 4));
        assertEquals(3, SettingsLayout.visibleSlots(3, 0, 4));
        assertTrue(SettingsLayout.indexInViewport(8, 5, 4, 9));
        assertFalse(SettingsLayout.indexInViewport(4, 5, 4, 9));
        assertFalse(SettingsLayout.indexInViewport(9, 5, 4, 9));
    }

    @Test
    void optionRowsStayInsideFooterBoundedViewport() {
        int rowsTop = 80;
        int footerTop = 220;
        int avail = SettingsLayout.optionViewport(rowsTop, footerTop);
        assertEquals(140, avail);
        int count = 20;
        int vis = SettingsLayout.visibleRows(avail, count);
        int rowH = SettingsLayout.rowH(avail, count);
        assertTrue(vis * rowH <= avail);
        assertTrue(SettingsLayout.indexInViewport(vis - 1, 0, vis, count));
        assertFalse(SettingsLayout.indexInViewport(vis, 0, vis, count));
    }

    @Test
    void keyboardFocusScrollsOffscreenOptionIntoView() {
        SettingsNavigator nav = new SettingsNavigator();
        nav.sync(List.of(group("g", 12)));
        nav.setVisibleOptions(4);
        nav.enterGroup(0);
        assertTrue(nav.moveOption(11));
        assertEquals(11, nav.optionIndex());
        assertEquals(8, nav.optionScroll());
        assertTrue(SettingsLayout.indexInViewport(
                nav.optionIndex(), nav.optionScroll(), nav.visibleOptions(), 12));
    }

    private static List<SettingsGroup> groups(int n) {
        List<SettingsGroup> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.add(group("g" + i, 2));
        }
        return list;
    }

    private static SettingsGroup group(String path, int options) {
        List<ToggleSetting> rows = new ArrayList<>();
        for (int i = 0; i < options; i++) {
            rows.add(new ToggleSetting(new ResourceLocation("test", path + "_" + i),
                    Component.literal(path + i), () -> false, v -> {}));
        }
        return new SettingsGroup(new ResourceLocation("test", path), 100,
                Component.literal(path), rows, () -> {}, () -> {});
    }
}
