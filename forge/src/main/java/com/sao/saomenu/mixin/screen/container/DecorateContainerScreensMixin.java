package com.sao.saomenu.mixin.screen.container;

import com.sao.saomenu.forge.client.screen.ContainerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.BeaconScreen;
import net.minecraft.client.gui.screens.inventory.BrewingStandScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.client.gui.screens.inventory.GrindstoneScreen;
import net.minecraft.client.gui.screens.inventory.LoomScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.SmithingScreen;
import net.minecraft.client.gui.screens.inventory.StonecutterScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DECORATE leaves: no texture substitution. Chrome is painted after native {@code renderBg}
 * so bubbles, maps, books, recipe lists, tabs, error icons and merchant arrows stay native.
 * KEEP / unknown subclass / disabled policy is a no-op inside {@link ContainerSkin}.
 */
@Mixin({
        CreativeModeInventoryScreen.class,
        BrewingStandScreen.class,
        AnvilScreen.class,
        SmithingScreen.class,
        EnchantmentScreen.class,
        GrindstoneScreen.class,
        StonecutterScreen.class,
        LoomScreen.class,
        CartographyTableScreen.class,
        BeaconScreen.class,
        MerchantScreen.class
})
public abstract class DecorateContainerScreensMixin {

    @Inject(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("RETURN"))
    private void saomenu$decorateChrome(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        ContainerSkin.paintDecorateChrome((AbstractContainerScreen<?>) (Object) this, graphics);
    }
}
