package com.sao.saomenu.api.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * One row in a menu column.
 *
 * <p>{@link #id()} is namespaced and stable for the contributing mod. {@link #label()} is
 * already a {@link Component} (translated or literal). {@link #icon()} is a namespaced
 * texture, ignored when {@link #stack()} is a non-empty item.</p>
 *
 * <p>Dynamic children must be owned and cached by the supplier. The menu reads the list
 * every frame and must not rebuild it.</p>
 *
 * @param id         namespaced identity
 * @param label      display text
 * @param icon       row glyph; {@code null} when {@code stack} is drawn instead
 * @param submenu    child column; {@code null} if this row has no children
 * @param onActivate click handler; {@code null} if the row only expands children
 * @param equip      builtin third-column equipment kind; {@code null} for addon rows
 * @param stack      inventory item; {@code null} for non-item rows
 * @param invSlot    backpack slot for equip/drop packets; {@code -1} if not an item
 */
public record MenuEntry(
        ResourceLocation id,
        Component label,
        ResourceLocation icon,
        Supplier<List<MenuEntry>> submenu,
        Consumer<MenuContext> onActivate,
        EquipKind equip,
        ItemStack stack,
        int invSlot
) {

    public MenuEntry {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
    }

    /** Which vanilla equipment column to show beside this row. */
    public enum EquipKind {
        WEAPON, ARMOR, TRINKET
    }

    /** Decorative or empty placeholder row. */
    public static MenuEntry of(ResourceLocation id, Component label, ResourceLocation icon) {
        return new MenuEntry(id, label, icon, null, null, null, null, -1);
    }

    /** Clickable row with no children. */
    public static MenuEntry action(ResourceLocation id, Component label, ResourceLocation icon,
                                   Consumer<MenuContext> onActivate) {
        return new MenuEntry(id, label, icon, null, onActivate, null, null, -1);
    }

    /** Row that expands a child column. */
    public static MenuEntry submenu(ResourceLocation id, Component label, ResourceLocation icon,
                                    Supplier<List<MenuEntry>> submenu) {
        return new MenuEntry(id, label, icon, submenu, null, null, null, -1);
    }

    /** Row that opens the builtin equipment column. */
    public static MenuEntry equipColumn(ResourceLocation id, Component label, ResourceLocation icon, EquipKind kind) {
        return new MenuEntry(id, label, icon, null, null, kind, null, -1);
    }

    /** Inventory item row. Label comes from the stack hover name. */
    public static MenuEntry item(ResourceLocation id, ItemStack stack, int invSlot) {
        Objects.requireNonNull(stack, "stack");
        return new MenuEntry(id, stack.getHoverName(), null, null, null, null, stack, invSlot);
    }

    /**
     * Child column snapshot. {@code null} means no children (callers keep the old null check).
     * The supplier owns caching; this method does not copy.
     */
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
