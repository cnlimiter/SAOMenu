package com.sao.saomenu.client.menu;

import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 一条菜单项。
 *
 * <p>取代了原先的 {@code MenuItem} record + {@code Action} enum 组合:
 * 行为从"枚举 + 大 switch"改成直接带一个处理器,所以<b>新增菜单项不再需要改动菜单屏</b>,
 * 也就不再有"枚举里有个分支没人用"这种死代码(例如原 {@code OPEN_STATS})。</p>
 *
 * @param label      语言键,或已经是最终文本的动态名(在线玩家名、物品名)
 * @param icon       贴图名({@code textures/gui/<icon>.png});{@code stack} 非空时忽略
 * @param submenu    二级列数据源;{@code null} 表示这一项没有子列。动态子列(在线玩家、
 *                   背包条目)由提供方自行缓存,菜单屏每帧只负责取用
 * @param onActivate 激活行为;{@code null} 表示只展开子列/仅作装饰
 * @param equip      非空表示点开这一项要在第三列展示已装备物品(武器/护甲/首饰)
 * @param stack      物品条目:渲染为 3D 物品
 * @param invSlot    物品条目:服务端装备/丢弃操作回传的背包槽位
 */
public record MenuEntry(
        String label,
        String icon,
        Supplier<List<MenuEntry>> submenu,
        Consumer<MenuContext> onActivate,
        EquipKind equip,
        ItemStack stack,
        int invSlot
) {

    /** 第三列展示哪一栏装备。 */
    public enum EquipKind {
        WEAPON, ARMOR, TRINKET
    }

    /** 无子列、无行为的普通项(仅用于占位提示)。 */
    public static MenuEntry of(String label, String icon) {
        return new MenuEntry(label, icon, null, null, null, null, -1);
    }

    /** 可直接点击执行的项。 */
    public static MenuEntry action(String label, String icon, Consumer<MenuContext> onActivate) {
        return new MenuEntry(label, icon, null, onActivate, null, null, -1);
    }

    /** 展开二级列的项。 */
    public static MenuEntry submenu(String label, String icon, Supplier<List<MenuEntry>> submenu) {
        return new MenuEntry(label, icon, submenu, null, null, null, -1);
    }

    /** 展开第三列"已装备物品"的项。 */
    public static MenuEntry equipColumn(String label, String icon, EquipKind kind) {
        return new MenuEntry(label, icon, null, null, kind, null, -1);
    }

    /** 背包物品条目。 */
    public static MenuEntry item(ItemStack stack, int invSlot) {
        return new MenuEntry(stack.getHoverName().getString(), "", null, null, null, stack, invSlot);
    }

    /** 二级列数据;没有子列时返回 {@code null}(调用点沿用旧的空判断)。 */
    public List<MenuEntry> children() {
        return submenu == null ? null : submenu.get();
    }

    public boolean hasChildren() {
        return submenu != null;
    }

    public boolean isItem() {
        return stack != null && !stack.isEmpty();
    }
}
