package com.sao.saomenu.client.hud;

import com.sao.saomenu.api.hud.HudBox;
import com.sao.saomenu.api.hud.HudElement;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.hud.HudPass;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Layout editor: registered bindings, hidden cannot drag, left-release save,
 * cancel/disconnect rollback, wrong-button release does not end a left drag.
 * Coordinates are GUI pixels.
 */
class HudLayoutEditorTest {

    private final float[] pos = {0.25f, 0.40f};
    private final boolean[] saved = {false};
    private boolean visible = true;
    private HudLayoutEditor editor;

    @AfterEach
    void cleanup() {
        if (editor != null) {
            editor.cancel();
        }
        HudLayoutEditor.dropUnsaved();
    }

    @Test
    void hiddenElementCannotStartDrag() {
        visible = false;
        editor = newEditor();
        assertFalse(editor.mouseClicked(null, 100, 80, 15, 15, 0));
        editor.mouseMoved(null, 100, 80, 50, 50);
        assertEquals(0.25f, pos[0]);
        assertEquals(0.40f, pos[1]);
        assertFalse(saved[0]);
    }

    @Test
    void clickOutsideBoxDoesNotDrag() {
        editor = newEditor();
        assertFalse(editor.mouseClicked(null, 100, 80, 90, 70, 0));
        editor.mouseMoved(null, 100, 80, 50, 50);
        assertEquals(0.25f, pos[0]);
    }

    @Test
    void leftReleaseAfterMoveSavesAndKeepsAnchor() {
        editor = newEditor();
        assertTrue(editor.mouseClicked(null, 100, 80, 15, 15, 0));
        editor.mouseMoved(null, 100, 80, 50, 40);
        assertTrue(editor.mouseReleased(null, 100, 80, 50, 40, 0));
        assertTrue(saved[0]);
        float x = pos[0];
        float y = pos[1];
        assertTrue(x != 0.25f || y != 0.40f, "拖动应改锚点");
        editor.mouseMoved(null, 100, 80, 8, 8);
        assertEquals(x, pos[0]);
        assertEquals(y, pos[1]);
    }

    @Test
    void wrongButtonReleaseDoesNotEndLeftDrag() {
        editor = newEditor();
        assertTrue(editor.mouseClicked(null, 100, 80, 15, 15, 0));
        editor.mouseMoved(null, 100, 80, 50, 40);
        float afterLeftMoveX = pos[0];
        float afterLeftMoveY = pos[1];
        assertFalse(editor.mouseReleased(null, 100, 80, 50, 40, 1), "右键松开不得结束左键拖动");
        assertFalse(saved[0]);
        editor.mouseMoved(null, 100, 80, 70, 55);
        assertTrue(pos[0] != afterLeftMoveX || pos[1] != afterLeftMoveY, "右键松开后左键拖动应继续");
        assertTrue(editor.mouseReleased(null, 100, 80, 70, 55, 0));
        assertTrue(saved[0]);
    }

    @Test
    void cancelRestoresPressAnchorWithoutSave() {
        editor = newEditor();
        assertTrue(editor.mouseClicked(null, 100, 80, 15, 15, 0));
        editor.mouseMoved(null, 100, 80, 50, 40);
        editor.cancel();
        assertEquals(0.25f, pos[0], 1.0e-6f);
        assertEquals(0.40f, pos[1], 1.0e-6f);
        assertFalse(saved[0]);
    }

    @Test
    void disconnectDropsUnsavedEdit() {
        editor = newEditor();
        assertTrue(editor.mouseClicked(null, 100, 80, 15, 15, 0));
        editor.mouseMoved(null, 100, 80, 50, 40);
        HudLayoutEditor.dropUnsaved();
        assertEquals(0.25f, pos[0], 1.0e-6f);
        assertEquals(0.40f, pos[1], 1.0e-6f);
        assertFalse(saved[0]);
    }

    @Test
    void grabUsesGuiPixelBox() {
        editor = newEditor();
        assertTrue(editor.mouseClicked(null, 200, 100, 20, 20, 0));
        // box (10,10,20,20), grab = (10/20, 10/20) = 0.5
        // move mouse to (10,10) keeps origin: (10 - 0.5*20) / (200-20) = 0 / 180 = 0
        editor.mouseMoved(null, 200, 100, 10, 10);
        assertEquals(0f, pos[0], 1.0e-5f);
        assertEquals(0f, pos[1], 1.0e-5f);
    }

    private HudLayoutEditor newEditor() {
        HudLayoutBinding binding = HudLayoutBinding.screenAnchor(
                50,
                (mc, w, h) -> new HudBox(10, 10, 20, 20),
                () -> pos[0],
                () -> pos[1],
                (x, y) -> {
                    pos[0] = x;
                    pos[1] = y;
                },
                () -> saved[0] = true);
        HudElement element = HudElement.builder(
                        new ResourceLocation("saomenu", "test_hud"),
                        100,
                        Component.literal("Test"),
                        ctx -> { })
                .passes(HudPass.MENU_OVERLAY)
                .visible(ctx -> visible)
                .layout(binding)
                .build();
        return new HudLayoutEditor(List.of(element));
    }
}
