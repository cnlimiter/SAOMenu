package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces exact-policy background draws while preserving Forge's background-render event. */
@Mixin(Screen.class)
public abstract class ScreenBackgroundMixin {

    @Inject(method = "renderBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void saomenu$reskinBackground(GuiGraphics graphics, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.paintBackdrop(graphics, screen);
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, graphics));
        ci.cancel();
    }

    @Inject(method = "renderDirtBackground(Lnet/minecraft/client/gui/GuiGraphics;)V", at = @At("HEAD"), cancellable = true)
    private void saomenu$reskinDirt(GuiGraphics graphics, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (!VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.paintBackdrop(graphics, screen);
        MinecraftForge.EVENT_BUS.post(new ScreenEvent.BackgroundRendered(screen, graphics));
        ci.cancel();
    }
}
