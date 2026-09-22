package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Top corners outside the controls. Panorama, logo, splash and footer branding stay unobscured. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void saomenu$titleChrome(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen.getClass() != TitleScreen.class || !VanillaScreenSkin.decorate(screen)) {
            return;
        }
        VanillaScreenSkin.titleChrome(graphics, screen);
    }
}
