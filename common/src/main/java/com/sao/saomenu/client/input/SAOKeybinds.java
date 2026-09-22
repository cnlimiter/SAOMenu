package com.sao.saomenu.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import com.sao.saomenu.client.skill.SaoSkill;
import com.sao.saomenu.client.skill.SaoSkillRegistry;
import com.sao.saomenu.client.skill.SaoSkills;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/** Key mappings and input dispatch only; client initialization belongs to SaoClientRuntime. */
public final class SAOKeybinds {
    public static final String CATEGORY = "key.categories." + SAOMenu.MOD_ID;

    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key." + SAOMenu.MOD_ID + ".open", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O, CATEGORY);

    /** Available both in-game and inside screens; repeat presses are latched until release. */
    public static final KeyMapping TOGGLE_UI = new KeyMapping(
            "key." + SAOMenu.MOD_ID + ".toggle_ui", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F8, CATEGORY);

    private static boolean recoveryHeld;

    /** Slot order matches the client skill registry; unmapped slots never fire. */
    public static final KeyMapping[] SKILL_KEYS = new KeyMapping[SaoSkills.HOTKEY_SLOTS];

    static {
        for (int i = 0; i < SKILL_KEYS.length; i++) {
            SKILL_KEYS[i] = new KeyMapping("key." + SAOMenu.MOD_ID + ".skill_slot" + (i + 1),
                    InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, CATEGORY);
        }
    }

    private SAOKeybinds() {
    }

    public static void tick(Minecraft client) {
        if (!client.isWindowActive()) {
            recoveryHeld = false;
        }
        while (TOGGLE_UI.consumeClick()) {
            // The platform handles this global action immediately, including while a GUI is open.
        }
        List<SaoSkill> skills = SaoSkillRegistry.skills();
        for (int i = 0; i < SKILL_KEYS.length; i++) {
            if (SKILL_KEYS[i].consumeClick() && client.player != null && i < skills.size()) {
                SaoSkills.activate(client.player, skills.get(i));
            }
        }
        while (OPEN_MENU.consumeClick()) {
            if (client.player == null || !SaoUi.enabled()) {
                continue;
            }
            if (client.screen == null) {
                client.setScreen(new SAOMenuScreen());
            } else if (client.screen instanceof SAOMenuScreen
                    || client.screen instanceof SAOSettingsScreen) {
                client.screen.onClose();
            }
        }
    }

    public static void toggleUiPressed() {
        if (recoveryHeld) {
            return;
        }
        recoveryHeld = true;
        if (!SaoUi.safeMode()) {
            SaoUi.setEnabled(!SaoUi.enabled());
        }
    }

    public static void releaseUiToggle() {
        recoveryHeld = false;
    }

    /** Discard held and queued mod actions before another world can receive them. */
    public static void reset() {
        release(OPEN_MENU);
        for (KeyMapping key : SKILL_KEYS) {
            release(key);
        }
    }

    private static void release(KeyMapping key) {
        key.setDown(false);
        while (key.consumeClick()) {
            // Consume queued input; it belongs to the session that just ended.
        }
    }
}
