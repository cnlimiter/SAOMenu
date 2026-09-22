package com.sao.saomenu.api.hud;

import com.sao.saomenu.api.UiContribution;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A registered HUD widget. Lower {@link #order()} renders first; layout editing uses
 * {@link HudLayoutBinding#priority()} when grab boxes overlap.
 *
 * <p>Default passes are {@link HudPass#WORLD} and {@link HudPass#MENU_OVERLAY}. An empty
 * pass set is valid for editor-only widgets (the vanilla-centered food bar).
 */
public final class HudElement implements UiContribution {
    private final ResourceLocation id;
    private final int order;
    private final Component label;
    private final HudRenderer renderer;
    private final Set<HudPass> passes;
    private final Predicate<HudRenderContext> visible;
    private final HudLayoutBinding layout;

    public HudElement(ResourceLocation id, int order, Component label, HudRenderer renderer,
                      Set<HudPass> passes, Predicate<HudRenderContext> visible,
                      HudLayoutBinding layout) {
        this.id = Objects.requireNonNull(id, "id");
        this.order = order;
        this.label = Objects.requireNonNull(label, "label");
        this.renderer = Objects.requireNonNull(renderer, "renderer");
        Objects.requireNonNull(passes, "passes");
        this.passes = passes.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(passes));
        this.visible = visible == null ? ctx -> true : visible;
        this.layout = layout;
    }

    public static Builder builder(ResourceLocation id, int order, Component label, HudRenderer renderer) {
        return new Builder(id, order, label, renderer);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public int order() {
        return order;
    }

    public Component label() {
        return label;
    }

    public HudRenderer renderer() {
        return renderer;
    }

    /** Frozen pass membership; do not mutate. */
    public Set<HudPass> passes() {
        return passes;
    }

    public boolean inPass(HudPass pass) {
        return passes.contains(pass);
    }

    public boolean visible(HudRenderContext context) {
        return visible.test(context);
    }

    /** {@code null} when the element is not user-movable. */
    public HudLayoutBinding layout() {
        return layout;
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final int order;
        private final Component label;
        private final HudRenderer renderer;
        private Set<HudPass> passes = EnumSet.of(HudPass.WORLD, HudPass.MENU_OVERLAY);
        private Predicate<HudRenderContext> visible = ctx -> true;
        private HudLayoutBinding layout;

        private Builder(ResourceLocation id, int order, Component label, HudRenderer renderer) {
            this.id = id;
            this.order = order;
            this.label = label;
            this.renderer = renderer;
        }

        public Builder passes(HudPass... passes) {
            EnumSet<HudPass> next = EnumSet.noneOf(HudPass.class);
            if (passes != null) {
                for (HudPass pass : passes) {
                    if (pass != null) {
                        next.add(pass);
                    }
                }
            }
            this.passes = next;
            return this;
        }

        public Builder visible(Predicate<HudRenderContext> visible) {
            this.visible = Objects.requireNonNull(visible, "visible");
            return this;
        }

        public Builder layout(HudLayoutBinding layout) {
            this.layout = layout;
            return this;
        }

        public HudElement build() {
            return new HudElement(id, order, label, renderer, passes, visible, layout);
        }
    }
}
