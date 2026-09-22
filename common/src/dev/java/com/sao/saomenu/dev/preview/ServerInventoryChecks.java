package com.sao.saomenu.dev.preview;

import com.sao.saomenu.SAOMenu;
import com.sao.saomenu.server.inventory.SAOItemActions;
import com.sao.saomenu.server.skill.SaoSkillCooldowns;
import com.sao.saomenu.skill.DualWieldSkill;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Destructive checks restricted to the preview's isolated integrated-server player. */
final class ServerInventoryChecks {
    private ServerInventoryChecks() {
    }

    static void run(ServerPlayer player) {
        var inventory = player.getInventory();
        inventory.clearContent();
        inventory.selected = 0;
        inventory.setItem(0, new ItemStack(Items.APPLE, 4));
        inventory.setItem(10, new ItemStack(Items.DIAMOND_SWORD));
        inventory.setItem(11, new ItemStack(Items.GOLDEN_SWORD));
        inventory.setItem(12, new ItemStack(Items.IRON_SWORD));
        inventory.setItem(13, new ItemStack(Items.STONE_SWORD));
        inventory.offhand.set(0, new ItemStack(Items.SHIELD));
        SaoSkillCooldowns.clearPlayer(player.getUUID());

        List<ItemStack> original = snapshot(player);
        SAOItemActions.handleDualWield(player, 10, 10);
        assertUnchanged(player, original, "same source slot");
        SAOItemActions.handleDualWield(player, 10, 0);
        assertUnchanged(player, original, "non-sword offhand source");
        SAOItemActions.handleDualWield(player, 10, 11);
        require(player.getMainHandItem().is(Items.DIAMOND_SWORD)
                && player.getOffhandItem().is(Items.GOLDEN_SWORD),
                "Invalid requests consumed cooldown or valid request failed");
        require(inventory.selected == 0, "Server changed the selected hotbar slot");
        for (ItemStack expected : original) {
            if (expected.isEmpty()) {
                continue;
            }
            int found = 0;
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                ItemStack actual = inventory.getItem(slot);
                if (ItemStack.isSameItemSameTags(expected, actual)) {
                    found += actual.getCount();
                }
            }
            require(found == expected.getCount(), "Dual wield lost or duplicated " + expected);
        }

        List<ItemStack> equipped = snapshot(player);
        SAOItemActions.handleDualWield(player, 12, 13);
        assertUnchanged(player, equipped, "active cooldown");
        SaoSkillCooldowns.clearPlayer(player.getUUID());
        SAOItemActions.handleDualWield(player, DualWieldSkill.KEEP_HAND, inventory.selected);
        assertUnchanged(player, equipped, "stealing the kept main hand");
        SAOItemActions.handleDualWield(player, DualWieldSkill.KEEP_HAND, 12);
        require(player.getMainHandItem().is(Items.DIAMOND_SWORD)
                && player.getOffhandItem().is(Items.IRON_SWORD), "KEEP_HAND did not preserve the main hand");
        SaoSkillCooldowns.clearPlayer(player.getUUID());
        inventory.setChanged();
        player.inventoryMenu.broadcastChanges();
        SAOMenu.LOGGER.info("[SAOMenu] preview server dual-wield passed: invalid atomicity, item conservation, cooldown, kept-hand alias");
    }

    private static List<ItemStack> snapshot(ServerPlayer player) {
        var inventory = player.getInventory();
        List<ItemStack> result = new ArrayList<>(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            result.add(inventory.getItem(slot).copy());
        }
        return result;
    }

    private static void assertUnchanged(ServerPlayer player, List<ItemStack> expected, String reason) {
        for (int slot = 0; slot < expected.size(); slot++) {
            require(ItemStack.matches(expected.get(slot), player.getInventory().getItem(slot)),
                    "Inventory changed after " + reason + " at slot " + slot);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
