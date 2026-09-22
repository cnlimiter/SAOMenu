package com.sao.saomenu.mixin.screen.frontend;

import com.sao.saomenu.forge.client.screen.VanillaScreenSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Visual-only progress bar. Fade, reload completion and logo stay native.
 * LoadingOverlay is not a Screen; gate on {@code SaoUi.enabled()} only.
 */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow
    private float currentProgress;

    @Inject(method = "drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V", at = @At("HEAD"), cancellable = true)
    private void saomenu$progressBar(GuiGraphics graphics, int minX, int minY, int maxX, int maxY, float fade,
                                    CallbackInfo ci) {
        if (((Object) this).getClass() != LoadingOverlay.class || !VanillaScreenSkin.overlayEnabled()) {
            return;
        }
        ci.cancel();
        VanillaScreenSkin.loadingProgress(graphics, minX, minY, maxX, maxY, this.currentProgress, fade);
    }
}
