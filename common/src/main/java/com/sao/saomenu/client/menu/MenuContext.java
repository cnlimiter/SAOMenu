package com.sao.saomenu.client.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

/**
 * 菜单处理器拿到的上下文。
 *
 * <p>面板提供方只依赖这个类就能写出完整交互,不需要知道菜单屏内部长什么样:
 * 打开别的界面走 {@link #openScreen},改菜单自身状态走 {@link #host}。</p>
 *
 * <p>动态列缓存由 {@link SaoPanels} 持有;失效走 {@link SaoPanels#resetSession()}。</p>
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

    /** 切换界面:统一走这里,保证 {@code lastScreen} 语义一致。 */
    public void openScreen(Screen next) {
        minecraft().setScreen(next);
    }
}
