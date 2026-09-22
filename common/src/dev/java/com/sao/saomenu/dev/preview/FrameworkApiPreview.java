package com.sao.saomenu.dev.preview;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.hud.HudLayoutBinding;
import com.sao.saomenu.api.menu.MenuContext;
import com.sao.saomenu.api.menu.MenuHost;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.widget.SaoConfirmDialog;
import com.sao.saomenu.api.widget.SaoScrollPane;
import com.sao.saomenu.api.widget.SaoTextField;
import com.sao.saomenu.client.screen.settings.SAOSettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.CompletableFuture;

/** Native-screen regression scenarios against the independently compiled addon binary. Dev only. */
final class FrameworkApiPreview {
    private static final String DRAFT = "API O 笔记";
    private static Screen notebook;
    private static SaoTextField field;
    private static HudLayoutBinding badge;
    private static float anchorX;
    private static float anchorY;
    private static int dragX;
    private static int dragY;
    private static String renderedFieldText = "";
    private static Font retainedBodyFont;
    private static CompletableFuture<Void> reload;
    private static int reloadedTicks;

    private FrameworkApiPreview() {
    }

    static boolean tick(Minecraft client, String out, int tick) {
        if (tick >= 148) {
            if (!reload.isDone() || client.getOverlay() != null) return false;
            reload.join();
            if (++reloadedTicks < 8) return false;
            require(client.screen instanceof BoundaryScreen, "Resource reload replaced the owned API screen");
            SAOMenuPreview.grab(client, out, "api_boundaries_reloaded.png");
            SAOMenu.LOGGER.info("[SAOMenu] API native checks passed: binary registration, rendered Unicode input, nested Tab/scroll, modal cancel/confirm, resize/remount, HUD cancel/save, 13 panels/40 rows with stable identity, 13 settings groups with isolated save/focus, tiny scroll viewports and retained-font rendering after reload");
            return true;
        }
        switch (tick) {
            case 0 -> {
                retainedBodyFont = SaoUi.bodyFont();
                // Missing subscribers during loader setup used to silently lose every addon contribution.
                SaoPanel panel = SaoUi.panels().stream().filter(p -> id("notebook").equals(p.id()))
                        .findFirst().orElseThrow(() -> new IllegalStateException("Addon registration was not dispatched"));
                badge = SaoUi.hudElements().stream().filter(h -> id("badge").equals(h.id()))
                        .findFirst().orElseThrow().layout();
                SaoUi.openMenu();
                ((MenuHost) client.screen).selectPanel(panel.id());
            }
            case 8 -> SAOMenuPreview.grab(client, out, "api_menu.png");
            case 10 -> {
                SaoPanel panel = SaoUi.panels().stream().filter(p -> id("notebook").equals(p.id())).findFirst().orElseThrow();
                panel.items().get().stream().filter(e -> id("edit").equals(e.id())).findFirst().orElseThrow()
                        .onActivate().accept(MenuContext.of((MenuHost) client.screen));
                notebook = client.screen;
                field = notebook.children().stream().filter(SaoTextField.class::isInstance)
                        .map(SaoTextField.class::cast).findFirst().orElseThrow();
                field.setValue("");
                notebook.setFocused(field);
                for (char c : DRAFT.toCharArray()) notebook.charTyped(c, 0);
                require(DRAFT.equals(field.getValue()), "Native screen did not route Unicode/O input");
            }
            case 14 -> SAOMenuPreview.grab(client, out, "api_form.png");
            case 16 -> {
                for (int i = 0; i < 16; i++) notebook.keyPressed(GLFW.GLFW_KEY_TAB, 0, 0);
                SaoScrollPane pane = notebook.children().stream().filter(SaoScrollPane.class::isInstance)
                        .map(SaoScrollPane.class::cast).findFirst().orElseThrow();
                AbstractWidget last = (AbstractWidget) pane.children().get(pane.children().size() - 1);
                require(pane.getFocused() == last, "Tab navigation did not reach the last nested control");
                require(last.getY() >= pane.getY() && last.getY() + last.getHeight() <= pane.getY() + pane.getHeight(),
                        "Keyboard-focused control remained outside the scroll viewport");
            }
            case 18 -> SAOMenuPreview.grab(client, out, "api_scroll_focus.png");
            case 20 -> {
                notebook.setFocused(field);
                field.setCursorPosition(2);
                field.setHighlightPos(5);
                click(notebook, "Clear draft");
                require(client.screen instanceof SaoConfirmDialog, "Confirmation did not open as a native layer");
            }
            case 24 -> SAOMenuPreview.grab(client, out, "api_dialog.png");
            case 26 -> {
                click(client.screen, CommonComponents.GUI_CANCEL.getString());
                require(client.screen == notebook && DRAFT.equals(field.getValue()), "Cancel lost the parent draft");
                require("I O".equals(field.getHighlighted()), "Dialog remounted the parent and lost native selection");
                client.options.guiScale().set(2);
                client.resizeDisplay();
                require(DRAFT.equals(field.getValue()) && "I O".equals(field.getHighlighted()), "Resize lost native edit state");
            }
            case 30 -> SAOMenuPreview.grab(client, out, "api_form_scale2.png");
            case 32 -> {
                click(notebook, "Clear draft");
                click(client.screen, CommonComponents.GUI_PROCEED.getString());
                require(client.screen == notebook && field.getValue().isEmpty(), "Confirm did not clear the draft");
                field.setValue(DRAFT);
                click(notebook, "Save");
                client.setScreen(null);
                client.setScreen(notebook);
                field = notebook.children().stream().filter(SaoTextField.class::isInstance)
                        .map(SaoTextField.class::cast).findFirst().orElseThrow();
                require(DRAFT.equals(field.getValue()), "Reopening the screen lost its draft model");
                renderedFieldText = "";
                field.setFormatter((text, index) -> {
                    renderedFieldText = text;
                    return net.minecraft.util.FormattedCharSequence.forward(text, net.minecraft.network.chat.Style.EMPTY);
                });
            }
            case 36 -> {
                require(DRAFT.equals(renderedFieldText), "Remounted field contains a value but renders an empty viewport");
                SAOMenuPreview.grab(client, out, "api_remount.png");
            }
            case 38 -> {
                client.options.guiScale().set(1);
                client.resizeDisplay();
                SaoUi.selectTheme(id("cyan"));
                SaoUi.openMenu();
                ((MenuHost) client.screen).selectPanel(id("notebook"));
            }
            case 48 -> {
                anchorX = badge.anchorX();
                anchorY = badge.anchorY();
                var box = badge.box(client, client.screen.width, client.screen.height);
                dragX = box.x() + 8;
                dragY = box.y() + 8;
                require(client.screen.mouseClicked(dragX, dragY, 0), "Addon HUD was not draggable");
                client.screen.mouseMoved(dragX - 80, dragY + 35);
                require(badge.anchorX() < anchorX, "Addon HUD did not follow the pointer");
                client.screen.mouseReleased(dragX - 80, dragY + 35, 1);
                client.setScreen(null);
                require(badge.anchorX() == anchorX && badge.anchorY() == anchorY,
                        "Wrong-button release or screen removal committed a pending drag");
                SaoUi.openMenu();
            }
            case 56 -> {
                var box = badge.box(client, client.screen.width, client.screen.height);
                dragX = box.x() + 8;
                dragY = box.y() + 8;
                client.screen.mouseClicked(dragX, dragY, 0);
                client.screen.mouseMoved(dragX - 60, dragY + 30);
                client.screen.mouseReleased(dragX - 60, dragY + 30, 0);
                require(badge.anchorX() < anchorX, "Left release did not retain the addon HUD anchor");
                ((MenuHost) client.screen).selectPanel(id("notebook"));
            }
            case 62 -> SAOMenuPreview.grab(client, out, "api_cyan_hud.png");
            case 64 -> ((MenuHost) client.screen).selectPanel(FrameworkApiFixtures.id("panel_7"));
            case 72 -> {
                for (int i = 0; i < 39; i++) client.screen.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
                client.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                require(FrameworkApiFixtures.activatedRow == 39, "Long menu could not activate its last row");
            }
            case 76 -> SAOMenuPreview.grab(client, out, "api_menu_overflow.png");
            case 78 -> FrameworkApiFixtures.reverseRows();
            case 82 -> {
                FrameworkApiFixtures.activatedRow = -1;
                client.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
                require(FrameworkApiFixtures.activatedRow == 39, "Dynamic row reorder changed selection identity");
                SAOMenuPreview.grab(client, out, "api_menu_reordered.png");
            }
            case 84 -> {
                SaoUi.openSettings(client.screen);
                ((SAOSettingsScreen) client.screen).debugShowPage("ROOT");
                for (int i = 0; i < SaoUi.settingsGroups().size(); i++) {
                    client.screen.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
                }
            }
            case 88 -> {
                SAOMenuPreview.grab(client, out, "api_settings_groups.png");
                client.screen.keyPressed(GLFW.GLFW_KEY_ENTER, 0, 0);
            }
            case 108 -> {
                field = client.screen.children().stream().filter(SaoTextField.class::isInstance)
                        .map(SaoTextField.class::cast).filter(box -> box.visible).findFirst().orElseThrow();
                require("Group 7".equals(field.getValue()), "Keyboard navigation did not reach the last settings group");
                client.screen.setFocused(field);
                field.setValue("Owned draft");
                client.screen.keyPressed(GLFW.GLFW_KEY_O, 0, 0);
                require(client.screen instanceof SAOSettingsScreen, "O closed a focused native setting");
                client.screen.charTyped('O', 0);
                require("Owned draftO".equals(field.getValue()), "Focused setting did not accept O");
            }
            case 112 -> {
                SAOMenuPreview.grab(client, out, "api_settings_native.png");
                client.screen.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
                client.screen.charTyped('X', 0);
                require("Owned draftO".equals(field.getValue()), "Hidden setting retained keyboard focus");
                require("Owned draftO".equals(FrameworkApiFixtures.saved[7])
                                && "Group 6".equals(FrameworkApiFixtures.saved[6]),
                        "Settings save crossed group ownership");
            }
            case 132 -> {
                ((SAOSettingsScreen) client.screen).debugShowPage("THEME");
                client.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
                client.screen.keyPressed(GLFW.GLFW_KEY_DOWN, 0, 0);
                for (int i = 0; i < SaoUi.themes().size(); i++) {
                    client.screen.keyPressed(GLFW.GLFW_KEY_RIGHT, 0, 0);
                }
                require(FrameworkApiFixtures.id("contrast").toString().equals(
                        com.sao.saomenu.config.SAOConfig.themeId()), "Theme overflow control could not reach its last choice");
            }
            case 136 -> SAOMenuPreview.grab(client, out, "api_theme_overflow.png");
            case 140 -> {
                SaoUi.selectTheme(id("cyan"));
                client.setScreen(new BoundaryScreen());
            }
            case 144 -> SAOMenuPreview.grab(client, out, "api_boundaries.png");
            case 146 -> reload = client.reloadResourcePacks();
            default -> { }
        }
        return false;
    }

    private static final class BoundaryScreen extends Screen {
        private static final String SAMPLE = "Retained font 012345";

        private BoundaryScreen() {
            super(Component.literal("API boundary verification"));
        }

        @Override
        protected void init() {
            addPane(20, 1, 4);
            addPane(40, 5, 2);
            addPane(60, 5, 4);
            addPane(80, 5, 20);
        }

        private void addPane(int x, int width, int height) {
            SaoScrollPane pane = addRenderableWidget(
                    new SaoScrollPane(x, 100, width, height, Component.empty()));
            pane.setContentHeight(80);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, width, height, 0xFF19212B);
            graphics.drawString(retainedBodyFont, SAMPLE, 40, 40, 0xFFFFFFFF, false);
            graphics.drawString(SaoUi.bodyFont(), SAMPLE, 40, 60, 0xFFFFFFFF, false);
            super.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private static ResourceLocation id(String path) {
        return new ResourceLocation("saomenu_showcase", path);
    }

    private static void click(Screen screen, String label) {
        Button button = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(b -> b.getMessage().getString().equals(label)).findFirst()
                .orElseThrow(() -> new IllegalStateException("Missing native action: " + label));
        double x = button.getX() + button.getWidth() / 2.0;
        double y = button.getY() + button.getHeight() / 2.0;
        require(screen.mouseClicked(x, y, 0), "Native action was not clickable: " + label);
        screen.mouseReleased(x, y, 0);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
