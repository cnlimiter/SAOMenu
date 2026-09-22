package com.sao.saomenu.client.hud;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * 通知数据层回归:容量上限、过期清理、Component 样式。
 */
class SAONotificationTest {

    @AfterEach
    void cleanup() {
        SAONotification.clear();
    }

    @Test
    void queueIsCappedAtMaxEntries() {
        for (int i = 0; i < 6; i++) {
            SAONotification.push(Component.literal("t" + i), Component.literal("m" + i));
        }
        assertEquals(4, SAONotification.size(), "最多保留 4 条通知");
    }

    @Test
    void pruneRemovesExpiredOnly() {
        SAONotification.push(Component.literal("fresh"), Component.empty());
        long now = net.minecraft.Util.getMillis();
        SAONotification.prune(now);
        assertEquals(1, SAONotification.size(), "新通知不应被清理");
        // 停留 2.6s + 淡出 0.3s 后应被移除
        SAONotification.prune(now + 4000);
        assertEquals(0, SAONotification.size(), "过期通知应被移除");
    }

    @Test
    void clearEmptiesQueue() {
        SAONotification.push(Component.literal("a"), Component.empty());
        SAONotification.push(Component.literal("b"), Component.empty());
        SAONotification.clear();
        assertEquals(0, SAONotification.size());
    }

    @Test
    void styledComponentIsPreserved() {
        Component title = Component.literal("Hi").withStyle(ChatFormatting.GOLD);
        Component message = Component.literal("body").withStyle(ChatFormatting.DARK_AQUA);
        SAONotification.push(title, message);
        assertSame(title, SAONotification.at(0).title());
        assertSame(message, SAONotification.at(0).message());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.GOLD),
                SAONotification.at(0).title().getStyle().getColor());
        assertEquals(TextColor.fromLegacyFormat(ChatFormatting.DARK_AQUA),
                SAONotification.at(0).message().getStyle().getColor());
        assertEquals("Hi", SAONotification.at(0).title().getString());
        assertEquals("body", SAONotification.at(0).message().getString());
    }

    @Test
    void missingIconStaysNull() {
        SAONotification.push(Component.literal("plain"), Component.empty());
        assertNull(SAONotification.at(0).icon());
        SAONotification.clear();
        SAONotification.push(Component.literal("n"), Component.empty(), null);
        assertNull(SAONotification.at(0).icon());
    }
}
