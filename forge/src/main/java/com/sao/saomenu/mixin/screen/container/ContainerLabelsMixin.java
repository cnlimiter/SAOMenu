package com.sao.saomenu.mixin.screen.container;

import com.sao.saomenu.forge.client.screen.ContainerSkin;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Keep native label metrics and centering; match contrast only on replaced surfaces. */
@Mixin({AbstractContainerScreen.class, InventoryScreen.class})
public abstract class ContainerLabelsMixin {
    @ModifyArg(method = "renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I"),
            index = 4)
    private int saomenu$labelColor(int original) {
        return ContainerSkin.reskin((AbstractContainerScreen<?>) (Object) this)
                ? SaoTheme.palette().textOnSurface() : original;
    }
}
