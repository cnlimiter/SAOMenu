package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restyle the death wash only. Delay ticker, buttons and clickable death text stay native. */
@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin {

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"),
            index = 4)
    private int saomenu$deathFrom(int colorFrom) {
        return VanillaScreenSkin.deathGradientFrom((Screen) (Object) this, colorFrom);
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;fillGradient(IIIIII)V"),
            index = 5)
    private int saomenu$deathTo(int colorTo) {
        return VanillaScreenSkin.deathGradientTo((Screen) (Object) this, colorTo);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"))
    private void saomenu$deathRails(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen.getClass() != DeathScreen.class || !VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.edgeFrame(graphics, screen);
    }
}
