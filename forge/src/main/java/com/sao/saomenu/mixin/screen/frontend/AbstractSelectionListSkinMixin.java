package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Temporarily suppress vanilla list dirt/header strips for RESKIN frontend screens,
 * then restore the original flags so recovery stays lossless on the same instance.
 */
@Mixin(AbstractSelectionList.class)
public abstract class AbstractSelectionListSkinMixin {

    @Shadow
    private boolean renderBackground;

    @Shadow
    private boolean renderTopAndBottom;

    @Shadow
    public abstract void setRenderBackground(boolean renderBackground);

    @Shadow
    public abstract void setRenderTopAndBottom(boolean renderTopAndBottom);

    @Unique
    private boolean saomenu$flagsPushed;

    @Unique
    private boolean saomenu$savedBackground;

    @Unique
    private boolean saomenu$savedTopAndBottom;

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("HEAD"))
    private void saomenu$beginListSkin(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        this.saomenu$flagsPushed = false;
        if (!VanillaScreenSkin.harmonizeLists()) {
            return;
        }
        this.saomenu$savedBackground = this.renderBackground;
        this.saomenu$savedTopAndBottom = this.renderTopAndBottom;
        this.saomenu$flagsPushed = true;
        this.setRenderBackground(false);
        this.setRenderTopAndBottom(false);
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("RETURN"))
    private void saomenu$endListSkin(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!this.saomenu$flagsPushed) {
            return;
        }
        this.saomenu$flagsPushed = false;
        this.setRenderBackground(this.saomenu$savedBackground);
        this.setRenderTopAndBottom(this.saomenu$savedTopAndBottom);
    }
}
