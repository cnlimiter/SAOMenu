package com.sao.saomenu.client.menu;

import com.sao.saomenu.SAOMenu;
import net.minecraft.resources.ResourceLocation;

/** 菜单贴图。集中声明,避免各绘制类各写一遍路径。 */
final class MenuAssets {

    static final ResourceLocation BTN_NORMAL = tex("btn_normal.png");
    static final ResourceLocation BTN_HOVER = tex("btn_hover.png");
    static final ResourceLocation LIST_NORMAL = tex("list_normal.png");
    static final ResourceLocation LIST_HOVER = tex("list_hover.png");
    static final ResourceLocation INDICATOR = tex("indicator.png");
    static final ResourceLocation PANEL = tex("panel.png");
    static final ResourceLocation ALERT = tex("alert.png");
    static final ResourceLocation BTN_OK = tex("btn_ok.png");
    static final ResourceLocation BTN_OK_HOVER = tex("btn_ok_hover.png");
    static final ResourceLocation BTN_CANCEL = tex("btn_cancel.png");
    static final ResourceLocation BTN_CANCEL_HOVER = tex("btn_cancel_hover.png");
    static final ResourceLocation BTN_PRESS = tex("btn_press.png");
    static final ResourceLocation LIST_PRESS = tex("list_press.png");
    static final ResourceLocation ACT_EQUIP = tex("item_run.png");
    static final ResourceLocation ACT_EQUIP_H = tex("item_run_hover.png");
    static final ResourceLocation ACT_INFO = tex("item_help.png");
    static final ResourceLocation ACT_INFO_H = tex("item_help_hover.png");
    static final ResourceLocation ACT_DROP = tex("item_remove.png");
    static final ResourceLocation ACT_DROP_H = tex("item_remove_hover.png");
    static final ResourceLocation ARROW_RIGHT = tex("arrow_right.png");
    static final ResourceLocation RING = tex("ring.png");
    static final ResourceLocation SILHOUETTE = tex("card_silhouette.png");

    private MenuAssets() {
    }

    static ResourceLocation tex(String name) {
        return new ResourceLocation(SAOMenu.MOD_ID, "textures/gui/" + name);
    }

    static ResourceLocation symbol(String icon, boolean hover) {
        return tex("symbol_" + icon + (hover ? "_hover" : "_normal") + ".png");
    }
}
