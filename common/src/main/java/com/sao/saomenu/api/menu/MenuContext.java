package com.sao.saomenu.api.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;

import java.util.Objects;

/**
 * What an addon handler may touch when a row is activated or a side card is drawn.
 *
 * <p>Do not import {@code SAOMenuScreen} or {@code MenuSession}. Open other screens with
 * {@link #openScreen}; change the open menu with {@link #host()}.</p>
 *
 * <h2>Lifecycle</h2>
 * Built at click/activation time (or once per side-card frame). Do not retain a context
 * past return: the host, player and screen can be replaced on the next client tick.
 * Dynamic list caches live on the contributing panel, not here.
 */
public final class MenuContext {

    private final MenuHost host;
    private final MenuEntry entry;

    private MenuContext(MenuHost host, MenuEntry entry) {
        this.host = Objects.requireNonNull(host, "host");
        this.entry = entry;
    }

    /** Side-card / non-activation pass: {@link #entry()} is {@code null}. */
    public static MenuContext of(MenuHost host) {
        return new MenuContext(host, null);
    }

    /** Activation of {@code entry}. */
    public static MenuContext of(MenuHost host, MenuEntry entry) {
        return new MenuContext(host, entry);
    }

    /**
     * The row being activated. {@code null} on side-card render passes, which are not
     * activations.
     */
    public MenuEntry entry() {
        return entry;
    }

    /** {@code true} when this context was created for a row click. */
    public boolean isActivation() {
        return entry != null;
    }

    public Minecraft minecraft() {
        return Minecraft.getInstance();
    }

    public MenuHost host() {
        return host;
    }

    /** Current host screen; pass this as the parent when opening a child screen. */
    public Screen screen() {
        return host.screen();
    }

    /** Local player, or {@code null} if no client world. */
    public LocalPlayer player() {
        return minecraft().player;
    }

    /** Replace the current screen. Use this instead of calling {@code Minecraft.setScreen} directly. */
    public void openScreen(Screen next) {
        minecraft().setScreen(next);
    }
}
