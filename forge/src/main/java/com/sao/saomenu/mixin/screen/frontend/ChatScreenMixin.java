package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.InBedChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Plate behind the native input row. EditBox, suggestions, history and send stay native.
 * Also runs when {@link InBedChatScreen} invokes this method.
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void saomenu$chatPlate(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Screen screen = (Screen) (Object) this;
        Class<?> type = screen.getClass();
        if (type != ChatScreen.class && type != InBedChatScreen.class) {
            return;
        }
        if (!VanillaScreenSkin.decorate(screen)) {
            return;
        }
        VanillaScreenSkin.chatPlate(graphics, screen.width, screen.height);
    }
}
