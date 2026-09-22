package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** CreateWorldScreen overrides dirt; replace only that draw. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {

    @Inject(method = "renderDirtBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void saomenu$reskinCreateWorldDirt(GuiGraphics graphics, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.paintBackdrop(graphics, screen);
        ci.cancel();
    }
}
