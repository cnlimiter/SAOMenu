package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Exact-class bed chat chrome, including the leave-bed-only path that skips ChatScreen.render. */
@Mixin(InBedChatScreen.class)
public abstract class InBedChatScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void saomenu$bedChatRail(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        if (screen.getClass() != InBedChatScreen.class || !VanillaScreenSkin.decorate(screen)) {
            return;
        }
        VanillaScreenSkin.topRail(graphics, screen);
    }
}
