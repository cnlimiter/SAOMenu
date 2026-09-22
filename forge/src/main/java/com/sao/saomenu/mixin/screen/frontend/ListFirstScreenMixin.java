package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LanguageSelectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * These leaves start at the list and never call Screen.renderBackground.
 * Paint the RESKIN wash first so suppressed list dirt has something to show through.
 */
@Mixin({SelectWorldScreen.class, LanguageSelectScreen.class})
public abstract class ListFirstScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void saomenu$listFirstBackdrop(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.paintBackdrop(graphics, screen);
    }
}
