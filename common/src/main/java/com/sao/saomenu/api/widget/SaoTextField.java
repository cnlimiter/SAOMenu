package com.sao.saomenu.api.widget;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.theme.ThemeColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Native {@link EditBox}. Value is applied before the responder so construction does not
 * look like an edit. There is no custom key capture path.
 */
public class SaoTextField extends EditBox {
    // EditBox has no selection-anchor getter; all native mutations pass through this setter.
    private int selectionAnchor;

    public SaoTextField(Font font, int x, int y, int width, int height, Component narration) {
        super(font, x, y, width, height, narration);
        applyThemeColors();
        setTooltip(Tooltip.create(narration));
    }

    public SaoTextField(Font font, UiRect bounds, Component narration) {
        this(font, bounds.x(), bounds.y(), bounds.width(), bounds.height(), narration);
    }

    public static SaoTextField of(Font font, UiRect bounds, Component narration,
                                  Component hint, String value, int maxLength,
                                  Consumer<String> onChange) {
        SaoTextField field = new SaoTextField(font, bounds, narration);
        String text = value == null ? "" : value;
        field.setMaxLength(Math.max(Math.max(1, maxLength), text.length()));
        field.setValue(text);
        if (hint != null) {
            field.setHint(hint);
        }
        if (onChange != null) {
            field.setResponder(onChange);
        }
        return field;
    }

    public static MultiLineEditBox multiline(Font font, UiRect bounds, Component placeholder, Component narration) {
        return new MultiLineEditBox(font, bounds.x(), bounds.y(),
                Math.max(1, bounds.width()), Math.max(1, bounds.height()), placeholder, narration);
    }

    @Override
    public void setHighlightPos(int position) {
        super.setHighlightPos(position);
        selectionAnchor = Math.max(0, Math.min(position, getValue().length()));
    }

    @Override
    public void setWidth(int width) {
        super.setWidth(width);
        // Refresh the native horizontal viewport without dropping the caret or selection.
        setHighlightPos(selectionAnchor);
    }

    public void setBounds(UiRect bounds) {
        setX(bounds.x());
        setY(bounds.y());
        setWidth(bounds.width());
        this.height = bounds.height();
    }

    private void applyThemeColors() {
        ThemeColors colors = SaoUi.theme().colors();
        setTextColor(colors.highlight());
        setTextColorUneditable(colors.textMuted());
    }
}
