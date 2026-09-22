package com.sao.examples;

import net.minecraftforge.common.ForgeConfigSpec;

/** Addon settings live in saomenu_showcase-client.toml, never SAOMenu's config file. */
public final class ShowcaseConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue BADGE;
    public static final ForgeConfigSpec.BooleanValue MARKER;
    public static final ForgeConfigSpec.DoubleValue X;
    public static final ForgeConfigSpec.DoubleValue Y;
    public static final ForgeConfigSpec.DoubleValue OPACITY;
    public static final ForgeConfigSpec.ConfigValue<String> LABEL;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        BADGE = builder.define("badge", true);
        MARKER = builder.define("worldMarker", true);
        X = builder.defineInRange("x", 0.80, 0.0, 1.0);
        Y = builder.defineInRange("y", 0.20, 0.0, 1.0);
        OPACITY = builder.defineInRange("opacity", 0.9, 0.2, 1.0);
        LABEL = builder.define("label", "Independent addon");
        SPEC = builder.build();
    }

    private ShowcaseConfig() {
    }

    public static void reset() {
        BADGE.set(true);
        MARKER.set(true);
        X.set(0.80);
        Y.set(0.20);
        OPACITY.set(0.9);
        LABEL.set("Independent addon");
    }
}
