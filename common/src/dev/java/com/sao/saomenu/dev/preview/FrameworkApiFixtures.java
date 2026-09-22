package com.sao.saomenu.dev.preview;

import com.sao.saomenu.api.SaoUiRegistry;
import com.sao.saomenu.api.menu.MenuEntry;
import com.sao.saomenu.api.menu.MenuIcon;
import com.sao.saomenu.api.menu.SaoPanel;
import com.sao.saomenu.api.settings.NativeSetting;
import com.sao.saomenu.api.settings.SettingsGroup;
import com.sao.saomenu.api.theme.ThemeDefinition;
import com.sao.saomenu.api.theme.ThemeTokens;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Development-only large registries. No preview contributions ship in the release JAR. */
public final class FrameworkApiFixtures {
    static List<MenuEntry> rows;
    static int activatedRow = -1;
    static final String[] values = new String[8];
    static final String[] saved = new String[8];

    private FrameworkApiFixtures() {
    }

    public static void register(SaoUiRegistry registry) {
        if (!SAOMenuPreview.requested() || !Boolean.getBoolean("saomenu.preview.api")) return;
        ResourceLocation icon = new ResourceLocation("saomenu_showcase", "textures/gui/notebook.png");
        List<MenuEntry> entries = new ArrayList<>();
        for (int row = 0; row < 40; row++) {
            int value = row;
            entries.add(MenuEntry.action(id("row_" + row), Component.literal("Probe row " + row), icon,
                    context -> activatedRow = value));
        }
        rows = List.copyOf(entries);
        for (int group = 0; group < 8; group++) {
            int index = group;
            registry.menu(SaoPanel.of(id("panel_" + group), 1000 + group,
                    Component.literal("Overflow panel " + group), MenuIcon.of(icon), () -> rows,
                    (graphics, context, bounds, eased, alpha, mouseX, mouseY) -> {
                        // Deliberately overdraw: the host must confine an uncooperative addon side card.
                        graphics.fill(bounds.x() - 2000, bounds.y() - 2000,
                                bounds.right() + 2000, bounds.bottom() + 2000, 0xFFDA35D5);
                        graphics.drawString(context.minecraft().font, "Clipped card", bounds.x() + 4,
                                bounds.y() + 4, 0xFF000000, false);
                    }));
            values[group] = saved[group] = "Group " + group;
            registry.settings(new SettingsGroup(id("group_" + group), 1000 + group,
                    Component.literal("Overflow group " + group), List.of(new NativeSetting(id("text_" + group),
                    Component.literal("Owned value"), () -> values[index], value -> values[index] = value, 64)),
                    () -> saved[index] = values[index], () -> values[index] = "Group " + index));
        }
        registry.theme(new ThemeDefinition(id("contrast"), 1000, Component.literal("Overflow theme"), 220,
                ThemeTokens.sao()));
    }

    static void reverseRows() {
        List<MenuEntry> reversed = new ArrayList<>(rows);
        Collections.reverse(reversed);
        rows = List.copyOf(reversed);
    }

    static ResourceLocation id(String path) {
        return new ResourceLocation("saomenu_preview", path);
    }
}
