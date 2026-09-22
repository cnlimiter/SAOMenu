package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiLayouts;
import com.sao.saomenu.api.layout.UiRect;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.Objects;

/**
 * Cancel-focused confirmation. The accept callback runs only after a successful pop.
 */
public class SaoConfirmDialog extends SaoDialog {
    private final Component message;
    private final Runnable onConfirm;
    private SaoButton cancel;
    private SaoButton confirm;
    private UiRect body;
    private ResourceLocation messageFont;
    private Language messageLanguage;
    private int wrapWidth = -1;
    private List<FormattedCharSequence> lines = List.of();

    public SaoConfirmDialog(Screen parent, Component title, Component message, Runnable onConfirm) {
        super(title, parent, 280, 120);
        this.message = Objects.requireNonNull(message, "message").copy();
        this.onConfirm = Objects.requireNonNull(onConfirm, "onConfirm");
    }

    public static void open(Screen parent, Component title, Component message, Runnable onConfirm) {
        SaoDialog.open(parent, new SaoConfirmDialog(parent, title, message, onConfirm));
    }

    @Override
    protected void buildContent(UiRect content) {
        cancel = addRenderableWidget(new SaoButton(0, 0, 1, 1, CommonComponents.GUI_CANCEL, button -> onClose()));
        confirm = addRenderableWidget(new SaoButton(0, 0, 1, 1, CommonComponents.GUI_PROCEED, button -> {
            if (finish()) {
                onConfirm.run();
            }
        }));
        layoutWidgets(content);
        setInitialFocus(cancel);
    }

    @Override
    protected void layoutWidgets(UiRect content) {
        UiRect[] buttons = UiLayouts.row(footerBounds(content), 2, 8);
        cancel.setBounds(buttons[0]);
        confirm.setBounds(buttons[1]);
        body = messageBounds(content);
        wrapWidth = -1;
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, UiRect content, int mouseX, int mouseY, float partialTick) {
        if (body.width() <= 0 || body.height() <= 0) {
            return;
        }
        var tokens = SaoUi.theme();
        if (!tokens.bodyFont().equals(messageFont) || wrapWidth != body.width()
                || messageLanguage != Language.getInstance()) {
            messageFont = tokens.bodyFont();
            messageLanguage = Language.getInstance();
            wrapWidth = body.width();
            Component styled = message.copy().withStyle(style -> style.withFont(messageFont));
            lines = this.font.split(styled, Math.max(1, wrapWidth));
        }
        graphics.enableScissor(body.x(), body.y(), body.right(), body.bottom());
        try {
            int y = body.y();
            for (FormattedCharSequence line : lines) {
                if (y >= body.bottom()) {
                    break;
                }
                graphics.drawString(this.font, line, body.x(), y, tokens.colors().textOnSurface(), false);
                y += this.font.lineHeight;
            }
        } finally {
            graphics.disableScissor();
        }
    }


    private static UiRect footerBounds(UiRect content) {
        int height = Math.min(20, Math.max(0, content.height()));
        return new UiRect(content.x(), content.bottom() - height, content.width(), height);
    }

    private static UiRect messageBounds(UiRect content) {
        int footer = Math.min(20, Math.max(0, content.height()));
        int gap = footer > 0 ? 6 : 0;
        return new UiRect(content.x(), content.y(), content.width(), Math.max(0, content.height() - footer - gap));
    }
}
