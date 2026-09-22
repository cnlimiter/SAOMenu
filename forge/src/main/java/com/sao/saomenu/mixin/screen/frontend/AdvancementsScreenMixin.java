package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Accent around the native window. Tabs, canvas drag, graph and tooltips stay native. */
@Mixin(AdvancementsScreen.class)
public abstract class AdvancementsScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/advancements/AdvancementsScreen;renderWindow(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void saomenu$advancementHalo(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen.getClass() != AdvancementsScreen.class || !VanillaScreenSkin.reskinBackground(screen)) {
            return;
        }
        VanillaScreenSkin.advancementHalo(graphics, screen);
    }
}
