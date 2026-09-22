package com.sao.saomenu.api;

import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.api.world.WorldOverlay;
import com.sao.saomenu.client.hud.SAONotification;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.runtime.UiRegistries;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Objects;

/**
 * Client-only UI access after the registration event has returned. Never load this class from
 * dedicated-server initialization. Mutation and rendering happen on the Minecraft client thread.
 * Registry lists are immutable, ordered snapshots; contributions are not copied each frame.
 */
public final class SaoUi {
    private SaoUi() {
    }

    public static List<SaoPanel> panels() {
        return UiRegistries.instance().panels();
    }

    public static List<HudElement> hudElements() {
        return UiRegistries.instance().hudElements();
    }

    public static List<SettingsGroup> settingsGroups() {
        return UiRegistries.instance().settingsGroups();
    }

    /** Registered theme definitions after deliberate user-file overlays are applied. */
    public static List<ThemeDefinition> themes() {
        return SaoTheme.definitions();
    }

    public static List<WorldOverlay> worldOverlays() {
        return UiRegistries.instance().worldOverlays();
    }

    /** Current palette, font resources and motion durations, cached until theme state changes. */
    public static ThemeTokens theme() {
        return SaoTheme.tokens();
    }

    /** Opens the in-world menu. A menu without a player/level has no valid gameplay context. */
    public static void openMenu() {
        Minecraft client = clientThread();
        if (client.player == null || client.level == null) {
            throw new IllegalStateException("The SAO menu requires an active client world");
        }
        client.setScreen(new SAOMenuScreen());
    }

    public static void openSettings(Screen parent) {
        clientThread().setScreen(new SAOSettingsScreen(parent));
    }

    public static void notify(Component title, Component message) {
        clientThread();
        SAONotification.push(Objects.requireNonNull(title, "title"), Objects.requireNonNull(message, "message"));
    }

    /** Icon snapshots and Component styling are preserved by the notification queue. */
    public static void notify(Component title, Component message, ItemStack icon) {
        clientThread();
        SAONotification.push(Objects.requireNonNull(title, "title"), Objects.requireNonNull(message, "message"),
                Objects.requireNonNull(icon, "icon"));
    }

    /** Selects in-memory theme and default hue. The owning settings screen controls persistence. */
    public static void selectTheme(ResourceLocation id) {
        clientThread();
        Objects.requireNonNull(id, "id");
        for (ThemeDefinition theme : themes()) {
            if (theme.id().equals(id)) {
                SaoTheme.select(id.toString());
                return;
            }
        }
        throw new IllegalArgumentException("Unknown theme: " + id);
    }

    private static Minecraft clientThread() {
        Minecraft client = Minecraft.getInstance();
        if (!client.isSameThread()) {
            throw new IllegalStateException("UI operations must run on the Minecraft client thread");
        }
        return client;
    }
}
