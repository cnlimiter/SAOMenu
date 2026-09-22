package com.sao.saomenu.api;

import net.minecraft.resources.ResourceLocation;

/** A client UI registration. Lower order renders first; ties use lexical namespaced ID order. */
public interface UiContribution {
    /** Stable identity in the contributing mod's namespace; duplicates in one domain are errors. */
    ResourceLocation id();

    /** Stable ordering, independent of mod discovery or event listener order. */
    int order();
}
