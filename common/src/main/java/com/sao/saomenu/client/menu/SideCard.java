package com.sao.saomenu.client.menu;

import com.sao.saomenu.client.MenuLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * 面板左侧那张卡。
 *
 * <p>可选:{@code SAO 设置}面板就没有侧卡。滑入进度与透明度由菜单屏算好传进来,
 * 卡片自己只负责画内容,不碰动画时序。</p>
 */
public interface SideCard {

    /**
     * @param rect  卡片矩形(菜单本地坐标,已含滑入位移)
     * @param eased 滑入进度 0..1(已缓动)
     * @param alpha 整体透明度
     */
    void render(GuiGraphics g, Minecraft mc, Font font, MenuHost host,
                MenuLayout.Rect rect, float eased, float alpha, int mouseX, int mouseY);
}
