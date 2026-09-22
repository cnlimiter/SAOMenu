package com.sao.examples;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.layout.UiLayouts;
import com.sao.saomenu.api.layout.UiRect;
import com.sao.saomenu.api.widget.SaoButton;
import com.sao.saomenu.api.widget.SaoConfirmDialog;
import com.sao.saomenu.api.widget.SaoScreen;
import com.sao.saomenu.api.widget.SaoScrollPane;
import com.sao.saomenu.api.widget.SaoTextField;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Native text editing, bounded scrolling and a layered confirmation, with addon-owned persistence. */
public final class ShowcaseScreen extends SaoScreen {
    private static final Component TITLE = Component.literal("Addon notebook");
    private static final Component LABEL = Component.literal("Badge label");
    private static final Component SUGGESTIONS = Component.literal("Choose a label, or type your own");
    private static final List<String> PRESETS = List.of(
            "Travel kit", "Smelting queue", "Dungeon route", "Builder's notebook",
            "Return to base", "Restock torches", "Repair equipment", "Harvest wheat",
            "Collect obsidian", "Explore the coast", "Map the stronghold", "Trade with villagers",
            "Sort the storage room", "Check the nether portal", "Bring a spare pickaxe", "Ready for adventure");
    private final List<SaoButton> presetButtons = new ArrayList<>();
    private String draft;
    private SaoTextField label;
    private SaoScrollPane suggestions;
    private SaoButton save;
    private SaoButton clear;
    private SaoButton back;

    public ShowcaseScreen(Screen parent) {
        super(TITLE, parent, 340, 250);
        draft = ShowcaseConfig.LABEL.get();
    }

    @Override
    protected void buildContent(UiRect content) {
        label = addRenderableWidget(SaoTextField.of(font, new UiRect(0, 0, 1, 20), LABEL,
                Component.literal("A short reminder"), draft, 64, value -> draft = value));
        suggestions = addRenderableWidget(new SaoScrollPane(new UiRect(0, 0, 1, 1), SUGGESTIONS));
        presetButtons.clear();
        for (int i = 0; i < PRESETS.size(); i++) {
            String preset = PRESETS.get(i);
            SaoButton button = new SaoButton(0, 0, 1, 20, Component.literal(preset),
                    ignored -> label.setValue(preset));
            presetButtons.add(suggestions.add(button, 0, i * 24));
        }
        save = addRenderableWidget(new SaoButton(0, 0, 1, 20, Component.literal("Save"), ignored -> {
            ShowcaseConfig.LABEL.set(draft);
            ShowcaseConfig.SPEC.save();
            SaoUi.notify(TITLE, Component.literal("Saved: " + draft));
        }));
        clear = addRenderableWidget(new SaoButton(0, 0, 1, 20, Component.literal("Clear draft"), ignored ->
                SaoConfirmDialog.open(this, Component.literal("Clear notebook draft?"),
                        Component.literal("The saved label stays unchanged until you press Save."),
                        () -> label.setValue(""))));
        back = addRenderableWidget(new SaoButton(0, 0, 1, 20, Component.literal("Back"), ignored -> onClose()));
        layoutWidgets(content);
        setInitialFocus(label);
    }

    @Override
    protected void layoutWidgets(UiRect content) {
        label.setBounds(new UiRect(content.x(), content.y() + 12, content.width(), 20));
        suggestions.setBounds(new UiRect(content.x(), content.y() + 52, content.width(),
                Math.max(0, content.height() - 80)));
        for (SaoButton button : presetButtons) {
            button.setWidth(Math.max(0, content.width() - 8));
        }
        UiRect[] footer = UiLayouts.row(new UiRect(content.x(), content.bottom() - 20,
                content.width(), 20), 3, 6);
        save.setBounds(footer[0]);
        clear.setBounds(footer[1]);
        back.setBounds(footer[2]);
    }

    @Override
    protected void renderForeground(GuiGraphics graphics, UiRect content, int mouseX, int mouseY, float partialTick) {
        int color = SaoUi.theme().colors().textOnSurface();
        graphics.drawString(font, LABEL, content.x(), content.y(), color, false);
        graphics.drawString(font, SUGGESTIONS, content.x(), content.y() + 40, color, false);
    }
}
