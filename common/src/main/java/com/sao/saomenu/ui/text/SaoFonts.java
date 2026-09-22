package com.sao.saomenu.ui.text;

import com.sao.saomenu.api.SaoUi;
import com.sao.saomenu.api.theme.ThemeTokens;
import com.sao.saomenu.client.runtime.SAOClientPlatform;
import com.sao.saomenu.ui.theme.SaoTheme;
import net.minecraft.client.gui.Font;

/** Lazy stable font handles: widgets survive theme changes and resource-pack reloads. */
public final class SaoFonts {
    private static Font body;
    private static Font display;

    private SaoFonts() {
    }

    public static Font body() {
        if (body == null) {
            body = SAOClientPlatform.createThemedFont(() -> SaoUi.enabled()
                    ? SaoTheme.tokens().bodyFont() : ThemeTokens.DEFAULT_FONT);
        }
        return body;
    }

    public static Font display() {
        if (display == null) {
            display = SAOClientPlatform.createThemedFont(() -> SaoUi.enabled()
                    ? SaoTheme.tokens().displayFont() : ThemeTokens.DEFAULT_FONT);
        }
        return display;
    }
}
