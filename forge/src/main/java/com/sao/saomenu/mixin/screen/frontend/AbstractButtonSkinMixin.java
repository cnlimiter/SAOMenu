package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import com.sao.saomenu.ui.render.SaoWidgetSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Skins only direct {@code Button} / {@code CycleButton} instances on an exact-policy
 * active screen. Icon subclasses keep their own {@code renderWidget}.
 */
@Mixin(AbstractButton.class)
public abstract class AbstractButtonSkinMixin extends AbstractWidget {
    protected AbstractButtonSkinMixin(int x, int y, int width, int height, Component label) {
        super(x, y, width, height, label);
    }

    @Inject(method = "renderWidget(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"), cancellable = true)
    private void saomenu$skinDirectButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractButton button = (AbstractButton) (Object) this;
        if (!VanillaScreenSkin.skinDirectButton(button)) {
            return;
        }
        ci.cancel();
        SaoWidgetSkin.button(graphics, button.getX(), button.getY(), button.getWidth(), button.getHeight(),
                button.getMessage(), this.active, button.isHoveredOrFocused(), this.alpha);
    }
}
