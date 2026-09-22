package com.sao.saomenu.mixin.accessor;

import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Distinguishes the native focus-loss pause from the interactive pause menu. */
@Mixin(PauseScreen.class)
public interface PauseScreenAccessor {
    @Accessor("showPauseMenu")
    boolean saomenu$showPauseMenu();
}
