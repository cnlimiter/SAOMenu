package com.sao.saomenu.api.hud;

/**
 * GUI HUD compose pass. Coordinates are screen GUI pixels, not menu-local space.
 *
 * <p>Each element is invoked at most once per pass. WORLD is skipped while the SAO menu
 * screen is open so a WORLD+overlay widget is not drawn twice.
 */
public enum HudPass {
    /** In-world HUD (platform GUI hook, menu closed). */
    WORLD,
    /** Drawn under the menu columns (hotbar dots). */
    MENU_UNDERLAY,
    /** Drawn over the menu columns (plates, clock, skills, menu map). */
    MENU_OVERLAY,
    /** Platform HUD tail, before screens, including while the SAO menu is open. */
    GAME_OVERLAY
}
