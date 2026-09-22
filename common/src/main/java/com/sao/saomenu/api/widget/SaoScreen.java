package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiLayouts;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.theme.ThemeColors;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.ui.animation.SaoMotion;
import com.sao.saomenu.ui.render.SaoDraw;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * World-visible SAO card. Subclasses place native widgets in {@link #buildContent(UiRect)}
 * via {@link #addRenderableWidget}. Resize keeps those instances; {@link #removed()} runs
 * {@link #disposeMount()} so the same object can be opened again.
 */
public abstract class SaoScreen extends Screen {
    protected final Screen parent;
    private final int preferredWidth;
    private final int preferredHeight;
    private UiRect cardBounds = new UiRect(0, 0, 0, 0);
    private UiRect contentBounds = new UiRect(0, 0, 0, 0);
    private boolean mounted;
    private boolean dimBackdrop;
    private long mountedAt;

    public SaoScreen(Component title, Screen parent, int preferredWidth, int preferredHeight) {
        super(title);
        this.parent = parent;
        this.preferredWidth = Math.max(0, preferredWidth);
        this.preferredHeight = Math.max(0, preferredHeight);
    }

    public final SaoScreen setDimBackdrop(boolean dim) {
        this.dimBackdrop = dim;
        return this;
    }

    public final UiRect contentBounds() {
        return contentBounds;
    }

    public final UiRect cardBounds() {
        return cardBounds;
    }

    public final Screen parent() {
        return parent;
    }

    protected int preferredWidth() {
        return preferredWidth;
    }

    protected int preferredHeight() {
        return preferredHeight;
    }

    protected int contentInset() {
        return 12;
    }

    protected int titleBand() {
        return 24;
    }

    protected boolean dimBackdrop() {
        return dimBackdrop;
    }

    protected UiRect layoutCard() {
        return UiLayouts.centered(this.width, this.height, preferredWidth(), preferredHeight(), 8);
    }

    /** Called after layout on first init of each mount. Recreate widgets here. */
    protected abstract void buildContent(UiRect content);

    /** Called after layout on resize so existing widgets can be moved, not rebuilt. */
    protected abstract void layoutWidgets(UiRect content);

    /** Per-mount cleanup. Do not assume this screen object is discarded. */
    protected void disposeMount() {
    }

    /** Drawn after the card and title, before native widgets. */
    protected void renderForeground(GuiGraphics graphics, UiRect content, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        this.font = SaoUi.bodyFont();
        if (mounted) {
            disposeMount();
        }
        setFocused(null);
        clearWidgets();
        layout();
        buildContent(contentBounds);
        mounted = true;
        mountedAt = Util.getMillis();
    }

    @Override
    protected void repositionElements() {
        this.font = SaoUi.bodyFont();
        if (!mounted) {
            // Vanilla calls repositionElements, not init(), when the same Screen is opened again.
            init();
            return;
        }
        GuiEventListener focus = getFocused();
        layout();
        layoutWidgets(contentBounds);
        if (focus != null) {
            setFocused(focus);
        }
    }

    @Override
    public void removed() {
        if (mounted) {
            disposeMount();
            mounted = false;
        }
        super.removed();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(parent);
        }
    }

    @Override
    public void tick() {
        super.tick();
        for (GuiEventListener child : children()) {
            if (child instanceof EditBox box) {
                box.tick();
            } else if (child instanceof MultiLineEditBox box) {
                box.tick();
            } else if (child instanceof SaoScrollPane pane) {
                pane.tick();
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ThemeTokens tokens = SaoUi.theme();
        float enter = enterAlpha(tokens);
        if (dimBackdrop()) {
            graphics.fill(0, 0, this.width, this.height, SaoDraw.mulAlpha(0xA0000000, enter));
        }
        drawCard(graphics, tokens, enter);
        drawTitle(graphics, tokens, enter);
        renderForeground(graphics, contentBounds, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void layout() {
        cardBounds = layoutCard();
        int pad = Math.max(0, contentInset());
        int band = titleBand();
        if (cardBounds.height() < band + pad + 8) {
            int v = Math.min(pad, Math.max(0, cardBounds.height() / 4));
            int h = Math.min(pad, Math.max(0, cardBounds.width() / 4));
            contentBounds = cardBounds.inset(h, v, h, v);
        } else {
            contentBounds = cardBounds.inset(pad, band, pad, pad);
        }
    }

    private float enterAlpha(ThemeTokens tokens) {
        int millis = tokens.enterMillis();
        if (millis <= 0) {
            return 1f;
        }
        return SaoMotion.easeOutCubic((Util.getMillis() - mountedAt) / (float) millis);
    }

    private void drawCard(GuiGraphics graphics, ThemeTokens tokens, float alpha) {
        ThemeColors colors = tokens.colors();
        UiRect card = cardBounds;
        if (card.width() <= 0 || card.height() <= 0) {
            return;
        }
        graphics.fill(card.x() + 3, card.y() + 3, card.right() + 3, card.bottom() + 3,
                SaoDraw.mulAlpha(colors.dialogShadow(), alpha));
        SaoDraw.roundedRect(graphics, card.x(), card.y(), card.width(), card.height(), 4,
                SaoDraw.mulAlpha(colors.dialogSurface(), alpha));
    }

    private void drawTitle(GuiGraphics graphics, ThemeTokens tokens, float alpha) {
        if (cardBounds.height() < titleBand()) {
            return;
        }
        var titleFont = SaoUi.displayFont();
        int color = SaoDraw.mulAlpha(tokens.colors().textOnSurface(), alpha);
        graphics.drawString(titleFont, this.title, cardBounds.centerX() - titleFont.width(this.title) / 2,
                cardBounds.y() + 8, color, false);
    }
}
