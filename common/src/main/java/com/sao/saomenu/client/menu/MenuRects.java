package com.sao.saomenu.client.menu;

import com.sao.saomenu.api.layout.UiRect;

/** Boundary conversion between layout math and the public rect. */
final class MenuRects {

    private MenuRects() {
    }

    static UiRect ui(MenuLayout.Rect rect) {
        return new UiRect(rect.x(), rect.y(), rect.w(), rect.h());
    }

    static MenuLayout.Rect local(UiRect rect) {
        return new MenuLayout.Rect(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
