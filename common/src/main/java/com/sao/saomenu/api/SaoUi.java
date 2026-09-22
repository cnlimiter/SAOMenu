package com.sao.saomenu.api;

import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.api.world.WorldOverlay;
import com.sao.saomenu.api.widget.SaoScreen;
import com.sao.saomenu.client.hud.SAONotification;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.runtime.SaoClientRuntime;
import com.sao.saomenu.client.runtime.UiRegistries;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.client.screen.SAOInviteScreen;
import com.sao.saomenu.config.SAOConfig;
import com.sao.saomenu.ui.text.SaoFonts;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
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
    private static final boolean SAFE_MODE = Boolean.getBoolean("saomenu.safeMode");

    private SaoUi() {
    }

    /** Effective visual switch. Startup safe mode cannot be overridden from a screen or addon. */
    public static boolean enabled() {
        return SAOConfig.frameworkEnabled() && !SAFE_MODE;
    }

    public static boolean safeMode() {
        return SAFE_MODE;
    }

    /** Persists the master switch and immediately restores native visuals and input ownership. */
    public static void setEnabled(boolean enabled) {
        Minecraft client = clientThread();
        if (enabled && SAFE_MODE) {
            throw new IllegalStateException("Restart without -Dsaomenu.safeMode=true to enable the UI framework");
        }
        SAOConfig.setFrameworkEnabled(enabled);
        SAOConfig.save();
        if (!enabled) {
            while (client.screen instanceof SaoScreen || client.screen instanceof SAOSettingsScreen) {
                if (client.screen instanceof SaoScreen owned) {
                    client.setScreen(owned.parent());
                } else {
                    client.screen.onClose();
                }
            }
            if (client.screen instanceof SAOMenuScreen || client.screen instanceof SAOInviteScreen) {
                // Recovery dismisses an invitation without sending accept/reject packets.
                client.setScreen(null);
            }
            SaoClientRuntime.resetUi(client);
        }
    }

    /** Theme-default body font; non-default Component font families remain intact. */
    public static Font bodyFont() {
        return SaoFonts.body();
    }

    /** Theme-default font for owned headings, without changing Minecraft's default font. */
    public static Font displayFont() {
        return SaoFonts.display();
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
        if (!enabled()) {
            throw new IllegalStateException("The SAO UI framework is disabled");
        }
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
