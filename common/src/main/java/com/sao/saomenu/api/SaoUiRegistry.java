package com.sao.saomenu.api;

import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.lifecycle.SessionListener;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.world.WorldOverlay;

/**
 * Registration window supplied by the client-only Forge {@code SaoUiRegisterEvent}.
 * Use it synchronously on the client thread. Registrations freeze when the event returns;
 * retaining this object does not permit later mutation. Duplicate IDs throw rather than replace.
 * Builtin contributions use this same interface before the event is posted.
 */
public interface SaoUiRegistry {
    void menu(SaoPanel panel);

    void hud(HudElement element);

    void settings(SettingsGroup group);

    void theme(ThemeDefinition theme);

    void world(WorldOverlay overlay);

    void session(SessionListener listener);
}
