package com.sao.saomenu.client.screen;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.widget.SaoButton;
import com.sao.saomenu.api.widget.SaoScreen;
import com.sao.saomenu.api.widget.SaoScrollPane;
import com.sao.saomenu.ui.render.SaoDraw;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Character attributes, separate from native cumulative statistics; values remain live. */
public class SAOStatsScreen extends SaoScreen {
    private static final int ROW_HEIGHT = 16;
    private static final List<Attribute> PREFERRED = List.of(
            Attributes.MAX_HEALTH, Attributes.ARMOR, Attributes.ARMOR_TOUGHNESS,
            Attributes.ATTACK_DAMAGE, Attributes.ATTACK_SPEED, Attributes.ATTACK_KNOCKBACK,
            Attributes.KNOCKBACK_RESISTANCE, Attributes.MOVEMENT_SPEED, Attributes.LUCK);

    private final Player player;
    private final List<AttributeRow> rows = new ArrayList<>();
    private SaoScrollPane attributes;
    private SaoButton done;

    public SAOStatsScreen(Screen parent, Player player) {
        super(Component.translatable("saomenu.menu.attributes"), parent, 320, 300);
        this.player = player;
        setDimBackdrop(true);
    }

    @Override
    protected void buildContent(UiRect content) {
        attributes = addRenderableWidget(new SaoScrollPane(content, getTitle()));
        List<Attribute> present = new ArrayList<>();
        for (Attribute attribute : BuiltInRegistries.ATTRIBUTE) {
            if (player.getAttribute(attribute) != null) present.add(attribute);
        }
        present.sort(Comparator.comparingInt((Attribute attribute) -> {
            int index = PREFERRED.indexOf(attribute);
            return index < 0 ? Integer.MAX_VALUE : index;
        }).thenComparing(Attribute::getDescriptionId));
        rows.clear();
        for (Attribute attribute : present) {
            AttributeRow row = new AttributeRow(attribute);
            attributes.add(row, 0, rows.size() * ROW_HEIGHT);
            rows.add(row);
        }
        done = addRenderableWidget(new SaoButton(content, Component.translatable("gui.done"), this::onClose));
        layoutWidgets(content);
    }

    @Override
    protected void layoutWidgets(UiRect content) {
        attributes.setBounds(new UiRect(content.x(), content.y(), content.width(), Math.max(0, content.height() - 24)));
        for (AttributeRow row : rows) row.setWidth(Math.max(0, content.width() - 8));
        int width = Math.min(80, content.width());
        int height = Math.min(20, content.height());
        done.setBounds(new UiRect(content.right() - width, content.bottom() - height, width, height));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_O) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** Preview observes the cached row text without refreshing it or changing screen state. */
    public List<String> debugRowLabels() {
        List<String> labels = new ArrayList<>(rows.size());
        for (AttributeRow row : rows) labels.add(row.getMessage().getString() + "=" + row.valueText);
        return labels;
    }

    private static String trim(double value) {
        float rounded = Math.round((float) value * 100f) / 100f;
        return rounded == Math.rint(rounded) ? String.valueOf((int) rounded) : String.valueOf(rounded);
    }

    private final class AttributeRow extends AbstractWidget {
        private final Attribute attribute;
        private double lastValue = Double.NaN;
        private String valueText = "";

        private AttributeRow(Attribute attribute) {
            super(0, 0, 0, ROW_HEIGHT, Component.translatable(attribute.getDescriptionId()));
            this.attribute = attribute;
        }

        private String valueText() {
            double value = player.getAttributeValue(attribute);
            if (Double.compare(value, lastValue) != 0) {
                lastValue = value;
                valueText = trim(value);
            }
            return valueText;
        }

        @Override
        protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            var colors = SaoUi.theme().colors();
            var font = SaoUi.bodyFont();
            if (isHoveredOrFocused()) graphics.fill(getX(), getY(), getX() + getWidth(), getY() + getHeight(), colors.highlight());
            String value = valueText();
            int valueX = getX() + getWidth() - 4 - font.width(value);
            int textY = SaoDraw.textY(font, getY(), getHeight());
            graphics.drawString(font, SaoDraw.clipTo(font, getMessage(), Math.max(0, valueX - getX() - 12)),
                    getX() + 4, textY, colors.textOnSurface(), false);
            graphics.drawString(font, value, valueX, textY, colors.accent(), false);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage().copy().append(": " + valueText()));
        }
    }
}
