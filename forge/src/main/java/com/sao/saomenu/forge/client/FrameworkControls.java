package com.sao.saomenu.forge.client;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.widget.SaoButton;
import net.minecraft.client.Minecraft;
import com.sao.saomenu.client.input.SAOKeybinds;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.controls.KeyBindsScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Recovery remains reachable while native reskins and HUD replacements are disabled. */
@Mod.EventBusSubscriber(modid = SAOMenu.MOD_ID, value = Dist.CLIENT)
public final class FrameworkControls {
    private static final Component ENABLE = Component.translatable("saomenu.recovery.enable");
    private static final Component DISABLE = Component.translatable("saomenu.recovery.disable");
    private static final Component SAFE = Component.translatable("saomenu.recovery.safe");

    private FrameworkControls() {
    }

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen.getClass() != TitleScreen.class && screen.getClass() != PauseScreen.class) {
            return;
        }
        int width = Math.min(136, Math.max(60, (screen.width - 24) / 2));
        SaoButton toggle = new SaoButton(8, 8, width, 20, label(),
                button -> SaoUi.setEnabled(!SaoUi.enabled())) {
            @Override
            protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
                setMessage(label());
                super.renderWidget(graphics, mouseX, mouseY, partialTick);
            }
        };
        toggle.active = !SaoUi.safeMode();
        toggle.setTooltip(Tooltip.create(Component.translatable(SaoUi.safeMode()
                ? "saomenu.recovery.safe_hint" : "saomenu.recovery.hint", SAOKeybinds.TOGGLE_UI.getTranslatedKeyMessage())));
        event.addListener(toggle);
        SaoButton settings = new SaoButton(width + 14, 8, Math.min(88, width), 20,
                Component.translatable("saomenu.settings.title"), button -> SaoUi.openSettings(screen));
        settings.active = !SaoUi.safeMode();
        event.addListener(settings);
    }

    private static Component label() {
        return SaoUi.safeMode() ? SAFE : SaoUi.enabled() ? DISABLE : ENABLE;
    }

    @SubscribeEvent
    public static void onScreenKey(ScreenEvent.KeyPressed.Pre event) {
        // Rebinding F8 must assign a key, not execute the old or new binding.
        if (event.getScreen() instanceof KeyBindsScreen bindings && bindings.selectedKey != null) {
            return;
        }
        if (SAOKeybinds.TOGGLE_UI.matches(event.getKeyCode(), event.getScanCode())) {
            SAOKeybinds.toggleUiPressed();
            event.setCanceled(true);
        } else if (event.getScreen().getClass() == PauseScreen.class && SaoUi.enabled()
                && SAOKeybinds.OPEN_MENU.matches(event.getKeyCode(), event.getScanCode())) {
            SaoUi.openMenu();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onScreenKeyRelease(ScreenEvent.KeyReleased.Pre event) {
        if (SAOKeybinds.TOGGLE_UI.matches(event.getKeyCode(), event.getScanCode())) {
            SAOKeybinds.releaseUiToggle();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (!SAOKeybinds.TOGGLE_UI.matches(event.getKey(), event.getScanCode())) {
            return;
        }
        if (event.getAction() == GLFW.GLFW_RELEASE) {
            SAOKeybinds.releaseUiToggle();
        } else if (event.getAction() == GLFW.GLFW_PRESS && Minecraft.getInstance().screen == null) {
            SAOKeybinds.toggleUiPressed();
        }
    }
}
