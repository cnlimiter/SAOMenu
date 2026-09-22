package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiLayouts;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.theme.ThemeColors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Clips both drawing and pointer input to its viewport. Children are not registered on
 * the parent screen; clicks outside the pane never reach them.
 */
public class SaoScrollPane extends AbstractWidget implements ContainerEventHandler {
    private final List<Entry> entries = new ArrayList<>();
    private final List<AbstractWidget> widgets = new ArrayList<>();
    private final List<AbstractWidget> widgetView = Collections.unmodifiableList(widgets);
    private GuiEventListener focused;
    private boolean dragging;
    private boolean scrolling;
    private int scroll;
    private int extraContentHeight;

    public SaoScrollPane(int x, int y, int width, int height, Component narration) {
        super(x, y, width, height, narration);
    }

    public SaoScrollPane(UiRect bounds, Component narration) {
        this(bounds.x(), bounds.y(), bounds.width(), bounds.height(), narration);
    }

    public void setBounds(UiRect bounds) {
        GuiEventListener previousFocus = focused;
        setX(bounds.x());
        setY(bounds.y());
        setWidth(bounds.width());
        this.height = bounds.height();
        scroll = UiLayouts.clampedScroll(scroll, contentHeight(), getHeight());
        applyChildPositions();
        if (previousFocus instanceof AbstractWidget widget && widget.visible && widget.active) {
            setFocused(previousFocus);
        } else {
            clearFocusIfOutside();
        }
    }

    /**
     * Adopts {@code widget} using its current position as content-local coordinates
     * relative to this pane's origin (before scroll).
     */
    public <T extends AbstractWidget> T add(T widget) {
        int localX = widget.getX() - getX();
        int localY = widget.getY() - getY() + scroll;
        return add(widget, localX, localY);
    }


    public <T extends AbstractWidget> T add(T widget, int localX, int localY) {
        entries.add(new Entry(widget, localX, localY));
        widgets.add(widget);
        applyChildPositions();
        return widget;
    }

    public void setContentHeight(int height) {
        extraContentHeight = Math.max(0, height);
        scroll = UiLayouts.clampedScroll(scroll, contentHeight(), getHeight());
        applyChildPositions();
        clearFocusIfOutside();
    }

    public int scrollAmount() {
        return scroll;
    }

    public int contentHeight() {
        int height = extraContentHeight;
        for (Entry entry : entries) {
            height = Math.max(height, entry.localY + entry.widget.getHeight());
        }
        return height;
    }

    public int maxScroll() {
        return Math.max(0, contentHeight() - getHeight());
    }

    public void scrollTo(int amount) {
        int next = UiLayouts.clampedScroll(amount, contentHeight(), getHeight());
        if (next != scroll) {
            scroll = next;
            applyChildPositions();
            clearFocusIfOutside();
        }
    }

    public boolean contains(double mouseX, double mouseY) {
        return active && visible && getWidth() > 0 && getHeight() > 0
                && mouseX >= getX() && mouseX < getX() + getWidth()
                && mouseY >= getY() && mouseY < getY() + getHeight();
    }

    public void tick() {
        for (Entry entry : entries) {
            if (!entry.widget.visible) {
                continue;
            }
            if (entry.widget instanceof EditBox box) {
                box.tick();
            } else if (entry.widget instanceof MultiLineEditBox box) {
                box.tick();
            }
        }
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return widgetView;
    }

    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event) {
        return active && visible ? ContainerEventHandler.super.nextFocusPath(event) : null;
    }

    @Override
    public GuiEventListener getFocused() {
        return focused;
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        if (focused != listener && focused != null) {
            focused.setFocused(false);
        }
        focused = listener;
        if (focused != null) {
            focused.setFocused(true);
            if (focused instanceof AbstractWidget widget) {
                ensureVisible(widget);
            }
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            setFocused((GuiEventListener) null);
        } else if (this.focused != null) {
            this.focused.setFocused(true);
        }
    }

    @Override
    public boolean isFocused() {
        return focused != null && focused.isFocused();
    }

    @Override
    public boolean isDragging() {
        return dragging;
    }

    @Override
    public void setDragging(boolean dragging) {
        this.dragging = dragging;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (button == 0 && maxScroll() > 0 && mouseX >= getX() + getWidth() - 5) {
            scrolling = true;
            setDragging(true);
            jumpThumb(mouseY);
            return true;
        }
        return ContainerEventHandler.super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            scrolling = false;
        }
        return ContainerEventHandler.super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!active) {
            return false;
        }
        if (scrolling && button == 0 && maxScroll() > 0) {
            jumpThumb(mouseY);
            return true;
        }
        return ContainerEventHandler.super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!contains(mouseX, mouseY)) {
            return false;
        }
        if (ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, delta)) {
            return true;
        }
        int before = scroll;
        scrollTo(scroll - (int) Math.round(delta * 10));
        return before != scroll;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!active || !visible) {
            return false;
        }
        if (ContainerEventHandler.super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        int next = switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> scroll + getHeight();
            case GLFW.GLFW_KEY_PAGE_UP -> scroll - getHeight();
            case GLFW.GLFW_KEY_HOME -> 0;
            case GLFW.GLFW_KEY_END -> maxScroll();
            default -> scroll;
        };
        if (next == scroll) {
            return false;
        }
        scrollTo(next);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return active && visible && ContainerEventHandler.super.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean inside = contains(mouseX, mouseY);
        int hoverX = inside ? mouseX : Integer.MIN_VALUE;
        int hoverY = inside ? mouseY : Integer.MIN_VALUE;
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        try {
            for (Entry entry : entries) {
                if (entry.widget.visible && intersectsViewport(entry.widget)) {
                    entry.widget.render(graphics, hoverX, hoverY, partialTick);
                }
            }
        } finally {
            graphics.disableScissor();
        }
        renderScrollbar(graphics);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, getMessage());
        if (focused instanceof AbstractWidget widget) {
            widget.updateNarration(output);
        }
    }

    private void applyChildPositions() {
        for (Entry entry : entries) {
            AbstractWidget widget = entry.widget;
            widget.setX(getX() + entry.localX);
            widget.setY(getY() + entry.localY - scroll);
            if (!intersectsViewport(widget) && widget.isFocused()) {
                widget.setFocused(false);
                if (focused == widget) {
                    focused = null;
                }
            }
        }
    }

    private void clearFocusIfOutside() {
        if (focused instanceof AbstractWidget widget && (!widget.visible || !intersectsViewport(widget))) {
            widget.setFocused(false);
            focused = null;
        }
    }

    private boolean intersectsViewport(AbstractWidget widget) {
        return widget.getX() < getX() + getWidth() && widget.getX() + widget.getWidth() > getX()
                && widget.getY() < getY() + getHeight() && widget.getY() + widget.getHeight() > getY();
    }

    private void ensureVisible(AbstractWidget widget) {
        for (Entry entry : entries) {
            if (entry.widget != widget) {
                continue;
            }
            if (entry.localY < scroll) {
                scrollTo(entry.localY);
            } else if (entry.localY + widget.getHeight() > scroll + getHeight()) {
                scrollTo(Math.min(entry.localY, entry.localY + widget.getHeight() - getHeight()));
            }
            return;
        }
    }

    private void jumpThumb(double mouseY) {
        int max = maxScroll();
        if (max <= 0) {
            return;
        }
        int track = Math.max(1, getHeight() - 2);
        float t = (float) ((mouseY - getY() - 1) / track);
        if (t < 0f) {
            t = 0f;
        } else if (t > 1f) {
            t = 1f;
        }
        scrollTo(Math.round(t * max));
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int max = maxScroll();
        if (max <= 0 || getWidth() < 3 || getHeight() < 3) {
            return;
        }
        ThemeColors colors = SaoUi.theme().colors();
        int trackX = getX() + getWidth() - 3;
        int trackY = getY() + 1;
        int trackH = getHeight() - 2;
        int thumbH = Math.min(trackH,
                Math.max(8, (int) (trackH * (getHeight() / (float) Math.max(1, contentHeight())))));
        int thumbY = trackY + (int) ((trackH - thumbH) * (scroll / (float) max));
        graphics.fill(trackX, trackY, trackX + 2, trackY + trackH, colors.divider());
        graphics.fill(trackX, thumbY, trackX + 2, thumbY + thumbH, colors.accent());
    }

    private record Entry(AbstractWidget widget, int localX, int localY) {
    }
}
