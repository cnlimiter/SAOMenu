package com.sao.saomenu.api.hud;

import net.minecraft.client.Minecraft;

import java.util.Objects;
import java.util.function.DoubleSupplier;

/**
 * Optional editor binding for a {@link HudElement}. Hit tests and drag math use screen
 * GUI pixels. The layout editor saves only on left-button release after a move, and
 * restores the press-time anchor when the menu closes or the session resets.
 *
 * <p>Higher {@link #priority()} wins when two bindings overlap. Builtin clock/skill/plate
 * /food/map use 50/40/30/20/10 so historical grab order is preserved.
 */
public final class HudLayoutBinding {
    private final int priority;
    private final HitTest hit;
    private final BoxSupplier box;
    private final DoubleSupplier anchorX;
    private final DoubleSupplier anchorY;
    private final Drag drag;
    private final Restore restore;
    private final Runnable save;

    public HudLayoutBinding(int priority, HitTest hit, BoxSupplier box,
                            DoubleSupplier anchorX, DoubleSupplier anchorY,
                            Drag drag, Restore restore, Runnable save) {
        this.priority = priority;
        this.hit = Objects.requireNonNull(hit, "hit");
        this.box = Objects.requireNonNull(box, "box");
        this.anchorX = Objects.requireNonNull(anchorX, "anchorX");
        this.anchorY = Objects.requireNonNull(anchorY, "anchorY");
        this.drag = Objects.requireNonNull(drag, "drag");
        this.restore = Objects.requireNonNull(restore, "restore");
        this.save = Objects.requireNonNull(save, "save");
    }

    /**
     * Screen-fraction anchor (0–1) using {@code (mouse - grab * box) / (screen - box)}.
     * Addon-owned storage: {@code set} during drag, {@code save} on left release.
     */
    public static HudLayoutBinding screenAnchor(int priority, BoxSupplier box,
                                                DoubleSupplier getX, DoubleSupplier getY,
                                                AnchorConsumer set, Runnable save) {
        Objects.requireNonNull(box, "box");
        Objects.requireNonNull(set, "set");
        return new HudLayoutBinding(
                priority,
                (mc, w, h, mx, my) -> box.get(mc, w, h).contains(mx, my),
                box,
                getX,
                getY,
                (mc, w, h, gx, gy, mx, my) -> {
                    HudBox b = box.get(mc, w, h);
                    float fx = (mx - gx * b.width()) / (float) Math.max(1, w - b.width());
                    float fy = (my - gy * b.height()) / (float) Math.max(1, h - b.height());
                    set.set(Math.max(0f, Math.min(1f, fx)), Math.max(0f, Math.min(1f, fy)));
                },
                set::set,
                save);
    }

    public static HudLayoutBinding screenAnchor(BoxSupplier box, DoubleSupplier getX, DoubleSupplier getY,
                                                AnchorConsumer set, Runnable save) {
        return screenAnchor(0, box, getX, getY, set, save);
    }

    public int priority() {
        return priority;
    }

    public boolean hit(Minecraft mc, int width, int height, int mouseX, int mouseY) {
        return hit.test(mc, width, height, mouseX, mouseY);
    }

    public HudBox box(Minecraft mc, int width, int height) {
        return box.get(mc, width, height);
    }

    public float anchorX() {
        return (float) anchorX.getAsDouble();
    }

    public float anchorY() {
        return (float) anchorY.getAsDouble();
    }

    public void moveTo(Minecraft mc, int width, int height, float grabFx, float grabFy,
                       int mouseX, int mouseY) {
        drag.moveTo(mc, width, height, grabFx, grabFy, mouseX, mouseY);
    }

    public void restore(float anchorX, float anchorY) {
        restore.restore(anchorX, anchorY);
    }

    public void save() {
        save.run();
    }

    @FunctionalInterface
    public interface HitTest {
        boolean test(Minecraft mc, int width, int height, int mouseX, int mouseY);
    }

    @FunctionalInterface
    public interface BoxSupplier {
        HudBox get(Minecraft mc, int width, int height);
    }

    @FunctionalInterface
    public interface Drag {
        void moveTo(Minecraft mc, int width, int height, float grabFx, float grabFy,
                    int mouseX, int mouseY);
    }

    @FunctionalInterface
    public interface Restore {
        void restore(float anchorX, float anchorY);
    }

    @FunctionalInterface
    public interface AnchorConsumer {
        void set(float x, float y);
    }
}
