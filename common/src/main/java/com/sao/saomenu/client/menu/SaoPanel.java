package com.sao.saomenu.client.menu;

import java.util.List;
import java.util.function.Supplier;

/**
 * 一个菜单面板:主按钮列上的一颗圆钮 + 它展开的一级项列 + 可选的左侧卡。
 *
 * <p>这就是"自由添加菜单"的单位。新增面板只需要
 * {@code SaoMenuRegistry.register(new SaoPanel(...))},不必改动菜单屏。</p>
 *
 * @param id       稳定标识(持久化与自检用;改动等于换一个面板)
 * @param icon     主按钮符号贴图名(对应 {@code symbol_<icon>_normal/hover.png})
 * @param items    一级项列数据源。菜单屏每帧取用,动态列请用
 *                 {@link MenuContext#cached} 包一层,否则每帧都会重建列表
 * @param sideCard 左侧卡;{@code null} 表示这个面板没有侧卡
 */
public record SaoPanel(
        String id,
        String icon,
        Supplier<List<MenuEntry>> items,
        SideCard sideCard
) {

    /** 无侧卡的面板。 */
    public static SaoPanel of(String id, String icon, Supplier<List<MenuEntry>> items) {
        return new SaoPanel(id, icon, items, null);
    }
}
