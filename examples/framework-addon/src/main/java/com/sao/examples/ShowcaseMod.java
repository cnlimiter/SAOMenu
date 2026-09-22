package com.sao.examples;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

/** No client class is loaded from the mod constructor. */
@Mod(ShowcaseMod.ID)
public final class ShowcaseMod {
    public static final String ID = "saomenu_showcase";

    public ShowcaseMod() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ShowcaseConfig.SPEC);
    }
}
