package com.sao.saomenu.mixin.screen.container;

import com.sao.saomenu.forge.client.screen.ContainerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AbstractFurnaceScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@code FurnaceScreen}/{@code BlastFurnaceScreen}/{@code SmokerScreen} do not override
 * {@code renderBg}. Local {@code AbstractFurnaceScreen.renderBg} has three
 * {@code blit(RL,IIIIII)} of the same {@code texture} field: ordinal 0 base plate, ordinal 1
 * lit flame (conditional), ordinal 2 burn arrow. Plate and arrow texture change; live values,
 * flame, progress width and controller stay native.
 * {@link ContainerSkin} gates on exact {@link com.sao.saomenu.client.screen.vanilla.VanillaUiPolicy}
 * class so unknown furnace subclasses KEEP every original draw call.
 */
@Mixin(AbstractFurnaceScreen.class)
public abstract class AbstractFurnaceScreenMixin {

    @Inject(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("HEAD"))
    private void saomenu$reskinPlate(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        ContainerSkin.paintReskinPlate((AbstractContainerScreen<?>) (Object) this, graphics);
    }

    @ModifyArg(
            method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 0
            ),
            index = 0
    )
    private ResourceLocation saomenu$blankBase(ResourceLocation texture) {
        return ContainerSkin.blankIfReskin(texture, (AbstractContainerScreen<?>) (Object) this);
    }

    @ModifyArg(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V",
                    ordinal = 2),
            index = 0)
    private ResourceLocation saomenu$progressTexture(ResourceLocation texture) {
        return ContainerSkin.reskin((AbstractContainerScreen<?>) (Object) this) ? ContainerSkin.PROGRESS : texture;
    }
}
