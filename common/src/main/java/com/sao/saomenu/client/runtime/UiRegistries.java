package com.sao.saomenu.client.runtime;

import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.world.WorldOverlay;

import java.util.List;

/** Startup-only mutation boundary, shared by builtin and addon contributions. */
public final class UiRegistries implements SaoUiRegistry {
    private enum Phase { NEW, REGISTERING, FROZEN }

    private static final UiRegistries INSTANCE = new UiRegistries();
    private final OrderedUiRegistry<SaoPanel> panels = new OrderedUiRegistry<>("menu");
    private final OrderedUiRegistry<HudElement> hud = new OrderedUiRegistry<>("HUD");
    private final OrderedUiRegistry<SettingsGroup> settings = new OrderedUiRegistry<>("settings");
    private final OrderedUiRegistry<ThemeDefinition> themes = new OrderedUiRegistry<>("theme");
    private final OrderedUiRegistry<WorldOverlay> world = new OrderedUiRegistry<>("world overlay");
    private final OrderedUiRegistry<SessionListener> sessions = new OrderedUiRegistry<>("session listener");
    private volatile Phase phase = Phase.NEW;
    private Thread owner;

    private UiRegistries() {
    }

    public static UiRegistries instance() {
        return INSTANCE;
    }

    public static void begin() {
        if (INSTANCE.phase != Phase.NEW) {
            throw new IllegalStateException("UI registration has already begun");
        }
        INSTANCE.owner = Thread.currentThread();
        INSTANCE.phase = Phase.REGISTERING;
    }

    public static void freeze() {
        INSTANCE.requireRegistrationThread();
        INSTANCE.panels.freeze();
        INSTANCE.hud.freeze();
        INSTANCE.settings.freeze();
        INSTANCE.themes.freeze();
        INSTANCE.world.freeze();
        INSTANCE.sessions.freeze();
        INSTANCE.phase = Phase.FROZEN;
    }

    public static boolean isReady() {
        return INSTANCE.phase == Phase.FROZEN;
    }

    private void requireRegistrationThread() {
        if (phase != Phase.REGISTERING) {
            throw new IllegalStateException("UI registration is only allowed during SaoUiRegisterEvent");
        }
        if (Thread.currentThread() != owner) {
            throw new IllegalStateException("UI registration must run synchronously on the client thread");
        }
    }

    @Override
    public void menu(SaoPanel panel) {
        requireRegistrationThread();
        panels.add(panel);
    }

    @Override
    public void hud(HudElement element) {
        requireRegistrationThread();
        hud.add(element);
    }

    @Override
    public void settings(SettingsGroup group) {
        requireRegistrationThread();
        settings.add(group);
    }

    @Override
    public void theme(ThemeDefinition theme) {
        requireRegistrationThread();
        themes.add(theme);
    }

    @Override
    public void world(WorldOverlay overlay) {
        requireRegistrationThread();
        world.add(overlay);
    }

    @Override
    public void session(SessionListener listener) {
        requireRegistrationThread();
        sessions.add(listener);
    }

    public List<SaoPanel> panels() {
        return panels.entries();
    }

    public List<HudElement> hudElements() {
        return hud.entries();
    }

    public List<SettingsGroup> settingsGroups() {
        return settings.entries();
    }

    public List<ThemeDefinition> themes() {
        return themes.entries();
    }

    public List<WorldOverlay> worldOverlays() {
        return world.entries();
    }

    public List<SessionListener> sessionListeners() {
        return sessions.entries();
    }
}
