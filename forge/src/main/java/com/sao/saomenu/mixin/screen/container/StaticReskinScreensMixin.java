package com.sao.saomenu.mixin.screen.container;

import com.sao.saomenu.forge.client.screen.ContainerSkin;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.CraftingScreen;
import net.minecraft.client.gui.screens.inventory.DispenserScreen;
import net.minecraft.client.gui.screens.inventory.HopperScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RESKIN leaves whose {@code renderBg} only blits a static plate (plus Inventory's entity call,
 * which is not a {@code blit}). Local bytecode:
 * <ul>
 *   <li>{@code InventoryScreen.renderBg} — one {@code blit(RL,IIIIII)} at INVOKE #0, then
 *       {@code renderEntityInInventoryFollowsMouse}</li>
 *   <li>{@code ContainerScreen.renderBg} — two {@code blit(RL,IIIIII)} of {@code generic_54}
 *       (container rows + player inventory); both are static base</li>
 *   <li>{@code ShulkerBoxScreen}/{@code HopperScreen}/{@code DispenserScreen}/{@code CraftingScreen}
 *       — one {@code blit(RL,IIIIII)} each</li>
 * </ul>
 * No ordinal: every matching blit in these methods is a base texture. KEEP returns the original
 * {@link ResourceLocation}. Recipe book, player preview and controllers are untouched.
 */
@Mixin({
        InventoryScreen.class,
        ContainerScreen.class,
        ShulkerBoxScreen.class,
        HopperScreen.class,
        DispenserScreen.class,
        CraftingScreen.class
})
public abstract class StaticReskinScreensMixin {

    @Inject(method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V", at = @At("HEAD"))
    private void saomenu$reskinPlate(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        ContainerSkin.paintReskinPlate((AbstractContainerScreen<?>) (Object) this, graphics);
    }

    @ModifyArg(
            method = "renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"
            ),
            index = 0
    )
    private ResourceLocation saomenu$blankBase(ResourceLocation texture) {
        return ContainerSkin.blankIfReskin(texture, (AbstractContainerScreen<?>) (Object) this);
    }
}
