package com.sao.saomenu.client.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 菜单处理器拿到的上下文。
 *
 * <p>面板提供方只依赖这个类就能写出完整交互,不需要知道菜单屏内部长什么样:
 * 打开别的界面走 {@link #openScreen},改菜单自身状态走 {@link #host}。</p>
 */
public final class MenuContext {

    private final MenuHost host;
    private final MenuEntry entry;

    private MenuContext(MenuHost host, MenuEntry entry) {
        this.host = host;
        this.entry = entry;
    }

    /** 由宿主在激活条目时构造。 */
    public static MenuContext of(MenuHost host, MenuEntry entry) {
        return new MenuContext(host, entry);
    }

    /** 当前被激活的条目(处理器里常要用它的 label / stack)。 */
    public MenuEntry entry() {
        return entry;
    }

    public Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    public MenuHost host() {
        return host;
    }

    /** 当前宿主界面:新建子界面时作为"返回目标"。 */
    public Screen screen() {
        return host.screen();
    }

    /** 本地玩家;主菜单等无世界场景返回 {@code null}。 */
    public LocalPlayer player() {
        return minecraft().player;
    }

    public Player playerOrNull() {
        return minecraft().player;
    }

    public ItemStack entryStack() {
        return entry == null || entry.stack() == null ? ItemStack.EMPTY : entry.stack();
    }

    /** 切换界面:统一走这里,保证 {@code lastScreen} 语义一致。 */
    public void openScreen(Screen next) {
        minecraft().setScreen(next);
    }

    /** 把一个二级列数据源包成带缓存的 supplier,避免每帧重建。 */
    public static java.util.function.Supplier<List<MenuEntry>> cached(
            java.util.function.Supplier<List<MenuEntry>> source, long ttlMs) {
        CachedSupplier c = new CachedSupplier(source, ttlMs);
        CACHES.add(c);
        return c;
    }

    /** 立即失效所有缓存:置顶/换序之后要马上按新顺序重排,不能等 TTL 到期。 */
    public static void invalidateCached() {
        for (CachedSupplier c : CACHES) {
            c.invalidate();
        }
    }

    private static final List<CachedSupplier> CACHES = new java.util.ArrayList<>();

    /** 按 TTL 缓存的 supplier:菜单屏每帧都会取子列,不缓存就等于每帧重建列表。 */
    private static final class CachedSupplier implements java.util.function.Supplier<List<MenuEntry>> {
        private final java.util.function.Supplier<List<MenuEntry>> source;
        private final long ttlMs;
        private List<MenuEntry> value;
        private long stamp;

        CachedSupplier(java.util.function.Supplier<List<MenuEntry>> source, long ttlMs) {
            this.source = source;
            this.ttlMs = ttlMs;
        }

        void invalidate() {
            value = null;
        }

        @Override
        public List<MenuEntry> get() {
            long now = net.minecraft.Util.getMillis();
            if (value == null || now - stamp > ttlMs) {
                value = source.get();
                stamp = now;
            }
            return value;
        }
    }
}
