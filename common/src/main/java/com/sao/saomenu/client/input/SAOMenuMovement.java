package com.sao.saomenu.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import com.sao.saomenu.client.menu.SAOMenuScreen;
import com.sao.saomenu.config.SAOConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;

import java.util.Arrays;

/** Movement polling, binding cache and owned sprint state for the non-pausing world menu. */
public final class SAOMenuMovement {
    private static final int[] CODES = new int[7];
    private static final String[] BINDINGS = new String[CODES.length];
    private static KeyMapping[] movementKeys;
    private static boolean sprintForced;

    private SAOMenuMovement() {
    }

    private static KeyMapping[] keys(Minecraft mc) {
        if (movementKeys == null) {
            var options = mc.options;
            movementKeys = new KeyMapping[]{options.keyUp, options.keyDown, options.keyLeft,
                    options.keyRight, options.keyJump, options.keyShift, options.keySprint};
        }
        return movementKeys;
    }

    private static void resolveCodes(KeyMapping[] keys) {
        for (int i = 0; i < keys.length; i++) {
            String binding = keys[i].saveString();
            if (!binding.equals(BINDINGS[i])) {
                CODES[i] = 0;
                for (int code = 32; code <= 348; code++) {
                    if (keys[i].matches(code, 0)) {
                        CODES[i] = code;
                        break;
                    }
                }
                BINDINGS[i] = binding;
            }
        }
    }

    /** Menu rendering and keyboard ticks share one binding snapshot, not two stale caches. */
    public static void pollKeys(Minecraft mc, boolean blocked) {
        if (blocked) {
            releaseKeys(mc);
            return;
        }
        KeyMapping[] keys = keys(mc);
        resolveCodes(keys);
        long window = mc.getWindow().getWindow();
        for (int i = 0; i < keys.length; i++) {
            keys[i].setDown(CODES[i] != 0 && InputConstants.isKeyDown(window, CODES[i]));
        }
    }

    public static boolean setKeyState(Minecraft mc, int keyCode, int scanCode, boolean down) {
        for (KeyMapping key : keys(mc)) {
            if (key.matches(keyCode, scanCode)) {
                key.setDown(down);
                return true;
            }
        }
        return false;
    }

    public static void releaseKeys(Minecraft mc) {
        for (KeyMapping key : keys(mc)) {
            key.setDown(false);
        }
    }

    /** Called after vanilla KeyboardInput.tick while the SAO menu is open. */
    public static void apply(Input input) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof SAOMenuScreen screen) || mc.player == null) {
            return;
        }
        pollKeys(mc, screen.isMovementBlocked());
        KeyMapping[] keys = keys(mc);
        input.up = keys[0].isDown();
        input.down = keys[1].isDown();
        input.left = keys[2].isDown();
        input.right = keys[3].isDown();
        input.jumping = keys[4].isDown();
        input.shiftKeyDown = keys[5].isDown();
        input.forwardImpulse = (input.up ? 1f : 0f) - (input.down ? 1f : 0f);
        input.leftImpulse = (input.left ? 1f : 0f) - (input.right ? 1f : 0f);
    }

    /** Reset only state owned by this controller when leaving a session. */
    public static void reset(Minecraft mc) {
        if (movementKeys != null) {
            releaseKeys(mc);
            movementKeys = null;
        }
        Arrays.fill(CODES, 0);
        Arrays.fill(BINDINGS, null);
        if (sprintForced) {
            mc.options.keySprint.setDown(false);
            sprintForced = false;
        }
    }

    /** Let vanilla enforce hunger, collision and movement restrictions on sprinting. */
    public static void autoSprint() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        KeyMapping sprint = mc.options.keySprint;
        boolean want = SAOConfig.autoSprint() && mc.options.keyUp.isDown();
        if (want) {
            if (!sprint.isDown()) {
                sprint.setDown(true);
                sprintForced = true;
            }
        } else if (sprintForced) {
            sprint.setDown(false);
            sprintForced = false;
        }
    }
}
