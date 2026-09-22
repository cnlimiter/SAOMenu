package com.sao.saomenu.skill;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TieredItem;

/** Rules used by both the client preview and the authoritative server transaction. */
public final class DualWieldSkill {
    public static final SaoSkillDefinition DEFINITION = new SaoSkillDefinition("dual_wield", 60);
    /** Keep the sword already in the requested hand; never an inventory index. */
    public static final int KEEP_HAND = -2;

    private DualWieldSkill() {
    }

    public static boolean isSword(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof SwordItem) {
            return true;
        }
        if (!(stack.getItem() instanceof TieredItem)) {
            return false;
        }
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.contains("sword") || path.contains("blade") || path.contains("katana");
    }
}
